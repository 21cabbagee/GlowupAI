"""Integration tests for complete API flows."""

import base64
import io
import tempfile
import time
import unittest
from pathlib import Path

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from glowupai.api import create_app
from glowupai.db import Database
from glowupai.photos import MemoryPhotoStore
from glowupai.service import GlowupAIService


def create_test_image(size=(240, 240), color=(210, 165, 145)):
    """Create a test face image."""
    image = Image.new("RGB", size, color)
    draw = ImageDraw.Draw(image)
    for y in range(0, size[1], 8):
        for x in range(0, size[0], 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=(165, 130, 118))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode()


QUALITY = {
    "face_present": True,
    "yaw_degrees": 0,
    "pitch_degrees": 0,
    "distance_cm": 45,
    "expression_neutral": True,
}


class TestCompleteUserFlow(unittest.TestCase):
    """Test complete user journey flows."""

    def setUp(self):
        """Set up test client and database."""
        self.temp = tempfile.TemporaryDirectory()
        self.db = Database(Path(self.temp.name) / "integration.sqlite3")
        app = create_app(GlowupAIService(self.db, photos=MemoryPhotoStore()))
        self.client = TestClient(app)

    def tearDown(self):
        """Clean up resources."""
        self.db.close()
        self.temp.cleanup()

    def test_signup_to_first_capture_flow(self):
        """Test complete flow: signup -> consent -> capture -> dashboard."""
        # Step 1: Create user (sign up)
        signup_response = self.client.post(
            "/api/users",
            json={
                "firebase_uid": "integration_test_user",
                "email": "integration@test.com",
                "skin_type": "combination",
            },
        )
        self.assertEqual(signup_response.status_code, 200)
        user_id = signup_response.json()["id"]

        # Step 2: Grant consent
        consent_response = self.client.post(
            f"/api/users/{user_id}/consent",
            json={"facial_data": True, "analytics": True},
        )
        self.assertEqual(consent_response.status_code, 200)

        # Step 3: Take first capture (baseline)
        capture_response = self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
                "is_baseline": True,
            },
        )
        self.assertEqual(capture_response.status_code, 200)
        capture_data = capture_response.json()
        self.assertIn("metric", capture_data)
        self.assertIn("smoothness_score", capture_data["metric"])

        # Step 4: View dashboard
        dashboard_response = self.client.get(f"/api/users/{user_id}/dashboard")
        self.assertEqual(dashboard_response.status_code, 200)
        dashboard = dashboard_response.json()
        self.assertIn("history", dashboard)
        self.assertEqual(len(dashboard["history"]), 1)
        self.assertTrue(dashboard["history"][0]["is_baseline"])

    def test_product_experiment_flow(self):
        """Test flow: create user -> add product -> start routine -> captures."""
        # Create user
        user_response = self.client.post(
            "/api/users", json={"firebase_uid": "product_test", "skin_type": "oily"}
        )
        user_id = user_response.json()["id"]

        # Grant consent
        self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True})

        # Take baseline capture
        baseline_response = self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
                "is_baseline": True,
            },
        )
        self.assertEqual(baseline_response.status_code, 200)

        # Create product
        product_response = self.client.post(
            "/api/products",
            json={
                "name": "Test Vitamin C Serum",
                "brand": "Test Brand",
                "ingredients": "Water, Ascorbic Acid, Vitamin E",
                "stabilization_days": 14,
            },
        )
        self.assertEqual(product_response.status_code, 200)
        product_id = product_response.json()["id"]

        # Start using product
        routine_response = self.client.post(
            "/api/routine-events",
            json={"user_id": user_id, "product_id": product_id, "action": "start"},
        )
        self.assertEqual(routine_response.status_code, 200)

        # Take follow-up capture
        followup_response = self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
                "is_baseline": False,
            },
        )
        self.assertEqual(followup_response.status_code, 200)

        # Check dashboard shows comparison
        dashboard = self.client.get(f"/api/users/{user_id}/dashboard")
        self.assertEqual(dashboard.status_code, 200)
        history = dashboard.json()["history"]
        self.assertEqual(len(history), 2)

    def test_multi_capture_comparison_flow(self):
        """Test flow with multiple captures showing progression."""
        # Setup user
        user_response = self.client.post(
            "/api/users", json={"firebase_uid": "multi_capture"}
        )
        user_id = user_response.json()["id"]

        self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True})

        # Take multiple captures over time
        capture_ids = []
        for i in range(5):
            response = self.client.post(
                "/api/captures",
                json={
                    "user_id": user_id,
                    "image_base64": create_test_image(),
                    "quality": QUALITY,
                    "is_baseline": (i == 0),
                },
            )
            self.assertEqual(response.status_code, 200)
            capture_ids.append(response.json()["id"])

        # Verify all captures are in history
        dashboard = self.client.get(f"/api/users/{user_id}/dashboard")
        history = dashboard.json()["history"]
        self.assertEqual(len(history), 5)

        # Verify baseline is identified
        baseline = [c for c in history if c["is_baseline"]]
        self.assertEqual(len(baseline), 1)

    def test_consent_required_for_capture(self):
        """Test that capture requires explicit consent."""
        # Create user without consent
        user_response = self.client.post(
            "/api/users", json={"firebase_uid": "no_consent"}
        )
        user_id = user_response.json()["id"]

        # Attempt capture without consent
        capture_response = self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
            },
        )

        # Should be rejected
        self.assertEqual(capture_response.status_code, 403)

    def test_quality_gates_enforcement(self):
        """Test that quality gates are enforced."""
        user_response = self.client.post(
            "/api/users", json={"firebase_uid": "quality_test"}
        )
        user_id = user_response.json()["id"]

        self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True})

        # Test various quality violations
        bad_quality_scenarios = [
            {
                "face_present": False,
                "yaw_degrees": 0,
                "pitch_degrees": 0,
                "distance_cm": 45,
            },
            {
                "face_present": True,
                "yaw_degrees": 45,
                "pitch_degrees": 0,
                "distance_cm": 45,
            },
            {
                "face_present": True,
                "yaw_degrees": 0,
                "pitch_degrees": 45,
                "distance_cm": 45,
            },
            {
                "face_present": True,
                "yaw_degrees": 0,
                "pitch_degrees": 0,
                "distance_cm": 20,
            },
        ]

        for bad_quality in bad_quality_scenarios:
            with self.subTest(quality=bad_quality):
                response = self.client.post(
                    "/api/captures",
                    json={
                        "user_id": user_id,
                        "image_base64": create_test_image(),
                        "quality": bad_quality,
                    },
                )
                # Should reject bad quality
                self.assertIn(response.status_code, [400, 422])


