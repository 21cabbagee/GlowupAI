"""Persistence helpers for validated AI evidence and provenance.

This module is intentionally provider agnostic.  It stores only application
contracts after validation and never accepts image bytes or provider payloads.
All reads that can be user visible require an owner id.
"""

from __future__ import annotations

import json
import uuid

try:
    from datetime import UTC, datetime
except ImportError:  # Python 3.9 compatibility for the local test runner
    from datetime import datetime, timezone

    UTC = timezone.utc
from typing import Any, Mapping


def _now() -> str:
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def _dump(value: Any) -> str:
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def _load(value: Any, default: Any = None) -> Any:
    if value is None:
        return default
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default


class AIStorage:
    """Small repository for versioned analyses and comparisons."""

    def __init__(self, db: Any) -> None:
        self.db = db

    def create_analysis(
        self,
        *,
        user_id: str,
        capture_id: str,
        task_type: str,
        request_key: str,
        schema_version: str,
        vision_provider: str,
        vision_model_id: str,
        vision_reasoning_effort: str = "medium",
        language_provider: str | None = None,
        language_model_id: str | None = None,
        language_reasoning_effort: str | None = None,
        language_fallback_provider: str | None = "openai",
        language_fallback_model_id: str | None = None,
        language_fallback_reasoning_effort: str | None = "medium",
        prompt_version: str,
        preprocessing_version: str,
        input_digest: str,
        image_input: Mapping[str, Any] | None = None,
        data_class: str,
        policy_decision: str,
        status: str = "queued",
        analysis_id: str | None = None,
    ) -> dict[str, Any]:
        """Create or return the durable work item for an idempotency key."""

        analysis_id = analysis_id or str(uuid.uuid4())
        self.db.execute(
            """
            INSERT INTO ai_analyses (
                id, user_id, capture_id, task_type, request_key, schema_version,
                vision_provider, vision_model_id, vision_reasoning_effort,
                language_provider, language_model_id, language_reasoning_effort,
                language_fallback_provider, language_fallback_model_id,
                language_fallback_reasoning_effort, prompt_version,
                preprocessing_version, input_digest, image_input_json, data_class, policy_decision,
                status, language_mode
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(request_key) DO NOTHING
            """,
            (
                analysis_id,
                user_id,
                capture_id,
                task_type,
                request_key,
                schema_version,
                vision_provider,
                vision_model_id,
                vision_reasoning_effort,
                language_provider,
                language_model_id,
                language_reasoning_effort,
                language_fallback_provider,
                language_fallback_model_id,
                language_fallback_reasoning_effort,
                prompt_version,
                preprocessing_version,
                input_digest,
                _dump(self._safe_image_input(image_input)),
                data_class,
                policy_decision,
                status,
                "unavailable",
            ),
        )
        row = self.db.fetchone(
            "SELECT * FROM ai_analyses WHERE request_key = ? AND user_id = ?",
            (request_key, user_id),
        )
        if row is None:
            raise RuntimeError("analysis work item could not be persisted")
        return self._analysis(dict(row))

    def get_analysis(self, analysis_id: str, user_id: str) -> dict[str, Any] | None:
        row = self.db.fetchone(
            "SELECT * FROM ai_analyses WHERE id = ? AND user_id = ?",
            (analysis_id, user_id),
        )
        return self._analysis(dict(row)) if row else None

    def get_analysis_by_request(
        self, request_key: str, user_id: str
    ) -> dict[str, Any] | None:
        row = self.db.fetchone(
            "SELECT * FROM ai_analyses WHERE request_key = ? AND user_id = ?",
            (request_key, user_id),
        )
        return self._analysis(dict(row)) if row else None

    def claim_running(self, analysis_id: str, user_id: str) -> bool:
        result = self.db.execute(
            "UPDATE ai_analyses SET status='running', safe_error_code=NULL "
            # Claiming is a compare-and-set.  A running item belongs to the
            # worker that claimed it; allowing another worker to update it
            # makes idempotency keys ineffective and can duplicate provider
            # calls.  Stale-job recovery must explicitly reset status to
            # queued before retrying.
            "WHERE id = ? AND user_id = ? AND status = 'queued'",
            (analysis_id, user_id),
        )
        return bool(result.rowcount)

    def save_vision(
        self,
        analysis_id: str,
        user_id: str,
        vision: Mapping[str, Any],
        *,
        status: str = "completed",
        error_code: str | None = None,
    ) -> bool:
        result = self.db.execute(
            "UPDATE ai_analyses SET validated_vision_json=?, status=?, "
            "safe_error_code=?, completed_at=? WHERE id=? AND user_id=?",
            (
                _dump(dict(vision)),
                status,
                error_code,
                (
                    _now()
                    if status in {"completed", "needs_retake", "failed", "unavailable"}
                    else None
                ),
                analysis_id,
                user_id,
            ),
        )
        return bool(result.rowcount)

    def update_image_input(
        self,
        analysis_id: str,
        user_id: str,
        image_input: Mapping[str, Any],
    ) -> bool:
        """Persist only canonical provider-derivative metadata."""
        result = self.db.execute(
            "UPDATE ai_analyses SET image_input_json=? WHERE id=? AND user_id=?",
            (_dump(self._safe_image_input(image_input)), analysis_id, user_id),
        )
        return bool(result.rowcount)

    def save_language(
        self,
        analysis_id: str,
        user_id: str,
        language: Mapping[str, Any],
        *,
        mode: str,
        provider: str | None = None,
        model_id: str | None = None,
        reasoning_effort: str | None = "medium",
    ) -> bool:
        result = self.db.execute(
            "UPDATE ai_analyses SET validated_language_json=?, language_mode=?, "
            "language_provider=COALESCE(?, language_provider), "
            "language_model_id=COALESCE(?, language_model_id), "
            "language_reasoning_effort=COALESCE(?, language_reasoning_effort), "
            "status='completed', safe_error_code=NULL, completed_at=? "
            "WHERE id=? AND user_id=?",
            (
                _dump(dict(language)),
                mode,
                provider,
                model_id,
                reasoning_effort,
                _now(),
                analysis_id,
                user_id,
            ),
        )
        return bool(result.rowcount)

    def mark_unavailable(
        self,
        analysis_id: str,
        user_id: str,
        *,
        mode: str = "unavailable",
        error_code: str,
        status: str = "unavailable",
    ) -> bool:
        result = self.db.execute(
            "UPDATE ai_analyses SET status=?, language_mode=?, safe_error_code=?, "
            "completed_at=? WHERE id=? AND user_id=?",
            (status, mode, error_code, _now(), analysis_id, user_id),
        )
        return bool(result.rowcount)

    def create_comparison(
        self,
        *,
        user_id: str,
        earlier_analysis_id: str,
        later_analysis_id: str,
        request_key: str,
        schema_version: str,
        vision_provider: str,
        vision_model_id: str,
        vision_reasoning_effort: str = "medium",
        language_provider: str | None = None,
        language_model_id: str | None = None,
        comparison: Mapping[str, Any] | None = None,
        language: Mapping[str, Any] | None = None,
        language_mode: str = "unavailable",
        comparison_id: str | None = None,
    ) -> dict[str, Any]:
        comparison_id = comparison_id or str(uuid.uuid4())
        self.db.execute(
            """
            INSERT INTO ai_comparisons (
                id, user_id, earlier_analysis_id, later_analysis_id, request_key,
                schema_version, vision_provider, vision_model_id,
                vision_reasoning_effort, language_provider, language_model_id,
                language_mode, validated_result_json, validated_language_json,
                completed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(request_key) DO NOTHING
            """,
            (
                comparison_id,
                user_id,
                earlier_analysis_id,
                later_analysis_id,
                request_key,
                schema_version,
                vision_provider,
                vision_model_id,
                vision_reasoning_effort,
                language_provider,
                language_model_id,
                language_mode,
                _dump(dict(comparison)) if comparison is not None else None,
                _dump(dict(language)) if language is not None else None,
                _now() if comparison is not None else None,
            ),
        )
        row = self.db.fetchone(
            "SELECT * FROM ai_comparisons WHERE request_key=? AND user_id=?",
            (request_key, user_id),
        )
        if row is None:
            raise RuntimeError("comparison work item could not be persisted")
        return self._comparison(dict(row))

    def save_comparison(
        self,
        comparison_id: str,
        user_id: str,
        comparison: Mapping[str, Any],
        *,
        language: Mapping[str, Any] | None = None,
        language_mode: str | None = None,
    ) -> bool:
        updates = ["validated_result_json=?", "completed_at=?"]
        params: list[Any] = [_dump(dict(comparison)), _now()]
        if language is not None:
            updates.append("validated_language_json=?")
            params.append(_dump(dict(language)))
        if language_mode is not None:
            updates.append("language_mode=?")
            params.append(language_mode)
        params.extend([comparison_id, user_id])
        result = self.db.execute(
            "UPDATE ai_comparisons SET "  # nosec B608
            + ", ".join(updates)
            + " WHERE id=? AND user_id=?",
            tuple(params),
        )
        return bool(result.rowcount)

    def get_comparison(self, comparison_id: str, user_id: str) -> dict[str, Any] | None:
        row = self.db.fetchone(
            "SELECT * FROM ai_comparisons WHERE id=? AND user_id=?",
            (comparison_id, user_id),
        )
        return self._comparison(dict(row)) if row else None

    def list_user_analyses(
        self, user_id: str, limit: int = 100
    ) -> list[dict[str, Any]]:
        limit = max(1, min(int(limit), 500))
        rows = self.db.fetchall(
            "SELECT * FROM ai_analyses WHERE user_id=? ORDER BY created_at DESC LIMIT ?",
            (user_id, limit),
        )
        return [self._analysis(dict(row)) for row in rows]

    def purge_user(self, user_id: str) -> None:
        """Explicitly remove AI records for soft-delete implementations."""

        # FKs handle this on hard deletion, but explicit deletes also support
        # account-deletion workflows that retain the users row for audit.
        self.db.execute("DELETE FROM ai_comparisons WHERE user_id=?", (user_id,))
        self.db.execute("DELETE FROM ai_analyses WHERE user_id=?", (user_id,))
        self.db.execute("DELETE FROM ai_usage_reservations WHERE user_id=?", (user_id,))
        self.db.execute("DELETE FROM ai_policy_consents WHERE user_id=?", (user_id,))

    def set_policy_consent(
        self,
        *,
        user_id: str,
        policy_version: str,
        scope: str,
        granted: bool,
    ) -> dict[str, Any]:
        """Record the latest consent decision for a provider/data scope."""

        now = _now()
        self.db.execute(
            """
            INSERT INTO ai_policy_consents
                (user_id, policy_version, scope, granted_at, revoked_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(user_id, policy_version, scope) DO UPDATE SET
                granted_at=excluded.granted_at,
                revoked_at=excluded.revoked_at
            """,
            (
                user_id,
                policy_version,
                scope,
                now if granted else None,
                None if granted else now,
            ),
        )
        row = self.db.fetchone(
            "SELECT * FROM ai_policy_consents WHERE user_id=? AND policy_version=? AND scope=?",
            (user_id, policy_version, scope),
        )
        if row is None:
            raise RuntimeError("AI policy consent could not be persisted")
        return dict(row)

    def get_policy_consent(
        self, *, user_id: str, policy_version: str, scope: str
    ) -> dict[str, Any] | None:
        row = self.db.fetchone(
            "SELECT * FROM ai_policy_consents WHERE user_id=? AND policy_version=? AND scope=?",
            (user_id, policy_version, scope),
        )
        return dict(row) if row else None

    @staticmethod
    def _safe_image_input(value: Mapping[str, Any] | None) -> dict[str, Any]:
        """Persist derivative metadata only; never accept image material."""

        if not isinstance(value, Mapping):
            return {}
        allowed = {
            "preprocessing_version",
            "width",
            "height",
            "bytes",
            "detail",
            "digest",
        }
        return {key: value[key] for key in allowed if key in value}

    @staticmethod
    def _analysis(row: dict[str, Any]) -> dict[str, Any]:
        row["validated_vision"] = _load(row.pop("validated_vision_json", None))
        row["validated_language"] = _load(row.pop("validated_language_json", None))
        row["image_input"] = _load(row.pop("image_input_json", None), {})
        return row

    @staticmethod
    def _comparison(row: dict[str, Any]) -> dict[str, Any]:
        row["validated_result"] = _load(row.pop("validated_result_json", None))
        row["validated_language"] = _load(row.pop("validated_language_json", None))
        return row


__all__ = ["AIStorage"]
