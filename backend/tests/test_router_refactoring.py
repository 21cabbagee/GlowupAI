"""
Comprehensive endpoint validation test for router refactoring.
Tests all 70 endpoints across 5 routers:
- Users Router: 9 endpoints
- Captures Router: 16 endpoints
- Analytics Router: 8 endpoints
- Subscriptions Router: 21 endpoints
- Admin Router: 16 endpoints
"""

from __future__ import annotations

import base64
import io
import json
import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from glowupai.complete_api import create_complete_app
from glowupai.complete_db import FullDatabase
from glowupai.complete_service import CompleteGlowupAIService
from glowupai.config import Settings
from glowupai.photos import MemoryPhotoStore


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


class RouterRefactoringValidationTest(unittest.TestCase):
    """Validate all endpoints after router refactoring."""

    def setUp(self):
        """Set up test client and database."""
        self.temp = tempfile.TemporaryDirectory()
        self.db = FullDatabase(Path(self.temp.name) / "test.sqlite3")
        test_settings = Settings(
            db_path=Path(self.temp.name) / "test.sqlite3",
            photo_dir=None,
            gemini_api_key=None,
            gemini_enabled=False,
            admin_token="test_admin_token_12345",
            auth_required=False,
        )
        self.service = CompleteGlowupAIService(
            self.db, settings=test_settings, photos=MemoryPhotoStore()
        )
        self.client = TestClient(create_complete_app(self.service))
        self.admin_headers = {"Authorization": "Bearer test_admin_token_12345"}

        # Track results for reporting
        self.results = {
            "users": {},
            "captures": {},
            "analytics": {},
            "subscriptions": {},
            "admin": {},
        }

    def tearDown(self):
        """Clean up test resources."""
        self.db.close()
        self.temp.cleanup()

    def create_test_user(self) -> str:
        """Create a test user and return user_id."""
        response = self.client.post("/api/users", json={"skin_type": "combination"})
        self.assertEqual(response.status_code, 200, f"Failed to create user: {response.text}")
        user_id = response.json()["user"]["id"]

        # Grant consent
        self.client.post(
            f"/api/users/{user_id}/consent",
            json={"facial_data": True, "policy_version": "1.0"}
        )
        return user_id

    def create_premium_user(self) -> str:
        """Create a premium user and return user_id."""
        user_id = self.create_test_user()
        response = self.client.post(
            f"/api/users/{user_id}/subscription/upgrade",
            json={"source": "test"}
        )
        self.assertEqual(response.status_code, 200, f"Failed to upgrade user: {response.text}")
        return user_id

    def create_test_product(self) -> dict:
        """Create a test product and return product data."""
        response = self.client.post(
            "/api/products",
            json={
                "name": "Test Product",
                "category": "serum",
                "ingredients": "Water, Niacinamide",
                "stabilization_days": 14,
            }
        )
        self.assertEqual(response.status_code, 200, f"Failed to create product: {response.text}")
        return response.json()

    def create_test_capture(self, user_id: str, is_baseline: bool = False) -> dict:
        """Create a test capture and return capture data."""
        response = self.client.post(
            "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": sample_image(),
                "vertical": "skin",
                "is_baseline": is_baseline,
            }
        )
        self.assertEqual(response.status_code, 200, f"Failed to create capture: {response.text}")
        return response.json()

    def test_endpoint(self, router: str, name: str, method: str, path: str,
                      expected_status: int = 200, **kwargs):
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

            success = response.status_code == expected_status
            self.results[router][name] = {
                "status": "PASS" if success else "FAIL",
                "expected": expected_status,
                "actual": response.status_code,
                "method": method,
                "path": path,
            }

            if not success:
                self.results[router][name]["error"] = response.text

            return response

        except Exception as e:
            self.results[router][name] = {
                "status": "ERROR",
                "method": method,
                "path": path,
                "error": str(e),
            }
            raise

    # ========================================================================
    # USERS ROUTER TESTS (9 endpoints)
    # ========================================================================

    def test_users_01_create_user(self):
        """POST /api/users"""
        response = self.test_endpoint(
            "users", "create_user", "POST", "/api/users",
            json={"skin_type": "oily"}
        )
        self.assertIn("user", response.json())

    def test_users_02_auth_session(self):
        """POST /api/auth/session"""
        # This requires a valid Firebase token, so we expect 401 without proper auth
        self.test_endpoint(
            "users", "auth_session", "POST", "/api/auth/session",
            expected_status=401
        )

    def test_users_03_get_user(self):
        """GET /api/users/{id}"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "users", "get_user", "GET", f"/api/users/{user_id}"
        )
        result = response.json()
        self.assertIn("user", result)
        self.assertIn("appearance_profiles", result)
        self.assertIn("entitlement", result)
        self.assertIn("experience_profile", result)

    def test_users_04_get_profile(self):
        """GET /api/users/{user_id}/profile"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "users", "get_profile", "GET", f"/api/users/{user_id}/profile"
        )
        self.assertIn("id", response.json())

    def test_users_05_update_profile(self):
        """PATCH /api/users/{user_id}/profile"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "users", "update_profile", "PATCH", f"/api/users/{user_id}/profile",
            json={
                "display_name": "Test User",
                "skin_type": "dry",
                "experience_level": "beginner"
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_users_06_consent(self):
        """POST /api/users/{user_id}/consent"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "users", "consent", "POST", f"/api/users/{user_id}/consent",
            json={"facial_data": True, "policy_version": "1.0"}
        )
        self.assertEqual(response.status_code, 200)

    def test_users_07_consent_data_collection(self):
        """POST /api/users/{user_id}/consent/data-collection"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "users", "consent_data_collection", "POST",
            f"/api/users/{user_id}/consent/data-collection",
            json={"granted": True, "policy_version": "1.0"}
        )
        self.assertEqual(response.status_code, 200)

    def test_users_08_export_user(self):
        """GET /api/users/{user_id}/export"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "users", "export_user", "GET", f"/api/users/{user_id}/export"
        )
        self.assertIn("user", response.json())

    def test_users_09_delete_user(self):
        """DELETE /api/users/{user_id}"""
        user_id = self.create_test_user()
        self.test_endpoint(
            "users", "delete_user", "DELETE", f"/api/users/{user_id}",
            expected_status=204
        )

    # ========================================================================
    # CAPTURES ROUTER TESTS (16 endpoints)
    # ========================================================================

    def test_captures_01_create_capture(self):
        """POST /api/captures"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "create_capture", "POST", "/api/captures",
            json={
                "user_id": user_id,
                "image_base64": sample_image(),
                "vertical": "skin",
            }
        )
        self.assertIn("capture_id", response.json())

    def test_captures_02_submit_feedback(self):
        """POST /api/captures/{capture_id}/feedback"""
        user_id = self.create_test_user()
        capture = self.create_test_capture(user_id)
        capture_id = capture["capture_id"]

        # This endpoint requires proper auth with Firebase token
        self.test_endpoint(
            "captures", "submit_feedback", "POST",
            f"/api/captures/{capture_id}/feedback",
            json={
                "feedback_type": "incorrect",
                "issues": ["redness"],
                "comment": "Test feedback"
            },
            expected_status=401  # Expected without valid Firebase token
        )

    def test_captures_03_capture_guide(self):
        """GET /api/users/{user_id}/capture-guide"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "capture_guide", "GET",
            f"/api/users/{user_id}/capture-guide?vertical=skin"
        )
        self.assertIn("state", response.json())

    def test_captures_04_dashboard(self):
        """GET /api/users/{user_id}/dashboard"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "dashboard", "GET",
            f"/api/users/{user_id}/dashboard?vertical=skin"
        )
        self.assertIn("history", response.json())

    def test_captures_05_history(self):
        """GET /api/users/{user_id}/history"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "history", "GET",
            f"/api/users/{user_id}/history?vertical=skin"
        )
        self.assertIn("captures", response.json())

    def test_captures_06_get_check_ins(self):
        """GET /api/users/{user_id}/check-ins"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "get_check_ins", "GET",
            f"/api/users/{user_id}/check-ins"
        )
        self.assertIsInstance(response.json(), list)

    def test_captures_07_create_check_in(self):
        """POST /api/users/{user_id}/check-ins"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "create_check_in", "POST",
            f"/api/users/{user_id}/check-ins",
            json={
                "routine_state": "steady",
                "skin_feel": "better",
                "note": "Test check-in"
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_captures_08_weekly_recap(self):
        """GET /api/users/{user_id}/weekly-recap"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "weekly_recap", "GET",
            f"/api/users/{user_id}/weekly-recap?vertical=skin"
        )
        self.assertIn("captures", response.json())

    def test_captures_09_measurement_feedback(self):
        """POST /api/users/{user_id}/measurement-feedback"""
        user_id = self.create_test_user()
        capture = self.create_test_capture(user_id)
        response = self.test_endpoint(
            "captures", "measurement_feedback", "POST",
            f"/api/users/{user_id}/measurement-feedback",
            json={
                "capture_id": capture["capture_id"],
                "agreement": "fair",
                "note": "Looks accurate"
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_captures_10_get_labels(self):
        """GET /api/users/{user_id}/labels"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "captures", "get_labels", "GET",
            f"/api/users/{user_id}/labels"
        )
        self.assertIsInstance(response.json(), list)

    def test_captures_11_add_label(self):
        """POST /api/users/{user_id}/labels"""
        user_id = self.create_test_user()
        capture = self.create_test_capture(user_id)
        photo_id = capture["photo_id"]

        response = self.test_endpoint(
            "captures", "add_label", "POST",
            f"/api/users/{user_id}/labels",
            json={
                "photo_id": photo_id,
                "label_type": "blemish",
                "value": "present",
                "confidence": 0.9
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_captures_12_reprocess(self):
        """POST /api/users/{user_id}/reprocess"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "captures", "reprocess", "POST",
            f"/api/users/{user_id}/reprocess",
            json={"model_version": "deterministic-v2"}
        )
        self.assertIn("job_id", response.json())

    def test_captures_13_reprocess_status(self):
        """GET /api/users/{user_id}/reprocess/{job_id}"""
        user_id = self.create_premium_user()
        reprocess_response = self.client.post(
            f"/api/users/{user_id}/reprocess",
            json={"model_version": "deterministic-v2"}
        )
        job_id = reprocess_response.json()["job_id"]

        response = self.test_endpoint(
            "captures", "reprocess_status", "GET",
            f"/api/users/{user_id}/reprocess/{job_id}"
        )
        self.assertIn("status", response.json())

    def test_captures_14_shelf_scan(self):
        """POST /api/users/{user_id}/shelf-scan"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "captures", "shelf_scan", "POST",
            f"/api/users/{user_id}/shelf-scan",
            json={"image_base64": sample_image()}
        )
        self.assertIn("job_id", response.json())

    def test_captures_15_shelf_scan_status(self):
        """GET /api/users/{user_id}/shelf-scan/{job_id}"""
        user_id = self.create_premium_user()
        scan_response = self.client.post(
            f"/api/users/{user_id}/shelf-scan",
            json={"image_base64": sample_image()}
        )
        job_id = scan_response.json()["job_id"]

        response = self.test_endpoint(
            "captures", "shelf_scan_status", "GET",
            f"/api/users/{user_id}/shelf-scan/{job_id}"
        )
        self.assertIn("status", response.json())

    def test_captures_16_shelf_scan_confirm(self):
        """POST /api/users/{user_id}/shelf-scan/{job_id}/confirm"""
        user_id = self.create_premium_user()
        scan_response = self.client.post(
            f"/api/users/{user_id}/shelf-scan",
            json={"image_base64": sample_image()}
        )
        job_id = scan_response.json()["job_id"]

        response = self.test_endpoint(
            "captures", "shelf_scan_confirm", "POST",
            f"/api/users/{user_id}/shelf-scan/{job_id}/confirm",
            json={"selections": []}
        )
        self.assertEqual(response.status_code, 200)

    # ========================================================================
    # ANALYTICS ROUTER TESTS (8 endpoints)
    # ========================================================================

    def test_analytics_01_analytics(self):
        """GET /api/users/{user_id}/analytics"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "analytics", "analytics", "GET",
            f"/api/users/{user_id}/analytics"
        )
        self.assertIn("captures_this_week", response.json())

    def test_analytics_02_get_engagement(self):
        """GET /api/users/{user_id}/engagement"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "analytics", "get_engagement", "GET",
            f"/api/users/{user_id}/engagement"
        )
        self.assertIsInstance(response.json(), list)

    def test_analytics_03_engagement_event(self):
        """POST /api/users/{user_id}/engagement"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "analytics", "engagement_event", "POST",
            f"/api/users/{user_id}/engagement",
            json={
                "event_type": "dashboard_viewed",
                "reference_id": None,
                "metadata": {}
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_analytics_04_get_context_events(self):
        """GET /api/users/{user_id}/context-events"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "analytics", "get_context_events", "GET",
            f"/api/users/{user_id}/context-events"
        )
        self.assertIsInstance(response.json(), list)

    def test_analytics_05_add_context_event(self):
        """POST /api/users/{user_id}/context-events"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "analytics", "add_context_event", "POST",
            f"/api/users/{user_id}/context-events",
            json={
                "event_type": "diet_change",
                "value": "started_supplements",
                "notes": "Test context event"
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_analytics_06_root_cause(self):
        """GET /api/users/{user_id}/root-cause"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "analytics", "root_cause", "GET",
            f"/api/users/{user_id}/root-cause?metric=texture_score"
        )
        self.assertIn("analysis", response.json())

    def test_analytics_07_budget_optimizer(self):
        """GET /api/users/{user_id}/budget-optimizer"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "analytics", "budget_optimizer", "GET",
            f"/api/users/{user_id}/budget-optimizer"
        )
        self.assertIn("recommendations", response.json())

    def test_analytics_08_derm_export(self):
        """GET /api/users/{user_id}/derm-export"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "analytics", "derm_export", "GET",
            f"/api/users/{user_id}/derm-export"
        )
        self.assertIn("report", response.json())

    # ========================================================================
    # SUBSCRIPTIONS ROUTER TESTS (21 endpoints)
    # ========================================================================

    def test_subscriptions_01_subscription(self):
        """GET /api/users/{user_id}/subscription"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "subscriptions", "subscription", "GET",
            f"/api/users/{user_id}/subscription"
        )
        self.assertIn("plan", response.json())

    def test_subscriptions_02_upgrade(self):
        """POST /api/users/{user_id}/subscription/upgrade"""
        user_id = self.create_test_user()
        response = self.test_endpoint(
            "subscriptions", "upgrade", "POST",
            f"/api/users/{user_id}/subscription/upgrade",
            json={"source": "test"}
        )
        self.assertIn("plan", response.json())

    def test_subscriptions_03_cancel_subscription(self):
        """POST /api/users/{user_id}/subscription/cancel"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "cancel_subscription", "POST",
            f"/api/users/{user_id}/subscription/cancel",
            json={}
        )
        self.assertEqual(response.json()["plan"], "free")

    def test_subscriptions_04_create_product(self):
        """POST /api/products"""
        response = self.test_endpoint(
            "subscriptions", "create_product", "POST", "/api/products",
            json={
                "name": "Test Serum",
                "category": "serum",
                "ingredients": "Water, Niacinamide",
                "stabilization_days": 14
            }
        )
        self.assertIn("id", response.json())

    def test_subscriptions_05_search_products(self):
        """GET /api/products/search"""
        response = self.test_endpoint(
            "subscriptions", "search_products", "GET",
            "/api/products/search?q=serum"
        )
        self.assertIsInstance(response.json(), list)

    def test_subscriptions_06_lookup_product(self):
        """GET /api/products/lookup"""
        response = self.test_endpoint(
            "subscriptions", "lookup_product", "GET",
            "/api/products/lookup?barcode=123456789",
            expected_status=404  # No product with this barcode
        )

    def test_subscriptions_07_product_detail(self):
        """GET /api/products/{product_id}"""
        user_id = self.create_test_user()
        product = self.create_test_product()
        response = self.test_endpoint(
            "subscriptions", "product_detail", "GET",
            f"/api/products/{product['id']}?user_id={user_id}"
        )
        self.assertIn("id", response.json())

    def test_subscriptions_08_ingredient_explainer(self):
        """GET /api/products/{product_id}/ingredient-explainer"""
        user_id = self.create_premium_user()
        product = self.create_test_product()
        response = self.test_endpoint(
            "subscriptions", "ingredient_explainer", "GET",
            f"/api/products/{product['id']}/ingredient-explainer?user_id={user_id}"
        )
        self.assertIn("ingredients", response.json())

    def test_subscriptions_09_predict_product(self):
        """GET /api/products/{product_id}/predict"""
        user_id = self.create_premium_user()
        product = self.create_test_product()
        response = self.test_endpoint(
            "subscriptions", "predict_product", "GET",
            f"/api/products/{product['id']}/predict?user_id={user_id}"
        )
        self.assertIn("prediction", response.json())

    def test_subscriptions_10_purchase_guidance(self):
        """POST /api/users/{user_id}/purchase-guidance"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "purchase_guidance", "POST",
            f"/api/users/{user_id}/purchase-guidance",
            json={
                "name": "New Product",
                "category": "moisturizer",
                "ingredients": "Water, Glycerin",
                "price_cents": 2500,
                "currency": "USD"
            }
        )
        self.assertIn("recommendation", response.json())

    def test_subscriptions_11_routine_event(self):
        """POST /api/routine-events"""
        user_id = self.create_test_user()
        product = self.create_test_product()
        response = self.test_endpoint(
            "subscriptions", "routine_event", "POST",
            "/api/routine-events",
            json={
                "user_id": user_id,
                "product_id": product["id"],
                "action": "start",
                "slot": "morning"
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_subscriptions_12_confound_check(self):
        """GET /api/users/{user_id}/confound-check"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "confound_check", "GET",
            f"/api/users/{user_id}/confound-check"
        )
        self.assertIn("confounds", response.json())

    def test_subscriptions_13_create_experiment(self):
        """POST /api/experiments"""
        user_id = self.create_premium_user()
        product = self.create_test_product()
        response = self.test_endpoint(
            "subscriptions", "create_experiment", "POST",
            "/api/experiments",
            json={
                "user_id": user_id,
                "name": "Test Experiment",
                "hypothesis": "Reduces redness",
                "product_id": product["id"],
                "primary_metric": "redness_score",
                "target_days": 14
            }
        )
        self.assertIn("id", response.json())

    def test_subscriptions_14_experiments(self):
        """GET /api/users/{user_id}/experiments"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "experiments", "GET",
            f"/api/users/{user_id}/experiments"
        )
        self.assertIsInstance(response.json(), list)

    def test_subscriptions_15_experiment_detail(self):
        """GET /api/users/{user_id}/experiments/{experiment_id}"""
        user_id = self.create_premium_user()
        product = self.create_test_product()
        exp_response = self.client.post(
            "/api/experiments",
            json={
                "user_id": user_id,
                "name": "Test",
                "product_id": product["id"],
                "target_days": 14
            }
        )
        experiment_id = exp_response.json()["id"]

        response = self.test_endpoint(
            "subscriptions", "experiment_detail", "GET",
            f"/api/users/{user_id}/experiments/{experiment_id}"
        )
        self.assertIn("id", response.json())

    def test_subscriptions_16_experiment_status(self):
        """POST /api/users/{user_id}/experiments/{experiment_id}/status"""
        user_id = self.create_premium_user()
        product = self.create_test_product()
        exp_response = self.client.post(
            "/api/experiments",
            json={
                "user_id": user_id,
                "name": "Test",
                "product_id": product["id"],
                "target_days": 14
            }
        )
        experiment_id = exp_response.json()["id"]

        response = self.test_endpoint(
            "subscriptions", "experiment_status", "POST",
            f"/api/users/{user_id}/experiments/{experiment_id}/status",
            json={"user_id": user_id, "status": "completed"}
        )
        self.assertEqual(response.status_code, 200)

    def test_subscriptions_17_qna(self):
        """POST /api/users/{user_id}/qna"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "qna", "POST",
            f"/api/users/{user_id}/qna",
            json={"question": "How is my skin improving?"}
        )
        self.assertIn("answer", response.json())

    def test_subscriptions_18_qna_history(self):
        """GET /api/users/{user_id}/qna"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "qna_history", "GET",
            f"/api/users/{user_id}/qna"
        )
        self.assertIsInstance(response.json(), list)

    def test_subscriptions_19_discover(self):
        """GET /api/users/{user_id}/discover"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "discover", "GET",
            f"/api/users/{user_id}/discover"
        )
        self.assertIn("cards", response.json())

    def test_subscriptions_20_offers(self):
        """GET /api/users/{user_id}/commerce/offers"""
        user_id = self.create_premium_user()
        response = self.test_endpoint(
            "subscriptions", "offers", "GET",
            f"/api/users/{user_id}/commerce/offers"
        )
        self.assertIsInstance(response.json(), list)

    def test_subscriptions_21_click_offer(self):
        """POST /api/users/{user_id}/commerce/offers/{offer_id}/click"""
        user_id = self.create_premium_user()
        # Create an offer first via admin endpoint
        product = self.create_test_product()
        offer_response = self.client.post(
            "/api/admin/offers",
            json={
                "product_id": product["id"],
                "merchant": "TestMerchant",
                "url": "https://example.com",
                "price_cents": 1999,
                "currency": "USD"
            },
            headers=self.admin_headers
        )
        offer_id = offer_response.json()["id"]

        response = self.test_endpoint(
            "subscriptions", "click_offer", "POST",
            f"/api/users/{user_id}/commerce/offers/{offer_id}/click",
            json={}
        )
        self.assertEqual(response.status_code, 200)

    # ========================================================================
    # ADMIN ROUTER TESTS (16 endpoints)
    # ========================================================================

    def test_admin_01_metrics(self):
        """GET /api/metrics"""
        response = self.test_endpoint(
            "admin", "metrics", "GET", "/api/metrics",
            headers=self.admin_headers
        )
        self.assertIn("requests", response.json())

    def test_admin_02_add_offer(self):
        """POST /api/admin/offers"""
        product = self.create_test_product()
        response = self.test_endpoint(
            "admin", "add_offer", "POST", "/api/admin/offers",
            json={
                "product_id": product["id"],
                "merchant": "TestMerchant",
                "url": "https://example.com/product",
                "price_cents": 2999,
                "currency": "USD"
            },
            headers=self.admin_headers
        )
        self.assertIn("id", response.json())

    def test_admin_03_triage_question(self):
        """POST /api/triage"""
        response = self.test_endpoint(
            "admin", "triage_question", "POST", "/api/triage",
            json={"text": "How do I improve my skin texture?"}
        )
        self.assertIn("category", response.json())

    def test_admin_04_audit(self):
        """GET /api/admin/audit"""
        response = self.test_endpoint(
            "admin", "audit", "GET", "/api/admin/audit?limit=10",
            headers=self.admin_headers
        )
        self.assertIsInstance(response.json(), list)

    def test_admin_05_measurement_feedback_summary(self):
        """GET /api/admin/measurement-feedback"""
        response = self.test_endpoint(
            "admin", "measurement_feedback_summary", "GET",
            "/api/admin/measurement-feedback",
            headers=self.admin_headers
        )
        self.assertIn("total", response.json())

    def test_admin_06_admin_analytics(self):
        """GET /api/admin/analytics"""
        response = self.test_endpoint(
            "admin", "admin_analytics", "GET",
            "/api/admin/analytics?days=7",
            headers=self.admin_headers
        )
        self.assertIn("summary", response.json())

    def test_admin_07_admin_analytics_daily(self):
        """GET /api/admin/analytics/daily"""
        response = self.test_endpoint(
            "admin", "admin_analytics_daily", "GET",
            "/api/admin/analytics/daily?days=30",
            headers=self.admin_headers
        )
        self.assertIsInstance(response.json(), list)

    def test_admin_08_admin_analytics_events(self):
        """GET /api/admin/analytics/events"""
        response = self.test_endpoint(
            "admin", "admin_analytics_events", "GET",
            "/api/admin/analytics/events?days=7",
            headers=self.admin_headers
        )
        self.assertIsInstance(response.json(), list)

    def test_admin_09_admin_feedback(self):
        """GET /api/admin/feedback"""
        response = self.test_endpoint(
            "admin", "admin_feedback", "GET",
            "/api/admin/feedback?limit=100",
            headers=self.admin_headers
        )
        self.assertIn("stats", response.json())

    def test_admin_10_admin_feedback_corrections(self):
        """GET /api/admin/feedback/corrections"""
        response = self.test_endpoint(
            "admin", "admin_feedback_corrections", "GET",
            "/api/admin/feedback/corrections?limit=100",
            headers=self.admin_headers
        )
        self.assertIsInstance(response.json(), list)

    def test_admin_11_admin_feedback_accuracy(self):
        """GET /api/admin/feedback/accuracy"""
        response = self.test_endpoint(
            "admin", "admin_feedback_accuracy", "GET",
            "/api/admin/feedback/accuracy",
            headers=self.admin_headers
        )
        self.assertIn("accuracy", response.json())

    def test_admin_12_admin_monitoring(self):
        """GET /api/admin/monitoring"""
        response = self.test_endpoint(
            "admin", "admin_monitoring", "GET",
            "/api/admin/monitoring",
            headers=self.admin_headers
        )
        self.assertIn("status", response.json())

    def test_admin_13_admin_monitoring_daily(self):
        """GET /api/admin/monitoring/daily-report"""
        response = self.test_endpoint(
            "admin", "admin_monitoring_daily", "GET",
            "/api/admin/monitoring/daily-report",
            headers=self.admin_headers
        )
        self.assertIn("date", response.json())

    def test_admin_14_admin_data_collection_stats(self):
        """GET /api/admin/data-collection/stats"""
        response = self.test_endpoint(
            "admin", "admin_data_collection_stats", "GET",
            "/api/admin/data-collection/stats",
            headers=self.admin_headers
        )
        self.assertIn("total_users", response.json())

    def test_admin_15_admin_data_collection_export(self):
        """POST /api/admin/data-collection/export"""
        response = self.test_endpoint(
            "admin", "admin_data_collection_export", "POST",
            "/api/admin/data-collection/export",
            json={
                "output_dir": "/tmp/export",
                "min_quality": 0.8,
                "max_samples": 100
            },
            headers=self.admin_headers
        )
        self.assertIn("exported", response.json())

    def test_admin_16_admin_data_collection_cleanup(self):
        """POST /api/admin/data-collection/cleanup"""
        response = self.test_endpoint(
            "admin", "admin_data_collection_cleanup", "POST",
            "/api/admin/data-collection/cleanup",
            json={"retention_days": 365},
            headers=self.admin_headers
        )
        self.assertIn("deleted", response.json())

    # ========================================================================
    # SUMMARY AND REPORTING
    # ========================================================================

    def test_zzz_print_summary(self):
        """Print comprehensive test summary (runs last due to zzz prefix)."""
        print("\n" + "="*80)
        print("ROUTER REFACTORING VALIDATION SUMMARY")
        print("="*80)

        total_tests = 0
        total_pass = 0
        total_fail = 0
        total_error = 0

        for router_name, tests in self.results.items():
            if not tests:
                continue

            print(f"\n{router_name.upper()} ROUTER ({len(tests)} endpoints):")
            print("-" * 80)

            for test_name, result in tests.items():
                status = result["status"]
                total_tests += 1

                if status == "PASS":
                    total_pass += 1
                    symbol = "✓"
                elif status == "FAIL":
                    total_fail += 1
                    symbol = "✗"
                else:
                    total_error += 1
                    symbol = "!"

                method = result.get("method", "")
                path = result.get("path", "")
                print(f"  {symbol} {test_name:40s} {method:6s} {path}")

                if status == "FAIL":
                    print(f"      Expected: {result['expected']}, Got: {result['actual']}")
                    if "error" in result:
                        error_msg = result["error"][:100]
                        print(f"      Error: {error_msg}...")
                elif status == "ERROR":
                    print(f"      Error: {result.get('error', 'Unknown error')}")

        print("\n" + "="*80)
        print(f"TOTAL: {total_tests} endpoints tested")
        print(f"  ✓ PASS:  {total_pass}")
        print(f"  ✗ FAIL:  {total_fail}")
        print(f"  ! ERROR: {total_error}")
        print("="*80 + "\n")

        # Assert all tests passed
        if total_fail > 0 or total_error > 0:
            self.fail(f"Some endpoints failed validation: {total_fail} failures, {total_error} errors")


if __name__ == "__main__":
    # Run tests with verbose output
    unittest.main(verbosity=2)
