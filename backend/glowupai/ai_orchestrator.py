"""Durable, policy-aware Luna -> Gemini -> Luna orchestration.

The orchestrator owns provider order and retry boundaries.  Adapters are
injected protocols, which keeps tests offline and prevents request handlers
from selecting arbitrary models or passing raw images to Gemini.
"""

from __future__ import annotations

import hashlib
import json
import logging
from dataclasses import dataclass
from typing import Any, Mapping, Protocol

from .ai_contracts import (
    DataClass,
    EvidencePacket,
    LanguageMode,
    LanguageResponse,
    ProviderResponse,
    ProviderUsage,
    VisionComparison,
    VisionObservation,
    parse_comparison,
    parse_language_response,
    parse_product_scan,
    parse_vision_observation,
)
from .ai_policy import PolicyEvaluation, evaluate_gemini
from .ai_quota import (
    AIQuotaManager,
    QuotaExceeded,
    QuotaLimits,
    UsageReservation,
    utc_day_window,
    utc_month_window,
)
from .ai_storage import AIStorage

logger = logging.getLogger(__name__)


class VisionAdapter(Protocol):
    def analyze_image(self, image_bytes: bytes, **kwargs: Any) -> Any: ...


class LanguageAdapter(Protocol):
    def generate_text(self, evidence: Mapping[str, Any], **kwargs: Any) -> Any: ...


@dataclass(frozen=True)
class OrchestrationResult:
    analysis: dict[str, Any]
    vision: VisionObservation
    language: LanguageResponse | None
    language_mode: LanguageMode
    gemini_policy: PolicyEvaluation


class AIOrchestrationError(RuntimeError):
    def __init__(
        self, code: str, message: str, *, provider_invoked: bool = False
    ) -> None:
        super().__init__(message)
        self.code = code
        self.provider_invoked = provider_invoked


