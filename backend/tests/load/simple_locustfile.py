"""Simplified load tests for GlowupAI backend.

Tests the three key endpoints mentioned in requirements:
- GET /api/users/:id/dashboard (should be fast with caching)
- POST /api/captures (may be slow, acceptable)
- GET /api/users/:id/history

Run with:
    locust -f simple_locustfile.py --host=http://localhost:8000
"""

import base64
import io
import random
from typing import Any

from locust import HttpUser, between, task
from PIL import Image, ImageDraw


def create_test_image(size=(240, 240)):
    """Create a test face image."""
    color = (
        random.randint(150, 250),
        random.randint(100, 220),
        random.randint(80, 200),
    )
    image = Image.new("RGB", size, color)
    draw = ImageDraw.Draw(image)

    # Add some texture
    for y in range(0, size[1], 8):
        for x in range(0, size[0], 8):
            if (x // 8 + y // 8) % 2:
                fill_color = tuple(max(0, c - 30) for c in color)
                draw.rectangle((x, y, x + 3, y + 3), fill=fill_color)

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


class GlowupUser(HttpUser):
    """Simulated user testing key endpoints."""

    wait_time = between(1, 3)

    def on_start(self):
        """Initialize user session - create user and grant consent."""
        # Create user
        response = self.client.post(
            "/api/users",
            json={
                "firebase_uid": f"load_test_user_{random.randint(1000, 999999)}",
                "email": f"loadtest{random.randint(1000, 999999)}@example.com",
                "skin_type": random.choice(["normal", "dry", "oily", "combination"]),
            },
            name="/api/users",
        )

        if response.status_code == 200:
            try:
                self.user_id = response.json()["user"]["id"]

                # Grant consent
                consent_response = self.client.post(
                    f"/api/users/{self.user_id}/consent",
                    json={"facial_data": True, "analytics": True},
                    name="/api/users/:id/consent",
                )

                # Take baseline capture
                if consent_response.status_code == 200:
                    self.client.post(
                        "/api/captures",
                        json={
                            "user_id": self.user_id,
                            "image_base64": create_test_image(),
                            "quality": QUALITY,
                            "is_baseline": True,
                        },
                        name="/api/captures [baseline]",
                    )
            except (KeyError, ValueError) as e:
                self.user_id = None
        else:
            self.user_id = None

    @task(5)
    def view_dashboard(self):
        """GET /api/users/:id/dashboard - Should be fast with caching."""
        if not self.user_id:
            return

        with self.client.get(
            f"/api/users/{self.user_id}/dashboard",
            name="/api/dashboard",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    if "history" in data or "streak" in data:
                        response.success()
                    else:
                        response.failure("Invalid dashboard structure")
                except Exception as e:
                    response.failure(f"JSON parse error: {e}")
            else:
                response.failure(f"Status: {response.status_code}")

    @task(3)
    def take_capture(self):
        """POST /api/captures - May be slow, acceptable."""
        if not self.user_id:
            return

        with self.client.post(
            "/api/captures",
            json={
                "user_id": self.user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
                "is_baseline": False,
            },
            name="/api/captures/analyze",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    # Accept any successful response structure
                    response.success()
                except Exception as e:
                    response.failure(f"JSON parse error: {e}")
            elif response.status_code == 403:
                # Consent issue - not a load test failure
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")

    @task(2)
    def view_history(self):
        """GET /api/users/:id/history - Performance critical."""
        if not self.user_id:
            return

        with self.client.get(
            f"/api/users/{self.user_id}/history",
            name="/api/history",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    response.success()
                except Exception as e:
                    response.failure(f"JSON parse error: {e}")
            elif response.status_code == 404:
                # New user with no history is acceptable
                response.success()
            else:
                response.failure(f"Status: {response.status_code}")


if __name__ == "__main__":
    print("Load test for key endpoints:")
    print("- GET /api/dashboard (should be fast with caching)")
    print("- POST /api/captures/analyze (may be slow, acceptable)")
    print("- GET /api/history")
    print("")
    print("Run with:")
    print("  locust -f simple_locustfile.py --users 10 --spawn-rate 2")
