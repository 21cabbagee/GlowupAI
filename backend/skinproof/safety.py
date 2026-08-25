from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class SafetyResult:
    scope: str
    message: str
    matched_terms: tuple[str, ...] = ()

    def as_dict(self) -> dict:
        return {"scope": self.scope, "message": self.message, "matched_terms": list(self.matched_terms)}


TRIAGE_TERMS = (
    "changing mole", "bleeding", "severe pain", "painful", "cystic", "pus", "infected",
    "wound", "ulcer", "rapidly spreading", "suspicious lesion", "skin cancer",
)


def triage(text: str) -> SafetyResult:
    lowered = text.casefold()
    matches = tuple(term for term in TRIAGE_TERMS if term in lowered)
    if matches:
        return SafetyResult(
            scope="dermatology_review",
            message="This may be outside cosmetic tracking. Please consult a qualified dermatologist; SkinProof does not diagnose skin conditions.",
            matched_terms=matches,
        )
    return SafetyResult(
        scope="cosmetic_tracking",
        message="SkinProof can explain your measured trend, but it cannot diagnose or rule out a medical condition.",
    )
