from __future__ import annotations

import json
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any, Callable, Dict, List, Optional

from .service import now_iso, row_dict

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


def uid() -> str:
    return str(uuid.uuid4())


def dump(value: Any) -> str:
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


class SubscriptionService:
    """Subscription management, billing, and entitlements."""

    def __init__(self, db: Any, parent_service: Any) -> None:
        self.db = db
        self.parent = parent_service

    def entitlement(self, user_id: str) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        row = self.db.fetchone(
            "SELECT * FROM entitlements WHERE user_id = ?", (user_id,)
        )
        if not row:
            self.db.execute("INSERT INTO entitlements (user_id) VALUES (?)", (user_id,))
            row = self.db.fetchone(
                "SELECT * FROM entitlements WHERE user_id = ?", (user_id,)
            )
        return row_dict(row)

    def is_premium(self, user_id: str) -> bool:
        entitlement = self.entitlement(user_id)
        return entitlement["plan"] == "premium" and entitlement["status"] == "active"

    def require_premium(self, user_id: str, feature: str) -> Dict[str, Any]:
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

    def upgrade(self, user_id: str, source: str = "local_checkout", audit_fn: Optional[Callable] = None) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        renews = (
            (datetime.now(timezone.utc) + timedelta(days=30))
            .isoformat()
            .replace("+00:00", "Z")
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

    def downgrade(self, user_id: str, audit_fn: Optional[Callable] = None) -> Dict[str, Any]:
        self.parent.require_user(user_id)
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

    def _free_unlocked_product_id(self, user_id: str) -> Optional[str]:
        row = self.db.fetchone(
            "SELECT reference_id FROM engagement_events WHERE user_id=? AND event_type='free_verdict_unlocked' ORDER BY occurred_at LIMIT 1",
            (user_id,),
        )
        return row["reference_id"] if row else None

    def list_subscriptions(self, limit: int = 100) -> List[Dict[str, Any]]:
        """List all subscriptions (admin endpoint)."""
        rows = self.db.fetchall(
            "SELECT * FROM entitlements ORDER BY started_at DESC LIMIT ?", (limit,)
        )
        return [row_dict(row) for row in rows]

    def create_subscription(self, user_id: str, plan: str = "premium", source: str = "api") -> Dict[str, Any]:
        """Create or upgrade a subscription."""
        self.parent.require_user(user_id)
        if plan == "premium":
            return self.upgrade(user_id, source)
        else:
            # For free plan, just return the entitlement
            return self.entitlement(user_id)

    def verdicts_for_user(self, user_id: str, refresh_verdicts_fn: Callable, record_engagement_fn: Callable) -> List[Dict[str, Any]]:
        verdicts = refresh_verdicts_fn(user_id)
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
