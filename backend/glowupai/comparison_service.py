"""Owner-scoped comparable-photo progress orchestration."""

from __future__ import annotations

from datetime import UTC
from typing import Any

from .attribution import parse_time
from .ai_orchestrator import AIOrchestrationError


class ComparisonService:
    """Select two owned captures and delegate comparison to Luna orchestration."""

    def __init__(self, db: Any, parent_service: Any, photos: Any) -> None:
        self.db = db
        self.parent = parent_service
        self.photos = photos

    def _capture(self, user_id: str, capture_id: str) -> dict[str, Any]:
        row = self.db.fetchone(
            "SELECT * FROM photo_captures WHERE id=? AND user_id=?",
            (capture_id, user_id),
        )
        if not row:
            raise ValueError("capture not found")
        return dict(row)

    def compare(
        self,
        user_id: str,
        earlier_capture_id: str,
        later_capture_id: str,
        vertical: str = "skin",
    ) -> dict[str, Any]:
        self.parent.require_user(user_id)
        if vertical != "skin":
            raise ValueError("vertical must be skin")
        if earlier_capture_id == later_capture_id:
            raise ValueError("comparison requires two different captures")
        earlier = self._capture(user_id, earlier_capture_id)
        later = self._capture(user_id, later_capture_id)
        earlier_at = parse_time(earlier["captured_at"]).astimezone(UTC)
        later_at = parse_time(later["captured_at"]).astimezone(UTC)
        if earlier_at >= later_at:
            raise ValueError("earlier capture must precede later capture")
        age_days = (later_at - earlier_at).total_seconds() / 86400
        if age_days < 1 or age_days > 365:
            raise ValueError("captures must be between 1 and 365 days apart")

        orchestrator = getattr(self.parent, "ai_orchestrator", None)
        if orchestrator is None:
            return {
                "comparison_id": None,
                "status": "unavailable",
                "reason": "provider_disabled",
                "comparable": False,
                "reasons": ["AI comparison is not enabled for this deployment."],
                "changes": [],
                "language": None,
            }
        analyses = []
        for capture in (earlier, later):
            analysis = self.db.fetchone(
                "SELECT id,status FROM ai_analyses WHERE capture_id=? AND user_id=? "
                "ORDER BY created_at DESC LIMIT 1",
                (capture["id"], user_id),
            )
            if not analysis or analysis["status"] != "completed":
                return {
                    "comparison_id": None,
                    "status": "unavailable",
                    "reason": "analysis_unavailable",
                    "comparable": False,
                    "reasons": [
                        "Both captures need completed observations before comparison."
                    ],
                    "changes": [],
                    "language": None,
                }
            analyses.append(str(analysis["id"]))

        try:
            result = orchestrator.run_comparison_analysis(
                user_id=user_id,
                earlier_analysis_id=analyses[0],
                later_analysis_id=analyses[1],
                earlier_image_bytes=self.photos.read(earlier["raw_ref"]),
                later_image_bytes=self.photos.read(later["raw_ref"]),
                data_class="personal_face",
            )
        except AIOrchestrationError as exc:
            return {
                "comparison_id": None,
                "status": "unavailable",
                "reason": exc.code,
                "comparable": False,
                "reasons": ["The comparison is temporarily unavailable."],
                "changes": [],
                "language": None,
            }
        comparison = result.get("comparison") or {}
        return {
            "comparison_id": result.get("comparison_id"),
            "status": "completed",
            "earlier_capture_id": earlier_capture_id,
            "later_capture_id": later_capture_id,
            "captured_at": {
                "earlier": earlier["captured_at"],
                "later": later["captured_at"],
            },
            "comparison": comparison,
            "comparable": bool(comparison.get("comparable", False)),
            "reasons": comparison.get("reasons", []),
            "changes": comparison.get("changes", []),
            "language": result.get("language"),
            "language_mode": result.get("language_mode", "unavailable"),
        }

    def detail(self, user_id: str, comparison_id: str) -> dict[str, Any]:
        self.parent.require_user(user_id)
        row = self.db.fetchone(
            "SELECT * FROM ai_comparisons WHERE id=? AND user_id=?",
            (comparison_id, user_id),
        )
        if not row:
            raise ValueError("comparison not found")
        result = dict(row)
        # Keep detail responses shape-compatible with POST responses while
        # retaining no internal owner/analysis/image references.
        result["comparison_id"] = result.pop("id", comparison_id)
        import json

        result["comparison"] = (
            json.loads(result.pop("validated_result_json"))
            if result.get("validated_result_json")
            else None
        )
        result["language"] = (
            json.loads(result.pop("validated_language_json"))
            if result.get("validated_language_json")
            else None
        )
        result.setdefault(
            "status", "completed" if result.get("comparison") else "unavailable"
        )
        # Do not expose analysis internals or image references from this route.
        result.pop("user_id", None)
        result.pop("earlier_analysis_id", None)
        result.pop("later_analysis_id", None)
        result.pop("request_key", None)
        return result
