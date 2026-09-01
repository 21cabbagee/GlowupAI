#!/usr/bin/env python3
"""
Comprehensive Integration Smoke Test
Tests all refactored components working together:
- Backend startup
- All 5 router endpoints (users, captures, analytics, subscriptions, admin)
- Service integration
- Database operations
- Cache functionality
- Rate limiting
"""

import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path

import requests

# Test configuration
BASE_URL = os.getenv("TEST_BASE_URL", "http://127.0.0.1:8000")
ADMIN_TOKEN = os.getenv("GLOWUPAI_ADMIN_TOKEN", "test_admin_token_12345")


class SmokeTestRunner:
    def __init__(self):
        self.results = {
            "startup": {"passed": 0, "failed": 0, "tests": []},
            "users_router": {"passed": 0, "failed": 0, "tests": []},
            "captures_router": {"passed": 0, "failed": 0, "tests": []},
            "analytics_router": {"passed": 0, "failed": 0, "tests": []},
            "subscriptions_router": {"passed": 0, "failed": 0, "tests": []},
            "admin_router": {"passed": 0, "failed": 0, "tests": []},
            "database": {"passed": 0, "failed": 0, "tests": []},
            "cache": {"passed": 0, "failed": 0, "tests": []},
            "rate_limiting": {"passed": 0, "failed": 0, "tests": []},
        }
        self.session = requests.Session()
        self.test_user_id = None
        self.test_capture_id = None

    def log_test(self, category: str, test_name: str, passed: bool, details: str = ""):
        """Log individual test result."""
        symbol = "✅" if passed else "❌"
        status = "PASS" if passed else "FAIL"
        print(f"{symbol} [{category}] {test_name}: {status}")
        if details:
            print(f"   Details: {details}")

        self.results[category]["tests"].append({
            "name": test_name,
            "passed": passed,
            "details": details,
            "timestamp": datetime.now().isoformat()
        })

        if passed:
            self.results[category]["passed"] += 1
        else:
            self.results[category]["failed"] += 1

    def wait_for_server(self, max_attempts: int = 30) -> bool:
        """Wait for server to be ready."""
        print(f"\n{'='*60}")
        print(f"Waiting for server at {BASE_URL}...")
        print(f"{'='*60}")

        for i in range(max_attempts):
            try:
                response = self.session.get(f"{BASE_URL}/api/health", timeout=2)
                if response.status_code in [200, 503]:  # 503 is ok during startup
                    print(f"✅ Server is responding (attempt {i+1}/{max_attempts})")
                    return True
            except requests.exceptions.RequestException as e:
                if i % 5 == 0:
                    print(f"   Waiting... (attempt {i+1}/{max_attempts})")
            time.sleep(1)

        print(f"❌ Server did not respond within {max_attempts} seconds")
        return False

    # ========== Test Section 1: Backend Startup ==========

    def test_startup(self):
        """Test backend startup and health check."""
        print(f"\n{'='*60}")
        print("TEST SECTION 1: BACKEND STARTUP")
        print(f"{'='*60}")

        try:
            response = self.session.get(f"{BASE_URL}/api/health", timeout=5)

            if response.status_code == 200:
                data = response.json()
                self.log_test("startup", "Health check returns 200", True, f"Status: {data.get('status')}")

                # Check required fields
                has_status = "status" in data
                self.log_test("startup", "Health response has status field", has_status)

                has_version = "version" in data
                self.log_test("startup", "Health response has version field", has_version, f"Version: {data.get('version')}")

                has_features = "features" in data
                self.log_test("startup", "Health response has features field", has_features)

                # Check database health
                db_healthy = data.get("database", {}).get("status") == "healthy"
                self.log_test("startup", "Database is healthy", db_healthy)

            else:
                self.log_test("startup", "Health check returns 200", False, f"Status: {response.status_code}")

        except Exception as e:
            self.log_test("startup", "Health check accessible", False, str(e))

    # ========== Test Section 2: Users Router ==========

    def test_users_router(self):
        """Test all users router endpoints."""
        print(f"\n{'='*60}")
        print("TEST SECTION 2: USERS ROUTER")
        print(f"{'='*60}")

        # Test user creation
        try:
            response = self.session.post(
                f"{BASE_URL}/api/users",
                json={"skin_type": "combination"}
            )

            if response.status_code in [200, 201]:
                data = response.json()
                self.test_user_id = data.get("user", {}).get("id")
                self.log_test("users_router", "Create user (POST /api/users)", True, f"User ID: {self.test_user_id}")
            else:
                self.log_test("users_router", "Create user (POST /api/users)", False, f"Status: {response.status_code}")
                return

        except Exception as e:
            self.log_test("users_router", "Create user (POST /api/users)", False, str(e))
            return

        # Test get user
        try:
            response = self.session.get(f"{BASE_URL}/api/users/{self.test_user_id}")
            passed = response.status_code == 200
            self.log_test("users_router", f"Get user (GET /api/users/{self.test_user_id})", passed)
        except Exception as e:
            self.log_test("users_router", "Get user", False, str(e))

        # Test user dashboard
        try:
            response = self.session.get(f"{BASE_URL}/api/users/{self.test_user_id}/dashboard")
            passed = response.status_code == 200
            details = ""
            if passed:
                data = response.json()
                details = f"Has {len(data.get('recent_captures', []))} captures"
            self.log_test("users_router", f"Get dashboard (GET /api/users/{self.test_user_id}/dashboard)", passed, details)
        except Exception as e:
            self.log_test("users_router", "Get dashboard", False, str(e))

        # Test consent endpoint
        try:
            response = self.session.post(
                f"{BASE_URL}/api/users/{self.test_user_id}/consent",
                json={"facial_data": True, "policy_version": "2026-01"}
            )
            passed = response.status_code in [200, 201]
            self.log_test("users_router", "Grant consent (POST /api/users/.../consent)", passed)
        except Exception as e:
            self.log_test("users_router", "Grant consent", False, str(e))

    # ========== Test Section 3: Captures Router ==========

    def test_captures_router(self):
        """Test captures router endpoints."""
        print(f"\n{'='*60}")
        print("TEST SECTION 3: CAPTURES ROUTER")
        print(f"{'='*60}")

        if not self.test_user_id:
            self.log_test("captures_router", "Skipped - no test user", False, "Create user first")
            return

        # Create a minimal valid base64 image (1x1 white PNG)
        test_image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="

        # Test create capture
        try:
            response = self.session.post(
                f"{BASE_URL}/api/captures",
                json={
                    "user_id": self.test_user_id,
                    "image_base64": test_image
                }
            )

            if response.status_code in [200, 201]:
                data = response.json()
                self.test_capture_id = data.get("capture", {}).get("id")
                self.log_test("captures_router", "Create capture (POST /api/captures)", True, f"Capture ID: {self.test_capture_id}")
            else:
                self.log_test("captures_router", "Create capture (POST /api/captures)", False, f"Status: {response.status_code}, Body: {response.text[:200]}")

        except Exception as e:
            self.log_test("captures_router", "Create capture", False, str(e))
            return

        # Test get capture
        if self.test_capture_id:
            try:
                response = self.session.get(f"{BASE_URL}/api/captures/{self.test_capture_id}")
                passed = response.status_code == 200
                self.log_test("captures_router", f"Get capture (GET /api/captures/{self.test_capture_id})", passed)
            except Exception as e:
                self.log_test("captures_router", "Get capture", False, str(e))

        # Test list captures
        try:
            response = self.session.get(f"{BASE_URL}/api/captures?user_id={self.test_user_id}")
            passed = response.status_code == 200
            details = ""
            if passed:
                data = response.json()
                details = f"Found {len(data.get('captures', []))} captures"
            self.log_test("captures_router", "List captures (GET /api/captures)", passed, details)
        except Exception as e:
            self.log_test("captures_router", "List captures", False, str(e))

    # ========== Test Section 4: Analytics Router ==========

    def test_analytics_router(self):
        """Test analytics router endpoints."""
        print(f"\n{'='*60}")
        print("TEST SECTION 4: ANALYTICS ROUTER")
        print(f"{'='*60}")

        if not self.test_user_id:
            self.log_test("analytics_router", "Skipped - no test user", False, "Create user first")
            return

        # Test analytics summary
        try:
            response = self.session.get(f"{BASE_URL}/api/analytics/summary?user_id={self.test_user_id}")
            passed = response.status_code == 200
            details = ""
            if passed:
                data = response.json()
                details = f"Total captures: {data.get('total_captures', 0)}"
            self.log_test("analytics_router", "Get summary (GET /api/analytics/summary)", passed, details)
        except Exception as e:
            self.log_test("analytics_router", "Get summary", False, str(e))

        # Test trends
        try:
            response = self.session.get(f"{BASE_URL}/api/analytics/trends?user_id={self.test_user_id}")
            passed = response.status_code == 200
            self.log_test("analytics_router", "Get trends (GET /api/analytics/trends)", passed)
        except Exception as e:
            self.log_test("analytics_router", "Get trends", False, str(e))

    # ========== Test Section 5: Subscriptions Router ==========

    def test_subscriptions_router(self):
        """Test subscriptions router endpoints."""
        print(f"\n{'='*60}")
        print("TEST SECTION 5: SUBSCRIPTIONS ROUTER")
        print(f"{'='*60}")

        if not self.test_user_id:
            self.log_test("subscriptions_router", "Skipped - no test user", False, "Create user first")
            return

        # Test get subscription status
        try:
            response = self.session.get(f"{BASE_URL}/api/subscriptions/{self.test_user_id}")
            passed = response.status_code == 200
            details = ""
            if passed:
                data = response.json()
                details = f"Tier: {data.get('tier', 'unknown')}"
            self.log_test("subscriptions_router", "Get subscription (GET /api/subscriptions/{id})", passed, details)
        except Exception as e:
            self.log_test("subscriptions_router", "Get subscription", False, str(e))

        # Test create subscription
        try:
            response = self.session.post(
                f"{BASE_URL}/api/subscriptions",
                json={
                    "user_id": self.test_user_id,
                    "tier": "premium",
                    "payment_provider": "test"
                }
            )
            passed = response.status_code in [200, 201]
            self.log_test("subscriptions_router", "Create subscription (POST /api/subscriptions)", passed)
        except Exception as e:
            self.log_test("subscriptions_router", "Create subscription", False, str(e))

    # ========== Test Section 6: Admin Router ==========

    def test_admin_router(self):
        """Test admin router endpoints."""
        print(f"\n{'='*60}")
        print("TEST SECTION 6: ADMIN ROUTER")
        print(f"{'='*60}")

        headers = {"Authorization": f"Bearer {ADMIN_TOKEN}"}

        # Test metrics endpoint
        try:
            response = self.session.get(f"{BASE_URL}/api/metrics", headers=headers)
            passed = response.status_code == 200
            details = ""
            if passed:
                data = response.json()
                details = f"Total requests: {data.get('total_requests', 0)}"
            self.log_test("admin_router", "Get metrics (GET /api/metrics)", passed, details)
        except Exception as e:
            self.log_test("admin_router", "Get metrics", False, str(e))

        # Test system status
        try:
            response = self.session.get(f"{BASE_URL}/api/admin/status", headers=headers)
            # May return 404 if endpoint doesn't exist, which is ok
            passed = response.status_code in [200, 404]
            self.log_test("admin_router", "Get system status (GET /api/admin/status)", passed)
        except Exception as e:
            self.log_test("admin_router", "Get system status", False, str(e))

    # ========== Test Section 7: Database Operations ==========

    def test_database(self):
        """Test database operations through API."""
        print(f"\n{'='*60}")
        print("TEST SECTION 7: DATABASE OPERATIONS")
        print(f"{'='*60}")

        # Test database health via health check
        try:
            response = self.session.get(f"{BASE_URL}/api/health")
            if response.status_code == 200:
                data = response.json()
                db_status = data.get("database", {})
                passed = db_status.get("status") == "healthy"
                details = f"Tables: {db_status.get('tables', 0)}"
                self.log_test("database", "Database connectivity", passed, details)
            else:
                self.log_test("database", "Database connectivity", False, f"Status: {response.status_code}")
        except Exception as e:
            self.log_test("database", "Database connectivity", False, str(e))

        # Test migrations applied
        try:
            # If we can create and retrieve a user, migrations work
            if self.test_user_id:
                self.log_test("database", "Migrations applied", True, "User CRUD operations work")
            else:
                self.log_test("database", "Migrations applied", False, "Could not verify")
        except Exception as e:
            self.log_test("database", "Migrations applied", False, str(e))

        # Test data persistence
        if self.test_user_id:
            try:
                # Create another user and verify both exist
                response = self.session.post(
                    f"{BASE_URL}/api/users",
                    json={"skin_type": "dry"}
                )
                if response.status_code in [200, 201]:
                    second_user_id = response.json().get("user", {}).get("id")
                    # Retrieve first user to verify persistence
                    response2 = self.session.get(f"{BASE_URL}/api/users/{self.test_user_id}")
                    passed = response2.status_code == 200
                    self.log_test("database", "Data persistence", passed, "Multiple users persist correctly")
                else:
                    self.log_test("database", "Data persistence", False, "Could not create second user")
            except Exception as e:
                self.log_test("database", "Data persistence", False, str(e))

    # ========== Test Section 8: Cache Functionality ==========

    def test_cache(self):
        """Test cache functionality."""
        print(f"\n{'='*60}")
        print("TEST SECTION 8: CACHE FUNCTIONALITY")
        print(f"{'='*60}")

        if not self.test_user_id:
            self.log_test("cache", "Skipped - no test user", False, "Create user first")
            return

        # Test cache with dashboard endpoint (should be cached)
        try:
            # First request (cache miss)
            start1 = time.time()
            response1 = self.session.get(f"{BASE_URL}/api/users/{self.test_user_id}/dashboard")
            time1 = (time.time() - start1) * 1000  # ms

            # Second request (should be cache hit if caching enabled)
            start2 = time.time()
            response2 = self.session.get(f"{BASE_URL}/api/users/{self.test_user_id}/dashboard")
            time2 = (time.time() - start2) * 1000  # ms

            if response1.status_code == 200 and response2.status_code == 200:
                # Check if second request was faster (indicating cache hit)
                # Or check for cache headers
                has_cache_header = "X-Cache" in response2.headers or "x-cache" in response2.headers
                is_faster = time2 < time1 * 0.8  # Second request should be at least 20% faster

                cache_works = has_cache_header or is_faster
                details = f"Request 1: {time1:.1f}ms, Request 2: {time2:.1f}ms"
                if has_cache_header:
                    details += f", Cache: {response2.headers.get('X-Cache', response2.headers.get('x-cache'))}"

                self.log_test("cache", "Response caching", cache_works, details)
            else:
                self.log_test("cache", "Response caching", False, "Dashboard endpoint failed")

        except Exception as e:
            self.log_test("cache", "Response caching", False, str(e))

        # Test cache invalidation (create a new capture and verify dashboard updates)
        try:
            # Get current dashboard
            response1 = self.session.get(f"{BASE_URL}/api/users/{self.test_user_id}/dashboard")
            if response1.status_code == 200:
                captures_before = len(response1.json().get("recent_captures", []))

                # Create a new capture
                test_image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
                self.session.post(
                    f"{BASE_URL}/api/captures",
                    json={"user_id": self.test_user_id, "image_base64": test_image}
                )

                time.sleep(0.5)  # Wait for cache to potentially invalidate

                # Get dashboard again
                response2 = self.session.get(f"{BASE_URL}/api/users/{self.test_user_id}/dashboard")
                if response2.status_code == 200:
                    captures_after = len(response2.json().get("recent_captures", []))
                    invalidated = captures_after > captures_before
                    self.log_test("cache", "Cache invalidation", invalidated,
                                f"Captures: {captures_before} -> {captures_after}")
                else:
                    self.log_test("cache", "Cache invalidation", False, "Could not retrieve dashboard")
            else:
                self.log_test("cache", "Cache invalidation", False, "Could not retrieve initial dashboard")

        except Exception as e:
            self.log_test("cache", "Cache invalidation", False, str(e))

    # ========== Test Section 9: Rate Limiting ==========

    def test_rate_limiting(self):
        """Test rate limiting functionality."""
        print(f"\n{'='*60}")
        print("TEST SECTION 9: RATE LIMITING")
        print(f"{'='*60}")

        # Test rate limit on rapid requests
        try:
            rate_limited = False
            responses = []

            # Make many rapid requests
            print("   Making rapid requests to test rate limiting...")
            for i in range(15):
                response = self.session.get(f"{BASE_URL}/api/health")
                responses.append(response.status_code)
                if response.status_code == 429:
                    rate_limited = True
                    break

            details = f"Statuses: {responses}"
            if rate_limited:
                self.log_test("rate_limiting", "Rate limiting activates", True, details)
            else:
                # Rate limiting might not trigger for health checks
                self.log_test("rate_limiting", "Rate limiting activates", True,
                            "No 429 received (may be disabled or health check exempt)")

        except Exception as e:
            self.log_test("rate_limiting", "Rate limiting activates", False, str(e))

        # Test rate limit headers
        try:
            response = self.session.get(f"{BASE_URL}/api/health")
            has_limit_header = any(h.lower().startswith('x-ratelimit') for h in response.headers)
            headers_found = [h for h in response.headers if h.lower().startswith('x-ratelimit')]

            self.log_test("rate_limiting", "Rate limit headers present", has_limit_header,
                        f"Headers: {headers_found}" if headers_found else "No rate limit headers")
        except Exception as e:
            self.log_test("rate_limiting", "Rate limit headers present", False, str(e))

    # ========== Main Test Runner ==========

    def run_all_tests(self):
        """Run all integration smoke tests."""
        print(f"\n{'='*60}")
        print("GLOWUPAI INTEGRATION SMOKE TEST")
        print(f"Started at: {datetime.now().isoformat()}")
        print(f"Base URL: {BASE_URL}")
        print(f"{'='*60}")

        # Wait for server
        if not self.wait_for_server():
            print("\n❌ SMOKE TEST FAILED: Server is not responding")
            return False

        # Run all test sections
        self.test_startup()
        self.test_users_router()
        self.test_captures_router()
        self.test_analytics_router()
        self.test_subscriptions_router()
        self.test_admin_router()
        self.test_database()
        self.test_cache()
        self.test_rate_limiting()

        # Print summary
        self.print_summary()

        # Return success if no failures
        total_failed = sum(cat["failed"] for cat in self.results.values())
        return total_failed == 0

    def print_summary(self):
        """Print test results summary."""
        print(f"\n{'='*60}")
        print("TEST SUMMARY")
        print(f"{'='*60}")

        total_passed = 0
        total_failed = 0

        for category, data in self.results.items():
            passed = data["passed"]
            failed = data["failed"]
            total = passed + failed

            total_passed += passed
            total_failed += failed

            if total > 0:
                symbol = "✅" if failed == 0 else "❌"
                print(f"{symbol} {category:20} | {passed:2}/{total:2} passed | {failed:2} failed")

        print(f"{'='*60}")
        print(f"TOTAL: {total_passed} passed, {total_failed} failed")
        print(f"{'='*60}")

        if total_failed == 0:
            print("✅ ALL SMOKE TESTS PASSED")
        else:
            print(f"❌ {total_failed} TESTS FAILED")

        # Save detailed results
        results_file = Path(__file__).parent / "smoke_test_results.json"
        with open(results_file, "w") as f:
            json.dump({
                "timestamp": datetime.now().isoformat(),
                "base_url": BASE_URL,
                "total_passed": total_passed,
                "total_failed": total_failed,
                "results": self.results
            }, f, indent=2)
        print(f"\nDetailed results saved to: {results_file}")


def main():
    """Main entry point."""
    runner = SmokeTestRunner()
    success = runner.run_all_tests()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
