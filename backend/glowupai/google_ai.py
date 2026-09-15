from __future__ import annotations

import json
from collections.abc import Mapping
from typing import Any

from .insights import GroundedInsightService

GEMINI_MODEL = "gemini-3.5-flash-lite"

_VERDICT_SYSTEM_INSTRUCTION = """You write concise cosmetic-tracking explanations for GlowUpAI.
Use only the JSON evidence supplied by the application. The label, measurements,
noise floors, and attribution decision are authoritative and must not be changed.
Do not diagnose, treat, or rule out a medical condition. Do not invent numbers,
dates, products, or causes. If the evidence is unclear, say so plainly. Return
one short paragraph with no markdown and no medical advice."""

_QNA_SYSTEM_INSTRUCTION = """You answer questions inside GlowUpAI, a cosmetic
measurement and routine-tracking product. Use only the supplied JSON evidence.
Never invent a measurement, event, product property, date, or citation. Explain
trends as measurements rather than diagnoses. Do not diagnose, treat, or rule
out medical conditions. If the evidence cannot answer the question, say that
the evidence is insufficient and suggest a more specific evidence question.
Keep the answer under 120 words and return plain text only."""

_SHELF_SCAN_SYSTEM_INSTRUCTION = """You read a photo of skincare product
packaging for GlowUpAI, a cosmetic tracking app. Identify each distinct product
visible. For each one return an object with: name (best-guess product name),
brand (or null), category (one of cleanser, moisturizer, serum, sunscreen,
exfoliant, treatment, other), and ingredients
(a list of ingredient names read from the label if legible, else an empty
list). Never invent a product that is not visibly in the photo. If you cannot
read a field confidently, use null rather than guessing. Return ONLY a JSON
array of these objects with no markdown fences and no commentary."""


def _provider_safe(value: Any, key: str = "") -> Any:
    """Keep provider payloads JSON-safe and exclude image/raw-photo material."""

    lowered_key = key.casefold()
    if any(marker in lowered_key for marker in ("raw", "bytes", "image")):
        return None
    if isinstance(value, dict):
        result = {}
        for item_key, item_value in list(value.items())[:50]:
            safe_value = _provider_safe(item_value, str(item_key))
            if safe_value is not None:
                result[str(item_key)] = safe_value
        return result
    if isinstance(value, (list, tuple)):
        return [_provider_safe(item, key) for item in list(value)[:50]]
    if isinstance(value, (str, int, float, bool)) or value is None:
        return value[:2000] if isinstance(value, str) else value
    return str(value)[:500]


_LANGUAGE_RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "answer": {"type": "string"},
        "evidence_ids": {"type": "array", "items": {"type": "string"}},
        "limitations": {"type": "array", "items": {"type": "string"}},
        "suggested_actions": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "action": {"type": "string"},
                    "label": {"type": "string"},
                    "reference_id": {"type": "string", "nullable": True},
                },
                "required": ["action", "label", "reference_id"],
            },
        },
    },
    "required": ["answer", "evidence_ids", "limitations", "suggested_actions"],
}


