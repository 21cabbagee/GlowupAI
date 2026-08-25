from __future__ import annotations

import base64
import io
import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from skinproof.api import create_app
from skinproof.db import Database
from skinproof.photos import MemoryPhotoStore
from skinproof.service import SkinProofService


def png() -> str:
    image = Image.new("RGB", (240, 240), (210, 165, 145))
    draw = ImageDraw.Draw(image)
    for y in range(0, 240, 8):
        for x in range(0, 240, 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=(165, 130, 118))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode()


QUALITY = {"face_present": True, "yaw_degrees": 0, "pitch_degrees": 0, "distance_cm": 45, "expression_neutral": True}


class ApiTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.db = Database(Path(self.temp.name) / "api.sqlite3")
        app = create_app(SkinProofService(self.db, photos=MemoryPhotoStore()))
        self.client = TestClient(app)

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def test_http_core_loop(self):
        user = self.client.post("/api/users", json={"skin_type": "combination"})
        self.assertEqual(user.status_code, 200)
        user_id = user.json()["id"]
        self.assertEqual(self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True}).status_code, 200)
        product = self.client.post("/api/products", json={"name": "API serum", "stabilization_days": 0, "ingredients": "Water, Niacinamide"})
        self.assertEqual(product.status_code, 200)
        product_id = product.json()["id"]
        self.assertEqual(self.client.post("/api/routine-events", json={"user_id": user_id, "product_id": product_id, "action": "start"}).status_code, 200)
        capture = self.client.post("/api/captures", json={"user_id": user_id, "image_base64": png(), "quality": QUALITY, "is_baseline": True})
        self.assertEqual(capture.status_code, 200)
        self.assertEqual(capture.json()["metric"]["model_version"], "deterministic-3.0")
        dashboard = self.client.get(f"/api/users/{user_id}/dashboard")
        self.assertEqual(dashboard.status_code, 200)
        self.assertEqual(len(dashboard.json()["history"]), 1)

    def test_photo_capture_requires_explicit_consent(self):
        user_id = self.client.post("/api/users", json={}).json()["id"]
        response = self.client.post("/api/captures", json={"user_id": user_id, "image_base64": png(), "quality": QUALITY})
        self.assertEqual(response.status_code, 403)


if __name__ == "__main__":
    unittest.main()
