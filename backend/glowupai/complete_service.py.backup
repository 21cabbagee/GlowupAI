from __future__ import annotations

import json
import re
import statistics
import uuid
from datetime import UTC, datetime, timedelta, timezone

from .attribution import parse_time
from .catalog import explain, parse_ingredients
from .complete_db import FullDatabase
from .google_ai import build_insight_service, build_vision_service
from .insights import GroundedInsightService
from .jobs import JobRunner
from .metrics import MetricResult, analyze
from .safety import triage
from .service import GlowupAIService, now_iso, row_dict

VERTICALS = ("skin",)

# Free tier gets one lifetime product verdict in full (see _unlock_free_verdict)
# plus capture, coaching, confound warnings, shelf-scan, and commerce/offers.
# Everything below stays Premium-gated. "product_verdicts" is handled by the
# one-free-verdict unlock rather than a flat boolean, so it is intentionally
# absent from this set.
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
FREE_HISTORY_DAYS = 90
CONTEXT_EVENT_TYPES = {
    "sleep",
    "travel",
    "weather",
    "cycle",
    "stress",
    "diet",
    "custom",
}
CHECK_IN_ROUTINE_STATES = {"steady", "changed", "missed", "not_sure"}
CHECK_IN_SKIN_FEELS = {"better", "same", "worse", "not_sure"}
METRIC_LABELS = {
    "redness_score": "redness",
    "blemish_count": "blemish signs",
    "darkspot_area": "dark-spot area",
    "texture_score": "texture",
}
CAPTURE_PROTOCOL_VERSION = "standardized-v1"


def uid() -> str:
    return str(uuid.uuid4())


def dump(value) -> str:
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def load(value, default=None):
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default if default is not None else {}


def as_date(value: str) -> datetime:
    parsed = parse_time(value)
    return parsed.astimezone(UTC)


def day(value: datetime) -> str:
    return value.astimezone(UTC).date().isoformat()