class GoogleGeminiInsightService:
    """Optional text-only Gemini language layer with a local fallback.

    This adapter deliberately has no image input path. Personal evidence is
    checked by the optional policy object before the network call; the
    orchestrator can use Luna text reasoning when this stage fails.
    """

    def __init__(
        self,
        api_key: str,
        model: str,
        client: Any | None = None,
        fallback: GroundedInsightService | None = None,
        policy: Any | None = None,
        data_class: str = "public_catalog",
        personal_data_enabled: bool = False,
        eligibility_review_id: str | None = None,
    ) -> None:
        self.model = model
        self.fallback = fallback or GroundedInsightService()
        self.policy = policy
        self.data_class = data_class
        self.personal_data_enabled = personal_data_enabled
        self.eligibility_review_id = eligibility_review_id
        self.last_usage: dict[str, int] = {}
        self.last_request_id: str | None = None
        self.client: Any = client
        if self.client is None:
            from google import genai

            self.client = genai.Client(api_key=api_key)

    def _generate(self, instruction: str, payload: dict) -> str:
        self._assert_policy()
        safe_payload = _provider_safe(payload)
        response = self.client.models.generate_content(
            model=self.model,
            contents=json.dumps(
                safe_payload,
                separators=(",", ":"),
                sort_keys=True,
            ),
            config={
                "system_instruction": instruction,
                "temperature": 0.2,
                "max_output_tokens": 220,
            },
        )
        usage = getattr(response, "usage_metadata", None)
        if usage is not None:

            def usage_value(name: str) -> int:
                value = getattr(usage, name, None)
                return (
                    int(value) if isinstance(value, (int, float)) and value >= 0 else 0
                )

            self.last_usage = {
                "input_tokens": usage_value("prompt_token_count"),
                "output_tokens": usage_value("candidates_token_count"),
                "total_tokens": usage_value("total_token_count"),
            }
        response_id = getattr(response, "response_id", None) or getattr(
            response, "id", None
        )
        self.last_request_id = str(response_id)[:160] if response_id else None
        text = getattr(response, "text", None)
        if not isinstance(text, str) or not text.strip():
            raise RuntimeError("Gemini returned an empty response")
        return text.strip().replace("```", "")

    def _generate_structured(self, instruction: str, payload: dict) -> Any:
        """Generate bounded JSON for the orchestrator's LanguageResponse contract."""

        self._assert_policy()
        response = self.client.models.generate_content(
            model=self.model,
            contents=json.dumps(
                _provider_safe(payload),
                separators=(",", ":"),
                sort_keys=True,
            ),
            config={
                "system_instruction": instruction,
                "response_mime_type": "application/json",
                "response_schema": _LANGUAGE_RESPONSE_SCHEMA,
                "temperature": 0.2,
                "max_output_tokens": 500,
            },
        )
        usage = getattr(response, "usage_metadata", None)
        if usage is not None:

            def usage_value(name: str) -> int:
                value = getattr(usage, name, None)
                return (
                    int(value) if isinstance(value, (int, float)) and value >= 0 else 0
                )

            self.last_usage = {
                "input_tokens": usage_value("prompt_token_count"),
                "output_tokens": usage_value("candidates_token_count"),
                "total_tokens": usage_value("total_token_count"),
            }
        response_id = getattr(response, "response_id", None) or getattr(
            response, "id", None
        )
        self.last_request_id = str(response_id)[:160] if response_id else None
        text = getattr(response, "text", None)
        if not isinstance(text, str) or not text.strip():
            raise RuntimeError("Gemini returned an empty response")
        cleaned = (
            text.strip()
            .removeprefix("```json")
            .removeprefix("```")
            .removesuffix("```")
            .strip()
        )
        try:
            parsed = json.loads(cleaned)
        except (TypeError, ValueError, json.JSONDecodeError) as exc:
            raise RuntimeError("Gemini returned malformed structured output") from exc
        if not isinstance(parsed, dict):
            raise RuntimeError("Gemini returned an invalid language object")
        return parsed

    def _assert_policy(self) -> None:
        if self.policy is not None:
            if hasattr(self.policy, "require_gemini"):
                self.policy.require_gemini(stage="language", data_class=self.data_class)
                return
            if hasattr(self.policy, "allowed") and not self.policy.allowed:
                raise RuntimeError("Gemini evidence policy denied this request")
            return
        # Direct adapter construction still gets a conservative gate. The
        # builder passes deployment settings; old public-catalog callers stay
        # compatible while user-derived classes fail closed.
        try:
            from .ai_policy import evaluate_gemini

            policy_decision = evaluate_gemini(
                self.data_class,
                personal_data_enabled=self.personal_data_enabled,
                eligibility_review_id=self.eligibility_review_id,
            )
            if not policy_decision.allowed:
                raise RuntimeError(policy_decision.reason)
        except ImportError:
            if self.data_class in {"personal_face", "personal_history"}:
                raise RuntimeError(
                    "Gemini personal evidence policy denied this request"
                )

    def generate_text(
        self,
        evidence: Mapping[str, Any],
        *,
        question: str | None = None,
        **kwargs: Any,
    ) -> Any:
        """Provider-neutral text interface; it sends text JSON only."""

        del kwargs
        payload: dict[str, Any] = {"evidence": dict(evidence)}
        if question:
            payload["question"] = question[:2000]
        parsed = self._generate_structured(_QNA_SYSTEM_INSTRUCTION, payload)
        try:
            from .ai_contracts import ProviderResponse, ProviderUsage

            return ProviderResponse(
                output=parsed,
                usage=ProviderUsage(
                    input_tokens=self.last_usage.get("input_tokens", 0),
                    output_tokens=self.last_usage.get("output_tokens", 0),
                    request_id=self.last_request_id,
                ),
                model_id=self.model,
            )
        except (ImportError, TypeError, ValueError):
            return parsed

    def generate(self, evidence: dict) -> str:
        self._assert_policy()
        try:
            return self._generate(_VERDICT_SYSTEM_INSTRUCTION, {"evidence": evidence})
        except Exception:  # noqa: BLE001  # Catch-all for LLM API errors with fallback
            return self.fallback.generate(evidence)

    def answer(self, question: str, evidence: dict) -> str | None:
        self._assert_policy()
        try:
            return self._generate(
                _QNA_SYSTEM_INSTRUCTION,
                {"question": question, "evidence": evidence},
            )
        except Exception:  # noqa: BLE001  # Catch-all for LLM API errors
            return None


