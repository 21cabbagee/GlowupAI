from __future__ import annotations

import base64
import io
import json
from types import SimpleNamespace
import unittest

from PIL import Image

from glowupai.config import Settings
from glowupai.openai_ai import (
    LUNA_MODEL,
    OpenAILunaService,
    LunaConfigurationError,
    LunaPolicyError,
    LunaResponseError,
    prepare_luna_image,
    build_luna_service,
)


def image_bytes(width: int = 1200, height: int = 900) -> bytes:
    image = Image.new("RGB", (width, height), (180, 140, 120))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


class FakeResponses:
    def __init__(self, value: object):
        self.value = value
        self.calls: list[dict] = []

    def create(self, **kwargs):
        self.calls.append(kwargs)
        return self.value


class FakeClient:
    def __init__(self, value: object):
        self.responses = FakeResponses(value)


OBSERVATION = {
    "quality": {"usable": True, "issues": []},
    "observations": [],
    "limitations": ["Lighting can affect apparent redness."],
    "summary": "Only visible appearance is described.",
}


LANGUAGE = {
    "answer": "The evidence is limited.",
    "evidence_ids": [],
    "limitations": [],
    "suggested_actions": [],
}


class OpenAILunaTests(unittest.TestCase):
    def _service(self, response):
        return OpenAILunaService(
            "sk-test-secret",
            client=FakeClient(response),
        )

    def test_observation_uses_medium_reasoning_low_detail_and_derivative_only(self):
        response = SimpleNamespace(
            output_text=json.dumps(OBSERVATION),
            usage=SimpleNamespace(input_tokens=10, output_tokens=20, total_tokens=30),
            id="resp-test",
            status="completed",
        )
        service = self._service(response)
        original = image_bytes()
        result = service.observe_with_metadata(original)
        request = service.client.responses.calls[0]

        self.assertEqual(result.value, OBSERVATION)
        self.assertEqual(request["model"], LUNA_MODEL)
        self.assertEqual(request["reasoning"], {"effort": "medium"})
        self.assertIn("cosmetic image observation", request["instructions"])
        image_part = request["input"][0]["content"][1]
        self.assertEqual(image_part["detail"], "low")
        sent = base64.b64decode(image_part["image_url"].split(",", 1)[1])
        self.assertNotEqual(sent, original)
        self.assertLessEqual(result.image.width, 768)
        self.assertLessEqual(result.image.height, 768)
        self.assertEqual(result.image.bytes, len(sent))
        self.assertEqual(result.usage["input_tokens"], 10)
        self.assertEqual(service.last_image_input["detail"], "low")
        self.assertEqual(service.last_image_input["digest"], result.image.digest)

    def test_text_fallback_sends_evidence_without_image_fields(self):
        response = SimpleNamespace(output_text=json.dumps(LANGUAGE), status="completed")
        service = self._service(response)
        result = service.reason_with_metadata(
            {
                "observations": [],
                "capture_id": "capture-1",
                "raw_photo_bytes": "never-send",
                "image_url": "never-send",
            }
        )
        request = service.client.responses.calls[0]
        self.assertEqual(result.value, LANGUAGE)
        serialized = request["input"][0]["content"][0]["text"]
        self.assertNotIn("never-send", serialized)
        self.assertNotIn("image_url", serialized)
        self.assertEqual(request["reasoning"]["effort"], "medium")
        self.assertIn("evidence-only language", request["instructions"])
        self.assertFalse(
            any(
                part.get("type") == "input_image"
                for item in request["input"]
                for part in item.get("content", [])
            )
        )

    def test_label_scan_uses_auto_detail_and_no_face_crop(self):
        response = SimpleNamespace(
            output_text=json.dumps({"products": []}), status="completed"
        )
        service = self._service(response)
        result = service.extract_products_with_metadata(image_bytes(400, 300))
        request = service.client.responses.calls[0]
        image_part = request["input"][0]["content"][1]
        self.assertEqual(image_part["detail"], "auto")
        self.assertEqual(result.value, {"products": []})

    def test_orchestrator_interfaces_return_provider_contracts(self):
        response = SimpleNamespace(
            output_text=json.dumps(OBSERVATION),
            usage=SimpleNamespace(input_tokens=4, output_tokens=5),
            status="completed",
        )
        service = self._service(response)
        provider_response = service.analyze_image(
            image_bytes(200, 200), mime_type="image/png", prompt_version="v1"
        )
        self.assertEqual(provider_response.output, OBSERVATION)
        self.assertEqual(provider_response.model_id, LUNA_MODEL)
        self.assertEqual(provider_response.usage.input_tokens, 4)

        response.output_text = json.dumps(LANGUAGE)
        language_response = service.generate_text(
            {"evidence_ids": [], "evidence": []}, task_type="text_reasoning_fallback"
        )
        self.assertEqual(language_response.output, LANGUAGE)
        self.assertEqual(language_response.usage.output_tokens, 5)

    def test_comparison_sends_two_derivatives_with_same_low_detail(self):
        comparison = {
            "comparable": True,
            "reasons": [],
            "changes": [],
            "limitations": [],
        }
        service = self._service(
            SimpleNamespace(output_text=json.dumps(comparison), status="completed")
        )
        result = service.compare_with_metadata(
            image_bytes(300, 250), image_bytes(300, 250)
        )
        parts = service.client.responses.calls[0]["input"][0]["content"]
        images = [part for part in parts if part.get("type") == "input_image"]
        self.assertEqual(result.value, comparison)
        self.assertEqual(len(images), 2)
        self.assertTrue(all(part["detail"] == "low" for part in images))

    def test_model_and_reasoning_are_strict(self):
        with self.assertRaises(LunaConfigurationError):
            OpenAILunaService("key", model="gpt-5-nano")
        with self.assertRaises(LunaConfigurationError):
            OpenAILunaService("key", reasoning_effort="low")

    def test_disabled_service_makes_no_request(self):
        fake = FakeClient(
            SimpleNamespace(output_text=json.dumps(OBSERVATION), status="completed")
        )
        service = OpenAILunaService("key", client=fake, enabled=False)
        with self.assertRaises(LunaPolicyError):
            service.observe(image_bytes(200, 200))
        self.assertEqual(fake.responses.calls, [])

    def test_derivative_is_deterministic_with_explicit_face_bbox(self):
        source = image_bytes(1000, 700)
        first = prepare_luna_image(source, face_bbox=(300, 150, 250, 260))
        second = prepare_luna_image(source, face_bbox=(300, 150, 250, 260))
        self.assertEqual(first.digest, second.digest)
        self.assertEqual(first.data, second.data)
        self.assertEqual(first.preprocessing_version, "face-derivative-v1")

    def test_malformed_structured_output_is_rejected_without_leaking_provider_text(
        self,
    ):
        response = SimpleNamespace(
            output_text="not-json <provider-internal-details>",
            status="completed",
        )
        service = self._service(response)

        with self.assertRaisesRegex(LunaResponseError, "malformed structured output"):
            service.observe(image_bytes(200, 200))

    def test_text_fallback_rejects_malformed_structured_output(self):
        response = SimpleNamespace(output_text="{broken", status="completed")
        service = self._service(response)

        with self.assertRaisesRegex(LunaResponseError, "malformed structured output"):
            service.reason({"observations": []})

    def test_builder_requires_opt_in_and_positive_cap(self):
        settings = Settings(
            db_path=__import__("pathlib").Path(".data/test.sqlite3"),
            photo_dir=None,
            luna_api_key="key",
            luna_enabled=True,
        )
        self.assertIsNone(build_luna_service(settings))


if __name__ == "__main__":
    unittest.main()
