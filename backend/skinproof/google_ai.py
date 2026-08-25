from __future__ import annotations

import json
from typing import Any

from .insights import GroundedInsightService


_VERDICT_SYSTEM_INSTRUCTION = """You write concise cosmetic-tracking explanations for SkinProof.
Use only the JSON evidence supplied by the application. The label, measurements,
noise floors, and attribution decision are authoritative and must not be changed.
Do not diagnose, treat, or rule out a medical condition. Do not invent numbers,
dates, products, or causes. If the evidence is unclear, say so plainly. Return
one short paragraph with no markdown and no medical advice."""

_QNA_SYSTEM_INSTRUCTION = """You answer questions inside SkinProof, a cosmetic
measurement and routine-tracking product. Use only the supplied JSON evidence.
Never invent a measurement, event, product property, date, or citation. Explain
trends as measurements rather than diagnoses. Do not diagnose, treat, or rule
out medical conditions. If the evidence cannot answer the question, say that
the evidence is insufficient and suggest a more specific evidence question.
Keep the answer under 120 words and return plain text only."""

_SHELF_SCAN_SYSTEM_INSTRUCTION = """You read a photo of skincare product
packaging for SkinProof, a cosmetic tracking app. Identify each distinct product
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


class GoogleGeminiInsightService:
    """Optional Gemini language layer with deterministic local fallback."""

    def __init__(self, api_key: str, model: str, client: Any | None = None, fallback: GroundedInsightService | None = None) -> None:
        self.model = model
        self.fallback = fallback or GroundedInsightService()
        self.client = client
        if self.client is None:
            from google import genai

            self.client = genai.Client(api_key=api_key)

    def _generate(self, instruction: str, payload: dict) -> str:
        response = self.client.models.generate_content(
            model=self.model,
            contents=json.dumps(_provider_safe(payload), separators=(",", ":"), sort_keys=True),
            config={
                "system_instruction": instruction,
                "temperature": 0.2,
                "max_output_tokens": 220,
            },
        )
        text = getattr(response, "text", None)
        if not isinstance(text, str) or not text.strip():
            raise RuntimeError("Gemini returned an empty response")
        return text.strip().replace("```", "")

    def generate(self, evidence: dict) -> str:
        try:
            return self._generate(_VERDICT_SYSTEM_INSTRUCTION, {"evidence": evidence})
        except Exception:
            return self.fallback.generate(evidence)

    def answer(self, question: str, evidence: dict) -> str | None:
        try:
            return self._generate(_QNA_SYSTEM_INSTRUCTION, {"question": question, "evidence": evidence})
        except Exception:
            return None


class GoogleGeminiVisionService:
    """Optional Gemini vision extraction for the shelf-scan flow."""

    def __init__(self, api_key: str, model: str, client: Any | None = None) -> None:
        self.model = model
        self.client = client
        if self.client is None:
            from google import genai

            self.client = genai.Client(api_key=api_key)

    def extract_products(self, image_bytes: bytes, mime_type: str = "image/jpeg") -> list[dict]:
        from google.genai import types

        response = self.client.models.generate_content(
            model=self.model,
            contents=[types.Part.from_bytes(data=image_bytes, mime_type=mime_type), "Identify every skincare product visible in this photo."],
            config={"system_instruction": _SHELF_SCAN_SYSTEM_INSTRUCTION, "temperature": 0.1, "max_output_tokens": 800},
        )
        text = getattr(response, "text", None)
        if not isinstance(text, str) or not text.strip():
            return []
        cleaned = text.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        try:
            parsed = json.loads(cleaned)
        except (TypeError, ValueError):
            return []
        if not isinstance(parsed, list):
            return []
        candidates = []
        for item in parsed[:20]:
            if not isinstance(item, dict) or not item.get("name"):
                continue
            candidates.append({
                "name": str(item.get("name"))[:160],
                "brand": (str(item["brand"])[:80] if item.get("brand") else None),
                "category": str(item.get("category") or "other")[:40],
                "ingredients": [str(x)[:80] for x in item.get("ingredients") or [] if str(x).strip()][:60],
            })
        return candidates


def build_vision_service(settings) -> GoogleGeminiVisionService | None:
    """Build the Gemini vision extractor when configured; None disables shelf-scan AI."""

    if not getattr(settings, "gemini_enabled", True):
        return None
    api_key = getattr(settings, "gemini_api_key", None)
    if not api_key:
        return None
    try:
        return GoogleGeminiVisionService(api_key=api_key, model=getattr(settings, "gemini_model", "gemini-3.5-flash-lite"))
    except Exception:
        return None


def build_insight_service(settings) -> GroundedInsightService | GoogleGeminiInsightService:
    """Build Gemini when explicitly configured; otherwise stay fully local."""

    if not getattr(settings, "gemini_enabled", True):
        return GroundedInsightService()
    api_key = getattr(settings, "gemini_api_key", None)
    if not api_key:
        return GroundedInsightService()
    try:
        return GoogleGeminiInsightService(
            api_key=api_key,
            model=getattr(settings, "gemini_model", "gemini-3.5-flash-lite"),
        )
    except Exception:
        return GroundedInsightService()