class GoogleGeminiVisionService:
    """Retired Gemini image adapter.

    Image understanding belongs to the paid Luna adapter. This class remains
    import-compatible for older callers, but refuses to transmit bytes so a
    stale shelf-scan route cannot silently violate the two-stage policy.
    """

    def __init__(self, api_key: str, model: str, client: Any | None = None) -> None:
        self.model = model
        self.client: Any = client
        if self.client is None:
            from google import genai

            self.client = genai.Client(api_key=api_key)

    def extract_products(
        self,
        image_bytes: bytes,
        mime_type: str = "image/jpeg",
    ) -> list[dict]:
        del image_bytes, mime_type
        raise RuntimeError(
            "Gemini image input is disabled; route image analysis through paid Luna"
        )


def build_vision_service(settings) -> GoogleGeminiVisionService | None:
    """Return the legacy Gemini image extractor only for explicit opt-in.

    New capture and product flows must use Luna for images. Keeping this
    opt-in escape hatch lets older deployments migrate without accidentally
    sending personal images to Gemini.
    """

    if not getattr(settings, "gemini_product_vision_enabled", False):
        return None
    if not getattr(settings, "gemini_enabled", True):
        return None
    api_key = getattr(settings, "gemini_api_key", None)
    if not api_key:
        return None
    try:
        return GoogleGeminiVisionService(
            api_key=api_key,
            model=getattr(settings, "gemini_model", "gemini-3.5-flash-lite"),
        )
    except (
        Exception
    ):  # noqa: BLE001  # Catch-all for service initialization with fallback
        return None


def build_insight_service(
    settings,
) -> GroundedInsightService | GoogleGeminiInsightService:
    """Build Gemini when explicitly configured; otherwise stay fully local."""

    if not getattr(settings, "gemini_enabled", True):
        return GroundedInsightService()
    api_key = getattr(settings, "gemini_api_key", None)
    if not api_key:
        return GroundedInsightService()
    model = getattr(settings, "gemini_model", GEMINI_MODEL)
    if model != GEMINI_MODEL:
        return GroundedInsightService()
    try:
        return GoogleGeminiInsightService(
            api_key=api_key,
            model=model,
            policy=getattr(settings, "ai_policy", None),
            data_class=getattr(settings, "gemini_data_class", "public_catalog"),
            personal_data_enabled=bool(
                getattr(settings, "gemini_personal_data_enabled", False)
            ),
            eligibility_review_id=getattr(
                settings, "gemini_eligibility_review_id", None
            ),
        )
    except (
        Exception
    ):  # noqa: BLE001  # Catch-all for service initialization with fallback
        return GroundedInsightService()
