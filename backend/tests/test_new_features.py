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


def image_b64() -> str:
    image = Image.new("RGB", (240, 240), (210, 165, 145))
    draw = ImageDraw.Draw(image)
    for y in range(0, 240, 8):
        for x in range(0, 240, 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=(165, 130, 118))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode()


class NewFeatureTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        path = Path(self.temp.name) / "features.sqlite3"
        self.db = FullDatabase(path)
        settings = Settings(db_path=path, photo_dir=None, gemini_enabled=False, admin_token="test-admin-token")
        self.service = CompleteSkinProofService(self.db, settings=settings, photos=MemoryPhotoStore())
        self.client = TestClient(create_complete_app(self.service))

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def user(self, premium: bool = True) -> str:
        created = self.client.post("/api/users", json={}).json()
        user_id = created["user"]["id"]
        self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True})
        if premium:
            self.client.post(f"/api/users/{user_id}/subscription/upgrade", json={})
        return user_id

    def test_checkin_recap_feedback_and_quality_copy_are_connected(self):
        user_id = self.user()
        first = self.client.post(
            "/api/captures",
            json={"user_id": user_id, "image_base64": image_b64(), "is_baseline": True, "captured_at": "2026-08-01T10:00:00Z"},
        )
        self.assertEqual(first.status_code, 200)
        self.assertIn("measurement", first.json())
        self.assertIn("confidence_message", first.json()["measurement"])
        second = self.client.post(
            "/api/captures",
            json={"user_id": user_id, "image_base64": image_b64(), "captured_at": "2026-08-05T10:00:00Z"},
        )
        self.assertEqual(second.status_code, 200)
        capture_id = second.json()["capture"]["id"]
        check_in = self.client.post(f"/api/users/{user_id}/check-ins", json={"routine_state": "steady", "skin_feel": "same"})
        self.assertEqual(check_in.status_code, 200)
        recap = self.client.get(f"/api/users/{user_id}/weekly-recap?as_of=2026-08-05T10:00:00Z")
        self.assertEqual(recap.status_code, 200)
        self.assertIn(recap.json()["status"], {"directional", "steady"})
        dashboard = self.client.get(f"/api/users/{user_id}/dashboard").json()
        self.assertIn("weekly_recap", dashboard)
        self.assertEqual(len(dashboard["check_ins"]), 1)
        feedback = self.client.post(f"/api/users/{user_id}/measurement-feedback", json={"capture_id": capture_id, "agreement": "fair"})
        self.assertEqual(feedback.status_code, 200)
        self.assertEqual(self.client.get("/api/admin/measurement-feedback", headers={"Authorization": "Bearer test-admin-token"}).json()["counts"]["fair"], 1)

    def test_purchase_guidance_lookup_and_premium_gate(self):
        free_user = self.user(premium=False)
        blocked = self.client.post(f"/api/users/{free_user}/purchase-guidance", json={"name": "Test serum", "ingredients": "Water, Niacinamide"})
        self.assertEqual(blocked.status_code, 403)

        user_id = self.user()
        product = self.client.post("/api/products", json={"name": "Known serum", "barcode": "890123", "ingredients": "Water, Niacinamide"}).json()
        lookup = self.client.get("/api/products/lookup?barcode=890123")
        self.assertEqual(lookup.status_code, 200)
        self.assertEqual(lookup.json()["id"], product["id"])
        guidance = self.client.post(f"/api/users/{user_id}/purchase-guidance", json={"barcode": "890123", "price_cents": 120000})
        self.assertEqual(guidance.status_code, 200)
        self.assertEqual(guidance.json()["product_id"], product["id"])
        self.assertIn("next_action", guidance.json())


if __name__ == "__main__":
    unittest.main()
