"""Provider neutral contracts for the Luna/Gemini analysis pipeline.

Provider responses are deliberately represented by small, strict models.  The
models in this module are application contracts; they are not claims that an
AI response is clinically accurate.  Provider adapters should parse their
structured output into these models before it is persisted or shown to a user.
"""

from __future__ import annotations

import json
from enum import StrEnum
from typing import Any, Generic, Mapping, TypeVar

from pydantic import BaseModel, ConfigDict, Field, field_validator


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class AnalysisStatus(StrEnum):
    NOT_REQUESTED = "not_requested"
    QUEUED = "queued"
    RUNNING = "running"
    COMPLETED = "completed"
    NEEDS_RETAKE = "needs_retake"
    UNAVAILABLE = "unavailable"
    FAILED = "failed"


class UnavailableReason(StrEnum):
    PROVIDER_DISABLED = "provider_disabled"
    POLICY_RESTRICTED = "policy_restricted"
    CONSENT_REQUIRED = "consent_required"
    QUOTA_EXHAUSTED = "quota_exhausted"
    MODEL_UNAVAILABLE = "model_unavailable"


class DataClass(StrEnum):
    """Classification used before evidence is sent to a second provider."""

    PERSONAL_FACE = "personal_face"
    PERSONAL_HISTORY = "personal_history"
    PRODUCT_ONLY = "product_only"
    PUBLIC_CATALOG = "public_catalog"
    ANONYMOUS_FIXTURE = "anonymous_fixture"


class PolicyDecision(StrEnum):
    ALLOW = "allow"
    DENY = "deny"
    REVIEW_REQUIRED = "review_required"


class LanguageMode(StrEnum):
    GEMINI_PRIMARY = "gemini_primary"
    LUNA_FALLBACK = "luna_fallback"
    LOCAL_FALLBACK = "local_fallback"
    UNAVAILABLE = "unavailable"


class Region(StrEnum):
    FOREHEAD = "forehead"
    LEFT_CHEEK = "left_cheek"
    RIGHT_CHEEK = "right_cheek"
    NOSE = "nose"
    CHIN = "chin"
    WHOLE_FACE = "whole_face"


class Concern(StrEnum):
    VISIBLE_REDNESS = "visible_redness"
    VISIBLE_SPOTS = "visible_spots"
    UNEVEN_TONE = "uneven_tone"
    VISIBLE_TEXTURE = "visible_texture"


class Visibility(StrEnum):
    VISIBLE = "visible"
    NOT_VISIBLE = "not_visible"
    UNCERTAIN = "uncertain"
    NOT_ASSESSABLE = "not_assessable"


class Extent(StrEnum):
    LOCALIZED = "localized"
    WIDESPREAD = "widespread"
    UNCERTAIN = "uncertain"
    NOT_ASSESSABLE = "not_assessable"


class QualityIssue(StrEnum):
    BLUR = "blur"
    LIGHTING = "lighting"
    POSE = "pose"
    OCCLUSION = "occlusion"
    MULTIPLE_FACES = "multiple_faces"
    NO_FACE = "no_face"
    RESOLUTION = "resolution"
    POSSIBLE_FILTER = "possible_filter"
    OTHER = "other"


class ChangeDirection(StrEnum):
    LESS_VISIBLE = "less_visible"
    SIMILAR = "similar"
    MORE_VISIBLE = "more_visible"
    UNCERTAIN = "uncertain"


class ProductCategory(StrEnum):
    CLEANSER = "cleanser"
    MOISTURIZER = "moisturizer"
    SERUM = "serum"
    SUNSCREEN = "sunscreen"
    EXFOLIANT = "exfoliant"
    TREATMENT = "treatment"
    OTHER = "other"


class Quality(StrictModel):
    usable: bool
    issues: list[QualityIssue] = Field(default_factory=list, max_length=8)


class Observation(StrictModel):
    region: Region
    concern: Concern
    visibility: Visibility
    extent: Extent
    description: str = Field(min_length=1, max_length=240)


