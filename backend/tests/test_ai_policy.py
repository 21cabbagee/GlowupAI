from __future__ import annotations

from pathlib import Path
import unittest

from glowupai.ai_contracts import DataClass
from glowupai.ai_policy import classify_evidence, evaluate_gemini
from glowupai.config import Settings


class AIPolicyTests(unittest.TestCase):
    def settings(self, **changes):
        values = {
            "db_path": Path(".data/test.sqlite3"),
            "photo_dir": None,
            "luna_enabled": True,
            "luna_api_key": "sk-test",
            "luna_monthly_spend_cap_usd": 20,
            "gemini_enabled": True,
            "gemini_api_key": "gem-test",
        }
        values.update(changes)
        return Settings(**values)

    def test_gemini_blocks_user_derived_evidence_without_opt_in(self):
        generic = evaluate_gemini(DataClass.PUBLIC_CATALOG)
        self.assertTrue(generic.allowed)
        personal = evaluate_gemini(DataClass.PERSONAL_FACE)
        self.assertFalse(personal.allowed)
        self.assertEqual(personal.decision.value, "review_required")

    def test_gemini_personal_gate_allows_reviewed_opt_in(self):
        decision = evaluate_gemini(
            DataClass.PERSONAL_HISTORY,
            personal_data_enabled=True,
            eligibility_review_id="review-1",
        )
        self.assertTrue(decision.allowed)

    def test_gemini_personal_gate_rejects_missing_review_or_opt_in(self):
        missing_review = evaluate_gemini(
            DataClass.PERSONAL_HISTORY,
            personal_data_enabled=True,
        )
        self.assertFalse(missing_review.allowed)
        self.assertEqual(missing_review.decision.value, "review_required")

        missing_opt_in = evaluate_gemini(
            DataClass.PERSONAL_FACE,
            personal_data_enabled=False,
            eligibility_review_id="review-1",
        )
        self.assertFalse(missing_opt_in.allowed)
        self.assertEqual(missing_opt_in.decision.value, "review_required")

    def test_gemini_rejects_unknown_classification_fail_closed(self):
        decision = evaluate_gemini("not-a-real-classification")
        self.assertFalse(decision.allowed)
        self.assertEqual(decision.data_class, DataClass.PERSONAL_HISTORY)

    def test_classifier_is_conservative(self):
        self.assertEqual(
            classify_evidence(has_face=True),
            DataClass.PERSONAL_FACE,
        )
        self.assertEqual(classify_evidence(product_only=True), DataClass.PRODUCT_ONLY)


if __name__ == "__main__":
    unittest.main()
