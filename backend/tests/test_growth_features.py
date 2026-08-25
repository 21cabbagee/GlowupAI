from __future__ import annotations

import base64
import io
import tempfile
import time
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from skinproof.complete_api import create_complete_app
from skinproof.complete_db import FullDatabase
from skinproof.complete_service import CompleteSkinProofService
from skinproof.config import Settings
from skinproof.photos import MemoryPhotoStore


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


def sample_image_b64() -> str:
    return base64.b64encode(image_bytes()).decode()


class GrowthFeatureTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = FullDatabase(Path(self.temp.name) / "growth.sqlite3")
        settings = Settings(db_path=Path(self.temp.name) / "growth.sqlite3", photo_dir=None, gemini_api_key=None, gemini_enabled=False, admin_token="test-admin-token")
        self.service = CompleteSkinProofService(self.db, settings=settings, photos=MemoryPhotoStore())
        self.client = TestClient(create_complete_app(self.service))

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    # -- helpers -------------------------------------------------------------

    def create_user(self, premium: bool = False) -> str:
        user = self.client.post("/api/users", json={"skin_type": "combination"}).json()
        user_id = user["user"]["id"]
        self.assertEqual(self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True}).status_code, 200)
        if premium:
            self.assertEqual(self.client.post(f"/api/users/{user_id}/subscription/upgrade", json={}).status_code, 200)
        return user_id

    def create_product(self, name: str, stabilization_days: int = 14, ingredients=None) -> dict:
        payload = {"name": name, "stabilization_days": stabilization_days}
        if ingredients is not None:
            payload["ingredients"] = ingredients
        response = self.client.post("/api/products", json=payload)
        self.assertEqual(response.status_code, 200)
        return response.json()

    def seed_verdict(self, user_id: str, product_id: str, start: datetime, direction: str = "investigate",
                      before_offsets=(5, 3, 1), after_offsets=(1, 3, 5)) -> None:
        """Build a before/after capture history through the service layer so a
        product resolves to a definitive (non-evidence_unclear) attribution
        label. `direction` picks which shape of evidence to produce:
        "investigate" (worsening redness) or "keep" (no measurable change)."""

        for offset in before_offsets:
            self.service.create_capture(user_id, image_bytes(), captured_at=(start - timedelta(days=offset)).isoformat())
        self.service.add_routine_event(user_id, product_id, "start", start.isoformat())
        for offset in after_offsets:
            img = image_bytes(red_spot=True) if direction == "investigate" else image_bytes()
            self.service.create_capture(user_id, img, captured_at=(start + timedelta(days=offset)).isoformat())

    def poll_job(self, user_id: str, job_id: str, path_prefix: str) -> dict:
        for _ in range(100):
            response = self.client.get(f"/api/users/{user_id}/{path_prefix}/{job_id}")
            self.assertEqual(response.status_code, 200)
            job = response.json()
            if job["status"] in ("completed", "failed"):
                return job
            time.sleep(0.02)
        self.fail("job did not complete in time")

    # -- 1. confound warning ---------------------------------------------------

    def test_confound_warning_flags_overlapping_stabilization_windows(self):
        user_id = self.create_user()
        product_a = self.create_product("Confound Serum A", stabilization_days=14)
        product_b = self.create_product("Confound Serum B", stabilization_days=14)

        # First product started: nothing else is active yet.
        first = self.client.post("/api/routine-events", json={"user_id": user_id, "product_id": product_a["id"], "action": "start"})
        self.assertEqual(first.status_code, 200)
        self.assertIsNone(first.json()["confound_warning"])

        direct_check = self.client.get(f"/api/users/{user_id}/confound-check")
        self.assertEqual(direct_check.status_code, 200)
        self.assertTrue(direct_check.json()["confounded"])
        self.assertEqual(direct_check.json()["active_windows"][0]["product_name"], "Confound Serum A")

        # Starting a second product while A's window is open should warn.
        second = self.client.post("/api/routine-events", json={"user_id": user_id, "product_id": product_b["id"], "action": "start"})
        self.assertEqual(second.status_code, 200)
        warning = second.json()["confound_warning"]
        self.assertIsNotNone(warning)
        self.assertTrue(warning["confounded"])
        names = [item["product_name"] for item in warning["active_windows"]]
        self.assertIn("Confound Serum A", names)

    # -- 2. capture coaching ----------------------------------------------------

    def test_bad_capture_quality_returns_actionable_coaching(self):
        user_id = self.create_user()
        response = self.client.post("/api/captures", json={
            "user_id": user_id,
            "image_base64": sample_image_b64(),
            "quality": {"yaw_degrees": 40},
        })
        self.assertEqual(response.status_code, 400)
        detail = response.json()["detail"]
        coaching = detail["quality"]["coaching"]
        self.assertTrue(coaching)
        for entry in coaching:
            self.assertIn("check", entry)
            self.assertIn("message", entry)
        self.assertIn("turn_head_less", [entry["check"] for entry in coaching])

    # -- 3 & 12. free-tier verdict unlock + sticky quota -------------------------

    def _seed_free_tier_verdict_scenario(self):
        user_id = self.create_user()
        product_a = self.create_product("Unlock Serum A", stabilization_days=0)
        product_b = self.create_product("Unlock Serum B", stabilization_days=0)
        product_c = self.create_product("Unlock Serum C (unclear)", stabilization_days=0)
        self.seed_verdict(user_id, product_a["id"], datetime(2026, 1, 1, tzinfo=timezone.utc), direction="investigate")
        self.seed_verdict(user_id, product_b["id"], datetime(2026, 2, 1, tzinfo=timezone.utc), direction="investigate")
        # Product C: a routine event but no post-stabilization capture -> evidence_unclear.
        self.service.add_routine_event(user_id, product_c["id"], "start", datetime(2026, 3, 1, tzinfo=timezone.utc).isoformat())
        return user_id, product_a["id"], product_b["id"], product_c["id"]

    def test_free_tier_unlocks_exactly_one_definitive_verdict(self):
        user_id, product_a_id, product_b_id, product_c_id = self._seed_free_tier_verdict_scenario()
        dashboard = self.client.get(f"/api/users/{user_id}/dashboard").json()
        by_product = {item["product_id"]: item for item in dashboard["verdicts"]}

        definitive_labels = [by_product[product_a_id]["label"], by_product[product_b_id]["label"]]
        self.assertEqual(sorted(definitive_labels), sorted(["investigate", "locked"]))
        locked_item = by_product[product_a_id] if by_product[product_a_id]["label"] == "locked" else by_product[product_b_id]
        self.assertEqual(locked_item["evidence"], {})

        # evidence_unclear is never locked, even though it wasn't the unlocked pick.
        self.assertEqual(by_product[product_c_id]["label"], "evidence_unclear")
        self.assertNotEqual(by_product[product_c_id]["generated_text"], "Upgrade to Premium to see this verdict and get unlimited product verdicts.")

        self.assertTrue(dashboard["features"]["product_verdicts_unlocked"])
        self.assertEqual(self.service._usage(user_id, "product_verdicts"), 1)

    def test_free_tier_quota_stays_unlocked_on_the_same_product(self):
        user_id, product_a_id, product_b_id, _ = self._seed_free_tier_verdict_scenario()
        first = self.client.get(f"/api/users/{user_id}/dashboard").json()
        unlocked_first = next(item["product_id"] for item in first["verdicts"] if item["product_id"] in (product_a_id, product_b_id) and item["label"] != "locked")

        second = self.client.get(f"/api/users/{user_id}/dashboard").json()
        unlocked_second = next(item["product_id"] for item in second["verdicts"] if item["product_id"] in (product_a_id, product_b_id) and item["label"] != "locked")

        self.assertEqual(unlocked_first, unlocked_second)
        locked_count = sum(1 for item in second["verdicts"] if item["label"] == "locked")
        self.assertEqual(locked_count, 1)
        # Re-evaluating the dashboard must not re-trigger the one-time unlock counter.
        self.assertEqual(self.service._usage(user_id, "product_verdicts"), 1)

    # -- 4. free-tier history cap -------------------------------------------------

    def test_free_tier_history_is_capped_to_90_days_but_premium_sees_all(self):
        user_id = self.create_user()
        now = datetime.now(timezone.utc)
        old_capture = self.service.create_capture(user_id, image_bytes(), captured_at=(now - timedelta(days=200)).isoformat())
        recent_capture = self.service.create_capture(user_id, image_bytes(), captured_at=(now - timedelta(days=1)).isoformat())

        free_history = self.service.history(user_id)
        free_ids = {item["id"] for item in free_history}
        self.assertNotIn(old_capture["id"], free_ids)
        self.assertIn(recent_capture["id"], free_ids)

        http_free_history = self.client.get(f"/api/users/{user_id}/history").json()
        self.assertNotIn(old_capture["id"], {item["id"] for item in http_free_history})

        self.assertEqual(self.client.post(f"/api/users/{user_id}/subscription/upgrade", json={}).status_code, 200)
        premium_history = self.client.get(f"/api/users/{user_id}/history").json()
        premium_ids = {item["id"] for item in premium_history}
        self.assertIn(old_capture["id"], premium_ids)
        self.assertIn(recent_capture["id"], premium_ids)

    # -- 5. commerce is free ------------------------------------------------------

    def test_commerce_offers_are_not_premium_gated(self):
        user_id = self.create_user()
        product = self.create_product("Commerce Cleanser")
        offer = self.client.post("/api/admin/offers", json={"product_id": product["id"], "merchant": "Acme", "url": "https://example.com/p", "price_cents": 1500}, headers={"Authorization": "Bearer test-admin-token"})
        self.assertEqual(offer.status_code, 200)
        offer_id = offer.json()["id"]

        offers_response = self.client.get(f"/api/users/{user_id}/commerce/offers")
        self.assertEqual(offers_response.status_code, 200)
        self.assertTrue(any(item["id"] == offer_id for item in offers_response.json()))

        click_response = self.client.post(f"/api/users/{user_id}/commerce/offers/{offer_id}/click")
        self.assertEqual(click_response.status_code, 200)

    # -- 6. shelf scan --------------------------------------------------------

    def test_shelf_scan_without_vision_configured_then_manual_confirm(self):
        user_id = self.create_user()
        scan = self.client.post(f"/api/users/{user_id}/shelf-scan", json={"image_base64": sample_image_b64()})
        self.assertEqual(scan.status_code, 200)
        self.assertEqual(scan.json()["status"], "queued")
        job_id = scan.json()["job_id"]

        job = self.poll_job(user_id, job_id, "shelf-scan")
        self.assertEqual(job["status"], "completed")
        self.assertEqual(job["result"]["candidates"], [])
        self.assertIn("not configured", job["result"]["message"])

        confirm = self.client.post(f"/api/users/{user_id}/shelf-scan/{job_id}/confirm", json={
            "selections": [{"name": "Manually Found Cleanser", "category": "cleanser", "ingredients": "Water", "stabilization_days": 10}],
        })
        self.assertEqual(confirm.status_code, 200)
        created = confirm.json()
        self.assertEqual(len(created), 1)
        self.assertEqual(created[0]["name"], "Manually Found Cleanser")
        self.assertEqual(created[0]["stabilization_days"], 10)

    # -- 7. pre-purchase prediction (premium) -------------------------------------

    def test_predict_product_is_premium_gated_and_reports_ingredient_overlap(self):
        free_user_id = self.create_user()
        owned_product = self.create_product("Own Investigate Serum", stabilization_days=0, ingredients="Niacinamide, Retinol")
        candidate_product = self.create_product("Candidate Toner", stabilization_days=0, ingredients="NIACINAMIDE, Vitamin C")

        blocked = self.client.get(f"/api/products/{candidate_product['id']}/predict?user_id={free_user_id}")
        self.assertEqual(blocked.status_code, 403)

        premium_user_id = self.create_user(premium=True)
        owned_for_premium = self.create_product("Premium Investigate Serum", stabilization_days=0, ingredients="Niacinamide, Retinol")
        self.seed_verdict(premium_user_id, owned_for_premium["id"], datetime(2026, 1, 1, tzinfo=timezone.utc), direction="investigate")
        # predict_product reads the persisted verdicts table directly, so force
        # a refresh_verdicts pass (as the dashboard would) before predicting.
        self.service.refresh_verdicts(premium_user_id)

        prediction = self.client.get(f"/api/products/{candidate_product['id']}/predict?user_id={premium_user_id}")
        self.assertEqual(prediction.status_code, 200)
        payload = prediction.json()
        self.assertEqual(payload["product_id"], candidate_product["id"])
        self.assertEqual(len(payload["overlap_with_investigate"]), 1)
        overlap = payload["overlap_with_investigate"][0]
        self.assertEqual(overlap["product_name"], "Premium Investigate Serum")
        self.assertEqual(overlap["shared_ingredients"], ["niacinamide"])
        self.assertIn("investigate", payload["headline"])

    # -- 8. root-cause search (premium) -------------------------------------------

    def test_root_cause_search_is_premium_gated_and_returns_valid_shape(self):
        free_user_id = self.create_user()
        self.assertEqual(self.client.get(f"/api/users/{free_user_id}/root-cause").status_code, 403)

        user_id = self.create_user(premium=True)
        now = datetime.now(timezone.utc)
        for index in range(6):
            self.service.create_capture(user_id, image_bytes(), captured_at=(now - timedelta(days=30 - index * 5)).isoformat())
            self.client.post(f"/api/users/{user_id}/context-events", json={"event_type": "sleep", "occurred_at": (now - timedelta(days=29 - index * 5)).isoformat()})

        response = self.client.get(f"/api/users/{user_id}/root-cause")
        self.assertEqual(response.status_code, 200)
        correlations = response.json()
        self.assertIsInstance(correlations, list)
        for item in correlations:
            self.assertIn("event_type", item)
            self.assertIn("normalized_effect", item)
            self.assertIn("message", item)

    # -- 9. routine budget optimizer (premium) -------------------------------------

    def test_budget_optimizer_is_premium_gated_and_estimates_cost_for_stable_keep(self):
        free_user_id = self.create_user()
        self.assertEqual(self.client.get(f"/api/users/{free_user_id}/budget-optimizer").status_code, 403)

        user_id = self.create_user(premium=True)
        product = self.create_product("Stable Keep Moisturizer", stabilization_days=0)
        start = datetime(2026, 1, 1, tzinfo=timezone.utc)
        self.seed_verdict(user_id, product["id"], start, direction="keep", before_offsets=(5, 3, 1), after_offsets=(35, 37, 40))
        offer = self.client.post("/api/admin/offers", json={"product_id": product["id"], "merchant": "Acme", "url": "https://example.com/p", "price_cents": 3000}, headers={"Authorization": "Bearer test-admin-token"})
        self.assertEqual(offer.status_code, 200)

        response = self.client.get(f"/api/users/{user_id}/budget-optimizer")
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        flagged = {item["product_id"]: item for item in payload["flagged"]}
        self.assertIn(product["id"], flagged)
        self.assertGreaterEqual(flagged[product["id"]]["days_stable"], 30)
        self.assertIsNotNone(flagged[product["id"]]["estimated_annual_cost_cents"])
        self.assertGreater(flagged[product["id"]]["estimated_annual_cost_cents"], 0)

    # -- 10. dermatologist export (premium) -----------------------------------------

    def test_derm_export_is_premium_gated_and_returns_printable_html(self):
        free_user_id = self.create_user()
        self.assertEqual(self.client.get(f"/api/users/{free_user_id}/derm-export").status_code, 403)

        user_id = self.create_user(premium=True)
        product = self.create_product("Export Product", stabilization_days=0)
        self.seed_verdict(user_id, product["id"], datetime(2026, 1, 1, tzinfo=timezone.utc), direction="investigate")

        response = self.client.get(f"/api/users/{user_id}/derm-export")
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertIsInstance(payload["printable_html"], str)
        self.assertGreater(len(payload["printable_html"]), 0)
        self.assertIn("<h1>", payload["printable_html"])

    # -- 11. experiment early-stop --------------------------------------------------

    def test_experiment_early_stop_flags_conclusive_evidence_before_target_days(self):
        user_id = self.create_user(premium=True)
        product = self.create_product("Early Stop Serum", stabilization_days=0)
        experiment = self.client.post("/api/experiments", json={
            "user_id": user_id, "name": "Early stop check", "product_id": product["id"], "target_days": 30,
        })
        self.assertEqual(experiment.status_code, 200)
        experiment_id = experiment.json()["id"]

        start = datetime.now(timezone.utc) - timedelta(days=10)
        self.seed_verdict(user_id, product["id"], start, direction="investigate", before_offsets=(5, 3, 1), after_offsets=(1, 3, 5))

        detail = self.client.get(f"/api/users/{user_id}/experiments/{experiment_id}").json()
        early_stop = detail["early_stop"]
        self.assertTrue(early_stop["conclusive"])
        self.assertEqual(early_stop["recommended_status"], "investigate")

        # A fresh experiment with no capture evidence yet is not conclusive.
        fresh_product = self.create_product("No Evidence Yet Serum", stabilization_days=0)
        fresh_experiment = self.client.post("/api/experiments", json={
            "user_id": user_id, "name": "No evidence yet", "product_id": fresh_product["id"], "target_days": 30,
        }).json()
        fresh_detail = self.client.get(f"/api/users/{user_id}/experiments/{fresh_experiment['id']}").json()
        self.assertFalse(fresh_detail["early_stop"]["conclusive"])


if __name__ == "__main__":
    unittest.main()
