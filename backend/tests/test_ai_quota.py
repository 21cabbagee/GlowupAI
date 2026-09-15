import pytest

from glowupai.ai_quota import AIQuotaManager, QuotaExceeded, QuotaLimits, utc_day_window
from glowupai.full_db import FullDatabase


@pytest.fixture
def quota_db(tmp_path):
    db = FullDatabase(tmp_path / "quota.sqlite3")
    db.execute("INSERT INTO users (id) VALUES ('owner')")
    yield db
    db.close()


def _reserve(manager, identity, *, model="gpt-5.6-luna", limits=None):
    return manager.reserve(
        provider="openai",
        project_alias="test",
        model_id=model,
        user_id="owner",
        task="vision",
        request_identity=identity,
        quota_window=utc_day_window(),
        estimated_tokens=100,
        estimated_cost_usd=0.01,
        limits=limits,
    )


def test_reserve_is_idempotent_for_replayed_attempt(quota_db):
    manager = AIQuotaManager(quota_db)
    first = _reserve(manager, "capture-1")
    replay = _reserve(manager, "capture-1")

    assert replay.id == first.id
    usage = manager.bucket_usage("openai", "test", "gpt-5.6-luna", utc_day_window())
    assert usage["reserved_requests"] == 1
    assert usage["reserved_tokens"] == 100


def test_user_quota_spans_models_and_reconciled_usage(quota_db):
    manager = AIQuotaManager(quota_db)
    limits = QuotaLimits(user_max_requests=1, user_max_tokens=200)
    first = _reserve(manager, "capture-1", limits=limits)
    manager.reconcile(first.id, actual_input_tokens=20, actual_output_tokens=30)

    with pytest.raises(QuotaExceeded, match="user request limit"):
        _reserve(manager, "capture-2", model="gemini-3.5-flash-lite", limits=limits)


def test_pre_call_failure_releases_budget_but_provider_ambiguity_does_not(quota_db):
    manager = AIQuotaManager(quota_db)
    first = _reserve(manager, "capture-1")
    assert manager.fail_without_call(first.id, error_code="policy_denied")
    assert (
        manager.bucket_usage("openai", "test", "gpt-5.6-luna", utc_day_window())[
            "reserved_requests"
        ]
        == 0
    )

    second = _reserve(manager, "capture-2")
    assert manager.mark_ambiguous(second.id)
    usage = manager.bucket_usage("openai", "test", "gpt-5.6-luna", utc_day_window())
    assert usage["reserved_requests"] == 1
    assert manager.fail_without_call(second.id, error_code="late_failure") is False
