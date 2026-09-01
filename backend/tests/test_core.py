from __future__ import annotations

import io
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

from PIL import Image, ImageDraw

from glowupai.capture import CaptureQuality
from glowupai.db import Database
from glowupai.face_alignment import align_face_safe, FaceAlignmentError
from glowupai.metrics import analyze
from glowupai.photos import MemoryPhotoStore
from glowupai.service import GlowupAIService


def image_bytes(red_spot: bool = False, dark_spot: bool = False) -> bytes:
    image = Image.new("RGB", (240, 240), (210, 165, 145))
    draw = ImageDraw.Draw(image)
    for y in range(0, 240, 8):
        for x in range(0, 240, 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=(165, 130, 118))
    if red_spot:
        draw.ellipse((90, 95, 122, 127), fill=(220, 45, 35))
    if dark_spot:
        draw.ellipse((145, 120, 176, 151), fill=(60, 35, 30))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


def flat_image_bytes() -> bytes:
    output = io.BytesIO()
    Image.new("RGB", (240, 240), (210, 165, 145)).save(output, format="PNG")
    return output.getvalue()


class CoreTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = Database(Path(self.temp.name) / "test.sqlite3")
        self.service = GlowupAIService(self.db, photos=MemoryPhotoStore())

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def test_capture_quality_has_hard_gates(self):
        accepted = CaptureQuality(distance_cm=45).evaluate()
        self.assertTrue(accepted.accepted)
        rejected = CaptureQuality(distance_cm=140).evaluate()
        self.assertFalse(rejected.accepted)
        self.assertIn("move_to_capture_distance", rejected.failed_checks)

    def test_metrics_are_deterministic_and_change_with_input(self):
        clean = analyze(image_bytes(), 0.9)
        irritated = analyze(image_bytes(red_spot=True), 0.9)
        self.assertEqual(clean.as_dict(), analyze(image_bytes(), 0.9).as_dict())
        self.assertGreaterEqual(irritated.redness_score, clean.redness_score)

    def test_server_measurements_cannot_be_overridden_by_client_quality(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        with self.assertRaises(ValueError):
            self.service.create_capture(
                user["id"], flat_image_bytes(), quality_data={"sharpness": 1.0}
            )

    def test_conservative_attribution_requires_stabilization_and_clean_window(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test serum", stabilization_days=7)
        day = datetime(2026, 1, 1, tzinfo=timezone.utc)
        quality = {
            "face_present": True,
            "distance_cm": 45,
            "yaw_degrees": 0,
            "pitch_degrees": 0,
            "expression_neutral": True,
        }
        self.service.create_capture(
            user["id"],
            image_bytes(),
            quality_data=quality,
            captured_at=day.isoformat(),
            is_baseline=True,
        )
        self.service.add_routine_event(
            user["id"], product["id"], "start", (day + timedelta(days=1)).isoformat()
        )
        self.service.create_capture(
            user["id"],
            image_bytes(red_spot=True),
            quality_data=quality,
            captured_at=(day + timedelta(days=4)).isoformat(),
        )
        early = self.service.attribution.evaluate_product(user["id"], product["id"])
        self.assertEqual(early.label, "evidence_unclear")
        self.service.create_capture(
            user["id"],
            image_bytes(),
            quality_data=quality,
            captured_at=(day + timedelta(days=10)).isoformat(),
        )
        later = self.service.attribution.evaluate_product(user["id"], product["id"])
        self.assertIn(later.label, {"keep", "likely_useful", "evidence_unclear"})
        self.assertGreaterEqual(later.evidence["n_after"], 1)

    def test_export_and_delete_remove_all_user_rows(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Cleanser")
        self.service.add_routine_event(user["id"], product["id"], "start")
        export = self.service.export_user(user["id"])
        self.assertEqual(export["user"]["id"], user["id"])
        self.service.delete_user(user["id"])
        self.assertIsNone(
            self.db.fetchone("SELECT id FROM users WHERE id = ?", (user["id"],))
        )

    def test_safety_triage_is_outside_llm_scope(self):
        result = self.service.triage_question("I have a changing mole and pain")
        self.assertEqual(result["scope"], "dermatology_review")

    def test_face_alignment_improves_consistency(self):
        """Test that face alignment reduces metric variance across rotated images."""
        # Create a base face-like image
        base_img = Image.new("RGB", (300, 300), (210, 165, 145))
        draw = ImageDraw.Draw(base_img)

        # Draw a simple face with eyes
        # Left eye
        draw.ellipse((90, 120, 110, 140), fill=(255, 255, 255))
        draw.ellipse((95, 125, 105, 135), fill=(0, 0, 0))

        # Right eye
        draw.ellipse((190, 120, 210, 140), fill=(255, 255, 255))
        draw.ellipse((195, 125, 205, 135), fill=(0, 0, 0))

        # Add some texture and a blemish
        for y in range(150, 200, 8):
            for x in range(100, 200, 8):
                draw.rectangle((x, y, x + 2, y + 2), fill=(165, 130, 118))
        draw.ellipse((140, 170, 160, 190), fill=(220, 45, 35))

        # Save base image
        base_bytes = io.BytesIO()
        base_img.save(base_bytes, format="PNG")
        base_bytes = base_bytes.getvalue()

        # Create rotated version (5 degrees)
        rotated_img = base_img.rotate(5, expand=False, fillcolor=(210, 165, 145))
        rotated_bytes = io.BytesIO()
        rotated_img.save(rotated_bytes, format="PNG")
        rotated_bytes = rotated_bytes.getvalue()

        # Analyze both with alignment
        result_base = analyze(base_bytes, 0.9)
        result_rotated = analyze(rotated_bytes, 0.9)

        # With alignment, metrics should be very similar
        # (within noise floor tolerance)
        blemish_diff = abs(result_base.blemish_count - result_rotated.blemish_count)
        texture_diff = abs(result_base.texture_score - result_rotated.texture_score)

        # Assert that differences are small (alignment reduces variance)
        self.assertLess(blemish_diff, 3.0, "Blemish count should be consistent with alignment")
        self.assertLess(texture_diff, 5.0, "Texture score should be consistent with alignment")

    def test_face_alignment_handles_no_face_gracefully(self):
        """Test that alignment gracefully handles images without faces."""
        # Create an image with no face
        no_face_img = Image.new("RGB", (200, 200), (100, 150, 200))
        no_face_bytes = io.BytesIO()
        no_face_img.save(no_face_bytes, format="PNG")
        no_face_bytes = no_face_bytes.getvalue()

        # Should not raise an exception - falls back to resized original
        aligned_bytes = align_face_safe(no_face_bytes)
        self.assertIsNotNone(aligned_bytes)

        # Verify output is valid image
        with Image.open(io.BytesIO(aligned_bytes)) as img:
            self.assertEqual(img.size, (256, 256))


if __name__ == "__main__":
    unittest.main()
