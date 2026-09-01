from __future__ import annotations

import json
import statistics
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any, Callable, Dict, List, Optional

from .attribution import parse_time
from .service import now_iso, row_dict

METRIC_LABELS = {
    "redness_score": "redness",
    "blemish_count": "blemish signs",
    "darkspot_area": "dark-spot area",
    "texture_score": "texture",
}
CHECK_IN_ROUTINE_STATES = {"steady", "changed", "missed", "not_sure"}
CHECK_IN_SKIN_FEELS = {"better", "same", "worse", "not_sure"}
CONTEXT_EVENT_TYPES = {
    "sleep",
    "travel",
    "weather",
    "cycle",
    "stress",
    "diet",
    "custom",
}


def uid() -> str:
    """Generate a unique identifier string.

    Returns:
        A UUID4 string representation.
    """
    return str(uuid.uuid4())


def dump(value: Any) -> str:
    """Serialize a Python value to a compact JSON string.

    Args:
        value: Any JSON-serializable Python object.

    Returns:
        Compact JSON string with sorted keys.
    """
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def load(value: Any, default: Optional[Any] = None) -> Any:
    """Deserialize a JSON string to a Python value.

    Args:
        value: JSON string to parse.
        default: Default value to return if parsing fails. Defaults to empty dict.

    Returns:
        Parsed Python object, or the default value if parsing fails.
    """
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default if default is not None else {}


def as_date(value: str) -> datetime:
    """Parse an ISO timestamp string to a UTC datetime.

    Args:
        value: ISO format timestamp string.

    Returns:
        Datetime object in UTC timezone.
    """
    parsed = parse_time(value)
    return parsed.astimezone(timezone.utc)


def day(value: datetime) -> str:
    """Convert a datetime to an ISO date string.

    Args:
        value: Datetime object to convert.

    Returns:
        ISO format date string (YYYY-MM-DD) in UTC.
    """
    return value.astimezone(timezone.utc).date().isoformat()


