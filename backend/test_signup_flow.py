#!/usr/bin/env python3
"""
Comprehensive User Signup Flow Test
Tests the complete flow end-to-end to identify where failures occur.
"""

import json
import sys
import traceback
from typing import Any, Dict, Optional

import httpx

BASE_URL = "http://localhost:8000"
TIMEOUT = 30.0


class SignupFlowTester:
    def __init__(self):
        self.client = httpx.Client(timeout=TIMEOUT)
        self.test_user_id = None
        self.test_capture_id = None
        self.results = []

    def log_step(self, step: str, success: bool, details: str = "", response: Optional[httpx.Response] = None):
        """Log test step result."""
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"\n{status} - {step}")
        if details:
            print(f"  Details: {details}")

        result = {
            "step": step,
            "success": success,
            "details": details,
        }

        if response:
            result["status_code"] = response.status_code
            result["headers"] = dict(response.headers)
            try:
                result["response_body"] = response.json()
            except:
                result["response_body"] = response.text[:500]

        self.results.append(result)

        if not success and response:
            print(f"  Status Code: {response.status_code}")
            print(f"  Response Headers: {dict(response.headers)}")
            try:
                print(f"  Response Body: {json.dumps(response.json(), indent=2)}")
            except:
                print(f"  Response Text: {response.text[:500]}")

    def test_server_health(self) -> bool:
        """Step 1: Check if server is running."""
        print("\n" + "="*60)
        print("STEP 1: SERVER HEALTH CHECK")
        print("="*60)

        try:
            response = self.client.get(f"{BASE_URL}/api/health")

            if response.status_code == 200:
                data = response.json()
                self.log_step(
                    "Server health check",
                    True,
                    f"Server is healthy. Status: {data.get('status')}",
                    response
                )
                return True
            else:
                self.log_step(
                    "Server health check",
                    False,
                    f"Server returned status {response.status_code}",
                    response
                )
                return False
        except Exception as e:
            self.log_step(
                "Server health check",
                False,
                f"Connection error: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def test_create_user(self) -> bool:
        """Step 2: Create a new user."""
        print("\n" + "="*60)
        print("STEP 2: CREATE USER")
        print("="*60)

        try:
            payload = {"skin_type": "combination"}
            response = self.client.post(
                f"{BASE_URL}/api/users",
                json=payload
            )

            if response.status_code in [200, 201]:
                data = response.json()
                self.test_user_id = data.get("user", {}).get("id")

                if self.test_user_id:
                    self.log_step(
                        "Create user (POST /api/users)",
                        True,
                        f"User created successfully. User ID: {self.test_user_id}",
                        response
                    )
                    return True
                else:
                    self.log_step(
                        "Create user (POST /api/users)",
                        False,
                        "User ID not found in response",
                        response
                    )
                    return False
            else:
                self.log_step(
                    "Create user (POST /api/users)",
                    False,
                    f"Failed with status {response.status_code}",
                    response
                )
                return False

        except Exception as e:
            self.log_step(
                "Create user (POST /api/users)",
                False,
                f"Exception: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def test_get_user_profile(self) -> bool:
        """Step 3: Get user profile."""
        print("\n" + "="*60)
        print("STEP 3: GET USER PROFILE")
        print("="*60)

        if not self.test_user_id:
            self.log_step("Get user profile", False, "No user ID available")
            return False

        try:
            response = self.client.get(f"{BASE_URL}/api/users/{self.test_user_id}")

            if response.status_code == 200:
                self.log_step(
                    "Get user profile (GET /api/users/{id})",
                    True,
                    "Profile retrieved successfully",
                    response
                )
                return True
            else:
                self.log_step(
                    "Get user profile (GET /api/users/{id})",
                    False,
                    f"Failed with status {response.status_code}",
                    response
                )
                return False

        except Exception as e:
            self.log_step(
                "Get user profile (GET /api/users/{id})",
                False,
                f"Exception: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def test_create_capture(self) -> bool:
        """Step 4: Create a photo capture."""
        print("\n" + "="*60)
        print("STEP 4: CREATE PHOTO CAPTURE")
        print("="*60)

        if not self.test_user_id:
            self.log_step("Create capture", False, "No user ID available")
            return False

        # Minimal valid 1x1 white PNG in base64
        test_image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="

        try:
            payload = {
                "user_id": self.test_user_id,
                "image_base64": test_image
            }
            response = self.client.post(
                f"{BASE_URL}/api/captures",
                json=payload
            )

            if response.status_code in [200, 201]:
                data = response.json()
                self.test_capture_id = data.get("capture", {}).get("id") or data.get("capture_id")

                if self.test_capture_id:
                    self.log_step(
                        "Create capture (POST /api/captures)",
                        True,
                        f"Capture created successfully. Capture ID: {self.test_capture_id}",
                        response
                    )
                    return True
                else:
                    self.log_step(
                        "Create capture (POST /api/captures)",
                        False,
                        "Capture ID not found in response",
                        response
                    )
                    return False
            else:
                self.log_step(
                    "Create capture (POST /api/captures)",
                    False,
                    f"Failed with status {response.status_code}",
                    response
                )
                return False

        except Exception as e:
            self.log_step(
                "Create capture (POST /api/captures)",
                False,
                f"Exception: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def test_get_dashboard(self) -> bool:
        """Step 5: Get user dashboard."""
        print("\n" + "="*60)
        print("STEP 5: GET USER DASHBOARD")
        print("="*60)

        if not self.test_user_id:
            self.log_step("Get dashboard", False, "No user ID available")
            return False

        try:
            response = self.client.get(f"{BASE_URL}/api/users/{self.test_user_id}/dashboard")

            if response.status_code == 200:
                data = response.json()
                captures_count = len(data.get("recent_captures", []))
                self.log_step(
                    "Get dashboard (GET /api/users/{id}/dashboard)",
                    True,
                    f"Dashboard retrieved successfully. Captures: {captures_count}",
                    response
                )
                return True
            else:
                self.log_step(
                    "Get dashboard (GET /api/users/{id}/dashboard)",
                    False,
                    f"Failed with status {response.status_code}",
                    response
                )
                return False

        except Exception as e:
            self.log_step(
                "Get dashboard (GET /api/users/{id}/dashboard)",
                False,
                f"Exception: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def test_analytics_summary(self) -> bool:
        """Step 6: Get analytics summary."""
        print("\n" + "="*60)
        print("STEP 6: GET ANALYTICS SUMMARY")
        print("="*60)

        if not self.test_user_id:
            self.log_step("Get analytics summary", False, "No user ID available")
            return False

        try:
            response = self.client.get(f"{BASE_URL}/api/analytics/summary?user_id={self.test_user_id}")

            if response.status_code == 200:
                data = response.json()
                self.log_step(
                    "Get analytics summary (GET /api/analytics/summary)",
                    True,
                    f"Analytics retrieved successfully. Total captures: {data.get('total_captures', 0)}",
                    response
                )
                return True
            else:
                self.log_step(
                    "Get analytics summary (GET /api/analytics/summary)",
                    False,
                    f"Failed with status {response.status_code}",
                    response
                )
                return False

        except Exception as e:
            self.log_step(
                "Get analytics summary (GET /api/analytics/summary)",
                False,
                f"Exception: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def test_analytics_trends(self) -> bool:
        """Step 7: Get analytics trends."""
        print("\n" + "="*60)
        print("STEP 7: GET ANALYTICS TRENDS")
        print("="*60)

        if not self.test_user_id:
            self.log_step("Get analytics trends", False, "No user ID available")
            return False

        try:
            response = self.client.get(f"{BASE_URL}/api/analytics/trends?user_id={self.test_user_id}")

            if response.status_code == 200:
                self.log_step(
                    "Get analytics trends (GET /api/analytics/trends)",
                    True,
                    "Trends retrieved successfully",
                    response
                )
                return True
            else:
                self.log_step(
                    "Get analytics trends (GET /api/analytics/trends)",
                    False,
                    f"Failed with status {response.status_code}",
                    response
                )
                return False

        except Exception as e:
            self.log_step(
                "Get analytics trends (GET /api/analytics/trends)",
                False,
                f"Exception: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def test_grant_consent(self) -> bool:
        """Step 8: Grant user consent."""
        print("\n" + "="*60)
        print("STEP 8: GRANT USER CONSENT")
        print("="*60)

        if not self.test_user_id:
            self.log_step("Grant consent", False, "No user ID available")
            return False

        try:
            payload = {
                "facial_data": True,
                "policy_version": "2026-01"
            }
            response = self.client.post(
                f"{BASE_URL}/api/users/{self.test_user_id}/consent",
                json=payload
            )

            if response.status_code in [200, 201]:
                self.log_step(
                    "Grant consent (POST /api/users/{id}/consent)",
                    True,
                    "Consent granted successfully",
                    response
                )
                return True
            else:
                self.log_step(
                    "Grant consent (POST /api/users/{id}/consent)",
                    False,
                    f"Failed with status {response.status_code}",
                    response
                )
                return False

        except Exception as e:
            self.log_step(
                "Grant consent (POST /api/users/{id}/consent)",
                False,
                f"Exception: {str(e)}\n{traceback.format_exc()}"
            )
            return False

    def run_all_tests(self):
        """Run all tests in sequence."""
        print("\n" + "="*60)
        print("USER SIGNUP FLOW TEST")
        print("Testing server at:", BASE_URL)
        print("="*60)

        # Run tests in order
        tests = [
            self.test_server_health,
            self.test_create_user,
            self.test_get_user_profile,
            self.test_create_capture,
            self.test_get_dashboard,
            self.test_analytics_summary,
            self.test_analytics_trends,
            self.test_grant_consent,
        ]

        for test in tests:
            result = test()
            if not result:
                print(f"\n⚠️  Test failed, continuing to next test...")

        # Print summary
        self.print_summary()

        # Save detailed results
        with open("/Users/21cabbage/GlowupAI/backend/signup_flow_test_results.json", "w") as f:
            json.dump(self.results, f, indent=2)
        print("\n📄 Detailed results saved to: signup_flow_test_results.json")

    def print_summary(self):
        """Print test summary."""
        print("\n" + "="*60)
        print("TEST SUMMARY")
        print("="*60)

        passed = sum(1 for r in self.results if r["success"])
        failed = sum(1 for r in self.results if not r["success"])
        total = len(self.results)

        print(f"Total tests: {total}")
        print(f"Passed: {passed} ✅")
        print(f"Failed: {failed} ❌")

        if failed > 0:
            print("\n❌ FAILED TESTS:")
            for result in self.results:
                if not result["success"]:
                    print(f"  - {result['step']}")
                    if "status_code" in result:
                        print(f"    Status: {result['status_code']}")
                    if result.get("details"):
                        print(f"    Details: {result['details'][:200]}")

        print("\n" + "="*60)

        # Identify root cause
        self.identify_root_cause()

    def identify_root_cause(self):
        """Identify the root cause of failures."""
        print("\n🔍 ROOT CAUSE ANALYSIS")
        print("="*60)

        first_failure = None
        for result in self.results:
            if not result["success"]:
                first_failure = result
                break

        if first_failure:
            print(f"\n⚠️  First failure at: {first_failure['step']}")
            print(f"Details: {first_failure['details']}")

            if "status_code" in first_failure:
                status = first_failure["status_code"]
                print(f"\nHTTP Status Code: {status}")

                if status == 401:
                    print("🔐 Likely cause: AUTHORIZATION REQUIRED")
                    print("   The endpoint requires authentication but no token was provided.")
                elif status == 404:
                    print("🔍 Likely cause: ENDPOINT NOT FOUND")
                    print("   The endpoint may not be registered or the URL is incorrect.")
                elif status == 422:
                    print("📝 Likely cause: VALIDATION ERROR")
                    print("   The request payload doesn't match expected schema.")
                elif status == 500:
                    print("💥 Likely cause: INTERNAL SERVER ERROR")
                    print("   Check server logs for stack trace.")
                    if "response_body" in first_failure:
                        print(f"\n   Response: {json.dumps(first_failure['response_body'], indent=2)}")

            print("\n💡 Recommendation:")
            if first_failure["step"].startswith("Create capture"):
                print("   - Check if the image processing pipeline is configured correctly")
                print("   - Verify that required environment variables are set")
                print("   - Check server logs for detailed error messages")
            elif first_failure["step"].startswith("Create user"):
                print("   - Verify database connection and migrations are applied")
                print("   - Check if user table exists and has correct schema")
            else:
                print("   - Check server logs for detailed error messages")
                print("   - Verify all dependencies are properly configured")
        else:
            print("✅ All tests passed! No failures detected.")


def main():
    """Main entry point."""
    tester = SignupFlowTester()
    tester.run_all_tests()


if __name__ == "__main__":
    main()
