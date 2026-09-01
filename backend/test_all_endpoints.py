#!/usr/bin/env python3
"""Comprehensive endpoint testing script for GlowUpAI backend."""

import json
import base64
from typing import Dict, Any, List, Tuple
import httpx

BASE_URL = "http://localhost:8000"

# Test data
DUMMY_IMAGE_BASE64 = base64.b64encode(b"fake_image_data").decode()
DUMMY_TOKEN = "Bearer test-token-123"

class EndpointTester:
    def __init__(self):
        self.working = []
        self.broken = []
        self.user_id = None
        self.capture_id = None
        self.product_id = None
        self.experiment_id = None
        self.job_id = None
        self.offer_id = None

    def test(self, method: str, path: str, **kwargs) -> Tuple[bool, str, Any]:
        """Test an endpoint and return (success, status, response)."""
        url = f"{BASE_URL}{path}"
        try:
            with httpx.Client(timeout=10.0) as client:
                if method == "GET":
                    resp = client.get(url, **kwargs)
                elif method == "POST":
                    resp = client.post(url, **kwargs)
                elif method == "PATCH":
                    resp = client.patch(url, **kwargs)
                elif method == "DELETE":
                    resp = client.delete(url, **kwargs)
                else:
                    return False, "UNKNOWN_METHOD", None

                if 200 <= resp.status_code < 300:
                    return True, f"{resp.status_code}", resp.json() if resp.content else {}
                else:
                    return False, f"{resp.status_code}", resp.text[:200]
        except Exception as e:
            return False, "EXCEPTION", str(e)[:200]

    def run_all_tests(self):
        """Run tests for all 75+ endpoints."""
        print("=" * 80)
        print("Testing GlowUpAI Backend - All Endpoints")
        print("=" * 80)

        # USERS ROUTER (9 endpoints)
        print("\n[USERS ROUTER]")

        # 1. POST /api/users
        success, status, data = self.test("POST", "/api/users", json={"skin_type": "combination"})
        self.log_result("POST /api/users", success, status, data)
        if success and data.get("user_id"):
            self.user_id = data["user_id"]

        # 2. POST /api/auth/session (needs valid Firebase token - will likely fail)
        success, status, data = self.test("POST", "/api/auth/session", headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/auth/session", success, status, data)

        # Need a user_id for the rest - use dummy if we don't have one
        test_user_id = self.user_id or "test-user-123"

        # 3. GET /api/users/{id}
        success, status, data = self.test("GET", f"/api/users/{test_user_id}", headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}", success, status, data)

        # 4. GET /api/users/{user_id}/profile
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/profile", headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/profile", success, status, data)

        # 5. PATCH /api/users/{user_id}/profile
        success, status, data = self.test("PATCH", f"/api/users/{test_user_id}/profile",
                                         json={"display_name": "Test User"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"PATCH /api/users/{test_user_id}/profile", success, status, data)

        # 6. POST /api/users/{user_id}/consent
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/consent",
                                         json={"facial_data": True, "policy_version": "1.0"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/consent", success, status, data)

        # 7. POST /api/users/{user_id}/consent/data-collection
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/consent/data-collection",
                                         json={"granted": True, "policy_version": "1.0"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/consent/data-collection", success, status, data)

        # 8. GET /api/users/{user_id}/export
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/export", headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/export", success, status, data)

        # 9. DELETE /api/users/{user_id} - Skip this to not delete test data
        # success, status, data = self.test("DELETE", f"/api/users/{test_user_id}", headers={"Authorization": DUMMY_TOKEN})
        # self.log_result(f"DELETE /api/users/{test_user_id}", success, status, data)
        print("  [SKIPPED] DELETE /api/users/{user_id} - Destructive operation")

        # CAPTURES ROUTER (16 endpoints)
        print("\n[CAPTURES ROUTER]")

        # 10. POST /api/captures
        success, status, data = self.test("POST", "/api/captures",
                                         json={
                                             "user_id": test_user_id,
                                             "image_base64": DUMMY_IMAGE_BASE64,
                                             "is_baseline": False,
                                             "vertical": "skin"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/captures", success, status, data)
        if success and data.get("capture_id"):
            self.capture_id = data["capture_id"]

        test_capture_id = self.capture_id or "test-capture-123"

        # 11. POST /api/captures/{capture_id}/feedback
        success, status, data = self.test("POST", f"/api/captures/{test_capture_id}/feedback",
                                         json={
                                             "feedback_type": "accuracy",
                                             "issues": ["blurry"],
                                             "comment": "Test feedback"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/captures/{test_capture_id}/feedback", success, status, data)

        # 12. GET /api/users/{user_id}/capture-guide
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/capture-guide",
                                         params={"vertical": "skin"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/capture-guide", success, status, data)

        # 13. GET /api/users/{user_id}/dashboard
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/dashboard",
                                         params={"vertical": "skin"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/dashboard", success, status, data)

        # 14. GET /api/users/{user_id}/history
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/history",
                                         params={"vertical": "skin"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/history", success, status, data)

        # 15. GET /api/users/{user_id}/check-ins
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/check-ins",
                                         params={"limit": 30},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/check-ins", success, status, data)

        # 16. POST /api/users/{user_id}/check-ins
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/check-ins",
                                         json={
                                             "routine_state": "steady",
                                             "skin_feel": "same",
                                             "note": "Test check-in"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/check-ins", success, status, data)

        # 17. GET /api/users/{user_id}/weekly-recap
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/weekly-recap",
                                         params={"vertical": "skin"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/weekly-recap", success, status, data)

        # 18. POST /api/users/{user_id}/measurement-feedback
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/measurement-feedback",
                                         json={
                                             "capture_id": test_capture_id,
                                             "agreement": "fair",
                                             "note": "Test feedback"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/measurement-feedback", success, status, data)

        # 19. GET /api/users/{user_id}/labels
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/labels", headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/labels", success, status, data)

        # 20. POST /api/users/{user_id}/labels
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/labels",
                                         json={
                                             "photo_id": test_capture_id,
                                             "label_type": "condition",
                                             "value": "acne"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/labels", success, status, data)

        # 21. POST /api/users/{user_id}/reprocess
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/reprocess",
                                         json={"model_version": "v2.0"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/reprocess", success, status, data)
        if success and data.get("job_id"):
            self.job_id = data["job_id"]

        test_job_id = self.job_id or "test-job-123"

        # 22. GET /api/users/{user_id}/reprocess/{job_id}
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/reprocess/{test_job_id}",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/reprocess/{test_job_id}", success, status, data)

        # 23. POST /api/users/{user_id}/shelf-scan
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/shelf-scan",
                                         json={"image_base64": DUMMY_IMAGE_BASE64},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/shelf-scan", success, status, data)

        # 24. GET /api/users/{user_id}/shelf-scan/{job_id}
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/shelf-scan/{test_job_id}",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/shelf-scan/{test_job_id}", success, status, data)

        # 25. POST /api/users/{user_id}/shelf-scan/{job_id}/confirm
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/shelf-scan/{test_job_id}/confirm",
                                         json={"selections": [{"product_id": "test", "quantity": 1}]},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/shelf-scan/{test_job_id}/confirm", success, status, data)

        # ANALYTICS ROUTER (10 endpoints)
        print("\n[ANALYTICS ROUTER]")

        # 26. GET /api/analytics/summary
        success, status, data = self.test("GET", "/api/analytics/summary", params={"user_id": test_user_id})
        self.log_result("GET /api/analytics/summary", success, status, data)

        # 27. GET /api/analytics/trends
        success, status, data = self.test("GET", "/api/analytics/trends",
                                         params={"user_id": test_user_id, "vertical": "skin"})
        self.log_result("GET /api/analytics/trends", success, status, data)

        # 28. GET /api/users/{user_id}/analytics
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/analytics",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/analytics", success, status, data)

        # 29. GET /api/users/{user_id}/engagement
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/engagement",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/engagement", success, status, data)

        # 30. POST /api/users/{user_id}/engagement
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/engagement",
                                         json={"event_type": "app_opened", "metadata": {"source": "test"}},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/engagement", success, status, data)

        # 31. GET /api/users/{user_id}/context-events
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/context-events",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/context-events", success, status, data)

        # 32. POST /api/users/{user_id}/context-events
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/context-events",
                                         json={"event_type": "weather_change", "value": "humid"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/context-events", success, status, data)

        # 33. GET /api/users/{user_id}/root-cause
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/root-cause",
                                         params={"metric": "texture_score"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/root-cause", success, status, data)

        # 34. GET /api/users/{user_id}/budget-optimizer
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/budget-optimizer",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/budget-optimizer", success, status, data)

        # 35. GET /api/users/{user_id}/derm-export
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/derm-export",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/derm-export", success, status, data)

        # SUBSCRIPTIONS ROUTER (23 endpoints)
        print("\n[SUBSCRIPTIONS ROUTER]")

        # 36. GET /api/subscriptions
        success, status, data = self.test("GET", "/api/subscriptions", params={"limit": 100})
        self.log_result("GET /api/subscriptions", success, status, data)

        # 37. POST /api/subscriptions
        success, status, data = self.test("POST", "/api/subscriptions",
                                         json={"user_id": test_user_id, "plan": "premium", "source": "api"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/subscriptions", success, status, data)

        # 38. GET /api/users/{user_id}/subscription
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/subscription",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/subscription", success, status, data)

        # 39. POST /api/users/{user_id}/subscription/upgrade
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/subscription/upgrade",
                                         json={"source": "local_checkout"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/subscription/upgrade", success, status, data)

        # 40. POST /api/users/{user_id}/subscription/cancel
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/subscription/cancel",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/subscription/cancel", success, status, data)

        # 41. POST /api/products
        success, status, data = self.test("POST", "/api/products",
                                         json={
                                             "name": "Test Product",
                                             "barcode": "123456",
                                             "category": "cleanser",
                                             "stabilization_days": 14
                                         })
        self.log_result("POST /api/products", success, status, data)
        if success and data.get("product_id"):
            self.product_id = data["product_id"]

        test_product_id = self.product_id or "test-product-123"

        # 42. GET /api/products/search
        success, status, data = self.test("GET", "/api/products/search", params={"q": "test"})
        self.log_result("GET /api/products/search", success, status, data)

        # 43. GET /api/products/lookup
        success, status, data = self.test("GET", "/api/products/lookup", params={"barcode": "123456"})
        self.log_result("GET /api/products/lookup", success, status, data)

        # 44. GET /api/products/{product_id}
        success, status, data = self.test("GET", f"/api/products/{test_product_id}",
                                         params={"user_id": test_user_id},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/products/{test_product_id}", success, status, data)

        # 45. GET /api/products/{product_id}/ingredient-explainer
        success, status, data = self.test("GET", f"/api/products/{test_product_id}/ingredient-explainer",
                                         params={"user_id": test_user_id},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/products/{test_product_id}/ingredient-explainer", success, status, data)

        # 46. GET /api/products/{product_id}/predict
        success, status, data = self.test("GET", f"/api/products/{test_product_id}/predict",
                                         params={"user_id": test_user_id},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/products/{test_product_id}/predict", success, status, data)

        # 47. POST /api/users/{user_id}/purchase-guidance
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/purchase-guidance",
                                         json={
                                             "name": "Test Product",
                                             "category": "cleanser",
                                             "price_cents": 1000,
                                             "currency": "INR"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/purchase-guidance", success, status, data)

        # 48. POST /api/routine-events
        success, status, data = self.test("POST", "/api/routine-events",
                                         json={
                                             "user_id": test_user_id,
                                             "product_id": test_product_id,
                                             "action": "started",
                                             "slot": "morning"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/routine-events", success, status, data)

        # 49. GET /api/users/{user_id}/confound-check
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/confound-check",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/confound-check", success, status, data)

        # 50. POST /api/experiments
        success, status, data = self.test("POST", "/api/experiments",
                                         json={
                                             "user_id": test_user_id,
                                             "name": "Test Experiment",
                                             "hypothesis": "Testing hypothesis",
                                             "product_id": test_product_id,
                                             "primary_metric": "redness_score",
                                             "target_days": 14
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/experiments", success, status, data)
        if success and data.get("experiment_id"):
            self.experiment_id = data["experiment_id"]

        test_experiment_id = self.experiment_id or "test-experiment-123"

        # 51. GET /api/users/{user_id}/experiments
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/experiments",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/experiments", success, status, data)

        # 52. GET /api/users/{user_id}/experiments/{experiment_id}
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/experiments/{test_experiment_id}",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/experiments/{test_experiment_id}", success, status, data)

        # 53. POST /api/users/{user_id}/experiments/{experiment_id}/status
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/experiments/{test_experiment_id}/status",
                                         json={"user_id": test_user_id, "status": "active"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/experiments/{test_experiment_id}/status", success, status, data)

        # 54. POST /api/users/{user_id}/qna
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/qna",
                                         json={"question": "What causes acne?"},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/qna", success, status, data)

        # 55. GET /api/users/{user_id}/qna
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/qna",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/qna", success, status, data)

        # 56. GET /api/users/{user_id}/discover
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/discover",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/discover", success, status, data)

        # 57. GET /api/users/{user_id}/commerce/offers
        success, status, data = self.test("GET", f"/api/users/{test_user_id}/commerce/offers",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"GET /api/users/{test_user_id}/commerce/offers", success, status, data)

        test_offer_id = "test-offer-123"

        # 58. POST /api/users/{user_id}/commerce/offers/{offer_id}/click
        success, status, data = self.test("POST", f"/api/users/{test_user_id}/commerce/offers/{test_offer_id}/click",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result(f"POST /api/users/{test_user_id}/commerce/offers/{test_offer_id}/click", success, status, data)

        # ADMIN ROUTER (17 endpoints)
        print("\n[ADMIN ROUTER]")

        # 59. GET /api/metrics
        success, status, data = self.test("GET", "/api/metrics", headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/metrics", success, status, data)

        # 60. GET /api/cache/diagnostics
        success, status, data = self.test("GET", "/api/cache/diagnostics", headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/cache/diagnostics", success, status, data)

        # 61. POST /api/admin/offers
        success, status, data = self.test("POST", "/api/admin/offers",
                                         json={
                                             "product_id": test_product_id,
                                             "merchant": "Amazon",
                                             "url": "https://amazon.com/test",
                                             "price_cents": 1000,
                                             "currency": "USD"
                                         },
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/admin/offers", success, status, data)

        # 62. POST /api/triage
        success, status, data = self.test("POST", "/api/triage",
                                         json={"text": "What is the best skincare routine?"})
        self.log_result("POST /api/triage", success, status, data)

        # 63. GET /api/admin/audit
        success, status, data = self.test("GET", "/api/admin/audit",
                                         params={"limit": 100},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/audit", success, status, data)

        # 64. GET /api/admin/measurement-feedback
        success, status, data = self.test("GET", "/api/admin/measurement-feedback",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/measurement-feedback", success, status, data)

        # 65. GET /api/admin/analytics
        success, status, data = self.test("GET", "/api/admin/analytics",
                                         params={"days": 7},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/analytics", success, status, data)

        # 66. GET /api/admin/analytics/daily
        success, status, data = self.test("GET", "/api/admin/analytics/daily",
                                         params={"days": 30},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/analytics/daily", success, status, data)

        # 67. GET /api/admin/analytics/events
        success, status, data = self.test("GET", "/api/admin/analytics/events",
                                         params={"days": 7},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/analytics/events", success, status, data)

        # 68. GET /api/admin/feedback
        success, status, data = self.test("GET", "/api/admin/feedback",
                                         params={"limit": 100},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/feedback", success, status, data)

        # 69. GET /api/admin/feedback/corrections
        success, status, data = self.test("GET", "/api/admin/feedback/corrections",
                                         params={"limit": 100},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/feedback/corrections", success, status, data)

        # 70. GET /api/admin/feedback/accuracy
        success, status, data = self.test("GET", "/api/admin/feedback/accuracy",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/feedback/accuracy", success, status, data)

        # 71. GET /api/admin/monitoring
        success, status, data = self.test("GET", "/api/admin/monitoring",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/monitoring", success, status, data)

        # 72. GET /api/admin/monitoring/daily-report
        success, status, data = self.test("GET", "/api/admin/monitoring/daily-report",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/monitoring/daily-report", success, status, data)

        # 73. GET /api/admin/data-collection/stats
        success, status, data = self.test("GET", "/api/admin/data-collection/stats",
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("GET /api/admin/data-collection/stats", success, status, data)

        # 74. POST /api/admin/data-collection/export
        success, status, data = self.test("POST", "/api/admin/data-collection/export",
                                         json={"output_dir": "/tmp/export", "min_quality": 0.75},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/admin/data-collection/export", success, status, data)

        # 75. POST /api/admin/data-collection/cleanup
        success, status, data = self.test("POST", "/api/admin/data-collection/cleanup",
                                         json={"retention_days": 365},
                                         headers={"Authorization": DUMMY_TOKEN})
        self.log_result("POST /api/admin/data-collection/cleanup", success, status, data)

        # Print summary
        self.print_summary()

    def log_result(self, endpoint: str, success: bool, status: str, data: Any):
        """Log the result of an endpoint test."""
        if success:
            print(f"  ✅ {endpoint} ({status})")
            self.working.append(endpoint)
        else:
            print(f"  ❌ {endpoint} ({status})")
            error_detail = str(data)[:100] if data else "No details"
            self.broken.append({"endpoint": endpoint, "status": status, "error": error_detail})

    def print_summary(self):
        """Print a summary of all test results."""
        print("\n" + "=" * 80)
        print("SUMMARY")
        print("=" * 80)
        print(f"\n✅ Working endpoints: {len(self.working)}")
        print(f"❌ Broken endpoints: {len(self.broken)}")
        print(f"📊 Total tested: {len(self.working) + len(self.broken)}")

        if self.broken:
            print("\n" + "=" * 80)
            print("BROKEN ENDPOINTS DETAILS")
            print("=" * 80)
            for item in self.broken:
                print(f"\n❌ {item['endpoint']}")
                print(f"   Status: {item['status']}")
                print(f"   Error: {item['error']}")

if __name__ == "__main__":
    tester = EndpointTester()
    tester.run_all_tests()