class ImageInput(StrictModel):
    preprocessing_version: str = Field(min_length=1, max_length=80)
    width: int = Field(gt=0, le=4096)
    height: int = Field(gt=0, le=4096)
    bytes: int = Field(ge=0, le=20_000_000)
    detail: str = Field(pattern=r"^(low|auto)$")
    digest: str | None = Field(default=None, max_length=128)


class VisionObservation(StrictModel):
    """The body returned by Luna for a face/capture observation."""

    schema_version: str = Field(default="cosmetic-observation-v1", max_length=80)
    quality: Quality
    observations: list[Observation] = Field(default_factory=list, max_length=24)
    limitations: list[str] = Field(default_factory=list, max_length=8)
    summary: str = Field(default="", max_length=600)

    @field_validator("limitations")
    @classmethod
    def _limit_text(cls, values: list[str]) -> list[str]:
        return [value[:180] for value in values]


class ComparisonChange(StrictModel):
    region: Region
    concern: Concern
    change: ChangeDirection
    description: str = Field(default="", max_length=240)


class VisionComparison(StrictModel):
    schema_version: str = Field(default="cosmetic-comparison-v1", max_length=80)
    comparable: bool
    reasons: list[str] = Field(default_factory=list, max_length=8)
    changes: list[ComparisonChange] = Field(default_factory=list, max_length=24)
    limitations: list[str] = Field(default_factory=list, max_length=8)

    @field_validator("reasons", "limitations")
    @classmethod
    def _limit_reason_text(cls, values: list[str]) -> list[str]:
        return [value[:180] for value in values]


class ProductCandidate(StrictModel):
    name: str | None = Field(default=None, max_length=160)
    brand: str | None = Field(default=None, max_length=80)
    category: ProductCategory = ProductCategory.OTHER
    label_text: str | None = Field(default=None, max_length=4000)
    ingredients: list[str] = Field(default_factory=list, max_length=60)
    unreadable_fields: list[str] = Field(default_factory=list, max_length=20)
    needs_confirmation: bool = True

    @field_validator("ingredients")
    @classmethod
    def _clean_ingredients(cls, values: list[str]) -> list[str]:
        return [value[:80] for value in values if value.strip()]


class ProductScan(StrictModel):
    schema_version: str = Field(default="product-scan-v1", max_length=80)
    products: list[ProductCandidate] = Field(default_factory=list, max_length=20)
    limitations: list[str] = Field(default_factory=list, max_length=8)


class SuggestedAction(StrictModel):
    action: str = Field(min_length=1, max_length=40)
    label: str = Field(min_length=1, max_length=100)
    reference_id: str | None = Field(default=None, max_length=128)


class LanguageResponse(StrictModel):
    schema_version: str = Field(default="language-response-v1", max_length=80)
    answer: str = Field(min_length=1, max_length=1200)
    evidence_ids: list[str] = Field(default_factory=list, max_length=24)
    limitations: list[str] = Field(default_factory=list, max_length=8)
    suggested_actions: list[SuggestedAction] = Field(default_factory=list, max_length=8)

    @field_validator("limitations")
    @classmethod
    def _limit_language_text(cls, values: list[str]) -> list[str]:
        return [value[:180] for value in values]


class EvidencePacket(StrictModel):
    """Minimal, provider-safe evidence passed to a language model."""

    evidence_ids: list[str] = Field(default_factory=list, max_length=48)
    evidence: list[dict[str, Any]] = Field(default_factory=list, max_length=48)
    question: str | None = Field(default=None, max_length=600)