class AnalyticsService:
    """Metrics, insights, engagement tracking, and analytics."""

    def __init__(self, db: Any, parent_service: Any) -> None:
        """Initialize the AnalyticsService.

        Args:
            db: Database connection instance.
            parent_service: Parent GlowupAIService instance for delegating core operations.
        """
        self.db = db
        self.parent = parent_service

    def _active_product_windows(
        self, user_id: str, exclude_product_id: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """Products currently inside their post-start/change stabilization window."""

        rows = self.db.fetchall(
            """SELECT e.product_id, e.action, e.timestamp, p.name, p.stabilization_days
               FROM routine_events e JOIN products p ON p.id = e.product_id
               WHERE e.user_id = ? ORDER BY e.timestamp""",
            (user_id,),
        )
        by_product: Dict[str, List[Dict[str, Any]]] = {}
        for row in rows:
            if exclude_product_id and row["product_id"] == exclude_product_id:
                continue
            by_product.setdefault(row["product_id"], []).append(row_dict(row))
        now = datetime.now(timezone.utc)
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
                    }
                )
        return active

    def confound_check(
        self, user_id: str, exclude_product_id: Optional[str] = None
    ) -> Dict[str, Any]:
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
        timestamp: Optional[str] = None,
        slot: str = "unspecified",
        dose: Optional[str] = None,
        frequency: Optional[str] = None,
        notes: Optional[str] = None,
        experiment_id: Optional[str] = None,
        audit_fn: Optional[Callable] = None,
    ) -> Dict[str, Any]:
        """Record a routine event for product usage tracking.

        Tracks start, change, or stop actions for skincare products, checking for
        potential confounding factors with active stabilization windows.

        Args:
            user_id: ID of the user.
            product_id: ID of the product.
            action: Action type ('start', 'change', 'stop').
            timestamp: When the event occurred. Defaults to now.
            slot: Usage timing ('morning', 'evening', etc).
            dose: Dosage or amount used.
            frequency: Usage frequency.
            notes: Optional user notes.
            experiment_id: Optional experiment this event belongs to.
            audit_fn: Optional audit logging function.

        Returns:
            Dictionary with event data and confound warning if applicable.
        """
        confound_warning = (
            self.confound_check(user_id, exclude_product_id=product_id)
            if action in ("start", "change")
            else {"confounded": False, "active_windows": []}
        )
        # Call base service method directly to avoid infinite recursion
        event = GlowupAIService.add_routine_event(self.parent, 
            user_id, product_id, action, timestamp, slot, dose, frequency, notes
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
        if audit_fn:
            audit_fn(
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

    def _weekly_metric_summary(self, metric: str, first: Dict[str, Any], last: Dict[str, Any]) -> Dict[str, Any]:
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
        self, user_id: str, vertical: str = "skin", as_of: Optional[str] = None, history_fn: Optional[Callable] = None, check_ins_fn: Optional[Callable] = None
    ) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        history = history_fn(user_id, vertical) if history_fn else []
        checkins = check_ins_fn(user_id, limit=50) if check_ins_fn else []
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
                else [as_date(item["captured_at"]) for item in history]
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
                "body": "One more standardized capture will let GlowUpAI separate a broad trend from a single-frame impression.",
                "next_action": "Take the next guided capture",
                "capture_count": len(history),
                "check_in_count": len(checkins),
                "metric_summaries": [],
                "confidence_label": history[-1].get(
                    "confidence_label", "building signal"
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

    def check_ins(self, user_id: str, limit: int = 30) -> List[Dict[str, Any]]:
        self.parent.require_user(user_id)
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
        note: Optional[str] = None,
        occurred_at: Optional[str] = None,
        record_engagement_fn: Optional[Callable] = None,
    ) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        if routine_state not in CHECK_IN_ROUTINE_STATES:
            raise ValueError(
                f"routine_state must be one of {sorted(CHECK_IN_ROUTINE_STATES)}"
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
        if record_engagement_fn:
            record_engagement_fn(
                user_id,
                "check_in_completed",
                check_in_id,
                {"routine_state": routine_state, "skin_feel": skin_feel},
            )
        return row_dict(
            self.db.fetchone("SELECT * FROM check_ins WHERE id=?", (check_in_id,))
        )

    def record_engagement(
        self,
        user_id: str,
        event_type: str,
        reference_id: Optional[str] = None,
        metadata: Optional[Any] = None,
    ) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        event_id = uid()
        self.db.execute(
            "INSERT INTO engagement_events (id,user_id,event_type,reference_id,metadata_json) VALUES (?,?,?,?,?)",
            (event_id, user_id, event_type, reference_id, dump(metadata or {})),
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM engagement_events WHERE id=?", (event_id,))
        )

    def engagement(self, user_id: str, capture_guide_fn: Optional[Callable] = None) -> Dict[str, Any]:
        self.parent.require_user(user_id)
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
        guide = capture_guide_fn(user_id) if capture_guide_fn else {"next_window_start": now_iso()}
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
                    "SELECT * FROM reminders WHERE user_id=? AND enabled=1", (user_id,)
                )
            ],
        }

    def analytics(self, user_id: str, history_fn: Optional[Callable] = None) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        history = history_fn(user_id) if history_fn else []
        events = self.db.fetchall(
            "SELECT event_type FROM engagement_events WHERE user_id=?", (user_id,)
        )
        event_types = [row["event_type"] for row in events]
        verdicts = self.db.fetchall(
            "SELECT label FROM verdicts WHERE user_id=?", (user_id,)
        )
        labels = [row["label"] for row in verdicts]
        return {
            "activation": bool(
                any(item["is_baseline"] for item in history)
                and self.db.fetchone(
                    "SELECT id FROM routine_events WHERE user_id=?", (user_id,)
                )
            ),
            "baseline_capture": any(item["is_baseline"] for item in history),
            "first_three_captures": len(history) >= 3,
            "weekly_verdict_open_rate": round(
                event_types.count("verdict_open") / max(1, labels.__len__()), 3
            ),
            "verdict_action_rate": round(
                event_types.count("verdict_action") / max(1, labels.__len__()), 3
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
    def _median_history_days(history: List[Dict[str, Any]]) -> int:
        if len(history) < 2:
            return 0
        dates = [as_date(item["captured_at"]) for item in history]
        return int((max(dates) - min(dates)).days)

    def add_context_event(
        self,
        user_id: str,
        event_type: str,
        value: Optional[str] = None,
        occurred_at: Optional[str] = None,
        notes: Optional[str] = None,
    ) -> Dict[str, Any]:
        self.parent.require_user(user_id)
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
            self.db.fetchone("SELECT * FROM context_events WHERE id=?", (event_id,))
        )

    def context_events(self, user_id: str) -> List[Dict[str, Any]]:
        self.parent.require_user(user_id)
        return [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM context_events WHERE user_id=? ORDER BY occurred_at DESC",
                (user_id,),
            )
        ]

    def root_cause_search(
        self, user_id: str, metric: str = "texture_score", history_fn: Optional[Callable] = None, require_premium_fn: Optional[Callable] = None
    ) -> List[Dict[str, Any]]:
        if require_premium_fn:
            require_premium_fn(user_id, "Root-cause search")
        if metric not in {
            "blemish_count",
            "redness_score",
            "darkspot_area",
            "texture_score",
        }:
            raise ValueError("unsupported metric")
        history = history_fn(user_id) if history_fn else []
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
        by_type: Dict[str, List[float]] = {}
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
                    }
                )
        return sorted(
            correlations, key=lambda item: abs(item["normalized_effect"]), reverse=True
        )

    def summary(self, user_id: str) -> Dict[str, Any]:
        """Get analytics summary for a user.

        Args:
            user_id: ID of the user.

        Returns:
            Dictionary with summary analytics including total captures and engagement stats.
        """
        self.parent.require_user(user_id)

        # Get total captures
        captures = self.db.fetchall(
            "SELECT id FROM photo_captures WHERE user_id=? AND status='accepted'",
            (user_id,),
        )
        total_captures = len(captures)

        # Get engagement stats
        engagement_events = self.db.fetchall(
            "SELECT event_type FROM engagement_events WHERE user_id=?",
            (user_id,),
        )

        # Get routine events
        routine_events = self.db.fetchall(
            "SELECT action FROM routine_events WHERE user_id=?",
            (user_id,),
        )

        # Get check-ins
        checkins = self.db.fetchall(
            "SELECT id FROM check_ins WHERE user_id=?",
            (user_id,),
        )

        # Get verdicts
        verdicts = self.db.fetchall(
            "SELECT label FROM verdicts WHERE user_id=?",
            (user_id,),
        )

        return {
            "user_id": user_id,
            "total_captures": total_captures,
            "total_engagement_events": len(engagement_events),
            "total_routine_events": len(routine_events),
            "total_check_ins": len(checkins),
            "total_verdicts": len(verdicts),
        }

    def trends(self, user_id: str, vertical: str = "skin") -> Dict[str, Any]:
        """Get trends analysis for a user.

        Args:
            user_id: ID of the user.
            vertical: Vertical category (default: "skin").

        Returns:
            Dictionary with trends analysis including metric changes over time.
        """
        self.parent.require_user(user_id)

        # Get capture history
        history_rows = self.db.fetchall(
            """SELECT captured_at, redness_score, blemish_count, darkspot_area,
                      texture_score, confidence
               FROM photo_captures
               WHERE user_id=? AND status='accepted' AND vertical=?
               ORDER BY captured_at ASC""",
            (user_id, vertical),
        )

        history = [row_dict(row) for row in history_rows]

        if len(history) < 2:
            return {
                "user_id": user_id,
                "status": "insufficient_data",
                "message": "At least 2 captures needed for trend analysis",
                "total_captures": len(history),
                "trends": [],
            }

        # Calculate trends for each metric
        trends = []
        first = history[0]
        last = history[-1]

        for metric in ["redness_score", "blemish_count", "darkspot_area", "texture_score"]:
            if first.get(metric) is not None and last.get(metric) is not None:
                first_value = float(first[metric])
                last_value = float(last[metric])
                delta = last_value - first_value
                percent_change = (
                    ((last_value - first_value) / first_value * 100) if first_value != 0 else 0.0
                )

                direction = "improved" if delta < 0 else "increased" if delta > 0 else "steady"

                trends.append({
                    "metric": metric,
                    "label": METRIC_LABELS.get(metric, metric),
                    "first_value": round(first_value, 3),
                    "last_value": round(last_value, 3),
                    "delta": round(delta, 3),
                    "percent_change": round(percent_change, 2),
                    "direction": direction,
                })

        return {
            "user_id": user_id,
            "status": "success",
            "total_captures": len(history),
            "period": {
                "start": first["captured_at"],
                "end": last["captured_at"],
            },
            "trends": trends,
        }