class AIOrchestrator:
    """Run one bounded analysis task and persist every stage."""

    def __init__(
        self,
        *,
        luna: VisionAdapter | None,
        gemini: LanguageAdapter | None,
        db: Any,
        settings: Any = None,
        storage: AIStorage | None = None,
        quota: AIQuotaManager | None = None,
        local_renderer: Any | None = None,
        consent_checker: Any | None = None,
    ) -> None:
        self.luna = luna
        self.gemini = gemini
        self.db = db
        self.settings = settings
        self.storage = storage or AIStorage(db)
        self.quota = quota or AIQuotaManager(db)
        self.local_renderer = local_renderer
        self.consent_checker = consent_checker

    @property
    def luna_enabled(self) -> bool:
        return bool(
            self.luna is not None and getattr(self.settings, "luna_enabled", True)
        )

    @property
    def luna_model(self) -> str:
        return getattr(self.settings, "luna_model", "gpt-5.6-luna")

    @property
    def luna_reasoning_effort(self) -> str:
        # Medium is a contract invariant, not an adjustable request option.
        effort = getattr(self.settings, "luna_reasoning_effort", "medium")
        if effort != "medium":
            raise RuntimeError("Luna reasoning effort must be medium")
        return effort

    @property
    def gemini_enabled(self) -> bool:
        return bool(
            self.gemini is not None and getattr(self.settings, "gemini_enabled", True)
        )

    def run_capture_analysis(
        self,
        *,
        user_id: str,
        capture_id: str,
        image_bytes: bytes,
        mime_type: str = "image/jpeg",
        image_input: Mapping[str, Any] | None = None,
        data_class: DataClass | str = DataClass.PERSONAL_FACE,
        task_type: str = "vision_observation",
        prompt_version: str = "skin-vision-v1",
        preprocessing_version: str = "face-derivative-v1",
        input_digest: str | None = None,
    ) -> OrchestrationResult:
        """Analyze one owned capture; ``image_bytes`` is passed only to Luna."""

        if not image_bytes:
            raise ValueError("image_bytes cannot be empty")
        self._require_active_owner(user_id)
        digest = input_digest or hashlib.sha256(image_bytes).hexdigest()
        request_key = self._request_key(
            user_id,
            capture_id,
            task_type,
            digest,
            prompt_version,
            preprocessing_version,
        )
        metadata = dict(image_input or {})
        metadata.setdefault("preprocessing_version", preprocessing_version)
        metadata.setdefault("width", 0)
        metadata.setdefault("height", 0)
        metadata.setdefault("bytes", len(image_bytes))
        metadata.setdefault(
            "detail", getattr(self.settings, "luna_image_detail", "low")
        )
        metadata["digest"] = digest
        created = self.storage.create_analysis(
            user_id=user_id,
            capture_id=capture_id,
            task_type=task_type,
            request_key=request_key,
            schema_version="cosmetic-observation-v1",
            vision_provider="openai",
            vision_model_id=self.luna_model,
            vision_reasoning_effort=self.luna_reasoning_effort,
            language_provider="gemini" if self.gemini_enabled else None,
            language_model_id=(
                getattr(self.settings, "gemini_model", None)
                if self.gemini_enabled
                else None
            ),
            # Gemini has its own generation controls; the ``medium``
            # reasoning invariant applies only to Luna requests.
            language_reasoning_effort=None,
            language_fallback_provider="openai",
            language_fallback_model_id=self.luna_model,
            language_fallback_reasoning_effort="medium",
            prompt_version=prompt_version,
            preprocessing_version=preprocessing_version,
            input_digest=digest,
            image_input=metadata,
            data_class=getattr(data_class, "value", str(data_class)),
            policy_decision="pending",
        )
        if created.get("status") in {"completed", "needs_retake"} and created.get(
            "validated_vision"
        ):
            vision = VisionObservation.model_validate(created["validated_vision"])
            language = (
                LanguageResponse.model_validate(created["validated_language"])
                if created.get("validated_language")
                else None
            )
            return OrchestrationResult(
                created,
                vision,
                language,
                LanguageMode(created.get("language_mode", "unavailable")),
                self._gemini_policy(data_class),
            )
        if not self.luna_enabled:
            self.storage.mark_unavailable(
                created["id"], user_id, error_code="provider_disabled"
            )
            raise AIOrchestrationError("provider_disabled", "Luna vision is disabled")
        if not self._consent_allowed(user_id, "luna_face"):
            self.storage.mark_unavailable(
                created["id"], user_id, error_code="consent_required"
            )
            raise AIOrchestrationError(
                "consent_required", "AI image analysis consent is required"
            )
        if not self.storage.claim_running(created["id"], user_id):
            latest = self.storage.get_analysis(created["id"], user_id)
            if latest and latest.get("validated_vision"):
                vision = VisionObservation.model_validate(latest["validated_vision"])
                language = (
                    LanguageResponse.model_validate(latest["validated_language"])
                    if latest.get("validated_language")
                    else None
                )
                return OrchestrationResult(
                    latest,
                    vision,
                    language,
                    LanguageMode(latest.get("language_mode", "unavailable")),
                    self._gemini_policy(data_class),
                )
            # A duplicate worker must never proceed past this point.  The
            # unique request key makes the work item idempotent, but without
            # this guard two workers could both call Luna after one has
            # already claimed a queued/running row.  Retries are represented
            # by a new request identity (or an explicit job reset), never by
            # silently replaying an in-flight request.
            status = (latest or {}).get("status", "running")
            if status in {"queued", "running"}:
                raise AIOrchestrationError(
                    "analysis_in_progress",
                    "This analysis is already being processed",
                )
            if status in {"failed", "unavailable", "needs_retake"}:
                raise AIOrchestrationError(
                    (latest or {}).get("safe_error_code") or "analysis_unavailable",
                    "This analysis is no longer available for replay",
                )

        try:
            reservation = self._reserve_luna(
                user_id=user_id,
                request_identity=request_key,
                task=task_type,
                fallback=False,
            )
        except AIOrchestrationError as exc:
            self.storage.mark_unavailable(created["id"], user_id, error_code=exc.code)
            raise
        try:
            response = self._vision_call(
                image_bytes,
                mime_type=mime_type,
                task_type=task_type,
                detail=metadata["detail"],
                prompt_version=prompt_version,
            )
            provider_image = getattr(self.luna, "last_image_input", None)
            if isinstance(provider_image, Mapping):
                self.storage.update_image_input(created["id"], user_id, provider_image)
            vision = parse_vision_observation(response.output)
            self._reconcile(reservation, response.usage)
        except Exception as exc:  # provider and schema errors are safe to persist
            if reservation:
                if isinstance(exc, AIOrchestrationError) and not exc.provider_invoked:
                    self.quota.fail_without_call(reservation.id, error_code=exc.code)
                else:
                    self.quota.mark_ambiguous(
                        reservation.id, error_code="vision_failed"
                    )
            code = self._safe_error_code(exc)
            self.storage.mark_unavailable(
                created["id"], user_id, error_code=code, status="failed"
            )
            raise AIOrchestrationError(
                code, "Luna vision analysis failed", provider_invoked=True
            ) from exc

        status = "completed" if vision.quality.usable else "needs_retake"
        self.storage.save_vision(
            created["id"],
            user_id,
            vision.model_dump(mode="json"),
            status=status,
            error_code=None if status == "completed" else "needs_retake",
        )
        policy = self._gemini_policy(data_class)
        self.db.execute(
            "UPDATE ai_analyses SET policy_decision=? WHERE id=? AND user_id=?",
            (policy.decision.value, created["id"], user_id),
        )
        if status != "completed":
            latest = self.storage.get_analysis(created["id"], user_id) or created
            return OrchestrationResult(
                latest, vision, None, LanguageMode.UNAVAILABLE, policy
            )

        evidence = self._evidence_packet(created["id"], vision)
        language, mode = self._language_stage(
            user_id=user_id,
            analysis_id=created["id"],
            evidence=evidence,
            policy=policy,
            task_type=task_type,
        )
        latest = self.storage.get_analysis(created["id"], user_id) or created
        return OrchestrationResult(latest, vision, language, mode, policy)

    def run_language(
        self,
        *,
        user_id: str,
        evidence: Mapping[str, Any] | EvidencePacket,
        task_type: str,
        data_class: DataClass | str = DataClass.PERSONAL_HISTORY,
        request_identity: str,
    ) -> dict[str, Any]:
        """Run Gemini text with one evidence-only Luna fallback.

        This is the entry point for Q&A, routine guidance and weekly recaps
        whose validated evidence already exists.  It does not persist a new
        analysis row or accept image bytes; callers persist conversation data
        in their existing domain tables.
        """

        self._require_active_owner(user_id)

        packet = (
            evidence
            if isinstance(evidence, EvidencePacket)
            else EvidencePacket.model_validate(evidence)
        )
        policy = self._gemini_policy(data_class)
        allowed_ids = set(packet.evidence_ids)
        if self.gemini_enabled and policy.allowed:
            reservation: UsageReservation | None
            try:
                reservation = self._reserve_gemini(
                    user_id, f"{request_identity}:gemini", task_type
                )
            except AIOrchestrationError:
                reservation = None
            try:
                if reservation is not None:
                    response = self._language_call(
                        self.gemini, packet, task_type=task_type
                    )
                    language = parse_language_response(response.output, allowed_ids)
                    self._reconcile(reservation, response.usage)
                    return {
                        "answer": language.model_dump(mode="json"),
                        "language_mode": LanguageMode.GEMINI_PRIMARY.value,
                        "policy": policy.decision.value,
                    }
            except Exception:
                if reservation:
                    self.quota.mark_ambiguous(
                        reservation.id, error_code="language_failed"
                    )
        if (
            policy.allowed
            and self.luna_enabled
            and getattr(self.settings, "luna_reasoning_fallback", True)
        ):
            try:
                reservation = self._reserve_luna(
                    user_id=user_id,
                    request_identity=f"{request_identity}:luna-fallback",
                    task="text_reasoning_fallback",
                    fallback=True,
                )
            except AIOrchestrationError:
                reservation = None
            try:
                if reservation is not None:
                    response = self._language_call(
                        self.luna,
                        packet,
                        task_type="text_reasoning_fallback",
                    )
                    language = parse_language_response(response.output, allowed_ids)
                    self._reconcile(reservation, response.usage)
                    return {
                        "answer": language.model_dump(mode="json"),
                        "language_mode": LanguageMode.LUNA_FALLBACK.value,
                        "policy": policy.decision.value,
                    }
            except Exception:
                if reservation:
                    self.quota.mark_ambiguous(
                        reservation.id, error_code="luna_fallback_failed"
                    )
        local_language = self._local_language(packet, request_identity)
        return {
            "answer": (
                local_language.model_dump(mode="json") if local_language else None
            ),
            "language_mode": (
                LanguageMode.LOCAL_FALLBACK.value
                if local_language
                else LanguageMode.UNAVAILABLE.value
            ),
            "policy": policy.decision.value,
        }

    def run_product_scan(
        self,
        *,
        user_id: str,
        image_bytes: bytes,
        request_identity: str,
    ) -> dict[str, Any]:
        """Run a bounded, product-only label scan through paid Luna.

        Product scans are image tasks too, so they share the same derivative,
        medium-reasoning and spend reservation guarantees as face observations.
        The returned draft is never written to a routine until the user
        confirms it.
        """
        if not image_bytes:
            raise ValueError("product scan image cannot be empty")
        self._require_active_owner(user_id)
        if not self.luna_enabled:
            raise AIOrchestrationError("provider_disabled", "Luna vision is disabled")
        reservation = self._reserve_luna(
            user_id=user_id,
            request_identity=request_identity,
            task="product_label_scan",
            fallback=False,
        )
        try:
            method = getattr(self.luna, "extract_products_with_metadata", None)
            if method is None:
                method = getattr(self.luna, "extract_products", None)
            if method is None:
                raise AIOrchestrationError(
                    "model_unavailable", "Luna adapter has no product method"
                )
            raw = method(
                image_bytes,
                detail=getattr(self.settings, "luna_label_detail", "auto"),
                reasoning_effort=self.luna_reasoning_effort,
            )
            # Adapters that expose the legacy list-only method are wrapped in
            # the same provider response contract without changing behavior.
            if isinstance(raw, list):
                raw = {"products": raw}
            response = self._normalize_response(raw)
            scan = parse_product_scan(response.output)
            self._reconcile(reservation, response.usage)
            return {
                "products": [
                    candidate.model_dump(mode="json") for candidate in scan.products
                ],
                "limitations": scan.limitations,
                "provider": "openai",
                "model_id": self.luna_model,
                "reasoning_effort": "medium",
            }
        except Exception as exc:
            if isinstance(exc, AIOrchestrationError) and not exc.provider_invoked:
                self.quota.fail_without_call(reservation.id, error_code=exc.code)
            else:
                self.quota.mark_ambiguous(
                    reservation.id, error_code="product_scan_failed"
                )
            if isinstance(exc, AIOrchestrationError):
                raise
            raise AIOrchestrationError(
                "product_scan_failed", "Luna product scan failed", provider_invoked=True
            ) from exc

    def _language_stage(
        self,
        *,
        user_id: str,
        analysis_id: str,
        evidence: EvidencePacket,
        policy: PolicyEvaluation,
        task_type: str,
    ) -> tuple[LanguageResponse | None, LanguageMode]:
        allowed_ids = set(evidence.evidence_ids)
        if self.gemini_enabled and policy.allowed:
            try:
                reservation = self._reserve_gemini(
                    user_id, f"{analysis_id}:gemini", task_type
                )
            except AIOrchestrationError as exc:
                # Quota denial is a Gemini failure and must enter the single
                # Luna text fallback path without spending another image call.
                logger.info(
                    "Gemini reservation denied; trying Luna text fallback",
                    extra={"code": exc.code},
                )
                reservation = None
            try:
                if reservation is not None:
                    response = self._language_call(
                        self.gemini, evidence, task_type=task_type
                    )
                    language = parse_language_response(response.output, allowed_ids)
                    self._reconcile(reservation, response.usage)
                    self.storage.save_language(
                        analysis_id,
                        user_id,
                        language.model_dump(mode="json"),
                        mode=LanguageMode.GEMINI_PRIMARY.value,
                        provider="gemini",
                        model_id=getattr(self.settings, "gemini_model", None),
                        reasoning_effort=None,
                    )
                    return language, LanguageMode.GEMINI_PRIMARY
            except Exception as exc:
                if reservation:
                    if (
                        isinstance(exc, AIOrchestrationError)
                        and not exc.provider_invoked
                    ):
                        self.quota.fail_without_call(
                            reservation.id, error_code=exc.code
                        )
                    else:
                        self.quota.mark_ambiguous(
                            reservation.id, error_code="language_failed"
                        )
                logger.info(
                    "Gemini language stage failed; trying Luna text fallback",
                    extra={"code": self._safe_error_code(exc)},
                )
        # A policy restriction is intentionally local. It must
        # not be bypassed by forwarding user-derived evidence to another API.
        if not policy.allowed:
            local_language = self._local_language(evidence, analysis_id)
            if local_language:
                self.storage.save_language(
                    analysis_id,
                    user_id,
                    local_language.model_dump(mode="json"),
                    mode=LanguageMode.LOCAL_FALLBACK.value,
                    provider=None,
                    model_id=None,
                    reasoning_effort=None,
                )
                return local_language, LanguageMode.LOCAL_FALLBACK
            return None, LanguageMode.UNAVAILABLE

        if self.luna_enabled and getattr(
            self.settings, "luna_reasoning_fallback", True
        ):
            try:
                reservation = self._reserve_luna(
                    user_id=user_id,
                    request_identity=f"{analysis_id}:luna-fallback",
                    task="text_reasoning_fallback",
                    fallback=True,
                )
            except AIOrchestrationError as exc:
                logger.info(
                    "Luna fallback reservation denied", extra={"code": exc.code}
                )
                reservation = None
            try:
                if reservation is not None:
                    response = self._language_call(
                        self.luna,
                        evidence,
                        task_type="text_reasoning_fallback",
                    )
                    language = parse_language_response(response.output, allowed_ids)
                    self._reconcile(reservation, response.usage)
                    self.storage.save_language(
                        analysis_id,
                        user_id,
                        language.model_dump(mode="json"),
                        mode=LanguageMode.LUNA_FALLBACK.value,
                        provider="openai",
                        model_id=self.luna_model,
                        reasoning_effort="medium",
                    )
                    return language, LanguageMode.LUNA_FALLBACK
            except Exception as exc:
                if reservation:
                    if (
                        isinstance(exc, AIOrchestrationError)
                        and not exc.provider_invoked
                    ):
                        self.quota.fail_without_call(
                            reservation.id, error_code=exc.code
                        )
                    else:
                        self.quota.mark_ambiguous(
                            reservation.id, error_code="luna_fallback_failed"
                        )
                logger.info(
                    "Luna language fallback failed",
                    extra={"code": self._safe_error_code(exc)},
                )

        local_language = self._local_language(evidence, analysis_id)
        if local_language:
            self.storage.save_language(
                analysis_id,
                user_id,
                local_language.model_dump(mode="json"),
                mode=LanguageMode.LOCAL_FALLBACK.value,
                provider=None,
                model_id=None,
                reasoning_effort=None,
            )
            return local_language, LanguageMode.LOCAL_FALLBACK
        return None, LanguageMode.UNAVAILABLE

    def run_comparison_analysis(
        self,
        *,
        user_id: str,
        earlier_analysis_id: str,
        later_analysis_id: str,
        earlier_image_bytes: bytes,
        later_image_bytes: bytes,
        data_class: DataClass | str = DataClass.PERSONAL_FACE,
        prompt_version: str = "skin-comparison-v1",
        preprocessing_version: str = "face-derivative-v1",
        earlier_digest: str | None = None,
        later_digest: str | None = None,
    ) -> dict[str, Any]:
        """Compare two owned captures with one Luna call and bounded language.

        The caller must perform ownership and comparable-photo selection before
        invoking this method.  This method never sends either image to Gemini.
        """

        if not earlier_image_bytes or not later_image_bytes:
            raise ValueError("comparison images cannot be empty")
        self._require_active_owner(user_id)
        earlier_digest = (
            earlier_digest or hashlib.sha256(earlier_image_bytes).hexdigest()
        )
        later_digest = later_digest or hashlib.sha256(later_image_bytes).hexdigest()
        request_key = hashlib.sha256(
            "|".join(
                (
                    user_id,
                    earlier_analysis_id,
                    later_analysis_id,
                    earlier_digest,
                    later_digest,
                    prompt_version,
                    preprocessing_version,
                )
            ).encode("utf-8")
        ).hexdigest()
        existing = self.db.fetchone(
            "SELECT * FROM ai_comparisons WHERE request_key=? AND user_id=?",
            (request_key, user_id),
        )
        if existing:
            row = dict(existing)
            result = (
                json.loads(row["validated_result_json"])
                if row.get("validated_result_json")
                else None
            )
            persisted_language = (
                json.loads(row["validated_language_json"])
                if row.get("validated_language_json")
                else None
            )
            return {
                "comparison": result,
                "language": persisted_language,
                "language_mode": row.get("language_mode", "unavailable"),
                "comparison_id": row["id"],
            }
        created = self.storage.create_comparison(
            user_id=user_id,
            earlier_analysis_id=earlier_analysis_id,
            later_analysis_id=later_analysis_id,
            request_key=request_key,
            schema_version="cosmetic-comparison-v1",
            vision_provider="openai",
            vision_model_id=self.luna_model,
            vision_reasoning_effort=self.luna_reasoning_effort,
            language_provider="gemini",
            language_model_id=getattr(self.settings, "gemini_model", None),
        )
        if not self.luna_enabled:
            raise AIOrchestrationError("provider_disabled", "Luna vision is disabled")
        if not self._consent_allowed(user_id, "luna_face"):
            raise AIOrchestrationError(
                "consent_required", "AI image analysis consent is required"
            )
        reservation = self._reserve_luna(
            user_id=user_id,
            request_identity=request_key,
            task="comparison",
            fallback=False,
        )
        try:
            response = self._comparison_call(
                earlier_image_bytes,
                later_image_bytes,
                prompt_version=prompt_version,
                detail=getattr(self.settings, "luna_image_detail", "low"),
            )
            comparison = parse_comparison(response.output)
            self._reconcile(reservation, response.usage)
        except Exception as exc:
            if isinstance(exc, AIOrchestrationError) and not exc.provider_invoked:
                self.quota.fail_without_call(reservation.id, error_code=exc.code)
            else:
                self.quota.mark_ambiguous(
                    reservation.id, error_code="comparison_failed"
                )
            raise AIOrchestrationError(
                "comparison_failed", "Luna comparison failed", provider_invoked=True
            ) from exc
        self.storage.save_comparison(
            created["id"], user_id, comparison.model_dump(mode="json")
        )
        evidence = EvidencePacket(
            evidence_ids=[earlier_analysis_id, later_analysis_id, created["id"]],
            evidence=[
                {
                    "id": created["id"],
                    "type": "comparison",
                    "value": comparison.model_dump(mode="json"),
                }
            ],
        )
        policy = self._gemini_policy(data_class)
        language: LanguageResponse | None = None
        mode = LanguageMode.UNAVAILABLE
        if self.gemini_enabled and policy.allowed:
            try:
                gemini_reservation = self._reserve_gemini(
                    user_id, f"{created['id']}:gemini", "comparison"
                )
            except AIOrchestrationError:
                gemini_reservation = None
            try:
                if gemini_reservation is not None:
                    response = self._language_call(
                        self.gemini, evidence, task_type="comparison"
                    )
                    language = parse_language_response(
                        response.output, set(evidence.evidence_ids)
                    )
                    self._reconcile(gemini_reservation, response.usage)
                    mode = LanguageMode.GEMINI_PRIMARY
            except Exception:
                if gemini_reservation:
                    self.quota.mark_ambiguous(
                        gemini_reservation.id, error_code="language_failed"
                    )
        if (
            language is None
            and policy.allowed
            and self.luna_enabled
            and getattr(self.settings, "luna_reasoning_fallback", True)
        ):
            try:
                fallback_reservation = self._reserve_luna(
                    user_id=user_id,
                    request_identity=f"{created['id']}:luna-fallback",
                    task="text_reasoning_fallback",
                    fallback=True,
                )
            except AIOrchestrationError:
                fallback_reservation = None
            try:
                if fallback_reservation is not None:
                    response = self._language_call(
                        self.luna,
                        evidence,
                        task_type="text_reasoning_fallback",
                    )
                    language = parse_language_response(
                        response.output, set(evidence.evidence_ids)
                    )
                    self._reconcile(fallback_reservation, response.usage)
                    mode = LanguageMode.LUNA_FALLBACK
            except Exception:
                if fallback_reservation:
                    self.quota.mark_ambiguous(
                        fallback_reservation.id, error_code="luna_fallback_failed"
                    )
        if language is None:
            language = LanguageResponse(
                answer=(
                    "These photos are comparable."
                    if comparison.comparable
                    else "These photos cannot be compared reliably."
                ),
                evidence_ids=[created["id"]],
                limitations=comparison.reasons[:3],
            )
            mode = LanguageMode.LOCAL_FALLBACK
        self.storage.save_comparison(
            created["id"],
            user_id,
            comparison.model_dump(mode="json"),
            language=language.model_dump(mode="json"),
            language_mode=mode.value,
        )
        return {
            "comparison_id": created["id"],
            "comparison": comparison.model_dump(mode="json"),
            "language": language.model_dump(mode="json"),
            "language_mode": mode.value,
            "policy": policy.decision.value,
        }

    def _vision_call(self, image_bytes: bytes, **kwargs: Any) -> ProviderResponse[Any]:
        if self.luna is None:
            raise AIOrchestrationError(
                "provider_disabled", "Luna adapter is unavailable"
            )
        method = getattr(self.luna, "analyze_image", None) or getattr(
            self.luna, "analyze", None
        )
        if method is None:
            raise AIOrchestrationError(
                "model_unavailable", "Luna adapter has no image method"
            )
        try:
            # Keep the model-quality contract explicit at the orchestration
            # boundary.  The adapter also validates this value, but callers
            # should never accidentally fall back to its default.
            kwargs.setdefault("reasoning_effort", self.luna_reasoning_effort)
            return self._normalize_response(method(image_bytes, **kwargs))
        except Exception as exc:
            raise AIOrchestrationError(
                "provider_error", "Luna image request failed", provider_invoked=True
            ) from exc

    def _comparison_call(
        self, earlier: bytes, later: bytes, **kwargs: Any
    ) -> ProviderResponse[Any]:
        if self.luna is None:
            raise AIOrchestrationError(
                "provider_disabled", "Luna adapter is unavailable"
            )
        method = (
            getattr(self.luna, "compare_images", None)
            or getattr(self.luna, "analyze_images", None)
            or getattr(self.luna, "compare", None)
        )
        if method is None:
            raise AIOrchestrationError(
                "model_unavailable", "Luna adapter has no comparison method"
            )
        try:
            kwargs.setdefault("reasoning_effort", self.luna_reasoning_effort)
            return self._normalize_response(method(earlier, later, **kwargs))
        except Exception as exc:
            raise AIOrchestrationError(
                "provider_error",
                "Luna comparison request failed",
                provider_invoked=True,
            ) from exc

    def _language_call(
        self, adapter: Any, evidence: EvidencePacket, **kwargs: Any
    ) -> ProviderResponse[Any]:
        if adapter is None:
            raise AIOrchestrationError(
                "provider_disabled", "language adapter is unavailable"
            )
        method = getattr(adapter, "generate_text", None) or getattr(
            adapter, "generate", None
        )
        if method is None:
            raise AIOrchestrationError(
                "model_unavailable", "language adapter has no text method"
            )
        payload = evidence.model_dump(mode="json")
        try:
            # This call has no image parameter by design.  The type and policy
            # boundary make accidental Gemini image submission impossible.
            if adapter is self.luna:
                kwargs.setdefault("reasoning_effort", self.luna_reasoning_effort)
            return self._normalize_response(method(payload, **kwargs))
        except AIOrchestrationError:
            raise
        except Exception as exc:
            raise AIOrchestrationError(
                "provider_error", "language request failed", provider_invoked=True
            ) from exc

    @staticmethod
    def _normalize_response(value: Any) -> ProviderResponse[Any]:
        if isinstance(value, ProviderResponse):
            return value
        # OpenAILunaService's metadata-aware image helpers return a small
        # ``LunaCallResult`` object. Normalize it without leaking the image
        # derivative itself into persistence or the language stage.
        if hasattr(value, "value") and hasattr(value, "usage"):
            usage = getattr(value, "usage") or {}
            if isinstance(usage, ProviderUsage):
                normalized_usage = usage
            elif isinstance(usage, Mapping):
                normalized_usage = ProviderUsage(
                    input_tokens=int(usage.get("input_tokens", 0) or 0),
                    output_tokens=int(usage.get("output_tokens", 0) or 0),
                    estimated_cost_usd=usage.get("estimated_cost_usd"),
                    request_id=getattr(value, "request_id", None),
                )
            else:
                normalized_usage = ProviderUsage(
                    request_id=getattr(value, "request_id", None)
                )
            return ProviderResponse(
                output=getattr(value, "value"),
                usage=normalized_usage,
                model_id=getattr(value, "model_id", None),
            )
        if isinstance(value, Mapping) and "output" in value:
            usage = value.get("usage") or {}
            if not isinstance(usage, Mapping):
                usage = {}
            normalized_usage = ProviderUsage(
                input_tokens=int(
                    usage.get("input_tokens", usage.get("prompt_tokens", 0)) or 0
                ),
                output_tokens=int(
                    usage.get("output_tokens", usage.get("completion_tokens", 0)) or 0
                ),
                estimated_cost_usd=usage.get("estimated_cost_usd"),
                request_id=usage.get("request_id"),
            )
            return ProviderResponse(
                output=value["output"],
                usage=normalized_usage,
                model_id=value.get("model_id"),
            )
        return ProviderResponse(output=value)

    def _reserve_luna(
        self, *, user_id: str, request_identity: str, task: str, fallback: bool
    ) -> UsageReservation:
        spend_cap = getattr(self.settings, "luna_monthly_spend_cap_usd", None)
        if spend_cap is not None and float(spend_cap) <= 0:
            raise AIOrchestrationError(
                "provider_disabled", "Luna spend cap is not enabled"
            )
        limits = QuotaLimits(
            max_requests=getattr(self.settings, "luna_max_requests_per_window", None),
            max_tokens=getattr(self.settings, "luna_max_tokens_per_window", None),
            max_cost_usd=(
                getattr(self.settings, "luna_max_cost_usd_per_window", None)
                if getattr(self.settings, "luna_max_cost_usd_per_window", None)
                is not None
                else spend_cap
            ),
            user_max_requests=(
                getattr(self.settings, "luna_user_daily_limit", None)
                or getattr(self.settings, "ai_daily_user_limit", None)
                or None
            ),
        )
        estimate = int(
            getattr(self.settings, "luna_fallback_estimated_tokens", 1200)
            if fallback
            else getattr(self.settings, "luna_image_estimated_tokens", 3500)
        )
        cost = float(
            getattr(self.settings, "luna_fallback_estimated_cost_usd", 0.0005)
            if fallback
            else getattr(self.settings, "luna_image_estimated_cost_usd", 0.001)
        )
        try:
            reservation = self.quota.reserve(
                provider="openai",
                project_alias=getattr(
                    self.settings, "luna_project_alias", "glowupai-luna"
                ),
                model_id=self.luna_model,
                user_id=user_id,
                task=task,
                request_identity=request_identity,
                quota_window=utc_month_window(),
                estimated_tokens=max(1, estimate),
                estimated_cost_usd=max(0.0, cost),
                limits=limits,
            )
            if reservation.state != "reserved":
                raise AIOrchestrationError(
                    "reservation_replayed",
                    "Luna request already has a terminal or ambiguous reservation",
                )
            return reservation
        except QuotaExceeded as exc:
            raise AIOrchestrationError("quota_exhausted", str(exc)) from exc

    def _reserve_gemini(
        self, user_id: str, request_identity: str, task: str
    ) -> UsageReservation:
        limits = QuotaLimits(
            max_requests=getattr(self.settings, "gemini_max_requests_per_window", None),
            max_tokens=getattr(self.settings, "gemini_max_tokens_per_window", None),
            user_max_requests=(
                getattr(self.settings, "gemini_user_daily_limit", None)
                or getattr(self.settings, "ai_daily_user_limit", None)
                or None
            ),
        )
        try:
            reservation = self.quota.reserve(
                provider="gemini",
                project_alias=getattr(
                    self.settings, "gemini_project_alias", "glowupai-gemini"
                ),
                model_id=getattr(
                    self.settings, "gemini_model", "gemini-3.5-flash-lite"
                ),
                user_id=user_id,
                task=task,
                request_identity=request_identity,
                quota_window=utc_day_window(),
                estimated_tokens=int(
                    getattr(self.settings, "gemini_estimated_tokens", 1000)
                ),
                limits=limits,
            )
            if reservation.state != "reserved":
                raise AIOrchestrationError(
                    "reservation_replayed",
                    "Gemini request already has a terminal or ambiguous reservation",
                )
            return reservation
        except QuotaExceeded as exc:
            raise AIOrchestrationError("quota_exhausted", str(exc)) from exc

    def _reconcile(
        self, reservation: UsageReservation | None, usage: ProviderUsage
    ) -> None:
        if reservation is None:
            return
        input_tokens = usage.input_tokens
        output_tokens = usage.output_tokens
        # Missing usage is billed conservatively at the reserved amount.  A
        # timeout never reaches this method and remains ambiguous.
        if input_tokens == 0 and output_tokens == 0:
            input_tokens = reservation.reserved_tokens
        self.quota.reconcile(
            reservation.id,
            actual_input_tokens=input_tokens,
            actual_output_tokens=output_tokens,
            actual_cost_usd=usage.estimated_cost_usd,
            provider_request_id=usage.request_id,
        )

    @staticmethod
    def _request_key(
        user_id: str,
        capture_id: str,
        task: str,
        digest: str,
        prompt: str,
        preprocessing: str,
    ) -> str:
        material = "|".join((user_id, capture_id, task, digest, prompt, preprocessing))
        return hashlib.sha256(material.encode("utf-8")).hexdigest()

    @staticmethod
    def _evidence_packet(analysis_id: str, vision: VisionObservation) -> EvidencePacket:
        return EvidencePacket(
            evidence_ids=[analysis_id],
            evidence=[
                {
                    "id": analysis_id,
                    "type": "vision_observation",
                    "value": vision.model_dump(mode="json"),
                }
            ],
        )

    def _local_language(
        self, evidence: EvidencePacket, analysis_id: str
    ) -> LanguageResponse | None:
        if self.local_renderer is not None:
            try:
                value = self.local_renderer(evidence.model_dump(mode="json"))
                return parse_language_response(value, set(evidence.evidence_ids))
            except Exception:
                logger.info(
                    "local AI renderer failed", extra={"analysis_id": analysis_id}
                )
        value = evidence.evidence[0].get("value") if evidence.evidence else {}
        summary = value.get("summary") if isinstance(value, Mapping) else None
        if not isinstance(summary, str) or not summary.strip():
            return None
        return LanguageResponse(
            answer=summary[:1200],
            evidence_ids=[analysis_id],
            limitations=[
                "This is a local factual rendering of the stored observation."
            ],
        )

    def _consent_allowed(self, user_id: str, scope: str) -> bool:
        if self.consent_checker is None:
            return True
        try:
            return bool(self.consent_checker(user_id, scope))
        except Exception:
            return False

    def _require_active_owner(self, user_id: str) -> None:
        """Stop provider work for accounts that entered deletion."""
        try:
            row = self.db.fetchone(
                "SELECT id FROM users WHERE id=? AND deleted_at IS NULL",
                (user_id,),
            )
        except Exception as exc:
            raise AIOrchestrationError(
                "owner_unavailable", "owner state is unavailable"
            ) from exc
        if not row:
            raise AIOrchestrationError("account_deleted", "account is unavailable")

    def _gemini_policy(self, data_class: DataClass | str) -> PolicyEvaluation:
        return evaluate_gemini(
            data_class,
            personal_data_enabled=bool(
                getattr(self.settings, "gemini_personal_data_enabled", False)
            ),
            eligibility_review_id=getattr(
                self.settings, "gemini_eligibility_review_id", None
            ),
        )

    @staticmethod
    def _safe_error_code(exc: Exception) -> str:
        if isinstance(exc, AIOrchestrationError):
            return exc.code
        return "provider_error"


__all__ = [
    "AIOrchestrationError",
    "AIOrchestrator",
    "LanguageAdapter",
    "OrchestrationResult",
    "VisionAdapter",
]
