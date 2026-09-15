from __future__ import annotations

from pathlib import Path
from types import SimpleNamespace
import unittest

from glowupai.config import Settings
from glowupai.google_ai import (
    GoogleGeminiInsightService,
    GoogleGeminiVisionService,
    build_insight_service,
)
from glowupai.insights import GroundedInsightService


class FakeModels:
    def __init__(self, text: str = "Grounded model response"):
        self.text = text
        self.calls = []

    def generate_content(self, **kwargs):
        self.calls.append(kwargs)
        return SimpleNamespace(text=self.text)


class FailingModels:
    def generate_content(self, **kwargs):
        raise RuntimeError("offline")


class FakeClient:
    def __init__(self, models):
        self.models = models


class GoogleAiTests(unittest.TestCase):
    def test_provider_sends_bounded_json_evidence_and_uses_configured_model(self):
        models = FakeModels()
        provider = GoogleGeminiInsightService(
            "test-key", "gemini-test", client=FakeClient(models)
        )
        evidence = {
            "label": "likely_useful",
            "product_name": "Serum",
            "days_stable": 14,
            "raw_photo_bytes": "must never be sent",
        }

        verdict_text = provider.generate(evidence)
        qna_text = provider.answer(
            "What changed?", {"captures": [{"redness_score": 0.2}]}
        )

        self.assertEqual(verdict_text, "Grounded model response")
        self.assertEqual(qna_text, "Grounded model response")
        self.assertEqual(len(models.calls), 2)
        self.assertTrue(all(call["model"] == "gemini-test" for call in models.calls))
        self.assertNotIn("raw_photo_bytes", models.calls[0]["contents"])
        self.assertNotIn("must never be sent", models.calls[0]["contents"])
        self.assertIn("system_instruction", models.calls[0]["config"])

    def test_personal_evidence_never_contains_raw_image_material(self):
        models = FakeModels()
        provider = GoogleGeminiInsightService(
            "test-key",
            "gemini-test",
            client=FakeClient(models),
            data_class="public_catalog",
        )
        provider.generate(
            {
                "capture_id": "capture-1",
                "observations": [{"region": "cheek", "concern": "texture"}],
                "image_bytes": "raw-image-must-not-cross-provider",
                "image_url": "data:image/jpeg;base64,secret",
                "nested": {"photo_digest": "safe metadata may be omitted"},
            }
        )

        contents = models.calls[0]["contents"]
        self.assertNotIn("raw-image-must-not-cross-provider", contents)
        self.assertNotIn("data:image/jpeg;base64,secret", contents)
        self.assertNotIn("image_bytes", contents)
        self.assertNotIn("image_url", contents)

    def test_generation_falls_back_and_qna_returns_none_on_provider_failure(self):
        provider = GoogleGeminiInsightService(
            "test-key", "gemini-test", client=FakeClient(FailingModels())
        )
        evidence = {"label": "keep", "product_name": "Serum", "days_stable": 14}

        fallback = provider.generate(evidence)

        self.assertIn("Serum", fallback)
        self.assertIsNone(provider.answer("What changed?", {}))

    def test_personal_data_is_blocked_before_gemini_network_call(self):
        models = FakeModels()
        provider = GoogleGeminiInsightService(
            "test-key",
            "gemini-test",
            client=FakeClient(models),
            data_class="personal_face",
        )
        with self.assertRaises(RuntimeError):
            provider.generate_text({"observations": []})
        self.assertEqual(models.calls, [])

    def test_legacy_gemini_image_adapter_never_sends_image_bytes(self):
        models = FakeModels()
        provider = GoogleGeminiVisionService(
            "test-key", "gemini-test", client=FakeClient(models)
        )
        with self.assertRaises(RuntimeError):
            provider.extract_products(b"raw-face-bytes")
        self.assertEqual(models.calls, [])

    def test_builder_is_local_when_no_key_is_configured(self):
        settings = Settings(
            db_path=Path(".data/test.sqlite3"), photo_dir=None, gemini_api_key=None
        )
        service = build_insight_service(settings)
        self.assertIsInstance(service, GroundedInsightService)


if __name__ == "__main__":
    unittest.main()