class ObservationEnvelope(StrictModel):
    """Server-owned persisted envelope, including provider provenance."""

    schema_version: str = Field(default="cosmetic-observation-v1", max_length=80)
    analysis_id: str
    capture_id: str
    status: AnalysisStatus
    vision_provider: str = Field(min_length=1, max_length=40)
    vision_model_id: str = Field(min_length=1, max_length=120)
    vision_reasoning_effort: str = Field(default="medium", pattern="^medium$")
    language_provider: str | None = Field(default=None, max_length=40)
    language_model_id: str | None = Field(default=None, max_length=120)
    language_reasoning_effort: str | None = Field(default=None, pattern="^medium$")
    language_fallback_provider: str | None = Field(default="openai", max_length=40)
    language_fallback_model_id: str | None = Field(default=None, max_length=120)
    language_fallback_reasoning_effort: str | None = Field(
        default="medium", pattern="^medium$"
    )
    language_mode: LanguageMode
    prompt_version: str = Field(min_length=1, max_length=80)
    image_input: ImageInput
    quality: Quality
    observations: list[Observation] = Field(default_factory=list, max_length=24)
    limitations: list[str] = Field(default_factory=list, max_length=8)
    summary: str = Field(default="", max_length=600)
    language: LanguageResponse | None = None


T = TypeVar("T")


class ProviderUsage(StrictModel):
    input_tokens: int = Field(default=0, ge=0)
    output_tokens: int = Field(default=0, ge=0)
    estimated_cost_usd: float | None = Field(default=None, ge=0)
    request_id: str | None = Field(default=None, max_length=200)


class ProviderResponse(StrictModel, Generic[T]):
    output: T | dict[str, Any] | str
    usage: ProviderUsage = Field(default_factory=ProviderUsage)
    model_id: str | None = Field(default=None, max_length=120)


def parse_json_object(value: Any) -> dict[str, Any]:
    """Parse adapter output while keeping parsing policy in one place."""

    if isinstance(value, Mapping):
        return dict(value)
    if isinstance(value, BaseModel):
        return value.model_dump(mode="json")
    if isinstance(value, str):
        parsed = json.loads(
            value.strip().removeprefix("```json").removesuffix("```").strip()
        )
        if isinstance(parsed, dict):
            return parsed
    raise ValueError("provider output must be a JSON object")


def parse_vision_observation(value: Any) -> VisionObservation:
    if isinstance(value, VisionObservation):
        return value
    if isinstance(value, ProviderResponse):
        value = value.output
    return VisionObservation.model_validate(parse_json_object(value))


def parse_comparison(value: Any) -> VisionComparison:
    if isinstance(value, VisionComparison):
        return value
    if isinstance(value, ProviderResponse):
        value = value.output
    return VisionComparison.model_validate(parse_json_object(value))


def parse_product_scan(value: Any) -> ProductScan:
    if isinstance(value, ProductScan):
        return value
    if isinstance(value, ProviderResponse):
        value = value.output
    return ProductScan.model_validate(parse_json_object(value))


def parse_language_response(
    value: Any, allowed_evidence_ids: set[str]
) -> LanguageResponse:
    if isinstance(value, LanguageResponse):
        parsed = value
    else:
        if isinstance(value, ProviderResponse):
            value = value.output
        parsed = LanguageResponse.model_validate(parse_json_object(value))
    unknown = set(parsed.evidence_ids) - allowed_evidence_ids
    if unknown:
        raise ValueError("language response references unknown evidence")
    return parsed


__all__ = [
    "AnalysisStatus",
    "ChangeDirection",
    "Concern",
    "DataClass",
    "Extent",
    "EvidencePacket",
    "ImageInput",
    "LanguageMode",
    "LanguageResponse",
    "Observation",
    "ObservationEnvelope",
    "PolicyDecision",
    "ProductCandidate",
    "ProductCategory",
    "ProductScan",
    "ProviderResponse",
    "ProviderUsage",
    "Quality",
    "QualityIssue",
    "Region",
    "SuggestedAction",
    "UnavailableReason",
    "VisionComparison",
    "VisionObservation",
    "Visibility",
    "parse_comparison",
    "parse_json_object",
    "parse_language_response",
    "parse_product_scan",
    "parse_vision_observation",
]
