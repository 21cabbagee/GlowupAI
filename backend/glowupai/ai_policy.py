"""Data-classification gates for the two-provider AI pipeline.

The policy is intentionally conservative.  Removing a face image does not
make its derived observations anonymous when they are linked to a user or
their history, so personalized evidence is denied unless an explicitly
reviewed personal-data configuration enables it.
"""

from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass

from .ai_contracts import DataClass, PolicyDecision


# ``slots`` is unavailable on the Python 3.9 runner used by the repository's
# local smoke tests; the project runtime remains compatible with newer Python.
@dataclass(frozen=True)
class PolicyEvaluation:
    decision: PolicyDecision
    data_class: DataClass
    reason: str
    review_id: str | None = None

    @property
    def allowed(self) -> bool:
        return self.decision is PolicyDecision.ALLOW


def classify_evidence(
    *,
    has_face: bool = False,
    has_user_history: bool = False,
    product_only: bool = False,
    public_catalog: bool = False,
    anonymous_fixture: bool = False,
) -> DataClass:
    """Return the strongest applicable data class.

    Flags are keyword-only so call sites make their data handling decision
    visible during review.  A face or user history takes precedence over the
    less sensitive labels.
    """

    if has_face:
        return DataClass.PERSONAL_FACE
    if has_user_history:
        return DataClass.PERSONAL_HISTORY
    if product_only:
        return DataClass.PRODUCT_ONLY
    if public_catalog:
        return DataClass.PUBLIC_CATALOG
    if anonymous_fixture:
        return DataClass.ANONYMOUS_FIXTURE
    # Unknown evidence must fail closed as personal history.
    return DataClass.PERSONAL_HISTORY


def evaluate_gemini(
    data_class: DataClass | str,
    *,
    personal_data_enabled: bool = False,
    eligibility_review_id: str | None = None,
) -> PolicyEvaluation:
    """Decide if validated evidence may be sent to Gemini.

    Personal evidence requires both a deployment opt-in and a recorded
    eligibility review identifier. Provider billing is configured separately.
    """

    try:
        classification = DataClass(data_class)
    except ValueError:
        classification = DataClass.PERSONAL_HISTORY
    is_personal = classification in {
        DataClass.PERSONAL_FACE,
        DataClass.PERSONAL_HISTORY,
    }
    if is_personal:
        if personal_data_enabled and eligibility_review_id:
            return PolicyEvaluation(
                PolicyDecision.ALLOW,
                classification,
                "personal evidence allowed by reviewed Gemini configuration",
                eligibility_review_id,
            )
        reason = "personal evidence requires an enabled, reviewed Gemini data-processing configuration"
        return PolicyEvaluation(
            PolicyDecision.REVIEW_REQUIRED,
            classification,
            reason,
            eligibility_review_id,
        )
    if classification in {
        DataClass.PRODUCT_ONLY,
        DataClass.PUBLIC_CATALOG,
        DataClass.ANONYMOUS_FIXTURE,
    }:
        return PolicyEvaluation(
            PolicyDecision.ALLOW,
            classification,
            "nonpersonal evidence is allowed for this pipeline mode",
            eligibility_review_id,
        )
    return PolicyEvaluation(
        PolicyDecision.DENY,
        classification,
        "unclassified evidence is denied",
        eligibility_review_id,
    )


def policy_consent_active(row: object | None) -> bool:
    """Interpret a persisted consent row without trusting provider payloads."""

    if row is None:
        return False
    if isinstance(row, Mapping):
        try:
            revoked = row["revoked_at"]
            granted = row["granted_at"]
        except (KeyError, TypeError):
            return False
    else:
        return False
    return bool(granted) and not revoked


__all__ = [
    "PolicyEvaluation",
    "classify_evidence",
    "evaluate_gemini",
    "policy_consent_active",
]
