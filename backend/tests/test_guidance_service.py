from __future__ import annotations

import io
import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Dict, List
from unittest.mock import Mock

from PIL import Image

from glowupai.full_db import FullDatabase
from glowupai.guidance_service import GuidanceService
from glowupai.photos import MemoryPhotoStore
from glowupai.service import GlowupAIService


def image_bytes() -> bytes:
    image = Image.new("RGB", (240, 240), (210, 165, 145))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


def quality_data() -> Dict[str, Any]:
    return {
        "face_present": True,
        "distance_cm": 45,
        "yaw_degrees": 0,
        "pitch_degrees": 0,
        "expression_neutral": True,
    }


class MockJobQueue:
    def __init__(self):
        self.jobs = {}
        self.next_job_id = 0

    def submit(self, job_type, fn, *args, **kwargs):
        job_id = f"job_{self.next_job_id}"
        self.next_job_id += 1
        result = fn(*args) if args else {"status": "pending"}
        self.jobs[job_id] = {
            "id": job_id,
            "type": job_type,
            "status": "completed",
            "result": result,
        }
        return job_id

    def get(self, job_id, user_id=None):
        return self.jobs.get(job_id)


class MockVisionService:
    def extract_products(self, image_bytes):
        return [
            {
                "name": "Test Moisturizer",
                "category": "moisturizer",
                "ingredients": "water, glycerin",
                "stabilization_days": 14,
            }
        ]


class GuidanceServiceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = FullDatabase(Path(self.temp.name) / "test.sqlite3")
        self.service = GlowupAIService(self.db, photos=MemoryPhotoStore())
        self.mock_jobs = MockJobQueue()
        self.mock_vision = MockVisionService()
        # Create a simple insights object instead of Mock
        class SimpleInsights:
            def answer(self, question, evidence):
                return "Test answer based on evidence"
        self.mock_insights = SimpleInsights()
        self.guidance = GuidanceService(
            self.db, self.service, self.mock_insights, self.mock_vision, self.mock_jobs
        )

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def test_create_experiment_basic(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum", stabilization_days=14)

        experiment = self.guidance.create_experiment(
            user["id"],
            "Test Experiment",
            "Testing if serum helps",
            product["id"],
            primary_metric="redness_score",
            target_days=14,
        )

        self.assertEqual(experiment["name"], "Test Experiment")
        self.assertEqual(experiment["status"], "running")
        self.assertEqual(len(experiment["products"]), 1)

    def test_create_experiment_invalid_metric(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum")

        with self.assertRaises(ValueError) as context:
            self.guidance.create_experiment(
                user["id"],
                "Test",
                "Hypothesis",
                product["id"],
                primary_metric="invalid_metric",
            )

        self.assertIn("unsupported", str(context.exception))

    def test_create_experiment_invalid_target_days(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum")

        with self.assertRaises(ValueError) as context:
            self.guidance.create_experiment(
                user["id"],
                "Test",
                "Hypothesis",
                product["id"],
                target_days=200,
            )

        self.assertIn("target_days", str(context.exception))

    def test_create_experiment_product_not_found(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        with self.assertRaises(ValueError) as context:
            self.guidance.create_experiment(
                user["id"],
                "Test",
                "Hypothesis",
                "nonexistent_product_id",
            )

        self.assertIn("not found", str(context.exception))

    def test_experiment_retrieval(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum")

        created = self.guidance.create_experiment(
            user["id"],
            "Test Experiment",
            "Hypothesis",
            product["id"],
        )

        experiment = self.guidance.experiment(created["id"], user["id"])

        self.assertEqual(experiment["name"], "Test Experiment")
        self.assertIn("products", experiment)
        self.assertIn("events", experiment)
        self.assertIn("early_stop", experiment)

    def test_experiments_list(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product1 = self.service.create_product("Serum A")
        product2 = self.service.create_product("Serum B")

        self.guidance.create_experiment(user["id"], "Exp 1", "Hyp 1", product1["id"])
        self.guidance.create_experiment(user["id"], "Exp 2", "Hyp 2", product2["id"])

        experiments = self.guidance.experiments(user["id"])

        self.assertEqual(len(experiments), 2)

    def test_set_experiment_status(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum")

        experiment = self.guidance.create_experiment(
            user["id"],
            "Test",
            "Hypothesis",
            product["id"],
        )

        updated = self.guidance.set_experiment_status(
            user["id"],
            experiment["id"],
            "completed",
        )

        self.assertEqual(updated["status"], "completed")
        self.assertIsNotNone(updated["end_at"])

    def test_set_experiment_status_invalid(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum")

        experiment = self.guidance.create_experiment(
            user["id"],
            "Test",
            "Hypothesis",
            product["id"],
        )

        with self.assertRaises(ValueError) as context:
            self.guidance.set_experiment_status(
                user["id"],
                experiment["id"],
                "invalid_status",
            )

        self.assertIn("invalid", str(context.exception))

    def test_ask_qna_basic(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        history_fn = lambda uid: []

        result = self.guidance.ask(
            user["id"],
            "How is my redness?",
            history_fn=history_fn,
        )

        self.assertIn("answer", result)
        self.assertIn("thread_id", result)
        self.assertIn("scope", result)

    def test_ask_qna_with_thread(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        history_fn = lambda uid: []

        result1 = self.guidance.ask(
            user["id"],
            "First question",
            history_fn=history_fn,
        )

        result2 = self.guidance.ask(
            user["id"],
            "Follow-up question",
            thread_id=result1["thread_id"],
            history_fn=history_fn,
        )

        self.assertEqual(result1["thread_id"], result2["thread_id"])

    def test_ask_qna_invalid_thread(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        history_fn = lambda uid: []

        with self.assertRaises(ValueError) as context:
            self.guidance.ask(
                user["id"],
                "Question",
                thread_id="nonexistent_thread",
                history_fn=history_fn,
            )

        self.assertIn("not found", str(context.exception))

    def test_qna_history_retrieval(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        history_fn = lambda uid: []

        self.guidance.ask(user["id"], "Question 1", history_fn=history_fn)
        self.guidance.ask(user["id"], "Question 2", history_fn=history_fn)

        history = self.guidance.qna_history(user["id"])

        self.assertGreater(len(history), 0)

    def test_deterministic_qna_redness_question(self):
        user = self.service.create_user()

        history = [
            {"captured_at": "2026-01-01T00:00:00Z", "redness_score": 0.5},
            {"captured_at": "2026-01-10T00:00:00Z", "redness_score": 0.3},
        ]

        answer = self.guidance._deterministic_qna_answer(
            user["id"],
            "How is my redness?",
            history,
            [],
        )

        self.assertIn("redness", answer.lower())

    def test_deterministic_qna_ingredient_question(self):
        user = self.service.create_user()

        answer = self.guidance._deterministic_qna_answer(
            user["id"],
            "What is this ingredient?",
            [],
            [],
        )

        self.assertIn("ingredient", answer.lower())

    def test_scan_shelf_basic(self):
        user = self.service.create_user()

        result = self.guidance.scan_shelf(user["id"], image_bytes())

        self.assertIn("job_id", result)
        self.assertEqual(result["status"], "queued")

    def test_scan_shelf_no_image(self):
        user = self.service.create_user()

        with self.assertRaises(ValueError) as context:
            self.guidance.scan_shelf(user["id"], b"")

        self.assertIn("image", str(context.exception))

    def test_shelf_scan_status(self):
        user = self.service.create_user()

        result = self.guidance.scan_shelf(user["id"], image_bytes())
        status = self.guidance.shelf_scan_status(user["id"], result["job_id"])

        self.assertEqual(status["status"], "completed")

    def test_shelf_scan_status_not_found(self):
        user = self.service.create_user()

        with self.assertRaises(ValueError) as context:
            self.guidance.shelf_scan_status(user["id"], "nonexistent_job")

        self.assertIn("not found", str(context.exception))

    def test_confirm_shelf_scan(self):
        user = self.service.create_user()

        scan_result = self.guidance.scan_shelf(user["id"], image_bytes())

        selections = [
            {
                "name": "Test Product",
                "category": "serum",
                "ingredients": "water, glycerin",
                "stabilization_days": 14,
            }
        ]

        created = self.guidance.confirm_shelf_scan(
            user["id"],
            scan_result["job_id"],
            selections,
        )

        self.assertEqual(len(created), 1)
        self.assertEqual(created[0]["name"], "Test Product")

    def test_confirm_shelf_scan_job_not_ready(self):
        user = self.service.create_user()

        with self.assertRaises(ValueError) as context:
            self.guidance.confirm_shelf_scan(
                user["id"],
                "nonexistent_job",
                [],
            )

        self.assertIn("not ready", str(context.exception))

    def test_dermatologist_report(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        profile_fn = lambda uid: {"entitlement": {"plan": "premium"}}
        history_fn = lambda uid: [
            {
                "captured_at": "2026-01-01T00:00:00Z",
                "model_version": "v1",
                "is_baseline": True,
            }
        ]
        refresh_verdicts_fn = lambda uid: []

        report = self.guidance.dermatologist_report(
            user["id"],
            profile_fn,
            history_fn,
            refresh_verdicts_fn=refresh_verdicts_fn,
        )

        self.assertIn("generated_at", report)
        self.assertIn("capture_count", report)
        self.assertIn("printable_html", report)
        self.assertIn("disclaimer", report)

    def test_dashboard_basic(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        profile_fn = lambda uid: {"entitlement": {"plan": "free"}}
        history_fn = lambda uid, vertical: []
        verdicts_fn = lambda uid: []

        dashboard = self.guidance.dashboard(
            user["id"],
            profile_fn=profile_fn,
            history_fn=history_fn,
            verdicts_for_user_fn=verdicts_fn,
        )

        self.assertIn("profile", dashboard)
        self.assertIn("history", dashboard)
        self.assertIn("verdicts", dashboard)
        self.assertIn("features", dashboard)

    def test_dashboard_with_premium(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        profile_fn = lambda uid: {"entitlement": {"plan": "premium"}}
        history_fn = lambda uid, vertical: []
        verdicts_fn = lambda uid: []
        experiments_fn = lambda uid: []

        dashboard = self.guidance.dashboard(
            user["id"],
            profile_fn=profile_fn,
            history_fn=history_fn,
            verdicts_for_user_fn=verdicts_fn,
            experiments_fn=experiments_fn,
        )

        self.assertEqual(dashboard["features"]["experiments"], 1)

    def test_triage_question(self):
        result = self.guidance.triage_question("Is this melanoma?")

        self.assertIn("scope", result)

    def test_early_stop_not_running(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum")

        experiment = self.guidance.create_experiment(
            user["id"],
            "Test",
            "Hypothesis",
            product["id"],
        )

        self.guidance.set_experiment_status(
            user["id"],
            experiment["id"],
            "completed",
        )

        experiment = self.guidance.experiment(experiment["id"], user["id"])
        early_stop = experiment["early_stop"]

        self.assertFalse(early_stop["conclusive"])

    def test_qna_evidence_compilation(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        history = [
            {
                "id": "capture_1",
                "captured_at": "2026-01-01T00:00:00Z",
                "redness_score": 0.5,
            }
        ]

        events = [
            {
                "timestamp": "2026-01-01T00:00:00Z",
                "product_id": "prod_1",
                "action": "start",
            }
        ]

        evidence = self.guidance._qna_evidence(user["id"], history, events)

        self.assertEqual(evidence["capture_count"], 1)
        self.assertIn("captures", evidence)
        self.assertIn("routine_events", evidence)

    def test_shelf_scan_without_vision(self):
        guidance_no_vision = GuidanceService(
            self.db, self.service, self.mock_insights, None, self.mock_jobs
        )

        result = guidance_no_vision._run_shelf_scan(image_bytes())

        self.assertEqual(len(result["candidates"]), 0)
        self.assertIn("not configured", result["message"])

    def test_confirm_shelf_scan_max_selections(self):
        user = self.service.create_user()

        scan_result = self.guidance.scan_shelf(user["id"], image_bytes())

        selections = [
            {
                "name": f"Product {i}",
                "category": "serum",
            }
            for i in range(25)
        ]

        created = self.guidance.confirm_shelf_scan(
            user["id"],
            scan_result["job_id"],
            selections,
        )

        self.assertLessEqual(len(created), 20)

    def test_free_unlocked_product_id(self):
        user = self.service.create_user()

        result = self.guidance._free_unlocked_product_id(user["id"])
        self.assertIsNone(result)

        self.db.execute(
            "INSERT INTO engagement_events (id, user_id, event_type, reference_id) VALUES (?, ?, ?, ?)",
            ("evt_1", user["id"], "free_verdict_unlocked", "prod_123"),
        )

        result = self.guidance._free_unlocked_product_id(user["id"])
        self.assertEqual(result, "prod_123")

    def test_create_experiment_with_callbacks(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)
        product = self.service.create_product("Test Serum")

        premium_calls = []
        consent_calls = []
        audit_calls = []

        def require_premium_fn(uid, feature):
            premium_calls.append({"user_id": uid, "feature": feature})

        def require_consent_fn(uid):
            consent_calls.append({"user_id": uid})

        def audit_fn(action, entity_type, entity_id, uid, metadata):
            audit_calls.append({
                "action": action,
                "entity_type": entity_type,
                "entity_id": entity_id,
                "user_id": uid,
                "metadata": metadata,
            })

        experiment = self.guidance.create_experiment(
            user["id"],
            "Test",
            "Hypothesis",
            product["id"],
            require_premium_fn=require_premium_fn,
            require_consent_fn=require_consent_fn,
            audit_fn=audit_fn,
        )

        self.assertEqual(len(premium_calls), 1)
        self.assertEqual(len(consent_calls), 1)
        self.assertEqual(len(audit_calls), 1)

    def test_ask_with_engagement_callback(self):
        user = self.service.create_user()
        self.service.grant_consent(user["id"], True)

        engagement_calls = []

        def record_engagement_fn(uid, event_type, ref_id):
            engagement_calls.append({
                "user_id": uid,
                "event_type": event_type,
                "reference_id": ref_id,
            })

        history_fn = lambda uid: []

        result = self.guidance.ask(
            user["id"],
            "Test question",
            history_fn=history_fn,
            record_engagement_fn=record_engagement_fn,
        )

        self.assertEqual(len(engagement_calls), 1)
        self.assertEqual(engagement_calls[0]["event_type"], "qna_answered")

    def test_scan_shelf_with_engagement_callback(self):
        user = self.service.create_user()

        engagement_calls = []

        def record_engagement_fn(uid, event_type, ref_id):
            engagement_calls.append({
                "user_id": uid,
                "event_type": event_type,
                "reference_id": ref_id,
            })

        result = self.guidance.scan_shelf(
            user["id"],
            image_bytes(),
            record_engagement_fn=record_engagement_fn,
        )

        self.assertEqual(len(engagement_calls), 1)
        self.assertEqual(engagement_calls[0]["event_type"], "shelf_scan_submitted")


if __name__ == "__main__":
    unittest.main()