class CompleteGlowupAIService(GlowupAIService):
    """Complete local product implementation for every user-facing phase.

    Cloud adapters, mobile SDKs, and payment credentials are injected at the
    deployment boundary; the product behavior itself is implemented here.
    """

    def __init__(self, db: FullDatabase, settings=None, photos=None):
        super().__init__(
            db, settings=settings, photos=photos, insights=GroundedInsightService(),
        )
        self.full_db = db
        self.insights = build_insight_service(self.settings)
        self.vision = build_vision_service(self.settings)
        self.jobs = JobRunner(db)

    def _audit(
        self,
        action: str,
        subject_type: str | None = None,
        subject_id: str | None = None,
        actor_id: str | None = None,
        metadata=None,
    ) -> None:
        self.db.execute(
            "INSERT INTO audit_log (id, actor_type, actor_id, action, subject_type, subject_id, metadata_json) VALUES (?, 'user', ?, ?, ?, ?, ?)",
            (uid(), actor_id, action, subject_type, subject_id, dump(metadata or {})),
        )

    def create_user(self, skin_type: str | None = None) -> dict:
        user = super().create_user(skin_type)
        self.db.execute(
            "INSERT INTO entitlements (user_id, plan) VALUES (?, 'free')", (user["id"],),
        )
        for vertical in VERTICALS:
            self.db.execute(
                "INSERT INTO appearance_profiles (id, user_id, vertical) VALUES (?, ?, ?)",
                (uid(), user["id"], vertical),
            )
        self._audit("user_created", "user", user["id"], user["id"])
        return self.profile(user["id"])

    def session_for_identity(
        self,
        firebase_uid: str,
        email: str | None = None,
        email_verified: bool = False,
        name: str | None = None,
    ) -> dict:
        """Exchange a verified Firebase uid for a SkinProof profile.

        Idempotent per `firebase_uid`: the first call creates the user,
        appearance profile, and free entitlement exactly like `create_user`
        and binds the uid; every later call for the same uid returns the same
        user's profile, never a duplicate.
        """

        existing = self.db.fetchone(
            "SELECT id FROM users WHERE firebase_uid = ?", (firebase_uid,),
        )
        if existing:
            return self.profile(existing["id"])
        created = self.create_user(None)
        user_id = created["user"]["id"]
        try:
            self.db.execute(
                "UPDATE users SET firebase_uid = ? WHERE id = ?",
                (firebase_uid, user_id),
            )
        except Exception:
            # Lost a create-race to a concurrent request for the same uid:
            # bind to whichever row won, and drop the extra one we made
            # rather than leaving an orphaned duplicate user behind.
            winner = self.db.fetchone(
                "SELECT id FROM users WHERE firebase_uid = ?", (firebase_uid,),
            )
            if not winner:
                raise
            self.delete_user(user_id)
            return self.profile(winner["id"])
        if name:
            profile_row = self.db.fetchone(
                "SELECT display_name FROM experience_profiles WHERE user_id=?",
                (user_id,),
            )
            if profile_row and not profile_row["display_name"]:
                self.db.execute(
                    "UPDATE experience_profiles SET display_name=? WHERE user_id=?",
                    (name.strip()[:80], user_id),
                )
        self._audit(
            "firebase_identity_bound",
            "user",
            user_id,
            user_id,
            {"email_verified": bool(email_verified)},
        )
        return self.profile(user_id)

    def grant_consent(
        self, user_id: str, facial_data: bool, policy_version: str | None = None,
    ) -> dict:
        result = super().grant_consent(user_id, facial_data, policy_version)
        self._audit(
            "consent_granted" if facial_data else "consent_declined",
            "user",
            user_id,
            user_id,
            {"facial_data": facial_data},
        )
        return self.profile(user_id)

    def profile(self, user_id: str) -> dict:
        user = self.require_user(user_id)
        profiles = [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM appearance_profiles WHERE user_id = ? ORDER BY vertical",
                (user_id,),
            )
        ]
        entitlement = row_dict(
            self.db.fetchone("SELECT * FROM entitlements WHERE user_id = ?", (user_id,)),
        )
        if not entitlement:
            self.db.execute("INSERT INTO entitlements (user_id) VALUES (?)", (user_id,))
            entitlement = row_dict(
                self.db.fetchone(
                    "SELECT * FROM entitlements WHERE user_id = ?", (user_id,),
                ),
            )
        row = self.db.fetchone(
            "SELECT * FROM experience_profiles WHERE user_id=?", (user_id,),
        )
        if not row:
            self.db.execute(
                "INSERT INTO experience_profiles (user_id) VALUES (?)", (user_id,),
            )
            row = self.db.fetchone(
                "SELECT * FROM experience_profiles WHERE user_id=?", (user_id,),
            )
        experience = row_dict(row)
        experience["goals"] = load(experience.pop("goals_json"), [])
        return {
            "user": user,
            "appearance_profiles": profiles,
            "entitlement": entitlement,
            "verticals": list(VERTICALS),
            "experience_profile": experience,
        }

    def update_profile(
        self,
        user_id: str,
        display_name: str | None = None,
        skin_type: str | None = None,
        focus_vertical: str | None = None,
        goals: list[str] | None = None,
        experience_level: str | None = None,
        onboarding_complete: bool | None = None,
    ) -> dict:
        self.require_user(user_id)
        if focus_vertical is not None and focus_vertical not in VERTICALS:
            raise ValueError("invalid focus vertical")
        if display_name is not None and not display_name.strip():
            raise ValueError("display name cannot be empty")
        if display_name is not None and len(display_name.strip()) > 80:
            raise ValueError("display name is too long")
        if goals is not None and (
            len(goals) > 8 or any(len(goal) > 80 for goal in goals)
        ):
            raise ValueError("choose up to 8 concise goals")
        self.db.execute(
            "INSERT INTO experience_profiles (user_id) VALUES (?) ON CONFLICT DO NOTHING",
            (user_id,),
        )
        current = self.db.fetchone(
            "SELECT * FROM experience_profiles WHERE user_id=?", (user_id,),
        )
        completed_at = current["onboarding_completed_at"]
        if onboarding_complete is True and not completed_at:
            completed_at = now_iso()
        elif onboarding_complete is False:
            completed_at = None
        self.db.execute(
            """UPDATE experience_profiles SET display_name=?,focus_vertical=?,goals_json=?,
               experience_level=?,onboarding_completed_at=?,updated_at=datetime('now') WHERE user_id=?""",
            (
                (
                    display_name.strip()
                    if display_name is not None
                    else current["display_name"]
                ),
                (
                    focus_vertical
                    if focus_vertical is not None
                    else current["focus_vertical"]
                ),
                dump(goals) if goals is not None else current["goals_json"],
                (
                    experience_level
                    if experience_level is not None
                    else current["experience_level"]
                ),
                completed_at,
                user_id,
            ),
        )
        if skin_type is not None:
            self.db.execute(
                "UPDATE users SET skin_type=? WHERE id=?", (skin_type, user_id),
            )
        self._audit(
            "profile_updated",
            "user",
            user_id,
            user_id,
            {"onboarding_complete": onboarding_complete},
        )
        return self.profile(user_id)

    def entitlement(self, user_id: str) -> dict:
        self.require_user(user_id)
        row = self.db.fetchone(
            "SELECT * FROM entitlements WHERE user_id = ?", (user_id,),
        )
        if not row:
            self.db.execute("INSERT INTO entitlements (user_id) VALUES (?)", (user_id,))
            row = self.db.fetchone(
                "SELECT * FROM entitlements WHERE user_id = ?", (user_id,),
            )
        return row_dict(row)

    def is_premium(self, user_id: str) -> bool:
        entitlement = self.entitlement(user_id)
        return entitlement["plan"] == "premium" and entitlement["status"] == "active"

    def require_premium(self, user_id: str, feature: str) -> dict:
        entitlement = self.entitlement(user_id)
        if entitlement["plan"] != "premium" or entitlement["status"] != "active":
            raise PermissionError(
                f"{feature} requires Premium; upgrade the plan to unlock it",
            )
        return entitlement

    # -- entitlement usage counters (quota-based features) -----------------

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

    def upgrade(self, user_id: str, source: str = "local_checkout") -> dict:
        self.require_user(user_id)
        renews = (
            (datetime.now(UTC) + timedelta(days=30))
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
        self._audit("premium_activated", "user", user_id, user_id)
        return self.entitlement(user_id)

    def downgrade(self, user_id: str) -> dict:
        self.require_user(user_id)
        self.db.execute(
            "UPDATE entitlements SET plan='free', status='cancelled' WHERE user_id=?",
            (user_id,),
        )
        self.db.execute(
            "INSERT INTO billing_events (id,user_id,event_type,payload_json) VALUES (?,?,?,?)",
            (uid(), user_id, "subscription_cancelled", dump({})),
        )
        self._audit("premium_cancelled", "user", user_id, user_id)
        return self.entitlement(user_id)

    # -- routine events + confound warning (free) ---------------------------

    def _active_product_windows(
        self, user_id: str, exclude_product_id: str | None = None,
    ) -> list[dict]:
        """Products currently inside their post-start/change stabilization window."""

        rows = self.db.fetchall(
            """SELECT e.product_id, e.action, e.timestamp, p.name, p.stabilization_days
               FROM routine_events e JOIN products p ON p.id = e.product_id
               WHERE e.user_id = ? ORDER BY e.timestamp""",
            (user_id,),
        )
        by_product: dict[str, list[dict]] = {}
        for row in rows:
            if exclude_product_id and row["product_id"] == exclude_product_id:
                continue
            by_product.setdefault(row["product_id"], []).append(row_dict(row))
        now = datetime.now(UTC)
        active = []
        for product_id, events in by_product.items():
            last_start = next(
                (e for e in reversed(events) if e["action"] in ("start", "change")),
                None,
            )
            if not last_start:
                continue
            start = parse_time(last_start["timestamp"])
            stable_at = start + timedelta(days=int(last_start["stabilization_days"]))
            stopped = any(
                e["action"] == "stop" and parse_time(e["timestamp"]) > start
                for e in events
            )
            if not stopped and now <= stable_at:
                active.append(
                    {
                        "product_id": product_id,
                        "product_name": last_start["name"],
                        "started_at": last_start["timestamp"],
                        "stable_at": (
                            start
                            + timedelta(days=int(last_start["stabilization_days"]))
                        )
                        .isoformat()
                        .replace("+00:00", "Z"),
                    },
                )
        return active

    def confound_check(
        self, user_id: str, exclude_product_id: str | None = None,
    ) -> dict:
        """Warn before a change that will make an in-progress window evidence_unclear."""

        active = self._active_product_windows(user_id, exclude_product_id)
        if not active:
            return {"confounded": False, "active_windows": []}
        names = ", ".join(item["product_name"] for item in active)
        return {
            "confounded": True,
            "active_windows": active,
            "message": f"Starting or changing a product now will overlap the stabilization window for {names}. Any evidence for {'it' if len(active) == 1 else 'them'} will come back evidence_unclear. Wait until the window closes, or accept the confound.",
        }

    def add_routine_event(
        self,
        user_id: str,
        product_id: str,
        action: str,
        timestamp: str | None = None,
        slot: str = "unspecified",
        dose: str | None = None,
        frequency: str | None = None,
        notes: str | None = None,
        experiment_id: str | None = None,
    ) -> dict:
        confound_warning = (
            self.confound_check(user_id, exclude_product_id=product_id)
            if action in ("start", "change")
            else {"confounded": False, "active_windows": []}
        )
        event = super().add_routine_event(
            user_id, product_id, action, timestamp, slot, dose, frequency, notes,
        )
        if experiment_id:
            experiment = self.db.fetchone(
                "SELECT * FROM experiments WHERE id=? AND user_id=?",
                (experiment_id, user_id),
            )
            if not experiment:
                raise ValueError("experiment not found")
            self.db.execute(
                "INSERT INTO experiment_events (experiment_id, routine_event_id) VALUES (?,?) ON CONFLICT DO NOTHING",
                (experiment_id, event["id"]),
            )
        self._audit(
            "routine_event_logged",
            "routine_event",
            event["id"],
            user_id,
            {"product_id": product_id, "action": action},
        )
        event["confound_warning"] = (
            confound_warning if confound_warning["confounded"] else None
        )
        return event

    # -- experiments (Premium) ----------------------------------------------

    def create_experiment(
        self,
        user_id: str,
        name: str,
        hypothesis: str | None,
        product_id: str,
        primary_metric: str = "redness_score",
        target_days: int = 14,
    ) -> dict:
        self.require_premium(user_id, "Experiments")
        self.require_consent(user_id)
        if primary_metric not in {
            "blemish_count",
            "redness_score",
            "darkspot_area",
            "texture_score",
        }:
            raise ValueError("unsupported primary metric")
        if target_days < 1 or target_days > 180:
            raise ValueError("target_days must be between 1 and 180")
        if not self.db.fetchone("SELECT id FROM products WHERE id=?", (product_id,)):
            raise ValueError("product not found")
        experiment_id = uid()
        start = now_iso()
        self.db.execute(
            "INSERT INTO experiments (id,user_id,name,hypothesis,primary_metric,status,start_at,target_days) VALUES (?,?,?,?,?,?,?,?)",
            (
                experiment_id,
                user_id,
                name.strip(),
                hypothesis,
                primary_metric,
                "running",
                start,
                target_days,
            ),
        )
        self.db.execute(
            "INSERT INTO experiment_products (experiment_id,product_id,role) VALUES (?,?, 'test')",
            (experiment_id, product_id),
        )
        self._audit(
            "experiment_started",
            "experiment",
            experiment_id,
            user_id,
            {"product_id": product_id},
        )
        return self.experiment(experiment_id, user_id)

    def _early_stop(self, user_id: str, experiment: dict) -> dict:
        """Tell the user when evidence is already conclusive, before target_days."""

        if experiment["status"] != "running" or not experiment["products"]:
            return {
                "conclusive": False,
                "message": "Not applicable to a non-running experiment.",
            }
        product_id = experiment["products"][0]["product_id"]
        try:
            result = self.attribution.evaluate_product(user_id, product_id)
        except ValueError:
            return {"conclusive": False, "message": "No product evidence yet."}
        days_stable = result.evidence.get("days_stable", 0)
        target_days = experiment["target_days"]
        if result.label == "likely_useful" and days_stable < target_days:
            return {
                "conclusive": True,
                "recommended_status": "completed",
                "message": f"Evidence is already conclusive after {days_stable} of {target_days} planned days: {result.product_name} is likely useful. You can stop the experiment early.",
            }
        if result.label == "investigate" and days_stable < target_days:
            return {
                "conclusive": True,
                "recommended_status": "investigate",
                "message": f"Evidence already shows a worsening signal after {days_stable} of {target_days} planned days. Stop and investigate before waiting the full window.",
            }
        return {
            "conclusive": False,
            "message": "Not yet conclusive; keep the routine stable until the target day count.",
        }

    def experiment(self, experiment_id: str, user_id: str) -> dict:
        row = self.db.fetchone(
            "SELECT * FROM experiments WHERE id=? AND user_id=?",
            (experiment_id, user_id),
        )
        if not row:
            raise ValueError("experiment not found")
        result = row_dict(row)
        result["products"] = [
            row_dict(item)
            for item in self.db.fetchall(
                "SELECT ep.*, p.name, p.category FROM experiment_products ep JOIN products p ON p.id=ep.product_id WHERE ep.experiment_id=?",
                (experiment_id,),
            )
        ]
        result["events"] = [
            row_dict(item)
            for item in self.db.fetchall(
                "SELECT e.*, p.name AS product_name FROM experiment_events x JOIN routine_events e ON e.id=x.routine_event_id JOIN products p ON p.id=e.product_id WHERE x.experiment_id=? ORDER BY e.timestamp",
                (experiment_id,),
            )
        ]
        result["captures"] = self.history(user_id)
        result["early_stop"] = self._early_stop(user_id, result)
        return result

    def experiments(self, user_id: str) -> list[dict]:
        self.require_premium(user_id, "Experiments")
        return [
            self.experiment(row["id"], user_id)
            for row in self.db.fetchall(
                "SELECT id FROM experiments WHERE user_id=? ORDER BY created_at DESC",
                (user_id,),
            )
        ]

    def set_experiment_status(
        self, user_id: str, experiment_id: str, status: str,
    ) -> dict:
        self.require_premium(user_id, "Experiments")
        if status not in {"planned", "running", "paused", "completed", "cancelled"}:
            raise ValueError("invalid experiment status")
        self.db.execute(
            "UPDATE experiments SET status=?, end_at=? WHERE id=? AND user_id=?",
            (
                status,
                now_iso() if status in {"completed", "cancelled"} else None,
                experiment_id,
                user_id,
            ),
        )
        return self.experiment(experiment_id, user_id)

    # -- captures -------------------------------------------------------------

    def _vertical_metrics(self, vertical: str, metric: dict) -> dict:
        return {
            key: metric.get(key)
            for key in (
                "blemish_count",
                "redness_score",
                "redness_delta",
                "darkspot_area",
                "texture_score",
            )
        }

    def _get_baseline_metrics(self, user_id: str, vertical: str = "skin") -> dict | None:
        """Get the baseline capture metrics for calculating relative changes."""
        row = self.db.fetchone(
            """SELECT m.blemish_count, m.redness_score, m.darkspot_area, m.texture_score
               FROM photo_captures c
               JOIN metric_snapshots m ON m.id=(SELECT m2.id FROM metric_snapshots m2
                                                 WHERE m2.photo_id=c.id
                                                 ORDER BY m2.created_at DESC LIMIT 1)
               LEFT JOIN appearance_captures a ON a.photo_id=c.id AND a.vertical=?
               WHERE c.user_id=? AND c.is_baseline=1
               ORDER BY c.captured_at
               LIMIT 1""",
            (vertical, user_id),
        )
        if not row:
            return None
        return row_dict(row)

    def _calculate_relative_change(self, current_value: float | None, baseline_value: float | None) -> float | None:
        """Calculate percentage change from baseline, handling division by zero."""
        if current_value is None or baseline_value is None:
            return None
        if baseline_value == 0:
            # Handle division by zero: if current is also 0, no change; otherwise infinite change
            if current_value == 0:
                return 0.0
            # Return None for infinite change cases
            return None
        return round(((current_value - baseline_value) / baseline_value) * 100, 2)

    def _add_baseline_comparison(self, user_id: str, metric: dict, vertical: str = "skin") -> dict:
        """Add baseline comparison data to metrics."""
        baseline = self._get_baseline_metrics(user_id, vertical)
        if not baseline:
            return {
                "has_baseline": False,
                "redness_change_pct": None,
                "blemish_change_pct": None,
                "darkspot_change_pct": None,
                "texture_change_pct": None,
            }

        return {
            "has_baseline": True,
            "redness_change_pct": self._calculate_relative_change(
                metric.get("redness_score"), baseline.get("redness_score"),
            ),
            "blemish_change_pct": self._calculate_relative_change(
                metric.get("blemish_count"), baseline.get("blemish_count"),
            ),
            "darkspot_change_pct": self._calculate_relative_change(
                metric.get("darkspot_area"), baseline.get("darkspot_area"),
            ),
            "texture_change_pct": self._calculate_relative_change(
                metric.get("texture_score"), baseline.get("texture_score"),
            ),
        }

    def create_capture(
        self,
        user_id: str,
        image_bytes: bytes,
        quality_data: dict | None = None,
        captured_at: str | None = None,
        device_meta: dict | None = None,
        is_baseline: bool = False,
        vertical: str = "skin",
        experiment_id: str | None = None,
    ) -> dict:
        if vertical not in VERTICALS:
            raise ValueError("vertical must be skin")
        if experiment_id and not self.db.fetchone(
            "SELECT id FROM experiments WHERE id=? AND user_id=?",
            (experiment_id, user_id),
        ):
            raise ValueError("experiment not found")
        result = super().create_capture(
            user_id, image_bytes, quality_data, captured_at, device_meta, is_baseline,
        )
        metric = result["metric"] or {}
        appearance_id = uid()
        self.db.execute(
            "INSERT INTO appearance_captures (id,user_id,photo_id,vertical,metrics_json,model_version,confidence) VALUES (?,?,?,?,?,?,?)",
            (
                appearance_id,
                user_id,
                result["id"],
                vertical,
                dump(self._vertical_metrics(vertical, metric)),
                metric.get("model_version", self.settings.model_version),
                metric.get("confidence", 0),
            ),
        )
        if result["is_baseline"]:
            self.db.execute(
                "UPDATE users SET baseline_date=? WHERE id=?",
                (result["captured_at"], user_id),
            )
            self.db.execute(
                "UPDATE appearance_profiles SET baseline_capture_id=? WHERE user_id=? AND vertical=?",
                (result["id"], user_id, vertical),
            )
        if experiment_id:
            self.db.execute(
                "INSERT INTO engagement_events (id,user_id,event_type,reference_id) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
                (uid(), user_id, "experiment_capture", experiment_id),
            )
        self.record_engagement(user_id, "capture_completed", result["id"])
        result["vertical"] = vertical
        result["capture"] = {"id": result["id"], "captured_at": result["captured_at"]}
        result["appearance_metrics"] = self._vertical_metrics(vertical, metric)
        result["measurement"] = self._measurement_explanation(
            {
                "confidence": metric.get("confidence", 0),
                "capture_quality": result.get("capture_quality", {}),
            },
        )
        result["capture_protocol"] = CAPTURE_PROTOCOL_VERSION
        # Add baseline comparison if this is not the baseline itself
        if not is_baseline:
            result["baseline_comparison"] = self._add_baseline_comparison(user_id, metric, vertical)
        else:
            result["baseline_comparison"] = {
                "has_baseline": False,
                "redness_change_pct": None,
                "blemish_change_pct": None,
                "darkspot_change_pct": None,
                "texture_change_pct": None,
            }
        return result

    @staticmethod
    def _measurement_explanation(item: dict) -> dict:
        confidence = float(item.get("confidence") or 0)
        quality = item.get("capture_quality") or {}
        quality_score = float(quality.get("score") or 0)
        failed = quality.get("failed_checks") or []
        if confidence >= 0.75 and quality_score >= 0.75:
            label = "strong comparison frame"
            message = (
                "This frame is well suited for comparing larger changes over time."
            )
        elif confidence >= 0.50 and quality_score >= 0.65:
            label = "directional frame"
            message = "Useful for a broad trend, but small changes may still be capture noise."
        else:
            label = "low-confidence frame"
            message = "Treat this as context rather than proof; use the next guided frame for a stronger comparison."
        return {
            "confidence_label": label,
            "confidence_message": message,
            "comparison_ready": confidence >= 0.50 and quality_score >= 0.65,
            "quality_score": round(quality_score, 3),
            "quality_issues": failed,
            "capture_protocol": CAPTURE_PROTOCOL_VERSION,
            "noise_floor_message": "A change smaller than the stated noise floor may not be a real appearance change.",
        }

    def _weekly_metric_summary(self, metric: str, first: dict, last: dict) -> dict:
        first_value = float(first[metric])
        last_value = float(last[metric])
        delta = round(last_value - first_value, 5)
        noise = float((last.get("noise_floor") or {}).get(metric, 0.0))
        if abs(delta) <= noise:
            direction = "steady"
            sentence = f"{METRIC_LABELS[metric].capitalize()} looks steady; the movement is inside the capture noise floor."
        elif delta < 0:
            direction = "improved"
            sentence = f"{METRIC_LABELS[metric].capitalize()} moved in a better direction, beyond the current noise floor."
        else:
            direction = "increased"
            sentence = f"{METRIC_LABELS[metric].capitalize()} increased beyond the current noise floor; keep the routine stable before changing it."
        return {
            "metric": metric,
            "label": METRIC_LABELS[metric],
            "direction": direction,
            "delta": delta,
            "noise_floor": round(noise, 5),
            "sentence": sentence,
        }

    def weekly_recap(
        self, user_id: str, vertical: str = "skin", as_of: str | None = None,
    ) -> dict:
        self.require_user(user_id)
        history = self.history(user_id, vertical)
        checkins = self.check_ins(user_id, limit=50)
        if not history:
            return {
                "status": "baseline_needed",
                "headline": "Your first frame starts the record.",
                "body": "Take one standardized capture. The next capture becomes your first real comparison.",
                "next_action": "Take your baseline capture",
                "capture_count": 0,
                "check_in_count": len(checkins),
                "metric_summaries": [],
                "confidence_label": "no measurement yet",
                "period": {"start": None, "end": None},
                "disclaimer": "Cosmetic appearance tracking only; this is not a diagnosis.",
            }
        anchor = (
            as_date(as_of)
            if as_of
            else max(
                [as_date(item["captured_at"]) for item in history]
                + [as_date(item["occurred_at"]) for item in checkins]
                if checkins
                else [as_date(item["captured_at"]) for item in history],
            )
        )
        current_start = anchor - timedelta(days=7)
        previous_start = anchor - timedelta(days=14)
        current = [
            item
            for item in history
            if current_start <= as_date(item["captured_at"]) <= anchor
        ]
        previous = [
            item
            for item in history
            if previous_start <= as_date(item["captured_at"]) < current_start
        ]
        comparison_mode = "week_over_week"
        if len(current) >= 1 and len(previous) >= 1:
            first, last = previous[-1], current[-1]
            compared = previous + current
        elif len(history) >= 2:
            comparison_mode = "first_to_latest"
            first, last = history[0], history[-1]
            compared = history[-2:]
        else:
            return {
                "status": "building_signal",
                "headline": "Your baseline is in. Build the comparison.",
                "body": "One more standardized capture will let SkinProof separate a broad trend from a single-frame impression.",
                "next_action": "Take the next guided capture",
                "capture_count": len(history),
                "check_in_count": len(checkins),
                "metric_summaries": [],
                "confidence_label": history[-1].get(
                    "confidence_label", "building signal",
                ),
                "period": {
                    "start": history[0]["captured_at"],
                    "end": history[-1]["captured_at"],
                },
                "disclaimer": "Cosmetic appearance tracking only; this is not a diagnosis.",
            }
        metric_summaries = [
            self._weekly_metric_summary(metric, first, last) for metric in METRIC_LABELS
        ]
        improved = [
            item["label"]
            for item in metric_summaries
            if item["direction"] == "improved"
        ]
        increased = [
            item["label"]
            for item in metric_summaries
            if item["direction"] == "increased"
        ]
        if improved and not increased:
            status = "directional"
            headline = "Your recent trend is moving in a better direction."
            body = f"{', '.join(improved[:2]).capitalize()} moved beyond the current noise floor. This is directional evidence, not a product verdict yet."
        elif increased and not improved:
            status = "directional"
            headline = "Your recent trend needs another steady window."
            body = f"{', '.join(increased[:2]).capitalize()} increased beyond the current noise floor. Keep the routine stable before drawing a conclusion."
        else:
            status = "steady"
            headline = "Your recent measurements are mostly steady."
            body = "No broad movement clearly cleared the current capture noise floor. That is useful information, not a failed result."
        average_confidence = statistics.mean(
            float(item.get("confidence") or 0) for item in compared
        )
        confidence_label = (
            "strong enough for a broad trend"
            if average_confidence >= 0.65
            else "still sensitive to capture noise"
        )
        recent_checkins = [
            item
            for item in checkins
            if current_start <= as_date(item["occurred_at"]) <= anchor
        ]
        return {
            "status": status,
            "headline": headline,
            "body": body,
            "next_action": "Keep the routine steady and capture again in the next guided window",
            "capture_count": len(current),
            "total_capture_count": len(history),
            "check_in_count": len(recent_checkins),
            "comparison_mode": comparison_mode,
            "confidence_label": confidence_label,
            "metric_summaries": metric_summaries,
            "period": {"start": first["captured_at"], "end": last["captured_at"]},
            "disclaimer": "Cosmetic appearance tracking only; this is not a diagnosis.",
        }

    def check_ins(self, user_id: str, limit: int = 30) -> list[dict]:
        self.require_user(user_id)
        rows = self.db.fetchall(
            "SELECT * FROM check_ins WHERE user_id=? ORDER BY occurred_at DESC LIMIT ?",
            (user_id, max(1, min(limit, 100))),
        )
        return [row_dict(row) for row in rows]

    def create_check_in(
        self,
        user_id: str,
        routine_state: str,
        skin_feel: str,
        note: str | None = None,
        occurred_at: str | None = None,
    ) -> dict:
        self.require_user(user_id)
        if routine_state not in CHECK_IN_ROUTINE_STATES:
            raise ValueError(
                f"routine_state must be one of {sorted(CHECK_IN_ROUTINE_STATES)}",
            )
        if skin_feel not in CHECK_IN_SKIN_FEELS:
            raise ValueError(f"skin_feel must be one of {sorted(CHECK_IN_SKIN_FEELS)}")
        timestamp = occurred_at or now_iso()
        parse_time(timestamp)
        check_in_id = uid()
        self.db.execute(
            "INSERT INTO check_ins (id,user_id,routine_state,skin_feel,note,occurred_at) VALUES (?,?,?,?,?,?)",
            (
                check_in_id,
                user_id,
                routine_state,
                skin_feel,
                note.strip() if note else None,
                timestamp,
            ),
        )
        self.record_engagement(
            user_id,
            "check_in_completed",
            check_in_id,
            {"routine_state": routine_state, "skin_feel": skin_feel},
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM check_ins WHERE id=?", (check_in_id,)),
        )

    def add_measurement_feedback(
        self, user_id: str, capture_id: str, agreement: str, note: str | None = None,
    ) -> dict:
        self.require_user(user_id)
        if agreement not in {"fair", "uncertain", "off"}:
            raise ValueError("agreement must be fair, uncertain, or off")
        if not self.db.fetchone(
            "SELECT id FROM photo_captures WHERE id=? AND user_id=?",
            (capture_id, user_id),
        ):
            raise ValueError("capture not found")
        feedback_id = uid()
        self.db.execute(
            """INSERT INTO measurement_feedback (id,user_id,capture_id,agreement,note)
            VALUES (?,?,?,?,?) ON CONFLICT (user_id,capture_id) DO UPDATE SET agreement=excluded.agreement,note=excluded.note,created_at=datetime('now')""",
            (
                feedback_id,
                user_id,
                capture_id,
                agreement,
                note.strip() if note else None,
            ),
        )
        self.record_engagement(
            user_id,
            "measurement_feedback_submitted",
            capture_id,
            {"agreement": agreement},
        )
        row = self.db.fetchone(
            "SELECT * FROM measurement_feedback WHERE user_id=? AND capture_id=?",
            (user_id, capture_id),
        )
        return row_dict(row)

    def measurement_feedback_summary(self) -> dict:
        rows = self.db.fetchall(
            "SELECT agreement, COUNT(*) AS count FROM measurement_feedback GROUP BY agreement",
        )
        counts = {"fair": 0, "uncertain": 0, "off": 0}
        for row in rows:
            counts[row["agreement"]] = int(row["count"])
        return {
            "counts": counts,
            "total": sum(counts.values()),
            "note": "User-reported agreement is a product-quality signal, not a clinical validation study.",
        }

    def history(self, user_id: str, vertical: str = "skin") -> list[dict]:
        self.require_user(user_id)
        if vertical not in VERTICALS:
            raise ValueError("invalid vertical")
        rows = self.db.fetchall(
            """SELECT c.id,c.captured_at,c.is_baseline,c.capture_quality_json,m.model_version,m.blemish_count,m.redness_score,m.redness_delta,m.darkspot_area,m.texture_score,m.confidence,m.noise_floor_json,a.metrics_json FROM photo_captures c JOIN metric_snapshots m ON m.id=(SELECT m2.id FROM metric_snapshots m2 WHERE m2.photo_id=c.id ORDER BY m2.created_at DESC LIMIT 1) LEFT JOIN appearance_captures a ON a.photo_id=c.id AND a.vertical=? WHERE c.user_id=? ORDER BY c.captured_at""",
            (vertical, user_id),
        )
        output = []
        for row in rows:
            item = row_dict(row)
            item["capture_quality"] = load(item.pop("capture_quality_json"))
            item["noise_floor"] = load(item.pop("noise_floor_json"))
            item["appearance_metrics"] = load(item.pop("metrics_json"), {})
            item.update(self._measurement_explanation(item))
            # Add baseline comparison for non-baseline captures
            if not item.get("is_baseline"):
                item["baseline_comparison"] = self._add_baseline_comparison(
                    user_id, item, vertical,
                )
            else:
                item["baseline_comparison"] = {
                    "has_baseline": False,
                    "redness_change_pct": None,
                    "blemish_change_pct": None,
                    "darkspot_change_pct": None,
                    "texture_change_pct": None,
                }
            output.append(item)
        if not self.is_premium(user_id):
            cutoff = datetime.now(UTC) - timedelta(days=FREE_HISTORY_DAYS)
            output = [item for item in output if as_date(item["captured_at"]) >= cutoff]
        return output

    def capture_guide(self, user_id: str, vertical: str = "skin") -> dict:
        history = self.history(user_id, vertical)
        captures = [as_date(item["captured_at"]) for item in history]
        latest = max(captures) if captures else None
        now = datetime.now(UTC)
        if not latest:
            return {
                "vertical": vertical,
                "state": "baseline_needed",
                "message": "Take a baseline capture to start your own history.",
                "next_window_start": now.isoformat().replace("+00:00", "Z"),
                "next_window_end": (now + timedelta(days=1))
                .isoformat()
                .replace("+00:00", "Z"),
            }
        start, end = latest + timedelta(days=3), latest + timedelta(days=7)
        state = (
            "due"
            if now >= start and now <= end
            else ("overdue" if now > end else "scheduled")
        )
        return {
            "vertical": vertical,
            "state": state,
            "last_capture": latest.isoformat().replace("+00:00", "Z"),
            "next_window_start": start.isoformat().replace("+00:00", "Z"),
            "next_window_end": end.isoformat().replace("+00:00", "Z"),
            "message": (
                "Capture now for a comparable measurement."
                if state in {"due", "overdue"}
                else "Your next guided window opens soon."
            ),
        }

    def record_engagement(
        self,
        user_id: str,
        event_type: str,
        reference_id: str | None = None,
        metadata=None,
    ) -> dict:
        self.require_user(user_id)
        event_id = uid()
        self.db.execute(
            "INSERT INTO engagement_events (id,user_id,event_type,reference_id,metadata_json) VALUES (?,?,?,?,?)",
            (event_id, user_id, event_type, reference_id, dump(metadata or {})),
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM engagement_events WHERE id=?", (event_id,)),
        )

    def engagement(self, user_id: str) -> dict:
        self.require_user(user_id)
        rows = self.db.fetchall(
            "SELECT captured_at FROM photo_captures WHERE user_id=? AND status='accepted' ORDER BY captured_at DESC",
            (user_id,),
        )
        dates = []
        seen = set()
        for row in rows:
            value = day(as_date(row["captured_at"]))
            if value not in seen:
                seen.add(value)
                dates.append(value)
        streak = 0
        if dates:
            streak = 1
            for index in range(1, len(dates)):
                gap = (
                    datetime.fromisoformat(dates[index - 1])
                    - datetime.fromisoformat(dates[index])
                ).days
                if 3 <= gap <= 7:
                    streak += 1
                else:
                    break
        guide = self.capture_guide(user_id)
        reminder_id = f"{user_id}:capture"
        self.db.execute(
            "INSERT INTO reminders (id,user_id,kind,next_at,cadence_days) VALUES (?,?,?,?,4) ON CONFLICT(id) DO UPDATE SET next_at=excluded.next_at",
            (reminder_id, user_id, "capture", guide["next_window_start"]),
        )
        return {
            "capture_streak": streak,
            "capture_count": len(dates),
            "capture_days": dates[:30],
            "guide": guide,
            "reminders": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM reminders WHERE user_id=? AND enabled=1", (user_id,),
                )
            ],
        }

    def analytics(self, user_id: str) -> dict:
        self.require_user(user_id)
        history = self.history(user_id)
        events = self.db.fetchall(
            "SELECT event_type FROM engagement_events WHERE user_id=?", (user_id,),
        )
        event_types = [row["event_type"] for row in events]
        verdicts = self.db.fetchall(
            "SELECT label FROM verdicts WHERE user_id=?", (user_id,),
        )
        labels = [row["label"] for row in verdicts]
        return {
            "activation": bool(
                any(item["is_baseline"] for item in history)
                and self.db.fetchone(
                    "SELECT id FROM routine_events WHERE user_id=?", (user_id,),
                ),
            ),
            "baseline_capture": any(item["is_baseline"] for item in history),
            "first_three_captures": len(history) >= 3,
            "weekly_verdict_open_rate": round(
                event_types.count("verdict_open") / max(1, labels.__len__()), 3,
            ),
            "verdict_action_rate": round(
                event_types.count("verdict_action") / max(1, labels.__len__()), 3,
            ),
            "median_history_days": self._median_history_days(history),
            "evidence_unclear_engagement_rate": round(
                sum(
                    1
                    for label in labels
                    if label == "evidence_unclear" and "verdict_action" in event_types
                )
                / max(1, labels.count("evidence_unclear")),
                3,
            ),
            "raw_events": len(events),
        }

    @staticmethod
    def _median_history_days(history: list[dict]) -> int:
        if len(history) < 2:
            return 0
        dates = [as_date(item["captured_at"]) for item in history]
        return int((max(dates) - min(dates)).days)

    # -- verdicts: one free lifetime unlock, then Premium --------------------

    def _free_unlocked_product_id(self, user_id: str) -> str | None:
        row = self.db.fetchone(
            "SELECT reference_id FROM engagement_events WHERE user_id=? AND event_type='free_verdict_unlocked' ORDER BY occurred_at LIMIT 1",
            (user_id,),
        )
        return row["reference_id"] if row else None

    def verdicts_for_user(self, user_id: str) -> list[dict]:
        verdicts = self.refresh_verdicts(user_id)
        if self.is_premium(user_id):
            return verdicts
        unlocked = self._free_unlocked_product_id(user_id)
        if not unlocked:
            definitive = next(
                (item for item in verdicts if item["label"] != "evidence_unclear"), None,
            )
            if definitive:
                unlocked = definitive["product_id"]
                self.record_engagement(user_id, "free_verdict_unlocked", unlocked)
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
                    },
                )
        return output

    def dashboard(self, user_id: str, vertical: str = "skin") -> dict:
        profile = self.profile(user_id)
        history = self.history(user_id, vertical)
        plan = profile["entitlement"]["plan"]
        verdicts = self.verdicts_for_user(user_id)
        features = {feature: int(plan == "premium") for feature in PREMIUM_FEATURES}
        features["product_verdicts_unlocked"] = int(
            self._free_unlocked_product_id(user_id) is not None or plan == "premium",
        )
        return {
            "profile": profile,
            "vertical": vertical,
            "history": history,
            "verdicts": verdicts,
            "experiments": self.experiments(user_id) if plan == "premium" else [],
            "engagement": self.engagement(user_id),
            "analytics": self.analytics(user_id),
            "weekly_recap": self.weekly_recap(user_id, vertical),
            "check_ins": self.check_ins(user_id, limit=20),
            "routine_events": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT e.*,p.name AS product_name FROM routine_events e JOIN products p ON p.id=e.product_id WHERE e.user_id=? ORDER BY e.timestamp DESC",
                    (user_id,),
                )
            ],
            "features": features,
            "disclaimer": "Cosmetic tracking only; SkinProof does not diagnose, treat, or rule out medical conditions.",
        }

    # -- grounded Q&A (Premium) ------------------------------------------------

    def _qna_evidence(
        self, user_id: str, history: list[dict], events: list[dict],
    ) -> dict:
        verdicts = [
            self._decode_verdict(row)
            for row in self.db.fetchall(
                """SELECT v.*,p.name AS product_name FROM verdicts v
                   JOIN products p ON p.id=v.product_id WHERE v.user_id=?
                   ORDER BY v.generated_at DESC LIMIT 20""",
                (user_id,),
            )
        ]
        return {
            "capture_count": len(history),
            "captures": history[-8:],
            "routine_events": events[-20:],
            "verdicts": verdicts,
        }

    def _deterministic_qna_answer(
        self, user_id: str, question: str, history: list[dict], events: list[dict],
    ) -> str:
        lowered = question.casefold()
        if "ingredient" in lowered or "inci" in lowered:
            return "Ingredient explanations are grounded in the maintained catalog. Open a product's ingredient analysis to see reviewed purposes and cautions; unknown entries are explicitly marked unknown."
        if "redness" in lowered and len(history) >= 2:
            delta = float(history[-1]["redness_score"]) - float(
                history[0]["redness_score"],
            )
            return f"Your measured redness changed by {delta:+.4f} from the first available capture to the latest. That is a measurement delta, not a diagnosis. I found {len(events)} routine events to compare against it."
        if any(word in lowered for word in ("why", "pattern", "correlat")):
            correlations = self.root_cause_search(user_id)
            if correlations:
                top = correlations[0]
                return f"The strongest pattern in your data: {top['message']}"
            return "I did not find a repeatable pattern between your logged context events and your measurements yet. Log more sleep/travel/weather events or capture more consistently to sharpen this."
        if any(word in lowered for word in ("work", "useful", "product", "keep")):
            verdicts = self.refresh_verdicts(user_id)
            return (
                "Your current evidence states are: "
                + (
                    ", ".join(
                        f"{item['product_name']}: {item['label']}" for item in verdicts
                    )
                    if verdicts
                    else "not enough clean post-stabilization evidence yet"
                )
                + "."
            )
        return f"I can ground this in {len(history)} captures and {len(events)} routine events. Ask about redness, a product, ingredients, cadence, patterns, or a specific date so I can cite the relevant evidence."

    def ask(self, user_id: str, question: str, thread_id: str | None = None) -> dict:
        self.require_premium(user_id, "Data Q&A")
        scope = triage(question)
        if thread_id:
            thread = self.db.fetchone(
                "SELECT * FROM qna_threads WHERE id=? AND user_id=?",
                (thread_id, user_id),
            )
            if not thread:
                raise ValueError("thread not found")
        else:
            thread_id = uid()
            self.db.execute(
                "INSERT INTO qna_threads (id,user_id,title) VALUES (?,?,?)",
                (thread_id, user_id, question[:120]),
            )
        self.db.execute(
            "INSERT INTO qna_messages (id,thread_id,role,content,scope) VALUES (?,?,?,?,?)",
            (uid(), thread_id, "user", question, scope.scope),
        )
        citations = []
        if scope.scope == "dermatology_review":
            answer = scope.message
        else:
            history = self.history(user_id)
            events = [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT e.timestamp,p.name,e.action FROM routine_events e JOIN products p ON p.id=e.product_id WHERE e.user_id=? ORDER BY e.timestamp",
                    (user_id,),
                )
            ]
            for item in history[-3:]:
                citations.append(
                    {"type": "capture", "date": item["captured_at"], "id": item["id"]},
                )
            provider_answer = getattr(self.insights, "answer", None)
            answer = (
                provider_answer(question, self._qna_evidence(user_id, history, events))
                if callable(provider_answer)
                else None
            )
            if not answer:
                answer = self._deterministic_qna_answer(
                    user_id, question, history, events,
                )
        self.db.execute(
            "INSERT INTO qna_messages (id,thread_id,role,content,citations_json,scope) VALUES (?,?,?,?,?,?)",
            (uid(), thread_id, "assistant", answer, dump(citations), scope.scope),
        )
        self.record_engagement(user_id, "qna_answered", thread_id)
        return {
            "thread_id": thread_id,
            "answer": answer,
            "scope": scope.scope,
            "citations": citations,
        }

    def qna_history(self, user_id: str) -> list[dict]:
        self.require_premium(user_id, "Data Q&A")
        return [
            row_dict(row) | {"citations": load(row["citations_json"], [])}
            for row in self.db.fetchall(
                "SELECT m.*,t.title FROM qna_messages m JOIN qna_threads t ON t.id=m.thread_id WHERE t.user_id=? ORDER BY m.created_at",
                (user_id,),
            )
        ]

    # -- context events + root-cause search (Premium) ------------------------

    def add_context_event(
        self,
        user_id: str,
        event_type: str,
        value: str | None = None,
        occurred_at: str | None = None,
        notes: str | None = None,
    ) -> dict:
        self.require_user(user_id)
        if event_type not in CONTEXT_EVENT_TYPES:
            raise ValueError(f"event_type must be one of {sorted(CONTEXT_EVENT_TYPES)}")
        timestamp = occurred_at or now_iso()
        parse_time(timestamp)
        event_id = uid()
        self.db.execute(
            "INSERT INTO context_events (id,user_id,event_type,value,notes,occurred_at) VALUES (?,?,?,?,?,?)",
            (event_id, user_id, event_type, value, notes, timestamp),
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM context_events WHERE id=?", (event_id,)),
        )

    def context_events(self, user_id: str) -> list[dict]:
        self.require_user(user_id)
        return [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM context_events WHERE user_id=? ORDER BY occurred_at DESC",
                (user_id,),
            )
        ]

    def root_cause_search(
        self, user_id: str, metric: str = "texture_score",
    ) -> list[dict]:
        self.require_premium(user_id, "Root-cause search")
        if metric not in {
            "blemish_count",
            "redness_score",
            "darkspot_area",
            "texture_score",
        }:
            raise ValueError("unsupported metric")
        history = self.history(user_id)
        if len(history) < 4:
            return []
        values = [
            (as_date(item["captured_at"]), float(item[metric]))
            for item in history
            if item.get(metric) is not None
        ]
        if len(values) < 4:
            return []
        overall_median = statistics.median(value for _, value in values)
        floor = (
            float(history[-1]["noise_floor"].get(metric, 0.0))
            if isinstance(history[-1]["noise_floor"], dict)
            else 0.0
        )
        events = self.context_events(user_id)
        by_type: dict[str, list[float]] = {}
        for event in events:
            occurred = as_date(event["occurred_at"])
            window = [
                value
                for captured, value in values
                if timedelta(days=3) <= (captured - occurred) <= timedelta(days=9)
            ]
            if not window:
                continue
            window_median = statistics.median(window)
            shift = window_median - overall_median
            normalized = round(shift / floor, 3) if floor else 0.0
            by_type.setdefault(event["event_type"], []).append(normalized)
        correlations = []
        for event_type, shifts in by_type.items():
            if len(shifts) < 2:
                continue
            average = statistics.mean(shifts)
            same_sign = all(s > 0 for s in shifts) or all(s < 0 for s in shifts)
            if same_sign and abs(average) >= 1.5:
                direction = "worse" if average > 0 else "better"
                correlations.append(
                    {
                        "event_type": event_type,
                        "occurrences": len(shifts),
                        "normalized_effect": round(average, 3),
                        "metric": metric,
                        "message": f"{metric.replace('_', ' ')} tends to move {direction} 3-9 days after a {event_type} event, across {len(shifts)} occurrences. This is a correlation from your own history, not proven causation — confirm it with a one-variable experiment before changing your routine.",
                    },
                )
        return sorted(
            correlations, key=lambda item: abs(item["normalized_effect"]), reverse=True,
        )

    # -- shelf scan → auto-logging (free) -------------------------------------

    def _run_shelf_scan(self, image_bytes: bytes) -> dict:
        if not self.vision:
            return {
                "candidates": [],
                "message": "AI shelf-scan is not configured; add products manually.",
            }
        candidates = self.vision.extract_products(image_bytes)
        return {
            "candidates": candidates,
            "message": (
                f"Found {len(candidates)} candidate product(s). Review and confirm before they are added."
                if candidates
                else "No products were recognized in this photo. Try better lighting or a closer shot of the labels."
            ),
        }

    def scan_shelf(self, user_id: str, image_bytes: bytes) -> dict:
        self.require_user(user_id)
        if not image_bytes:
            raise ValueError("image is required")
        job_id = self.jobs.submit(
            "shelf_scan", self._run_shelf_scan, image_bytes, user_id=user_id,
        )
        self.record_engagement(user_id, "shelf_scan_submitted", job_id)
        return {"job_id": job_id, "status": "queued"}

    def shelf_scan_status(self, user_id: str, job_id: str) -> dict:
        self.require_user(user_id)
        job = self.jobs.get(job_id, user_id=user_id)
        if not job:
            raise ValueError("shelf-scan job not found")
        return job

    def confirm_shelf_scan(
        self, user_id: str, job_id: str, selections: list[dict],
    ) -> list[dict]:
        self.require_user(user_id)
        job = self.jobs.get(job_id, user_id=user_id)
        if not job or job["status"] != "completed":
            raise ValueError("shelf-scan job is not ready")
        created = []
        for selection in selections[:20]:
            name = str(selection.get("name") or "").strip()
            if not name:
                continue
            created.append(
                self.create_product(
                    name=name,
                    barcode=None,
                    category=str(selection.get("category") or "other"),
                    ingredients=selection.get("ingredients"),
                    stabilization_days=int(selection.get("stabilization_days") or 14),
                ),
            )
        self.record_engagement(
            user_id, "shelf_scan_confirmed", job_id, {"created": len(created)},
        )
        return created

    # -- pre-purchase prediction (Premium) ------------------------------------

    def predict_product(self, user_id: str, product_id: str) -> dict:
        self.require_premium(user_id, "Pre-purchase prediction")
        product = self.db.fetchone("SELECT * FROM products WHERE id=?", (product_id,))
        if not product:
            raise ValueError("product not found")
        self.refresh_verdicts(user_id)
        candidate_ingredients = {
            i.casefold() for i in parse_ingredients(product["ingredients_json"])
        }
        own_verdicts = self.db.fetchall(
            """SELECT v.label, p.ingredients_json, p.name FROM verdicts v JOIN products p ON p.id = v.product_id
               WHERE v.user_id=? AND v.generated_at = (SELECT MAX(v2.generated_at) FROM verdicts v2 WHERE v2.user_id=v.user_id AND v2.product_id=v.product_id)""",
            (user_id,),
        )
        investigate_hits, useful_hits = [], []
        for row in own_verdicts:
            other_ingredients = {
                i.casefold() for i in parse_ingredients(row["ingredients_json"])
            }
            overlap = candidate_ingredients & other_ingredients
            if not overlap:
                continue
            if row["label"] == "investigate":
                investigate_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)},
                )
            elif row["label"] == "likely_useful":
                useful_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)},
                )
        cohort_rows = self.db.fetchall(
            """SELECT DISTINCT p.name, p.ingredients_json FROM verdicts v JOIN products p ON p.id=v.product_id WHERE v.label='likely_useful' AND v.user_id <> ?""",
            (user_id,),
        )
        cohort_hits = []
        for row in cohort_rows:
            overlap = candidate_ingredients & {
                i.casefold() for i in parse_ingredients(row["ingredients_json"])
            }
            if overlap:
                cohort_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)},
                )
        if investigate_hits:
            headline = f"{len(investigate_hits)} of your own products that came back 'investigate' share an active ingredient with this one."
        elif useful_hits:
            headline = f"{len(useful_hits)} of your own products that came back 'likely useful' share an active ingredient with this one."
        else:
            headline = "No overlap with your own verdict history yet — this would be a new ingredient profile for you."
        return {
            "product_id": product_id,
            "product_name": product["name"],
            "ingredients": sorted(candidate_ingredients),
            "overlap_with_investigate": investigate_hits[:10],
            "overlap_with_likely_useful": useful_hits[:10],
            "cohort_overlap": cohort_hits[:10],
            "headline": headline,
            "disclaimer": "This is an ingredient-overlap similarity signal from your own and cohort history, not a prediction of efficacy or safety. Only your own capture-based experiment can verify how this product performs for you.",
        }

    def lookup_product(self, barcode: str) -> dict | None:
        normalized = re.sub(r"[^0-9A-Za-z-]", "", barcode or "").strip()
        if not normalized:
            raise ValueError("barcode is required")
        row = self.db.fetchone("SELECT * FROM products WHERE barcode=?", (normalized,))
        return row_dict(row)

    def purchase_guidance(
        self,
        user_id: str,
        name: str | None = None,
        barcode: str | None = None,
        ingredients=None,
        category: str = "other",
        price_cents: int | None = None,
        currency: str = "INR",
    ) -> dict:
        self.require_premium(user_id, "Pre-purchase guidance")
        normalized_barcode = re.sub(r"[^0-9A-Za-z-]", "", barcode or "").strip() or None
        product = (
            self.db.fetchone(
                "SELECT * FROM products WHERE barcode=?", (normalized_barcode,),
            )
            if normalized_barcode
            else None
        )
        candidate_name = (product["name"] if product else (name or "")).strip()
        candidate_ingredients = parse_ingredients(
            product["ingredients_json"] if product else ingredients,
        )
        if not candidate_name:
            raise ValueError("product name or a known barcode is required")
        if not candidate_ingredients:
            return {
                "product_id": product["id"] if product else None,
                "product_name": candidate_name,
                "barcode": normalized_barcode,
                "ingredients": [],
                "signal": "missing_ingredients",
                "headline": "Add the ingredient list to compare this product with your history.",
                "next_action": "Paste the INCI list from the box or product page, then run the check again.",
                "overlap_with_investigate": [],
                "overlap_with_likely_useful": [],
                "cohort_overlap": [],
                "estimated_annual_cost_cents": None,
                "currency": currency,
                "disclaimer": "Ingredient overlap is a similarity signal, not a prediction of efficacy or safety.",
            }
        self.refresh_verdicts(user_id)
        candidate_set = {item.casefold() for item in candidate_ingredients}
        rows = self.db.fetchall(
            """SELECT v.label, p.ingredients_json, p.name FROM verdicts v JOIN products p ON p.id=v.product_id
            WHERE v.user_id=? AND v.generated_at=(SELECT MAX(v2.generated_at) FROM verdicts v2 WHERE v2.user_id=v.user_id AND v2.product_id=v.product_id)""",
            (user_id,),
        )
        investigate_hits, useful_hits = [], []
        for row in rows:
            overlap = candidate_set & {
                item.casefold() for item in parse_ingredients(row["ingredients_json"])
            }
            if not overlap:
                continue
            hit = {"product_name": row["name"], "shared_ingredients": sorted(overlap)}
            if row["label"] == "investigate":
                investigate_hits.append(hit)
            elif row["label"] in {"likely_useful", "keep"}:
                useful_hits.append(hit)
        cohort_rows = self.db.fetchall(
            """SELECT DISTINCT p.name, p.ingredients_json FROM verdicts v JOIN products p ON p.id=v.product_id
            WHERE v.label='likely_useful' AND v.user_id<>?""",
            (user_id,),
        )
        cohort_hits = []
        for row in cohort_rows:
            overlap = candidate_set & {
                item.casefold() for item in parse_ingredients(row["ingredients_json"])
            }
            if overlap:
                cohort_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)},
                )
        if investigate_hits:
            signal = "caution"
            headline = f"This shares ingredients with {len(investigate_hits)} product(s) that your history marked for investigation."
            next_action = "Do not add it alongside another new product. If you test it, change one variable and wait for the stabilization window."
        elif useful_hits:
            signal = "promising_similarity"
            headline = f"This shares ingredients with {len(useful_hits)} product(s) that looked useful in your history."
            next_action = "It is a reasonable candidate, but only a controlled capture series can verify how it performs for you."
        else:
            signal = "new_profile"
            headline = "No overlap with your own verdict history yet."
            next_action = "Treat it as a new ingredient profile: capture a baseline, start one product, and keep the rest of the routine steady."
        stabilization_days = int(product["stabilization_days"]) if product else 14
        annual_cost = None
        if price_cents is not None:
            annual_cost = round(price_cents * (365 / max(stabilization_days * 3, 60)))
        return {
            "product_id": product["id"] if product else None,
            "product_name": candidate_name,
            "barcode": normalized_barcode,
            "ingredients": sorted(candidate_set),
            "signal": signal,
            "headline": headline,
            "next_action": next_action,
            "overlap_with_investigate": investigate_hits[:10],
            "overlap_with_likely_useful": useful_hits[:10],
            "cohort_overlap": cohort_hits[:10],
            "estimated_annual_cost_cents": annual_cost,
            "currency": currency,
            "disclaimer": "Ingredient overlap is a similarity signal from your own and cohort history, not a prediction of efficacy or safety. Only your own standardized capture series can verify this product for you.",
        }

    # -- routine budget optimizer (Premium) -----------------------------------

    def budget_optimizer(self, user_id: str) -> dict:
        self.require_premium(user_id, "Routine budget optimizer")
        verdicts = self.refresh_verdicts(user_id)
        flagged = []
        total_annual_cents = 0
        for item in verdicts:
            evidence = item.get("evidence", {})
            if item["label"] not in ("keep",) or evidence.get("days_stable", 0) < 30:
                continue
            offer = self.db.fetchone(
                "SELECT AVG(price_cents) AS avg_price, currency FROM affiliate_offers WHERE product_id=? AND price_cents IS NOT NULL",
                (item["product_id"],),
            )
            usage_days = max(int(evidence.get("stabilization_days", 14)) * 3, 60)
            annual_cents = None
            if offer and offer["avg_price"]:
                annual_cents = round(float(offer["avg_price"]) * (365 / usage_days))
                total_annual_cents += annual_cents
            flagged.append(
                {
                    "product_id": item["product_id"],
                    "product_name": item["product_name"],
                    "days_stable": evidence.get("days_stable", 0),
                    "estimated_annual_cost_cents": annual_cents,
                    "currency": (
                        offer["currency"] if offer and offer["avg_price"] else None
                    )
                    or "USD",
                    "reason": "No measurable improvement beyond the capture noise floor after at least 30 days.",
                },
            )
        return {
            "flagged": flagged,
            "estimated_annual_waste_cents": total_annual_cents,
            "currency": "USD",
            "disclaimer": "Estimates use your logged stabilization window and any known offer price; products without a known price are flagged without a cost estimate.",
        }

    # -- dermatologist export (Premium) ---------------------------------------

    def dermatologist_report(self, user_id: str) -> dict:
        self.require_premium(user_id, "Dermatologist export")
        profile = self.profile(user_id)
        history = self.history(user_id)
        verdicts = self.refresh_verdicts(user_id)
        model_versions = sorted(
            {item["model_version"] for item in history if item.get("model_version")},
        )
        summary_rows = "".join(
            f"<tr><td>{v['product_name']}</td><td>{v['label']}</td><td>{v['evidence'].get('days_stable', 0)}</td><td>{v['evidence'].get('confidence', '')}</td></tr>"
            for v in verdicts
        )
        printable_html = (
            "<h1>SkinProof cosmetic measurement summary</h1>"
            f"<p>Generated {now_iso()} for a consenting user. Cosmetic tracking only; not a diagnosis.</p>"
            f"<p>{len(history)} captures across model version(s): {', '.join(model_versions) or 'n/a'}.</p>"
            "<table border=1 cellpadding=4><tr><th>Product</th><th>Evidence label</th><th>Days stable</th><th>Confidence</th></tr>"
            f"{summary_rows}</table>"
        )
        return {
            "generated_at": now_iso(),
            "capture_count": len(history),
            "model_versions": model_versions,
            "verdicts": verdicts,
            "printable_html": printable_html,
            "disclaimer": "This is a measurement history export for the user to optionally share, not a clinical or diagnostic document. SkinProof does not diagnose, treat, or rule out medical conditions.",
        }

    # -- discover / commerce ---------------------------------------------------

    def discover(self, user_id: str) -> dict:
        self.require_premium(user_id, "Discover")
        rows = self.db.fetchall(
            """SELECT p.id,p.name,p.category,v.label,v.evidence_json,v.user_id FROM verdicts v JOIN products p ON p.id=v.product_id WHERE v.label='likely_useful'""",
        )
        grouped = {}
        for row in rows:
            key = row["id"]
            grouped.setdefault(
                key,
                {
                    "product_id": key,
                    "name": row["name"],
                    "category": row["category"],
                    "users": set(),
                    "effects": [],
                },
            )
            grouped[key]["users"].add(row["user_id"])
            grouped[key]["effects"].append(
                load(row["evidence_json"], {}).get("composite_effect", 0),
            )
        recommendations = []
        for item in grouped.values():
            if len(item["users"]) >= 3:
                recommendations.append(
                    {
                        "product_id": item["product_id"],
                        "name": item["name"],
                        "category": item["category"],
                        "sample_size": len(item["users"]),
                        "average_effect": round(statistics.mean(item["effects"]), 3),
                        "reason": "Observed as likely useful across at least three consenting users; this does not replace your own experiment.",
                    },
                )
        return {
            "recommendations": sorted(
                recommendations, key=lambda item: item["average_effect"], reverse=True,
            )[:20],
            "minimum_cohort_size": 3,
            "disclaimer": "Discover is cohort evidence, never a personal verdict and never paid placement.",
        }

    def add_offer(
        self,
        product_id: str,
        merchant: str,
        url: str,
        price_cents: int | None = None,
        currency: str = "USD",
    ) -> dict:
        if not self.db.fetchone("SELECT id FROM products WHERE id=?", (product_id,)):
            raise ValueError("product not found")
        offer_id = uid()
        self.db.execute(
            "INSERT INTO affiliate_offers (id,product_id,merchant,url,price_cents,currency) VALUES (?,?,?,?,?,?)",
            (offer_id, product_id, merchant, url, price_cents, currency),
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM affiliate_offers WHERE id=?", (offer_id,)),
        )

    def offers(self, user_id: str, product_id: str | None = None) -> list[dict]:
        self.require_user(user_id)
        sql = "SELECT o.*,p.name AS product_name FROM affiliate_offers o JOIN products p ON p.id=o.product_id WHERE o.active=1"
        params = ()
        if product_id:
            sql += " AND o.product_id=?"
            params = (product_id,)
        return [
            row_dict(row)
            for row in self.db.fetchall(sql + " ORDER BY o.created_at DESC", params)
        ]

    def click_offer(self, user_id: str, offer_id: str) -> dict:
        self.require_user(user_id)
        offer = self.db.fetchone(
            "SELECT * FROM affiliate_offers WHERE id=? AND active=1", (offer_id,),
        )
        if not offer:
            raise ValueError("offer not found")
        self.db.execute(
            "INSERT INTO affiliate_clicks (id,user_id,offer_id) VALUES (?,?,?)",
            (uid(), user_id, offer_id),
        )
        self.record_engagement(user_id, "affiliate_click", offer_id)
        return row_dict(offer)

    # -- labels, reprocessing, export, misc ------------------------------------

    def labels(self, user_id: str) -> list[dict]:
        self.require_user(user_id)
        return [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM labels WHERE user_id=? ORDER BY created_at DESC",
                (user_id,),
            )
        ]

    def add_label(
        self,
        user_id: str,
        photo_id: str,
        label_type: str,
        value: str,
        confidence: float | None = None,
        notes: str | None = None,
    ) -> dict:
        self.require_user(user_id)
        if not self.db.fetchone(
            "SELECT id FROM photo_captures WHERE id=? AND user_id=?",
            (photo_id, user_id),
        ):
            raise ValueError("capture not found")
        label_id = uid()
        self.db.execute(
            "INSERT INTO labels (id,user_id,photo_id,label_type,value,confidence,notes) VALUES (?,?,?,?,?,?,?)",
            (label_id, user_id, photo_id, label_type, value, confidence, notes),
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM labels WHERE id=?", (label_id,)),
        )

    def _run_reprocess(self, user_id: str, model_version: str) -> dict:
        captures = self.db.fetchall(
            "SELECT * FROM photo_captures WHERE user_id=? AND status='accepted' ORDER BY captured_at",
            (user_id,),
        )
        processed = 0
        for capture in captures:
            quality = load(capture["capture_quality_json"])
            result = analyze(
                self.photos.read(capture["raw_ref"]),
                float(quality.get("score", 0)),
                None,
                model_version,
            )
            self.db.execute(
                "INSERT INTO metric_snapshots (id,photo_id,user_id,model_version,blemish_count,redness_score,redness_delta,darkspot_area,texture_score,confidence,noise_floor_json) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                (
                    uid(),
                    capture["id"],
                    user_id,
                    result.model_version,
                    result.blemish_count,
                    result.redness_score,
                    result.redness_delta,
                    result.darkspot_area,
                    result.texture_score,
                    result.confidence,
                    dump(result.noise_floors),
                ),
            )
            processed += 1
        return {"processed_count": processed, "model_version": model_version}

    def reprocess(self, user_id: str, model_version: str) -> dict:
        self.require_premium(user_id, "Historical reprocessing")
        job_id = self.jobs.submit(
            "reprocess", self._run_reprocess, user_id, model_version, user_id=user_id,
        )
        self._audit(
            "reprocess_queued",
            "user",
            user_id,
            user_id,
            {"model_version": model_version},
        )
        return {"job_id": job_id, "status": "queued"}

    def reprocess_status(self, user_id: str, job_id: str) -> dict:
        self.require_user(user_id)
        job = self.jobs.get(job_id, user_id=user_id)
        if not job:
            raise ValueError("reprocess job not found")
        return job

    def export_user(self, user_id: str) -> dict:
        self.require_user(user_id)
        return {
            "export_version": "3",
            "exported_at": now_iso(),
            "profile": self.profile(user_id),
            "consent_events": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM consent_events WHERE user_id=?", (user_id,),
                )
            ],
            "appearance_profiles": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM appearance_profiles WHERE user_id=?", (user_id,),
                )
            ],
            "routine_events": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM routine_events WHERE user_id=?", (user_id,),
                )
            ],
            "experiments": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM experiments WHERE user_id=?", (user_id,),
                )
            ],
            "captures_and_metrics": self.history(user_id),
            "appearance_captures": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM appearance_captures WHERE user_id=?", (user_id,),
                )
            ],
            "verdicts": [
                self._decode_verdict(row)
                for row in self.db.fetchall(
                    "SELECT v.*,p.name AS product_name FROM verdicts v JOIN products p ON p.id=v.product_id WHERE v.user_id=?",
                    (user_id,),
                )
            ],
            "context_events": self.context_events(user_id),
            "check_ins": self.check_ins(user_id, limit=100),
            "measurement_feedback": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM measurement_feedback WHERE user_id=? ORDER BY created_at",
                    (user_id,),
                )
            ],
            "qna": self.qna_history(user_id) if self.is_premium(user_id) else [],
            "engagement": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM engagement_events WHERE user_id=?", (user_id,),
                )
            ],
            "note": "Raw photo bytes remain in the configured photo store and are exported through an authenticated object-store workflow.",
        }

    def ingredient_explainer(self, user_id: str, product_id: str) -> dict:
        self.require_premium(user_id, "Ingredient analysis")
        product = self.db.fetchone("SELECT * FROM products WHERE id=?", (product_id,))
        if not product:
            raise ValueError("product not found")
        return explain(product["name"], product["ingredients_json"])

    def product_detail(self, user_id: str, product_id: str) -> dict:
        product = self.db.fetchone("SELECT * FROM products WHERE id=?", (product_id,))
        if not product:
            raise ValueError("product not found")
        result = row_dict(product)
        result["offers"] = self.offers(user_id, product_id)
        result["ingredient_analysis"] = (
            self.ingredient_explainer(user_id, product_id)
            if self.is_premium(user_id)
            else None
        )
        return result

    def admin_audit(self, limit: int = 100) -> list[dict]:
        return [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?",
                (max(1, min(limit, 1000)),),
            )
        ]

    def triage_question(self, text: str) -> dict:
        return triage(text).as_dict()

    # Data Collection, Feedback, and Monitoring Methods

    def submit_capture_feedback(
        self,
        capture_id: str,
        user_id: str,
        feedback_type: str,
        issues: list[str] | None = None,
        corrections: dict[str, float] | None = None,
        comment: str | None = None,
    ) -> dict:
        """Submit user feedback on a capture."""
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        feedback_id = feedback_collector.submit_feedback(
            capture_id=capture_id,
            user_id=user_id,
            feedback_type=feedback_type,
            issues=issues,
            corrections=corrections,
            comment=comment,
        )

        return {
            "feedback_id": feedback_id,
            "message": "Feedback submitted successfully",
        }

    def get_feedback_stats(self) -> dict:
        """Get feedback statistics for admin."""
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        return feedback_collector.get_feedback_stats()

    def get_feedback_corrections(self, limit: int = 100) -> list[dict]:
        """Get feedback corrections for retraining."""
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        return feedback_collector.get_pending_corrections(limit=limit)

    def get_metric_accuracy_analysis(self) -> dict:
        """Get metric accuracy analysis."""
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        return feedback_collector.get_metric_accuracy_analysis()

    def get_model_health_status(self) -> dict:
        """Get model health monitoring status."""
        from .ml_monitoring import ModelMonitor

        monitor = ModelMonitor(self.db)
        return monitor.get_health_status()

    def generate_monitoring_daily_report(self) -> dict:
        """Generate daily monitoring report."""
        from .ml_monitoring import ModelMonitor

        monitor = ModelMonitor(self.db)
        return monitor.generate_daily_report()

    def get_collection_stats(self) -> dict:
        """Get data collection statistics."""
        from .data_collection import DataCollector

        collector = DataCollector(self.db)
        return collector.get_collection_stats()

    def export_training_dataset(
        self,
        output_dir: str,
        min_quality: float = 0.75,
        max_samples: int | None = None,
    ) -> dict:
        """Export collected data as training dataset."""
        from .data_collection import DataCollector

        collector = DataCollector(self.db)
        stats = collector.export_training_dataset(
            output_dir=output_dir,
            min_quality=min_quality,
            max_samples=max_samples,
        )

        return {
            "message": "Dataset exported successfully",
            "stats": stats,
        }

    def cleanup_old_data(self, retention_days: int = 365) -> dict:
        """Cleanup old collected data."""
        from .data_collection import DataCollector

        collector = DataCollector(self.db)
        deleted_count = collector.cleanup_old_data(retention_days=retention_days)

        return {
            "message": f"Cleaned up {deleted_count} old data files",
            "deleted_count": deleted_count,
        }

    def record_data_collection_consent(
        self,
        user_id: str,
        granted: bool,
        policy_version: str = "1.0",
    ) -> dict:
        """Record user consent for data collection."""
        self.db.execute(
            """
            INSERT INTO consent_events (
                user_id,
                consent_type,
                granted,
                policy_version
            ) VALUES (?, ?, ?, ?)
            """,
            (user_id, "data_collection", 1 if granted else 0, policy_version),
        )

        return {
            "message": "Consent recorded successfully",
            "user_id": user_id,
            "granted": granted,
        }
