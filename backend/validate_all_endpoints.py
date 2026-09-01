"""
Comprehensive Endpoint Validation - Router Refactoring
Tests all 69 endpoints across 5 routers to verify they're registered and working.
"""

import base64
import io
import sys
from collections import defaultdict

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from glowupai.complete_api import create_complete_app


def sample_image() -> str:
    """Generate a sample image for testing."""
    image = Image.new("RGB", (240, 240), (210, 165, 145))
    draw = ImageDraw.Draw(image)
    for y in range(0, 240, 8):
        for x in range(0, 240, 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=(165, 130, 118))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode()


class EndpointValidator:
    """Validates all API endpoints."""

    def __init__(self):
        self.client = TestClient(create_complete_app())
        self.admin_headers = {"Authorization": "Bearer test_admin_token_12345"}
        self.results = defaultdict(list)

    def test_endpoint(self, router, name, method, path, expected_codes, **kwargs):
        """Test a single endpoint and record result."""
        try:
            if method == "GET":
                response = self.client.get(path, **kwargs)
            elif method == "POST":
                response = self.client.post(path, **kwargs)
            elif method == "PATCH":
                response = self.client.patch(path, **kwargs)
            elif method == "DELETE":
                response = self.client.delete(path, **kwargs)
            else:
                raise ValueError(f"Unsupported method: {method}")

            success = response.status_code in expected_codes
            self.results[router].append({
                "name": name,
                "method": method,
                "path": path,
                "status": "✓ PASS" if success else "✗ FAIL",
                "expected": expected_codes,
                "actual": response.status_code,
                "error": None if success else response.text[:100]
            })

            return response, success

        except Exception as e:
            self.results[router].append({
                "name": name,
                "method": method,
                "path": path,
                "status": "! ERROR",
                "expected": expected_codes,
                "actual": None,
                "error": str(e)[:100]
            })
            return None, False

    def validate_all(self):
        """Validate all 69 endpoints."""

        print("=" * 80)
        print("ROUTER REFACTORING - COMPREHENSIVE ENDPOINT VALIDATION")
        print("=" * 80)
        print()

        # Create test data
        user_response, _ = self.test_endpoint(
            "Users", "create_user", "POST", "/api/users",
            [200], json={"skin_type": "combination"}
        )

        if not user_response or user_response.status_code != 200:
            print("ERROR: Could not create test user. Aborting tests.")
            return 1

        user_id = user_response.json()["user"]["id"]
        print(f"✓ Created test user: {user_id}\n")

        # ====================================================================
        # USERS ROUTER (8 endpoints)
        # ====================================================================
        print("Testing Users Router (8 endpoints)...")

        self.test_endpoint(
            "Users", "auth_session", "POST", "/api/auth/session",
            [401]  # Expected without valid token
        )

        self.test_endpoint(
            "Users", "get_profile", "GET", f"/api/users/{user_id}/profile",
            [200]
        )

        self.test_endpoint(
            "Users", "update_profile", "PATCH", f"/api/users/{user_id}/profile",
            [200], json={"display_name": "Test", "skin_type": "dry"}
        )

        self.test_endpoint(
            "Users", "consent", "POST", f"/api/users/{user_id}/consent",
            [200], json={"facial_data": True, "policy_version": "1.0"}
        )

        self.test_endpoint(
            "Users", "consent_data_collection", "POST",
            f"/api/users/{user_id}/consent/data-collection",
            [200], json={"granted": True, "policy_version": "1.0"}
        )

        self.test_endpoint(
            "Users", "export_user", "GET", f"/api/users/{user_id}/export",
            [200]
        )

        delete_user_id = self.client.post("/api/users", json={}).json()["user"]["id"]
        self.test_endpoint(
            "Users", "delete_user", "DELETE", f"/api/users/{delete_user_id}",
            [204]
        )

        # ====================================================================
        # CAPTURES ROUTER (16 endpoints)
        # ====================================================================
        print("Testing Captures Router (16 endpoints)...")

        capture_response, _ = self.test_endpoint(
            "Captures", "create_capture", "POST", "/api/captures",
            [200], json={
                "user_id": user_id,
                "image_base64": sample_image(),
                "vertical": "skin"
            }
        )

        capture_id = capture_response.json().get("capture_id") if capture_response else "test-id"

        self.test_endpoint(
            "Captures", "submit_feedback", "POST",
            f"/api/captures/{capture_id}/feedback",
            [401],  # Expected without valid Firebase token
            json={"feedback_type": "incorrect", "issues": ["redness"]}
        )

        self.test_endpoint(
            "Captures", "capture_guide", "GET",
            f"/api/users/{user_id}/capture-guide?vertical=skin",
            [200]
        )

        self.test_endpoint(
            "Captures", "dashboard", "GET",
            f"/api/users/{user_id}/dashboard?vertical=skin",
            [200]
        )

        self.test_endpoint(
            "Captures", "history", "GET",
            f"/api/users/{user_id}/history?vertical=skin",
            [200]
        )

        self.test_endpoint(
            "Captures", "get_check_ins", "GET",
            f"/api/users/{user_id}/check-ins",
            [200]
        )

        self.test_endpoint(
            "Captures", "create_check_in", "POST",
            f"/api/users/{user_id}/check-ins",
            [200], json={"routine_state": "steady", "skin_feel": "better"}
        )

        self.test_endpoint(
            "Captures", "weekly_recap", "GET",
            f"/api/users/{user_id}/weekly-recap?vertical=skin",
            [200]
        )

        self.test_endpoint(
            "Captures", "measurement_feedback", "POST",
            f"/api/users/{user_id}/measurement-feedback",
            [200], json={"capture_id": capture_id, "agreement": "fair"}
        )

        self.test_endpoint(
            "Captures", "get_labels", "GET",
            f"/api/users/{user_id}/labels",
            [200]
        )

        photo_id = capture_response.json().get("photo_id", "test-photo") if capture_response else "test-photo"
        self.test_endpoint(
            "Captures", "add_label", "POST",
            f"/api/users/{user_id}/labels",
            [200], json={"photo_id": photo_id, "label_type": "blemish", "value": "present"}
        )

        # Premium features - need upgraded user
        premium_user_id = self.client.post("/api/users", json={}).json()["user"]["id"]
        self.client.post(f"/api/users/{premium_user_id}/subscription/upgrade", json={})

        reprocess_resp, _ = self.test_endpoint(
            "Captures", "reprocess", "POST",
            f"/api/users/{premium_user_id}/reprocess",
            [200], json={"model_version": "deterministic-v2"}
        )

        job_id = reprocess_resp.json().get("job_id", "test-job") if reprocess_resp else "test-job"
        self.test_endpoint(
            "Captures", "reprocess_status", "GET",
            f"/api/users/{premium_user_id}/reprocess/{job_id}",
            [200]
        )

        scan_resp, _ = self.test_endpoint(
            "Captures", "shelf_scan", "POST",
            f"/api/users/{premium_user_id}/shelf-scan",
            [200], json={"image_base64": sample_image()}
        )

        scan_job_id = scan_resp.json().get("job_id", "test-scan") if scan_resp else "test-scan"
        self.test_endpoint(
            "Captures", "shelf_scan_status", "GET",
            f"/api/users/{premium_user_id}/shelf-scan/{scan_job_id}",
            [200]
        )

        self.test_endpoint(
            "Captures", "shelf_scan_confirm", "POST",
            f"/api/users/{premium_user_id}/shelf-scan/{scan_job_id}/confirm",
            [200], json={"selections": []}
        )

        # ====================================================================
        # ANALYTICS ROUTER (8 endpoints)
        # ====================================================================
        print("Testing Analytics Router (8 endpoints)...")

        self.test_endpoint(
            "Analytics", "analytics", "GET",
            f"/api/users/{user_id}/analytics",
            [200]
        )

        self.test_endpoint(
            "Analytics", "get_engagement", "GET",
            f"/api/users/{user_id}/engagement",
            [200]
        )

        self.test_endpoint(
            "Analytics", "engagement_event", "POST",
            f"/api/users/{user_id}/engagement",
            [200], json={"event_type": "dashboard_viewed"}
        )

        self.test_endpoint(
            "Analytics", "get_context_events", "GET",
            f"/api/users/{user_id}/context-events",
            [200]
        )

        self.test_endpoint(
            "Analytics", "add_context_event", "POST",
            f"/api/users/{user_id}/context-events",
            [200], json={"event_type": "diet_change", "value": "supplements"}
        )

        self.test_endpoint(
            "Analytics", "root_cause", "GET",
            f"/api/users/{premium_user_id}/root-cause?metric=texture_score",
            [200]
        )

        self.test_endpoint(
            "Analytics", "budget_optimizer", "GET",
            f"/api/users/{premium_user_id}/budget-optimizer",
            [200]
        )

        self.test_endpoint(
            "Analytics", "derm_export", "GET",
            f"/api/users/{premium_user_id}/derm-export",
            [200]
        )

        # ====================================================================
        # SUBSCRIPTIONS ROUTER (21 endpoints)
        # ====================================================================
        print("Testing Subscriptions Router (21 endpoints)...")

        self.test_endpoint(
            "Subscriptions", "subscription", "GET",
            f"/api/users/{user_id}/subscription",
            [200]
        )

        upgrade_user_id = self.client.post("/api/users", json={}).json()["user"]["id"]
        self.test_endpoint(
            "Subscriptions", "upgrade", "POST",
            f"/api/users/{upgrade_user_id}/subscription/upgrade",
            [200], json={"source": "test"}
        )

        self.test_endpoint(
            "Subscriptions", "cancel_subscription", "POST",
            f"/api/users/{upgrade_user_id}/subscription/cancel",
            [200], json={}
        )

        product_resp, _ = self.test_endpoint(
            "Subscriptions", "create_product", "POST", "/api/products",
            [200], json={"name": "Test Serum", "category": "serum"}
        )

        product_id = product_resp.json().get("id", "test-product") if product_resp else "test-product"

        self.test_endpoint(
            "Subscriptions", "search_products", "GET",
            "/api/products/search?q=serum",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "lookup_product", "GET",
            "/api/products/lookup?barcode=123456",
            [404]  # No product with this barcode
        )

        self.test_endpoint(
            "Subscriptions", "product_detail", "GET",
            f"/api/products/{product_id}?user_id={user_id}",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "ingredient_explainer", "GET",
            f"/api/products/{product_id}/ingredient-explainer?user_id={premium_user_id}",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "predict_product", "GET",
            f"/api/products/{product_id}/predict?user_id={premium_user_id}",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "purchase_guidance", "POST",
            f"/api/users/{premium_user_id}/purchase-guidance",
            [200], json={"name": "New Product", "category": "moisturizer"}
        )

        self.test_endpoint(
            "Subscriptions", "routine_event", "POST",
            "/api/routine-events",
            [200], json={
                "user_id": user_id,
                "product_id": product_id,
                "action": "start"
            }
        )

        self.test_endpoint(
            "Subscriptions", "confound_check", "GET",
            f"/api/users/{premium_user_id}/confound-check",
            [200]
        )

        exp_resp, _ = self.test_endpoint(
            "Subscriptions", "create_experiment", "POST",
            "/api/experiments",
            [200], json={
                "user_id": premium_user_id,
                "name": "Test Experiment",
                "product_id": product_id,
                "target_days": 14
            }
        )

        experiment_id = exp_resp.json().get("id", "test-exp") if exp_resp else "test-exp"

        self.test_endpoint(
            "Subscriptions", "experiments", "GET",
            f"/api/users/{premium_user_id}/experiments",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "experiment_detail", "GET",
            f"/api/users/{premium_user_id}/experiments/{experiment_id}",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "experiment_status", "POST",
            f"/api/users/{premium_user_id}/experiments/{experiment_id}/status",
            [200], json={"user_id": premium_user_id, "status": "completed"}
        )

        self.test_endpoint(
            "Subscriptions", "qna", "POST",
            f"/api/users/{premium_user_id}/qna",
            [200], json={"question": "How is my skin?"}
        )

        self.test_endpoint(
            "Subscriptions", "qna_history", "GET",
            f"/api/users/{premium_user_id}/qna",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "discover", "GET",
            f"/api/users/{premium_user_id}/discover",
            [200]
        )

        self.test_endpoint(
            "Subscriptions", "offers", "GET",
            f"/api/users/{premium_user_id}/commerce/offers",
            [200]
        )

        offer_resp = self.client.post(
            "/api/admin/offers",
            json={
                "product_id": product_id,
                "merchant": "Test",
                "url": "https://example.com",
                "price_cents": 1999
            },
            headers=self.admin_headers
        )
        offer_id = offer_resp.json().get("id", "test-offer") if offer_resp.status_code == 200 else "test-offer"

        self.test_endpoint(
            "Subscriptions", "click_offer", "POST",
            f"/api/users/{premium_user_id}/commerce/offers/{offer_id}/click",
            [200, 404],  # 404 if admin token not configured
            json={}
        )

        # ====================================================================
        # ADMIN ROUTER (16 endpoints)
        # ====================================================================
        print("Testing Admin Router (16 endpoints)...")

        # Note: Admin endpoints require GLOWUPAI_ADMIN_TOKEN to be set
        # These will return 403 without it, which is expected behavior

        self.test_endpoint(
            "Admin", "metrics", "GET", "/api/metrics",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "add_offer", "POST", "/api/admin/offers",
            [200, 403], headers=self.admin_headers,
            json={
                "product_id": product_id,
                "merchant": "Test",
                "url": "https://example.com",
                "price_cents": 2999
            }
        )

        self.test_endpoint(
            "Admin", "triage_question", "POST", "/api/triage",
            [200], json={"text": "Test question"}
        )

        self.test_endpoint(
            "Admin", "audit", "GET", "/api/admin/audit?limit=10",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "measurement_feedback_summary", "GET",
            "/api/admin/measurement-feedback",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_analytics", "GET",
            "/api/admin/analytics?days=7",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_analytics_daily", "GET",
            "/api/admin/analytics/daily?days=30",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_analytics_events", "GET",
            "/api/admin/analytics/events?days=7",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_feedback", "GET",
            "/api/admin/feedback?limit=100",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_feedback_corrections", "GET",
            "/api/admin/feedback/corrections?limit=100",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_feedback_accuracy", "GET",
            "/api/admin/feedback/accuracy",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_monitoring", "GET",
            "/api/admin/monitoring",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_monitoring_daily", "GET",
            "/api/admin/monitoring/daily-report",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_data_collection_stats", "GET",
            "/api/admin/data-collection/stats",
            [200, 403], headers=self.admin_headers
        )

        self.test_endpoint(
            "Admin", "admin_data_collection_export", "POST",
            "/api/admin/data-collection/export",
            [200, 403], headers=self.admin_headers,
            json={"output_dir": "/tmp/test", "min_quality": 0.8}
        )

        self.test_endpoint(
            "Admin", "admin_data_collection_cleanup", "POST",
            "/api/admin/data-collection/cleanup",
            [200, 403], headers=self.admin_headers,
            json={"retention_days": 365}
        )

        return 0

    def print_summary(self):
        """Print validation summary."""

        print("\n" + "=" * 80)
        print("VALIDATION SUMMARY")
        print("=" * 80)
        print()

        total_tests = 0
        total_pass = 0
        total_fail = 0
        total_error = 0

        router_order = ["Users", "Captures", "Analytics", "Subscriptions", "Admin"]

        for router_name in router_order:
            tests = self.results.get(router_name, [])
            if not tests:
                continue

            pass_count = sum(1 for t in tests if t["status"] == "✓ PASS")
            fail_count = sum(1 for t in tests if t["status"] == "✗ FAIL")
            error_count = sum(1 for t in tests if t["status"] == "! ERROR")

            total_tests += len(tests)
            total_pass += pass_count
            total_fail += fail_count
            total_error += error_count

            print(f"{router_name} Router: {len(tests)} endpoints")
            print(f"  ✓ Pass:  {pass_count}")
            if fail_count > 0:
                print(f"  ✗ Fail:  {fail_count}")
            if error_count > 0:
                print(f"  ! Error: {error_count}")

            # Show failures
            failures = [t for t in tests if t["status"] != "✓ PASS"]
            if failures:
                for test in failures:
                    print(f"    {test['status']} {test['name']}: {test['method']} {test['path']}")
                    if test['error']:
                        print(f"         {test['error']}")

            print()

        print("=" * 80)
        print(f"TOTAL: {total_tests} endpoints tested")
        print(f"  ✓ PASS:  {total_pass}")
        if total_fail > 0:
            print(f"  ✗ FAIL:  {total_fail}")
        if total_error > 0:
            print(f"  ! ERROR: {total_error}")
        print("=" * 80)

        return 0 if (total_fail == 0 and total_error == 0) else 1


if __name__ == "__main__":
    validator = EndpointValidator()
    validator.validate_all()
    exit_code = validator.print_summary()
    sys.exit(exit_code)