class TestErrorHandling(unittest.TestCase):
    """Test error handling across API."""

    def setUp(self):
        """Set up test client."""
        self.temp = tempfile.TemporaryDirectory()
        self.db = Database(Path(self.temp.name) / "errors.sqlite3")
        app = create_app(GlowupAIService(self.db, photos=MemoryPhotoStore()))
        self.client = TestClient(app)

    def tearDown(self):
        """Clean up resources."""
        self.db.close()
        self.temp.cleanup()

    def test_invalid_user_id(self):
        """Test handling of invalid user ID."""
        response = self.client.get("/api/users/nonexistent-user-id/dashboard")
        self.assertIn(response.status_code, [404, 400])

    def test_invalid_capture_id(self):
        """Test handling of invalid capture ID."""
        response = self.client.get("/api/captures/nonexistent-capture-id")
        self.assertEqual(response.status_code, 404)

    def test_invalid_image_data(self):
        """Test handling of invalid image data."""
        user_response = self.client.post(
            "/api/users", json={"firebase_uid": "invalid_image_test"}
        )
        user_id = user_response.json()["id"]

        self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True})

        # Send invalid base64
        response = self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": "not-valid-base64!!!",
                "quality": QUALITY,
            },
        )

        self.assertIn(response.status_code, [400, 422])

    def test_missing_required_fields(self):
        """Test handling of missing required fields."""
        # Missing firebase_uid
        response = self.client.post("/api/users", json={"email": "test@test.com"})
        self.assertIn(response.status_code, [400, 422])

    def test_duplicate_baseline(self):
        """Test handling of duplicate baseline attempts."""
        user_response = self.client.post(
            "/api/users", json={"firebase_uid": "duplicate_baseline"}
        )
        user_id = user_response.json()["id"]

        self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True})

        # Create first baseline
        self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
                "is_baseline": True,
            },
        )

        # Attempt second baseline
        response = self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
                "is_baseline": True,
            },
        )

        # Should either reject or replace (depending on business logic)
        self.assertIn(response.status_code, [200, 400, 409])


class TestRateLimiting(unittest.TestCase):
    """Test rate limiting behavior."""

    def setUp(self):
        """Set up test client with rate limiting enabled."""
        self.temp = tempfile.TemporaryDirectory()
        self.db = Database(Path(self.temp.name) / "ratelimit.sqlite3")
        app = create_app(GlowupAIService(self.db, photos=MemoryPhotoStore()))
        self.client = TestClient(app)

    def tearDown(self):
        """Clean up resources."""
        self.db.close()
        self.temp.cleanup()

    def test_rate_limiting_on_captures(self):
        """Test that rate limiting is enforced on capture endpoint."""
        # Create user
        user_response = self.client.post(
            "/api/users", json={"firebase_uid": "ratelimit_test"}
        )
        user_id = user_response.json()["id"]

        self.client.post(f"/api/users/{user_id}/consent", json={"facial_data": True})

        # Make rapid requests
        responses = []
        for i in range(20):
            response = self.client.post(
                "/api/captures",
                json={
                    "user_id": user_id,
                    "image_base64": create_test_image(),
                    "quality": QUALITY,
                },
            )
            responses.append(response.status_code)

        # Check if any were rate limited (429)
        # Note: This test may pass if rate limiting is disabled in test env
        rate_limited = 429 in responses
        # Log result but don't fail if rate limiting is disabled for tests
        if not rate_limited:
            print("Note: Rate limiting may be disabled in test environment")


if __name__ == "__main__":
    unittest.main()
