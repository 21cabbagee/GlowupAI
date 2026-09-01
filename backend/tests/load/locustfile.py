"""Load tests for GlowupAI backend using Locust.

Run with:
    locust -f tests/load/locustfile.py --host=http://localhost:8000

Target metrics:
- 100 concurrent users
- p95 response time < 2s for capture endpoint
- No errors under normal load
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
    """Simulated user behavior for load testing."""

    wait_time = between(1, 3)  # Wait 1-3 seconds between tasks

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
            name="/api/users [signup]",
        )

        if response.status_code == 200:
            self.user_id = response.json()["user"]["id"]

            # Grant consent
            self.client.post(
                f"/api/users/{self.user_id}/consent",
                json={"facial_data": True, "analytics": True},
                name="/api/users/:id/consent",
            )

            # Take baseline capture
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
        else:
            self.user_id = None

    @task(10)
    def take_capture(self):
        """Take a capture - most common action."""
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
            name="/api/captures",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                data = response.json()
                if "metric" in data and "smoothness_score" in data["metric"]:
                    response.success()
                else:
                    response.failure("Invalid response structure")
            else:
                response.failure(f"Got status {response.status_code}")

    @task(5)
    def view_dashboard(self):
        """View dashboard - frequent action."""
        if not self.user_id:
            return

        with self.client.get(
            f"/api/users/{self.user_id}/dashboard",
            name="/api/users/:id/dashboard",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                data = response.json()
                if "history" in data:
                    response.success()
                else:
                    response.failure("Invalid dashboard structure")
            else:
                response.failure(f"Got status {response.status_code}")

    @task(2)
    def create_product(self):
        """Create a product - less frequent."""
        products = [
            {
                "name": "Vitamin C Serum",
                "brand": "Test Brand",
                "ingredients": "Water, Ascorbic Acid, Vitamin E",
            },
            {
                "name": "Retinol Cream",
                "brand": "Another Brand",
                "ingredients": "Water, Retinol, Hyaluronic Acid",
            },
            {
                "name": "Niacinamide Serum",
                "brand": "Brand X",
                "ingredients": "Water, Niacinamide, Zinc",
            },
        ]

        product = random.choice(products)
        self.client.post("/api/products", json=product, name="/api/products")

    @task(3)
    def manage_routine(self):
        """Start/stop products in routine."""
        if not self.user_id:
            return

        # Create a product first
        product_response = self.client.post(
            "/api/products",
            json={
                "name": f"Product {random.randint(1, 1000)}",
                "ingredients": "Water, Active Ingredient",
            },
            name="/api/products [for routine]",
        )

        if product_response.status_code == 200:
            product_data = product_response.json()
            product_id = product_data.get("id") or product_data.get("product", {}).get(
                "id"
            )

            # Start using product
            self.client.post(
                "/api/routine-events",
                json={
                    "user_id": self.user_id,
                    "product_id": product_id,
                    "action": random.choice(["start", "stop"]),
                },
                name="/api/routine-events",
            )

    @task(1)
    def view_roadmap(self):
        """View product roadmap - informational endpoint."""
        self.client.get("/api/roadmap", name="/api/roadmap")


class AdminUser(HttpUser):
    """Simulated admin user for testing analytics endpoints."""

    wait_time = between(5, 10)

    @task
    def check_analytics(self):
        """Check analytics dashboard."""
        # Note: This assumes admin endpoints exist
        # Adjust based on actual admin API structure
        self.client.get("/api/admin/stats", name="/api/admin/stats")


class MobileAppUser(HttpUser):
    """Simulated mobile app user with realistic usage pattern."""

    wait_time = between(2, 5)

    def on_start(self):
        """App launch - create/login user."""
        self.user_id = None

        # Simulate returning user or new signup
        if random.random() < 0.7:  # 70% returning users
            # Simulate login (in real app, would fetch existing user)
            response = self.client.post(
                "/api/users",
                json={
                    "firebase_uid": f"mobile_user_{random.randint(1, 100)}",
                    "email": f"mobile{random.randint(1, 100)}@example.com",
                },
                name="/api/users [login attempt]",
            )
            if response.status_code == 200:
                self.user_id = response.json()["user"]["id"]

    @task(8)
    def daily_capture(self):
        """Daily capture - most common mobile action."""
        if not self.user_id:
            return

        # Ensure consent
        self.client.post(
            f"/api/users/{self.user_id}/consent",
            json={"facial_data": True},
            name="/api/users/:id/consent [ensure]",
        )

        # Take capture
        self.client.post(
            "/api/captures",
            json={
                "user_id": self.user_id,
                "image_base64": create_test_image(),
                "quality": QUALITY,
            },
            name="/api/captures [mobile]",
        )

    @task(3)
    def check_progress(self):
        """Check progress on dashboard."""
        if not self.user_id:
            return

        self.client.get(
            f"/api/users/{self.user_id}/dashboard",
            name="/api/users/:id/dashboard [mobile]",
        )

    @task(1)
    def manage_products(self):
        """Browse and manage products."""
        # View products
        self.client.get("/api/products", name="/api/products [list]")

        # Maybe add a product
        if random.random() < 0.3:
            self.client.post(
                "/api/products",
                json={"name": "Mobile Product", "ingredients": "Water, Active"},
                name="/api/products [mobile add]",
            )


# Run configurations for different scenarios
# Note: These are not meant to be used directly, just documented patterns


if __name__ == "__main__":
    print("Load test scenarios:")
    print("1. Default: locust -f locustfile.py")
    print("2. Quick test: locust -f locustfile.py --users 10 --spawn-rate 2")
    print("3. Stress test: locust -f locustfile.py --users 200 --spawn-rate 10")
    print(
        "4. Soak test: locust -f locustfile.py --users 50 --spawn-rate 5 --run-time 1h"
    )
