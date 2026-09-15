import base64
from datetime import UTC, datetime, timedelta
from types import SimpleNamespace
from unittest.mock import Mock

import pytest

from glowupai.play_billing import PlayBilling, account_id, decrypt_token
from glowupai.subscription_service import SubscriptionService
from glowupai.full_db import FullDatabase


@pytest.fixture
def configured(monkeypatch):
    monkeypatch.setenv("GLOWUPAI_PLAY_PACKAGE_NAME", "com.glowup.ai")
    monkeypatch.setenv("GLOWUPAI_PLAY_PRODUCT_IDS", "premium")
    monkeypatch.setenv("GLOWUPAI_BILLING_KEY", base64.b64encode(b"k" * 32).decode())


def purchase(state="SUBSCRIPTION_STATE_ACTIVE", user="owner", days=30):
    return {
        "subscriptionState": state,
        "acknowledgementState": "ACKNOWLEDGEMENT_STATE_PENDING",
        "externalAccountIdentifiers": {"obfuscatedExternalAccountId": account_id(user)},
        "lineItems": [
            {
                "productId": "premium",
                "expiryTime": (datetime.now(UTC) + timedelta(days=days)).isoformat(),
            }
        ],
    }


def provider(data):
    session = Mock()
    session.get.return_value = SimpleNamespace(status_code=200, json=lambda: data)
    session.post.return_value = SimpleNamespace(status_code=204)
    return PlayBilling(session=session), session


def test_active_purchase_is_verified_and_acknowledged(configured):
    play, session = provider(purchase())
    assert play.verify("owner", "token")["active"]
    assert session.post.call_count == 1


def test_pending_purchase_without_expiry_is_not_acknowledged(configured):
    data = purchase("SUBSCRIPTION_STATE_PENDING")
    del data["lineItems"][0]["expiryTime"]
    play, session = provider(data)
    result = play.verify("owner", "token")
    assert result["active"] is False
    session.post.assert_not_called()


@pytest.mark.parametrize(
    "state,days,active",
    [
        ("SUBSCRIPTION_STATE_PENDING", 30, False),
        ("SUBSCRIPTION_STATE_ON_HOLD", 30, False),
        ("SUBSCRIPTION_STATE_EXPIRED", -1, False),
        ("SUBSCRIPTION_STATE_CANCELED", 30, True),
        ("SUBSCRIPTION_STATE_IN_GRACE_PERIOD", 1, True),
        ("SUBSCRIPTION_STATE_ACTIVE", -1, False),
    ],
)
def test_subscription_states(configured, state, days, active):
    play, _ = provider(purchase(state, days=days))
    assert play.verify("owner", "token")["active"] is active


def test_purchase_cannot_be_claimed_by_another_account(configured):
    play, session = provider(purchase(user="other"))
    with pytest.raises(PermissionError):
        play.verify("owner", "token")
    session.post.assert_not_called()


def test_wrong_product_and_failed_acknowledgement_do_not_grant_access(configured):
    data = purchase()
    data["lineItems"][0]["productId"] = "unapproved"
    play, _ = provider(data)
    with pytest.raises(ValueError):
        play.verify("owner", "token")
    play, session = provider(purchase())
    session.post.return_value.status_code = 503
    with pytest.raises(ValueError, match="acknowledgement failed"):
        play.verify("owner", "token")


def test_restore_is_idempotent_and_tokens_are_encrypted(configured, tmp_path):
    db = FullDatabase(tmp_path / "billing.sqlite3")
    db.execute("INSERT INTO users (id) VALUES ('owner')")
    parent = SimpleNamespace(
        require_user=lambda user: None, settings=SimpleNamespace(is_production=True)
    )
    service = SubscriptionService(db, parent)
    service.play, _ = provider(purchase())
    try:
        for _ in range(2):
            assert (
                service.verify_play_purchase("owner", "private-token")["plan"]
                == "premium"
            )
        rows = db.fetchall("SELECT * FROM play_purchases")
        assert len(rows) == 1
        assert "private-token" not in rows[0]["token_ciphertext"]
        assert decrypt_token("owner", rows[0]["token_ciphertext"]) == "private-token"
        service.play, _ = provider(purchase("SUBSCRIPTION_STATE_EXPIRED", days=-1))
        assert service.reconcile_play_purchases() == 1
        assert not service.is_premium("owner")
        with pytest.raises(PermissionError):
            service.upgrade("owner")
    finally:
        db.close()
