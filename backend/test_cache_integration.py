#!/usr/bin/env python3
"""Integration test for dashboard caching."""

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from fastapi import FastAPI, Header
from fastapi.testclient import TestClient

from glowupai.performance import CacheMiddleware, RedisCache


# Track call count to verify caching
call_count = 0


def create_app_with_cache():
    """Create app simulating real dashboard setup."""
    global call_count

    app = FastAPI()

    # Initialize cache
    cache = RedisCache(redis_url=None, default_ttl=300)
    app.state.cache = cache

    # Add cache middleware
    app.add_middleware(
        CacheMiddleware,
        cache=cache,
        cacheable_paths=["/api/users/", "/api/dashboard"]
    )

    @app.get("/api/users/{user_id}/dashboard")
    def dashboard(user_id: str, vertical: str = "skin", authorization: str = Header(default=None)):
        """Simulate slow dashboard query."""
        global call_count
        call_count += 1

        # Simulate database query
        time.sleep(0.1)  # 100ms query

        return {
            "user_id": user_id,
            "vertical": vertical,
            "captures": [
                {"id": f"cap{i}", "date": f"2024-{i:02d}-01"}
                for i in range(1, 11)
            ],
            "metrics": {
                "total_captures": 10,
                "baseline_count": 1,
                "latest_date": "2024-10-01"
            },
            "experiments": [],
            "call_number": call_count  # Track which call this was
        }

    @app.post("/api/captures")
    def create_capture(user_id: str):
        """Simulate capture creation - should invalidate cache."""
        # In production, this would invalidate the cache
        cache_key_pattern = f"cache:*{user_id}*dashboard*"
        deleted = cache.delete_pattern(cache_key_pattern)

        return {
            "capture_id": "new_capture_123",
            "cache_invalidated": deleted
        }

    return app


def test_dashboard_cache_integration():
    """Test dashboard caching with realistic scenario."""
    global call_count
    call_count = 0

    print("=" * 60)
    print("Dashboard Cache Integration Test")
    print("=" * 60)

    app = create_app_with_cache()
    client = TestClient(app)

    # Test 1: Initial dashboard load (cache miss)
    print("\n1. Initial dashboard load (should be slow):")
    start = time.time()
    response1 = client.get("/api/users/user123/dashboard?vertical=skin")
    duration1 = (time.time() - start) * 1000

    print(f"   Duration: {duration1:.2f}ms")
    print(f"   Status: {response1.status_code}")
    print(f"   X-Cache-Status: {response1.headers.get('X-Cache-Status')}")
    print(f"   Call number: {response1.json()['call_number']}")
    print(f"   Total function calls: {call_count}")

    # Test 2: Second dashboard load (cache hit)
    print("\n2. Second dashboard load (should be fast):")
    start = time.time()
    response2 = client.get("/api/users/user123/dashboard?vertical=skin")
    duration2 = (time.time() - start) * 1000

    print(f"   Duration: {duration2:.2f}ms")
    print(f"   Status: {response2.status_code}")
    print(f"   X-Cache-Status: {response2.headers.get('X-Cache-Status')}")
    print(f"   Call number: {response2.json()['call_number']}")
    print(f"   Total function calls: {call_count}")

    speedup = duration1 / duration2 if duration2 > 0 else 0
    print(f"   Speedup: {speedup:.1f}x")

    # Test 3: Third load (should still be cached)
    print("\n3. Third dashboard load (should still be fast):")
    start = time.time()
    response3 = client.get("/api/users/user123/dashboard?vertical=skin")
    duration3 = (time.time() - start) * 1000

    print(f"   Duration: {duration3:.2f}ms")
    print(f"   X-Cache-Status: {response3.headers.get('X-Cache-Status')}")
    print(f"   Total function calls: {call_count}")

    # Test 4: Different query params (should be cache miss)
    print("\n4. Different query params (should be new cache entry):")
    response4 = client.get("/api/users/user123/dashboard?vertical=hair")

    print(f"   X-Cache-Status: {response4.headers.get('X-Cache-Status')}")
    print(f"   Call number: {response4.json()['call_number']}")
    print(f"   Total function calls: {call_count}")

    # Test 5: Different user (should be cache miss)
    print("\n5. Different user (should be cache miss):")
    response5 = client.get("/api/users/user456/dashboard?vertical=skin")

    print(f"   X-Cache-Status: {response5.headers.get('X-Cache-Status')}")
    print(f"   Call number: {response5.json()['call_number']}")
    print(f"   Total function calls: {call_count}")

    # Test 6: Back to user123 (should be cache hit)
    print("\n6. Back to user123 with original params (should be cache hit):")
    response6 = client.get("/api/users/user123/dashboard?vertical=skin")

    print(f"   X-Cache-Status: {response6.headers.get('X-Cache-Status')}")
    print(f"   Call number: {response6.json()['call_number']}")
    print(f"   Total function calls: {call_count}")

    # Verify results
    print("\n" + "=" * 60)
    print("Verification")
    print("=" * 60)

    issues = []

    # Check that we only called the function 3 times (not 6)
    # Calls: 1=user123/skin, 2=user123/hair, 3=user456/skin
    # Cached: user123/skin (requests 2, 3, 6)
    expected_calls = 3
    if call_count != expected_calls:
        issues.append(f"Function called {call_count} times, expected {expected_calls}")
        print(f"✗ Function called {call_count} times, expected {expected_calls}")
    else:
        print(f"✓ Function called {expected_calls} times (correct)")

    # Check speedup
    if speedup < 2:
        issues.append(f"Speedup only {speedup:.1f}x, expected >2x")
        print(f"✗ Speedup only {speedup:.1f}x, expected >2x")
    else:
        print(f"✓ Speedup {speedup:.1f}x (good)")

    # Check cache headers
    if response1.headers.get('X-Cache-Status') != 'MISS':
        issues.append("First request should be MISS")
        print("✗ First request should be MISS")
    else:
        print("✓ First request was MISS")

    if response2.headers.get('X-Cache-Status') != 'HIT':
        issues.append("Second request should be HIT")
        print("✗ Second request should be HIT")
    else:
        print("✓ Second request was HIT")

    if response6.headers.get('X-Cache-Status') != 'HIT':
        issues.append("Sixth request should be HIT")
        print("✗ Sixth request should be HIT")
    else:
        print("✓ Sixth request was HIT")

    # Summary
    print("\n" + "=" * 60)
    if issues:
        print("FAILED - Issues found:")
        for issue in issues:
            print(f"  - {issue}")
        return False
    else:
        print("SUCCESS - All tests passed!")
        print(f"Cache providing {speedup:.1f}x speedup")
        return True


if __name__ == "__main__":
    success = test_dashboard_cache_integration()
    sys.exit(0 if success else 1)
