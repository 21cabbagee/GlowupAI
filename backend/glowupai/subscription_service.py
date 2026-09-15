from __future__ import annotations

import json
import logging
import uuid
from collections.abc import Callable
from datetime import UTC, datetime, timedelta
from typing import Any, cast

from .service import now_iso, row_dict
from .play_billing import (
    PlayBilling,
    PlayPurchaseNotFound,
    account_id,
    decrypt_token,
    encrypt_token,
)

PREMIUM_FEATURES = {
    "experiments",
    "ingredient_analysis",
    "long_history",
    "qna",
    "discover",
    "root_cause",
    "budget_optimizer",
    "derm_export",
    "product_prediction",
}

logger = logging.getLogger(__name__)


def uid() -> str:
    return str(uuid.uuid4())


def dump(value: Any) -> str:
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


class SubscriptionService:
    """Subscription management, billing, and entitlements."""

    def __init__(self, db: Any, parent_service: Any) -> None:
        self.db = db
        self.parent = parent_service
        self.play = PlayBilling()

    def entitlement(self, user_id: str) -> dict[str, Any]:
        self.parent.require_user(user_id)
        row = self.db.fetchone(
            "SELECT * FROM entitlements WHERE user_id = ?", (user_id,)
        )
        if not row:
            self.db.execute("INSERT INTO entitlements (user_id) VALUES (?)", (user_id,))
            row = self.db.fetchone(
                "SELECT * FROM entitlements WHERE user_id = ?", (user_id,)
            )
        entitlement = row_dict(row)
        if entitlement["source"] == "google_play" and entitlement.get("renews_at"):
            if datetime.fromisoformat(
                entitlement["renews_at"].replace("Z", "+00:00")
            ) <= datetime.now(UTC):
                self.db.execute(
                    "UPDATE entitlements SET plan='free', status='cancelled' WHERE user_id=?",
                    (user_id,),
                )
                entitlement.update(plan="free", status="cancelled")
        return entitlement

    def verify_play_purchase(
        self,
        user_id: str,
        purchase_token: str,
        *,
        purchase: dict | None = None,
    ) -> dict[str, Any]:
        self.parent.require_user(user_id)
        checked_at = datetime.now(UTC).isoformat()
        verified = self.play.verify(user_id, purchase_token, purchase=purchase)
        ciphertext = encrypt_token(user_id, purchase_token)
        self.db.execute(
            "INSERT INTO play_purchases (token_hash,user_id,token_ciphertext,active,expires_at,verified_at) "
            "VALUES (?,?,?,?,?,?) ON CONFLICT (token_hash) DO UPDATE SET "
            "active=excluded.active, expires_at=excluded.expires_at, verified_at=excluded.verified_at "
            "WHERE play_purchases.user_id=excluded.user_id AND play_purchases.verified_at < excluded.verified_at",
            (
                account_id(purchase_token),
                user_id,
                ciphertext,
                int(verified["active"]),
                verified["expires_at"],
                checked_at,
            ),
        )
        stored = self.db.fetchone(
            "SELECT user_id FROM play_purchases WHERE token_hash=?",
            (account_id(purchase_token),),
        )
        if stored["user_id"] != user_id:
            raise PermissionError("This purchase is already linked to another account")

        mapped = self.db.fetchone(
            "SELECT user_id FROM play_accounts WHERE account_hash=?",
            (account_id(user_id),),
        )
        if mapped and mapped["user_id"] != user_id:
            raise PermissionError(
                "This Google Play account is already linked elsewhere"
            )
        self.db.execute(
            "INSERT INTO play_accounts (account_hash,user_id) VALUES (?,?) ON CONFLICT (account_hash) DO NOTHING",
            (account_id(user_id), user_id),
        )
        self.entitlement(user_id)
        self._refresh_google_entitlement(user_id, verified["expires_at"])
        return self.entitlement(user_id)

    def refresh_play_purchase(self, purchase_token: str) -> bool:
        """Refresh a token received through RTDN and return whether it was linked."""
        token_hash = account_id(purchase_token)
        row = self.db.fetchone(
            "SELECT user_id FROM play_purchases WHERE token_hash=?", (token_hash,)
        )
        purchase = None
        try:
            purchase = self.play.get_subscription(purchase_token)
        except PlayPurchaseNotFound:
            if row:
                self._mark_play_purchase_inactive(purchase_token, row["user_id"])
                return True
            return False

        user_id = row["user_id"] if row else None
        if user_id is None:
            bound = self.play.obfuscated_account_id(purchase)
            if bound:
                mapped = self.db.fetchone(
                    "SELECT user_id FROM play_accounts WHERE account_hash=?", (bound,)
                )
                user_id = mapped["user_id"] if mapped else None
        if user_id is None and purchase.get("linkedPurchaseToken"):
            previous = self.db.fetchone(
                "SELECT user_id FROM play_purchases WHERE token_hash=?",
                (account_id(purchase["linkedPurchaseToken"]),),
            )
            user_id = previous["user_id"] if previous else None
        if user_id is None:
            # Purchases made without our obfuscated account ID cannot be safely assigned.
            # A Play token is not sufficient proof of which GlowUp account owns it.
            return False

        self.verify_play_purchase(user_id, purchase_token, purchase=purchase)
        return True

    def _mark_play_purchase_inactive(self, purchase_token: str, user_id: str) -> None:
        now = now_iso()
        self.db.execute(
            "UPDATE play_purchases SET active=0, expires_at=?, verified_at=? WHERE token_hash=? AND user_id=?",
            (now, now, account_id(purchase_token), user_id),
        )
        self._refresh_google_entitlement(user_id, now)

    def _refresh_google_entitlement(self, user_id: str, fallback_expiry: str) -> None:
        active = self.db.fetchone(
            "SELECT MAX(expires_at) AS expiry FROM play_purchases WHERE user_id=? AND active=1 AND expires_at > ?",
            (user_id, now_iso()),
        )["expiry"]
        self.db.execute(
            "UPDATE entitlements SET plan=?, status=?, renews_at=?, source='google_play' WHERE user_id=?",
            (
                "premium" if active else "free",
                "active" if active else "cancelled",
                active or fallback_expiry,
                user_id,
            ),
        )

    def reconcile_play_purchases(self, limit=100):
        rows = self.db.fetchall(
            "SELECT * FROM play_purchases ORDER BY verified_at LIMIT ?", (limit,)
        )
        completed = 0
        for row in rows:
            try:
                self.verify_play_purchase(
                    row["user_id"],
                    decrypt_token(row["user_id"], row["token_ciphertext"]),
                )
                completed += 1
            except PlayPurchaseNotFound:
                self._mark_play_purchase_inactive(
                    decrypt_token(row["user_id"], row["token_ciphertext"]),
                    row["user_id"],
                )
                completed += 1
            except Exception as exc:
                # Keep previous expiry on provider outages; never extend it.
                logger.warning(
                    "Play purchase reconciliation deferred",
                    extra={"error_type": type(exc).__name__},
                )
        return completed

    def is_premium(self, user_id: str) -> bool:
        entitlement = self.entitlement(user_id)
        return bool(
            entitlement["plan"] == "premium" and entitlement["status"] == "active"
        )

    def require_premium(self, user_id: str, feature: str) -> dict[str, Any]:
        entitlement = self.entitlement(user_id)
        if entitlement["plan"] != "premium" or entitlement["status"] != "active":
            raise PermissionError(
                f"{feature} requires Premium; upgrade the plan to unlock it"
            )
        return entitlement

    def _usage(self, user_id: str, feature: str) -> int:
        row = self.db.fetchone(
            "SELECT used_count FROM entitlement_usage WHERE user_id=? AND feature=?",
            (user_id, feature),
        )
        return int(row["used_count"]) if row else 0

    def _increment_usage(self, user_id: str, feature: str) -> None:
        self.db.execute(
            """INSERT INTO entitlement_usage (user_id, feature, used_count) VALUES (?, ?, 1)
               ON CONFLICT (user_id, feature) DO UPDATE SET used_count = entitlement_usage.used_count + 1""",
            (user_id, feature),
        )

    def upgrade(
        self,
        user_id: str,
        source: str = "local_checkout",
        audit_fn: Callable | None = None,
    ) -> dict[str, Any]:
        self.parent.require_user(user_id)
        if self.parent.settings.is_production:
            raise PermissionError(
                "Use a verified Google Play purchase to activate Premium"
            )
        renews = (
            (datetime.now(UTC) + timedelta(days=30)).isoformat().replace("+00:00", "Z")
        )
        self.db.execute(
            "UPDATE entitlements SET plan='premium', status='active', started_at=?, renews_at=?, source=? WHERE user_id=?",
            (now_iso(), renews, source, user_id),
        )
        self.db.execute(
            "INSERT INTO billing_events (id,user_id,event_type,provider,payload_json) VALUES (?,?,?,?,?)",
            (
                uid(),
                user_id,
                "subscription_activated",
                source,
                dump({"plan": "premium"}),
            ),
        )
        if audit_fn:
            audit_fn("premium_activated", "user", user_id, user_id)
        return self.entitlement(user_id)

    def downgrade(
        self, user_id: str, audit_fn: Callable | None = None
    ) -> dict[str, Any]:
        self.parent.require_user(user_id)
        if self.entitlement(user_id)["source"] == "google_play":
            raise PermissionError("Manage cancellation in Google Play subscriptions")
        self.db.execute(
            "UPDATE entitlements SET plan='free', status='cancelled' WHERE user_id=?",
            (user_id,),
        )
        self.db.execute(
            "INSERT INTO billing_events (id,user_id,event_type,payload_json) VALUES (?,?,?,?)",
            (uid(), user_id, "subscription_cancelled", dump({})),
        )
        if audit_fn:
            audit_fn("premium_cancelled", "user", user_id, user_id)
        return self.entitlement(user_id)

    def _free_unlocked_product_id(self, user_id: str) -> str | None:
        row = self.db.fetchone(
            "SELECT reference_id FROM engagement_events WHERE user_id=? AND event_type='free_verdict_unlocked' ORDER BY occurred_at LIMIT 1",
            (user_id,),
        )
        return cast(str | None, row["reference_id"]) if row else None

    def list_subscriptions(self, limit: int = 100) -> list[dict[str, Any]]:
        """List all subscriptions (admin endpoint)."""
        rows = self.db.fetchall(
            "SELECT * FROM entitlements ORDER BY started_at DESC LIMIT ?", (limit,)
        )
        return [row_dict(row) for row in rows]

    def create_subscription(
        self, user_id: str, plan: str = "premium", source: str = "api"
    ) -> dict[str, Any]:
        """Create or upgrade a subscription."""
        self.parent.require_user(user_id)
        if plan == "premium":
            return self.upgrade(user_id, source)
        else:
            # For free plan, just return the entitlement
            return self.entitlement(user_id)

    def verdicts_for_user(
        self,
        user_id: str,
        refresh_verdicts_fn: Callable,
        record_engagement_fn: Callable,
    ) -> list[dict[str, Any]]:
        verdicts: list[dict[str, Any]] = refresh_verdicts_fn(user_id)
        if self.is_premium(user_id):
            return verdicts
        unlocked = self._free_unlocked_product_id(user_id)
        if not unlocked:
            definitive = next(
                (item for item in verdicts if item["label"] != "evidence_unclear"), None
            )
            if definitive:
                unlocked = definitive["product_id"]
                record_engagement_fn(user_id, "free_verdict_unlocked", unlocked)
                self._increment_usage(user_id, "product_verdicts")
        output = []
        for item in verdicts:
            # evidence_unclear withholds nothing worth paywalling: it is
            # already just "not enough evidence yet," so show it free and
            # save the lock for a definitive label beyond the one unlocked.
            if item["product_id"] == unlocked or item["label"] == "evidence_unclear":
                output.append(item)
            else:
                output.append(
                    {
                        "product_id": item["product_id"],
                        "product_name": item["product_name"],
                        "label": "locked",
                        "generated_text": "Upgrade to Premium to see this verdict and get unlimited product verdicts.",
                        "evidence": {},
                    }
                )
        return output
