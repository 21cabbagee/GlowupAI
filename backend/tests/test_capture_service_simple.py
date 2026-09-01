from __future__ import annotations

import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Dict

from glowupai.capture_service import CaptureService
from glowupai.full_db import FullDatabase
from glowupai.photos import MemoryPhotoStore
from glowupai.service import GlowupAIService


class CaptureServiceSimpleTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = FullDatabase(Path(self.temp.name) / "test.sqlite3")
        self.photos = MemoryPhotoStore()
        self.service = GlowupAIService(self.db, photos=self.photos)
        from glowupai.config import Settings

        self.capture_service = CaptureService(
            self.db, self.service, self.photos, Settings.from_env()
        )

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def test_calculate_relative_change(self):
        change = self.capture_service._calculate_relative_change(0.6, 0.5)
        self.assertEqual(change, 20.0)

        change = self.capture_service._calculate_relative_change(0.4, 0.5)
        self.assertEqual(change, -20.0)

    def test_calculate_relative_change_zero_baseline(self):
        change = self.capture_service._calculate_relative_change(0.5, 0.0)
        self.assertIsNone(change)

        change = self.capture_service._calculate_relative_change(0.0, 0.0)
        self.assertEqual(change, 0.0)

    def test_calculate_relative_change_none_values(self):
        change = self.capture_service._calculate_relative_change(None, 0.5)
        self.assertIsNone(change)

        change = self.capture_service._calculate_relative_change(0.5, None)
        self.assertIsNone(change)

    def test_vertical_metrics_extraction(self):
        metric = {
            "blemish_count": 5,
            "redness_score": 0.7,
            "darkspot_area": 0.2,
            "texture_score": 0.4,
            "extra_field": "should_be_ignored",
        }

        result = self.capture_service._vertical_metrics("skin", metric)

        self.assertIn("blemish_count", result)
        self.assertIn("redness_score", result)
        self.assertNotIn("extra_field", result)

    def test_measurement_explanation_strong(self):
        item = {
            "confidence": 0.8,
            "capture_quality": {"score": 0.8, "failed_checks": []},
        }

        explanation = CaptureService._measurement_explanation(item)

        self.assertEqual(explanation["confidence_label"], "strong comparison frame")
        self.assertTrue(explanation["comparison_ready"])
        self.assertEqual(len(explanation["quality_issues"]), 0)

    def test_measurement_explanation_directional(self):
        item = {
            "confidence": 0.6,
            "capture_quality": {"score": 0.7, "failed_checks": []},
        }

        explanation = CaptureService._measurement_explanation(item)

        self.assertEqual(explanation["confidence_label"], "directional frame")
        self.assertTrue(explanation["comparison_ready"])

    def test_measurement_explanation_low_confidence(self):
        item = {
            "confidence": 0.4,
            "capture_quality": {"score": 0.5, "failed_checks": ["lighting"]},
        }

        explanation = CaptureService._measurement_explanation(item)

        self.assertEqual(explanation["confidence_label"], "low-confidence frame")
        self.assertFalse(explanation["comparison_ready"])
        self.assertIn("lighting", explanation["quality_issues"])

    def test_capture_guide_baseline_needed(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        history_fn = lambda uid, vertical: []
        guide = self.capture_service.capture_guide(user["id"], history_fn=history_fn)

        self.assertEqual(guide["state"], "baseline_needed")
        self.assertIn("baseline", guide["message"].lower())

    def test_capture_guide_with_history(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        day = datetime.now(timezone.utc) - timedelta(days=4)
        history_fn = lambda uid, vertical: [
            {
                "captured_at": day.isoformat(),
                "is_baseline": True,
            }
        ]

        guide = self.capture_service.capture_guide(user["id"], history_fn=history_fn)

        self.assertIn(guide["state"], ["scheduled", "due", "overdue"])

    def test_add_baseline_comparison_no_baseline(self):
        user = self.service.create_user()

        metric = {
            "redness_score": 0.5,
            "blemish_count": 5,
            "darkspot_area": 0.2,
            "texture_score": 0.4,
        }

        result = self.capture_service._add_baseline_comparison(user["id"], metric)

        self.assertFalse(result["has_baseline"])
        self.assertIsNone(result["redness_change_pct"])

    def test_history_invalid_vertical(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        with self.assertRaises(ValueError) as context:
            self.capture_service.history(user["id"], vertical="invalid")

        self.assertIn("invalid", str(context.exception))


if __name__ == "__main__":
    unittest.main()
