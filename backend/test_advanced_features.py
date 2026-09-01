"""
Test Advanced Features Endpoints
Tests the premium features: shelf-scan, reprocess, root-cause, derm-export, and budget-optimizer
"""

import base64
import io
import json
import time
from datetime import datetime
from typing import Any, Dict

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


def sample_shelf_image() -> str:
    """Generate a sample shelf/product image for testing."""
    image = Image.new("RGB", (640, 480), (240, 240, 240))
    draw = ImageDraw.Draw(image)

    # Draw some product-like rectangles
    draw.rectangle((50, 100, 150, 300), fill=(255, 200, 200), outline=(200, 100, 100), width=2)
    draw.rectangle((200, 100, 300, 300), fill=(200, 255, 200), outline=(100, 200, 100), width=2)
    draw.rectangle((350, 100, 450, 300), fill=(200, 200, 255), outline=(100, 100, 200), width=2)

    output = io.BytesIO()
    image.save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode()


class AdvancedFeaturesTest:
    """Test all advanced features endpoints."""

    def __init__(self):
        self.client = TestClient(create_complete_app())
        self.results = {
            "test_run": {
                "timestamp": datetime.now().isoformat(),
                "server": "http://localhost:8000"
            },
            "user_setup": {},
            "shelf_scan": {},
            "reprocess": {},
            "root_cause": {},
            "derm_export": {},
            "budget_optimizer": {},
            "summary": {}
        }
        self.user_id = None
        self.premium_user_id = None

    def log_test(self, category: str, name: str, response, success: bool, notes: str = ""):
        """Log test result."""
        result = {
            "name": name,
            "success": success,
            "status_code": response.status_code if response else None,
            "notes": notes
        }

        if response:
            try:
                result["response"] = response.json()
            except Exception:
                result["response_text"] = response.text[:200]

        if category not in self.results:
            self.results[category] = {}

        self.results[category][name] = result

        status = "✓" if success else "✗"
        print(f"  {status} {name}: {notes}")

        return result

    def run_tests(self):
        """Run all advanced features tests."""

        print("=" * 80)
        print("ADVANCED FEATURES ENDPOINT TESTING")
        print("=" * 80)
        print()

        # Step 1: Create user with captures
        print("Step 1: Creating test user with captures...")
        self.setup_user_with_captures()
        print()

        # Step 2: Test shelf scan
        print("Step 2: Testing shelf scan endpoints...")
        self.test_shelf_scan()
        print()

        # Step 3: Test reprocess
        print("Step 3: Testing reprocess endpoint...")
        self.test_reprocess()
        print()

        # Step 4: Test root cause analysis
        print("Step 4: Testing root cause analysis...")
        self.test_root_cause()
        print()

        # Step 5: Test derm export
        print("Step 5: Testing dermatologist export...")
        self.test_derm_export()
        print()

        # Step 6: Test budget optimizer
        print("Step 6: Testing budget optimizer...")
        self.test_budget_optimizer()
        print()

        self.print_summary()

        return self.results

    def setup_user_with_captures(self):
        """Create a premium user and add some captures."""

        # Create basic user
        response = self.client.post("/api/users", json={"skin_type": "combination"})
        success = response.status_code == 200

        if not success:
            self.log_test("user_setup", "create_user", response, False, "Failed to create user")
            return

        self.user_id = response.json()["user"]["id"]
        self.log_test("user_setup", "create_user", response, True, f"Created user {self.user_id}")

        # Upgrade to premium
        response = self.client.post(
            f"/api/users/{self.user_id}/subscription/upgrade",
            json={"source": "test"}
        )
        success = response.status_code == 200
        self.log_test("user_setup", "upgrade_to_premium", response, success,
                     "Upgraded to premium" if success else "Failed to upgrade")

        if not success:
            return

        self.premium_user_id = self.user_id

        # Create multiple captures for testing
        for i in range(3):
            response = self.client.post(
                "/api/captures",
                json={
                    "user_id": self.user_id,
                    "image_base64": sample_image(),
                    "vertical": "skin",
                    "is_baseline": i == 0
                }
            )
            success = response.status_code == 200
            self.log_test("user_setup", f"create_capture_{i+1}", response, success,
                         f"Capture {i+1} created" if success else f"Failed to create capture {i+1}")

            if success:
                capture_data = response.json()
                self.log_test("user_setup", f"capture_{i+1}_metrics", response, True,
                             f"Metrics: {list(capture_data.get('metrics', {}).keys())}")

    def test_shelf_scan(self):
        """Test shelf scan workflow: upload -> check status -> confirm."""

        if not self.premium_user_id:
            print("  ✗ Skipping shelf scan tests - no premium user")
            return

        # POST /api/users/{user_id}/shelf-scan
        print("  Testing shelf scan upload...")
        response = self.client.post(
            f"/api/users/{self.premium_user_id}/shelf-scan",
            json={"image_base64": sample_shelf_image()}
        )
        success = response.status_code == 200

        if not success:
            self.log_test("shelf_scan", "upload", response, False, "Failed to start shelf scan")
            return

        data = response.json()
        job_id = data.get("job_id")
        self.log_test("shelf_scan", "upload", response, True,
                     f"Started job {job_id}, status: {data.get('status')}")

        if not job_id:
            print("  ✗ No job_id returned")
            return

        # GET /api/users/{user_id}/shelf-scan/{job_id}
        print("  Checking shelf scan status...")
        max_checks = 5
        for check in range(max_checks):
            response = self.client.get(
                f"/api/users/{self.premium_user_id}/shelf-scan/{job_id}"
            )
            success = response.status_code == 200

            if not success:
                self.log_test("shelf_scan", f"status_check_{check+1}", response, False,
                             "Failed to get status")
                break

            data = response.json()
            status = data.get("status")
            self.log_test("shelf_scan", f"status_check_{check+1}", response, True,
                         f"Status: {status}, Products: {len(data.get('products', []))}")

            if status == "completed":
                break
            elif status == "failed":
                print(f"  ! Job failed: {data.get('error')}")
                break

            if check < max_checks - 1:
                time.sleep(0.5)

        # POST /api/users/{user_id}/shelf-scan/{job_id}/confirm
        print("  Confirming shelf scan selections...")
        response = self.client.post(
            f"/api/users/{self.premium_user_id}/shelf-scan/{job_id}/confirm",
            json={"selections": []}
        )
        success = response.status_code == 200
        self.log_test("shelf_scan", "confirm", response, success,
                     "Confirmed selections" if success else "Failed to confirm")

        if success:
            data = response.json()
            self.log_test("shelf_scan", "confirm_result", response, True,
                         f"Added {data.get('added', 0)} products")

    def test_reprocess(self):
        """Test reprocess workflow: start -> check status."""

        if not self.premium_user_id:
            print("  ✗ Skipping reprocess tests - no premium user")
            return

        # POST /api/users/{user_id}/reprocess
        print("  Starting reprocess...")
        response = self.client.post(
            f"/api/users/{self.premium_user_id}/reprocess",
            json={"model_version": "deterministic-v2"}
        )
        success = response.status_code == 200

        if not success:
            self.log_test("reprocess", "start", response, False, "Failed to start reprocess")
            return

        data = response.json()
        job_id = data.get("job_id")
        self.log_test("reprocess", "start", response, True,
                     f"Started job {job_id}, status: {data.get('status')}")

        if not job_id:
            print("  ✗ No job_id returned")
            return

        # GET /api/users/{user_id}/reprocess/{job_id}
        print("  Checking reprocess status...")
        max_checks = 5
        for check in range(max_checks):
            response = self.client.get(
                f"/api/users/{self.premium_user_id}/reprocess/{job_id}"
            )
            success = response.status_code == 200

            if not success:
                self.log_test("reprocess", f"status_check_{check+1}", response, False,
                             "Failed to get status")
                break

            data = response.json()
            status = data.get("status")
            progress = data.get("progress", {})
            self.log_test("reprocess", f"status_check_{check+1}", response, True,
                         f"Status: {status}, Progress: {progress.get('processed', 0)}/{progress.get('total', 0)}")

            if status == "completed":
                break
            elif status == "failed":
                print(f"  ! Job failed: {data.get('error')}")
                break

            if check < max_checks - 1:
                time.sleep(0.5)

    def test_root_cause(self):
        """Test root cause analysis."""

        if not self.premium_user_id:
            print("  ✗ Skipping root cause test - no premium user")
            return

        # GET /api/users/{user_id}/root-cause
        metrics = ["texture_score", "redness_index", "hydration_score"]

        for metric in metrics:
            response = self.client.get(
                f"/api/users/{self.premium_user_id}/root-cause",
                params={"metric": metric}
            )
            success = response.status_code == 200

            if success:
                data = response.json()
                correlations = data.get("correlations", [])
                top_factor = correlations[0] if correlations else None
                self.log_test("root_cause", f"analysis_{metric}", response, True,
                             f"Found {len(correlations)} correlations" +
                             (f", top: {top_factor.get('factor')}" if top_factor else ""))
            else:
                self.log_test("root_cause", f"analysis_{metric}", response, False,
                             f"Failed for metric {metric}")

    def test_derm_export(self):
        """Test dermatologist export."""

        if not self.premium_user_id:
            print("  ✗ Skipping derm export test - no premium user")
            return

        # GET /api/users/{user_id}/derm-export
        response = self.client.get(f"/api/users/{self.premium_user_id}/derm-export")
        success = response.status_code == 200

        if success:
            data = response.json()
            self.log_test("derm_export", "generate_report", response, True,
                         f"Report generated with {len(data.get('captures', []))} captures")

            # Log report sections
            sections = data.keys()
            self.log_test("derm_export", "report_sections", response, True,
                         f"Sections: {', '.join(sections)}")
        else:
            self.log_test("derm_export", "generate_report", response, False,
                         "Failed to generate report")

    def test_budget_optimizer(self):
        """Test budget optimizer."""

        if not self.premium_user_id:
            print("  ✗ Skipping budget optimizer test - no premium user")
            return

        # First add some products to routine
        print("  Adding products to routine for testing...")
        product_response = self.client.post(
            "/api/products",
            json={"name": "Test Cleanser", "category": "cleanser"}
        )

        if product_response.status_code == 200:
            product_id = product_response.json().get("id")
            self.client.post(
                "/api/routine-events",
                json={
                    "user_id": self.premium_user_id,
                    "product_id": product_id,
                    "action": "start"
                }
            )
            print(f"  ✓ Added product {product_id} to routine")

        # GET /api/users/{user_id}/budget-optimizer
        response = self.client.get(f"/api/users/{self.premium_user_id}/budget-optimizer")
        success = response.status_code == 200

        if success:
            data = response.json()
            recommendations = data.get("recommendations", [])
            savings = data.get("potential_savings_cents", 0)
            self.log_test("budget_optimizer", "get_recommendations", response, True,
                         f"Found {len(recommendations)} recommendations, potential savings: ${savings/100:.2f}")

            # Log recommendation types
            if recommendations:
                rec_types = [r.get("type") for r in recommendations]
                self.log_test("budget_optimizer", "recommendation_types", response, True,
                             f"Types: {', '.join(rec_types)}")
        else:
            self.log_test("budget_optimizer", "get_recommendations", response, False,
                         "Failed to get recommendations")

    def print_summary(self):
        """Print test summary."""

        print("=" * 80)
        print("TEST SUMMARY")
        print("=" * 80)
        print()

        categories = [
            "user_setup",
            "shelf_scan",
            "reprocess",
            "root_cause",
            "derm_export",
            "budget_optimizer"
        ]

        total_tests = 0
        total_pass = 0
        total_fail = 0

        for category in categories:
            tests = self.results.get(category, {})
            if not tests:
                continue

            pass_count = sum(1 for t in tests.values() if isinstance(t, dict) and t.get("success"))
            fail_count = len(tests) - pass_count

            total_tests += len(tests)
            total_pass += pass_count
            total_fail += fail_count

            print(f"{category.replace('_', ' ').title()}: {len(tests)} tests")
            print(f"  ✓ Pass:  {pass_count}")
            if fail_count > 0:
                print(f"  ✗ Fail:  {fail_count}")
            print()

        self.results["summary"] = {
            "total_tests": total_tests,
            "passed": total_pass,
            "failed": total_fail,
            "success_rate": f"{(total_pass / total_tests * 100):.1f}%" if total_tests > 0 else "0%"
        }

        print("=" * 80)
        print(f"TOTAL: {total_tests} tests")
        print(f"  ✓ PASS:  {total_pass}")
        if total_fail > 0:
            print(f"  ✗ FAIL:  {total_fail}")
        print(f"Success Rate: {self.results['summary']['success_rate']}")
        print("=" * 80)


def main():
    """Run tests and save results."""

    tester = AdvancedFeaturesTest()
    results = tester.run_tests()

    # Save results to file
    output_file = "/Users/21cabbage/.claude/jobs/66b0e7b8/tmp/test_advanced.json"

    try:
        with open(output_file, "w") as f:
            json.dump(results, f, indent=2)
        print(f"\n✓ Results saved to {output_file}")
    except Exception as e:
        print(f"\n✗ Failed to save results: {e}")
        print(f"\nResults:\n{json.dumps(results, indent=2)}")


if __name__ == "__main__":
    main()
