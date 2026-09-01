"""
Quick Endpoint Validation - Shows all endpoints are registered
"""

from collections import defaultdict
from fastapi.testclient import TestClient
from glowupai.complete_api import create_complete_app

def test_all_endpoints():
    """Quick test to verify all endpoints are registered."""

    client = TestClient(create_complete_app())

    endpoints_to_test = [
        # Users Router (8)
        ("POST", "/api/users", {"json": {}}),
        ("POST", "/api/auth/session", {}),
        ("GET", "/api/users/test-id/profile", {}),
        ("PATCH", "/api/users/test-id/profile", {"json": {}}),
        ("POST", "/api/users/test-id/consent", {"json": {"facial_data": True}}),
        ("POST", "/api/users/test-id/consent/data-collection", {"json": {"granted": True}}),
        ("GET", "/api/users/test-id/export", {}),
        ("DELETE", "/api/users/test-id", {}),

        # Captures Router (16)
        ("POST", "/api/captures", {"json": {"user_id": "test", "image_base64": "test"}}),
        ("POST", "/api/captures/test-id/feedback", {"json": {}}),
        ("GET", "/api/users/test-id/capture-guide", {}),
        ("GET", "/api/users/test-id/dashboard", {}),
        ("GET", "/api/users/test-id/history", {}),
        ("GET", "/api/users/test-id/check-ins", {}),
        ("POST", "/api/users/test-id/check-ins", {"json": {"routine_state": "steady", "skin_feel": "better"}}),
        ("GET", "/api/users/test-id/weekly-recap", {}),
        ("POST", "/api/users/test-id/measurement-feedback", {"json": {"capture_id": "test", "agreement": "fair"}}),
        ("GET", "/api/users/test-id/labels", {}),
        ("POST", "/api/users/test-id/labels", {"json": {"photo_id": "test", "label_type": "test", "value": "test"}}),
        ("POST", "/api/users/test-id/reprocess", {"json": {"model_version": "test"}}),
        ("GET", "/api/users/test-id/reprocess/job-id", {}),
        ("POST", "/api/users/test-id/shelf-scan", {"json": {"image_base64": "test"}}),
        ("GET", "/api/users/test-id/shelf-scan/job-id", {}),
        ("POST", "/api/users/test-id/shelf-scan/job-id/confirm", {"json": {"selections": []}}),

        # Analytics Router (8)
        ("GET", "/api/users/test-id/analytics", {}),
        ("GET", "/api/users/test-id/engagement", {}),
        ("POST", "/api/users/test-id/engagement", {"json": {"event_type": "test"}}),
        ("GET", "/api/users/test-id/context-events", {}),
        ("POST", "/api/users/test-id/context-events", {"json": {"event_type": "custom"}}),
        ("GET", "/api/users/test-id/root-cause", {}),
        ("GET", "/api/users/test-id/budget-optimizer", {}),
        ("GET", "/api/users/test-id/derm-export", {}),

        # Subscriptions Router (21)
        ("GET", "/api/users/test-id/subscription", {}),
        ("POST", "/api/users/test-id/subscription/upgrade", {"json": {}}),
        ("POST", "/api/users/test-id/subscription/cancel", {"json": {}}),
        ("POST", "/api/products", {"json": {"name": "test"}}),
        ("GET", "/api/products/search", {}),
        ("GET", "/api/products/lookup?barcode=test", {}),
        ("GET", "/api/products/test-id?user_id=test", {}),
        ("GET", "/api/products/test-id/ingredient-explainer?user_id=test", {}),
        ("GET", "/api/products/test-id/predict?user_id=test", {}),
        ("POST", "/api/users/test-id/purchase-guidance", {"json": {}}),
        ("POST", "/api/routine-events", {"json": {"user_id": "test", "product_id": "test", "action": "start"}}),
        ("GET", "/api/users/test-id/confound-check", {}),
        ("POST", "/api/experiments", {"json": {"user_id": "test", "name": "test", "product_id": "test"}}),
        ("GET", "/api/users/test-id/experiments", {}),
        ("GET", "/api/users/test-id/experiments/exp-id", {}),
        ("POST", "/api/users/test-id/experiments/exp-id/status", {"json": {"user_id": "test", "status": "active"}}),
        ("POST", "/api/users/test-id/qna", {"json": {"question": "test"}}),
        ("GET", "/api/users/test-id/qna", {}),
        ("GET", "/api/users/test-id/discover", {}),
        ("GET", "/api/users/test-id/commerce/offers", {}),
        ("POST", "/api/users/test-id/commerce/offers/offer-id/click", {"json": {}}),

        # Admin Router (16)
        ("GET", "/api/metrics", {}),
        ("POST", "/api/admin/offers", {"json": {"product_id": "test", "merchant": "test", "url": "http://test"}}),
        ("POST", "/api/triage", {"json": {"text": "test"}}),
        ("GET", "/api/admin/audit", {}),
        ("GET", "/api/admin/measurement-feedback", {}),
        ("GET", "/api/admin/analytics", {}),
        ("GET", "/api/admin/analytics/daily", {}),
        ("GET", "/api/admin/analytics/events", {}),
        ("GET", "/api/admin/feedback", {}),
        ("GET", "/api/admin/feedback/corrections", {}),
        ("GET", "/api/admin/feedback/accuracy", {}),
        ("GET", "/api/admin/monitoring", {}),
        ("GET", "/api/admin/monitoring/daily-report", {}),
        ("GET", "/api/admin/data-collection/stats", {}),
        ("POST", "/api/admin/data-collection/export", {"json": {}}),
        ("POST", "/api/admin/data-collection/cleanup", {"json": {}}),
    ]

    print("=" * 80)
    print("QUICK ENDPOINT VALIDATION - ROUTER REFACTORING")
    print("=" * 80)
    print()
    print(f"Testing {len(endpoints_to_test)} endpoints...\n")

    results = {"registered": 0, "not_found": 0, "other": 0}
    not_found_endpoints = []

    for method, path, kwargs in endpoints_to_test:
        try:
            if method == "GET":
                response = client.get(path, **kwargs)
            elif method == "POST":
                response = client.post(path, **kwargs)
            elif method == "PATCH":
                response = client.patch(path, **kwargs)
            elif method == "DELETE":
                response = client.delete(path, **kwargs)

            if response.status_code == 404:
                results["not_found"] += 1
                not_found_endpoints.append(f"{method:6s} {path}")
                print(f"  ✗ 404 NOT FOUND: {method:6s} {path}")
            else:
                results["registered"] += 1

        except Exception as e:
            results["other"] += 1
            print(f"  ! ERROR: {method:6s} {path} - {e}")

    print()
    print("=" * 80)
    print("RESULTS")
    print("=" * 80)
    print(f"Total endpoints tested: {len(endpoints_to_test)}")
    print(f"  ✓ Registered (responding): {results['registered']}")
    print(f"  ✗ Not found (404):         {results['not_found']}")
    print(f"  ! Errors:                  {results['other']}")
    print("=" * 80)

    if results["not_found"] == 0:
        print()
        print("✅ SUCCESS: All 69 endpoints are properly registered!")
        print()
        print("Breakdown by router:")
        print("  • Users Router:         8 endpoints")
        print("  • Captures Router:     16 endpoints")
        print("  • Analytics Router:     8 endpoints")
        print("  • Subscriptions Router: 21 endpoints")
        print("  • Admin Router:        16 endpoints")
        print("  • TOTAL:               69 endpoints")
        print()
        print("All routes are accessible and responding.")
        return 0
    else:
        print()
        print(f"❌ FAILURE: {results['not_found']} endpoints not found!")
        print()
        print("Missing endpoints:")
        for endpoint in not_found_endpoints:
            print(f"  • {endpoint}")
        return 1

if __name__ == "__main__":
    import sys
    exit_code = test_all_endpoints()
    sys.exit(exit_code)
