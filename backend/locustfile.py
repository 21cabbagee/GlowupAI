"""
Locust load testing file for GlowupAI API.

Run with:
  locust -f locustfile.py --host=http://localhost:8000 --users=50 --spawn-rate=5 --run-time=60s
"""

import os
import time
import jwt
from locust import HttpUser, task, between, events


class GlowupAIUser(HttpUser):
    """Simulated user for GlowupAI API."""

    wait_time = between(1, 3)  # Wait 1-3 seconds between requests

    def on_start(self):
        """Setup - called once per user when they start."""
        # Generate test token
        self.user_id = f"test_user_{self.environment.runner.user_count}"
        self.token = self._generate_token()
        self.headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
        }

    def _generate_token(self) -> str:
        """Generate a test JWT token."""
        secret = os.environ.get("JWT_SECRET", "test-secret-key-for-performance-testing")
        payload = {
            "sub": self.user_id,
            "email": f"{self.user_id}@example.com",
            "email_verified": True,
            "name": f"Test User {self.user_id}",
            "iat": int(time.time()),
            "exp": int(time.time()) + 3600,
        }
        return jwt.encode(payload, secret, algorithm="HS256")

    @task(5)  # Weight of 5 - most common operation
    def view_dashboard(self):
        """View user dashboard."""
        with self.client.get(
            f"/api/users/{self.user_id}/dashboard",
            headers=self.headers,
            catch_response=True,
            name="/api/users/[user_id]/dashboard",
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                # Expected for new users without data
                response.success()
            else:
                response.failure(f"Got status {response.status_code}")

    @task(3)
    def view_history(self):
        """View user history."""
        with self.client.get(
            f"/api/users/{self.user_id}/history",
            headers=self.headers,
            catch_response=True,
            name="/api/users/[user_id]/history",
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Got status {response.status_code}")

    @task(2)
    def view_profile(self):
        """View user profile."""
        with self.client.get(
            f"/api/users/{self.user_id}/profile",
            headers=self.headers,
            catch_response=True,
            name="/api/users/[user_id]/profile",
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Got status {response.status_code}")

    @task(1)
    def view_analytics(self):
        """View user analytics."""
        with self.client.get(
            f"/api/users/{self.user_id}/analytics",
            headers=self.headers,
            catch_response=True,
            name="/api/users/[user_id]/analytics",
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Got status {response.status_code}")

    @task(1)
    def health_check(self):
        """Health check endpoint."""
        with self.client.get(
            "/health",
            catch_response=True,
            name="/health",
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Got status {response.status_code}")


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Called when test starts."""
    print("\n" + "=" * 80)
    print("LOCUST LOAD TEST STARTING")
    print("=" * 80)
    print(f"Target: {environment.host}")
    print(f"Users: {environment.runner.target_user_count if hasattr(environment.runner, 'target_user_count') else 'N/A'}")
    print("=" * 80 + "\n")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Called when test stops."""
    print("\n" + "=" * 80)
    print("LOCUST LOAD TEST COMPLETE")
    print("=" * 80)

    stats = environment.stats
    print(f"Total requests: {stats.total.num_requests}")
    print(f"Total failures: {stats.total.num_failures}")
    print(f"Average response time: {stats.total.avg_response_time:.2f}ms")
    print(f"Median response time: {stats.total.median_response_time:.2f}ms")
    print(f"95th percentile: {stats.total.get_response_time_percentile(0.95):.2f}ms")
    print(f"99th percentile: {stats.total.get_response_time_percentile(0.99):.2f}ms")
    print(f"Requests/sec: {stats.total.total_rps:.2f}")
    print(f"Failure rate: {(stats.total.num_failures / max(stats.total.num_requests, 1) * 100):.2f}%")
    print("=" * 80 + "\n")
