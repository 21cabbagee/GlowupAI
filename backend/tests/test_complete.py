from __future__ import annotations

import base64
import io
import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from skinproof.complete_api import create_complete_app
from skinproof.complete_db import FullDatabase
from skinproof.complete_service import CompleteSkinProofService
from skinproof.config import Settings
from skinproof.photos import MemoryPhotoStore


def sample_image() -> str:
    image = Image.new("RGB", (240, 240), (210, 165, 145))
    draw = ImageDraw.Draw(image)
    for y in range(0, 240, 8):
        for x in range(0, 240, 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=(165, 130, 118))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode()


class CompleteApiTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = FullDatabase(Path(self.temp.name) / "complete.sqlite3")
        test_settings = Settings(db_path=Path(self.temp.name) / "complete.sqlite3", photo_dir=None, gemini_api_key=None, gemini_enabled=False)
        self.service = CompleteSkinProofService(self.db, settings=test_settings, photos=MemoryPhotoStore())
        self.client = TestClient(create_complete_app(self.service))

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def create_premium_user(self):
        user = self.client.post("/api/users", json={"skin_type": "combination"}).json()
        user_id = user["user"]["id"]
        self.assertEqual(self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True}).status_code, 200)
        self.assertEqual(self.client.post(f"/api/users/{user_id}/subscription/upgrade", json={}).status_code, 200)
        return user_id

    def test_complete_core_loop_and_premium_features(self):
        user_id = self.create_premium_user()
        product = self.client.post("/api/products", json={"name": "Complete serum", "category": "serum", "stabilization_days": 0, "ingredients": "Water, Niacinamide"}).json()
        experiment = self.client.post("/api/experiments", json={"user_id": user_id, "name": "Serum experiment", "hypothesis": "redness improves", "product_id": product["id"], "target_days": 7})
        self.assertEqual(experiment.status_code, 200)
        experiment_id = experiment.json()["id"]
        self.assertEqual(self.client.post("/api/routine-events", json={"user_id": user_id, "product_id": product["id"], "action": "start", "experiment_id": experiment_id}).status_code, 200)
        capture = self.client.post("/api/captures", json={"user_id": user_id, "image_base64": sample_image(), "vertical": "skin", "is_baseline": True, "experiment_id": experiment_id})
        self.assertEqual(capture.status_code, 200)
        dashboard = self.client.get(f"/api/users/{user_id}/dashboard")
        self.assertEqual(dashboard.status_code, 200)
        self.assertEqual(len(dashboard.json()["history"]), 1)
        self.assertEqual(self.client.post(f"/api/users/{user_id}/qna", json={"question": "How did redness change?"}).status_code, 200)
        self.assertEqual(self.client.get(f"/api/users/{user_id}/discover").status_code, 200)
        ingredient = self.client.get(f"/api/products/{product['id']}/ingredient-explainer?user_id={user_id}")
        self.assertEqual(ingredient.status_code, 200)
        self.assertEqual(self.client.post(f"/api/users/{user_id}/reprocess", json={"model_version": "deterministic-v2"}).status_code, 200)

    def test_verticals_and_capture_guidance(self):
        user_id = self.create_premium_user()
        capture = self.client.post("/api/captures", json={"user_id": user_id, "image_base64": sample_image(), "vertical": "skin", "is_baseline": True})
        self.assertEqual(capture.status_code, 200)
        self.assertEqual(capture.json()["vertical"], "skin")
        self.assertIn("texture_score", capture.json()["appearance_metrics"])
        guide = self.client.get(f"/api/users/{user_id}/capture-guide?vertical=skin")
        self.assertEqual(guide.status_code, 200)
        self.assertEqual(guide.json()["state"], "scheduled")

    def test_free_plan_gates_premium_features_and_keeps_history(self):
        user = self.client.post("/api/users", json={}).json()
        user_id = user["user"]["id"]
        product = self.client.post("/api/products", json={"name": "Free cleanser"}).json()
        response = self.client.post("/api/experiments", json={"user_id": user_id, "name": "Blocked", "product_id": product["id"]})
        self.assertEqual(response.status_code, 403)
        self.assertEqual(self.client.get(f"/api/users/{user_id}/discover").status_code, 403)

    def test_cancel_subscription_records_billing_event(self):
        user_id = self.create_premium_user()
        response = self.client.post(f"/api/users/{user_id}/subscription/cancel")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["plan"], "free")
        self.assertEqual(response.json()["status"], "cancelled")


if __name__ == "__main__":
    unittest.main()
