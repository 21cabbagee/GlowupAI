#!/usr/bin/env python3
"""Test CacheMiddleware with actual FastAPI responses."""

import asyncio
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from fastapi import FastAPI, Response
from fastapi.testclient import TestClient

from glowupai.performance import CacheMiddleware, RedisCache


def create_test_app():
    """Create a test FastAPI app with cache middleware."""
    app = FastAPI()

    # Create cache
    cache = RedisCache(redis_url=None, default_ttl=300)

    # Add cache middleware
    app.add_middleware(CacheMiddleware, cache=cache, cacheable_paths=["/api/dashboard", "/api/users/"])

    @app.get("/api/users/{user_id}/dashboard")
    def dashboard(user_id: str):
        """Simulate slow dashboard endpoint."""
        # Simulate database query delay
        time.sleep(0.1)
        return {
            "user_id": user_id,
            "captures": [{"id": f"cap{i}"} for i in range(10)],
            "timestamp": time.time()
        }

    @app.get("/api/captures")
    def captures():
        """Non-cacheable endpoint."""
        return {"captures": [], "timestamp": time.time()}

    @app.post("/api/users/{user_id}/dashboard")
    def dashboard_post(user_id: str):
        """POST request - should not be cached."""
        return {"message": "posted", "timestamp": time.time()}

    return app


def test_cache_middleware():
    """Test the cache middleware with FastAPI."""
    print("=" * 60)
    print("Testing CacheMiddleware with FastAPI")
    print("=" * 60)

    app = create_test_app()
    client = TestClient(app)

    # Test 1: First request should be slow (cache miss)
    print("\n1. Testing cache MISS (first request):")
    start = time.time()
    response1 = client.get("/api/users/user123/dashboard")
    duration1 = time.time() - start

    print(f"   Status: {response1.status_code}")
    print(f"   Duration: {duration1*1000:.2f}ms")
    print(f"   Response size: {len(response1.text)} bytes")
    print(f"   X-Response-Time header: {response1.headers.get('X-Response-Time', 'N/A')}")

    data1 = response1.json()
    print(f"   User ID: {data1['user_id']}")
    print(f"   Captures count: {len(data1['captures'])}")
    timestamp1 = data1['timestamp']

    # Test 2: Second request should be fast (cache hit)
    print("\n2. Testing cache HIT (second request):")
    start = time.time()
    response2 = client.get("/api/users/user123/dashboard")
    duration2 = time.time() - start

    print(f"   Status: {response2.status_code}")
    print(f"   Duration: {duration2*1000:.2f}ms")
    print(f"   Response size: {len(response2.text)} bytes")

    data2 = response2.json()
    timestamp2 = data2['timestamp']

    # Check if cached (timestamp should be the same)
    is_cached = timestamp1 == timestamp2
    print(f"   Same timestamp: {is_cached}")
    print(f"   Timestamp 1: {timestamp1}")
    print(f"   Timestamp 2: {timestamp2}")

    # Test 3: Performance comparison
    print("\n3. Performance comparison:")
    speedup = duration1 / duration2 if duration2 > 0 else float('inf')
    print(f"   First request: {duration1*1000:.2f}ms")
    print(f"   Second request: {duration2*1000:.2f}ms")
    print(f"   Speedup: {speedup:.2f}x")

    if speedup < 2:
        print("   ⚠️  WARNING: Cache not providing expected speedup!")
    else:
        print("   ✓ Cache is working!")

    # Test 4: Different user should not share cache
    print("\n4. Testing cache isolation (different user):")
    response3 = client.get("/api/users/user456/dashboard")
    data3 = response3.json()
    timestamp3 = data3['timestamp']

    print(f"   User 456 timestamp: {timestamp3}")
    print(f"   Different from user 123: {timestamp3 != timestamp1}")

    # Test 5: Non-cacheable endpoint
    print("\n5. Testing non-cacheable endpoint:")
    response4a = client.get("/api/captures")
    data4a = response4a.json()
    time.sleep(0.01)
    response4b = client.get("/api/captures")
    data4b = response4b.json()

    timestamps_different = data4a['timestamp'] != data4b['timestamp']
    print(f"   Timestamps different: {timestamps_different}")
    print(f"   Timestamp A: {data4a['timestamp']}")
    print(f"   Timestamp B: {data4b['timestamp']}")

    if timestamps_different:
        print("   ✓ Non-cacheable endpoints not cached")
    else:
        print("   ⚠️  WARNING: Non-cacheable endpoint was cached!")

    # Test 6: POST request should not be cached
    print("\n6. Testing POST request (should not cache):")
    response5a = client.post("/api/users/user123/dashboard")
    data5a = response5a.json()
    time.sleep(0.01)
    response5b = client.post("/api/users/user123/dashboard")
    data5b = response5b.json()

    timestamps_different = data5a['timestamp'] != data5b['timestamp']
    print(f"   Timestamps different: {timestamps_different}")

    if timestamps_different:
        print("   ✓ POST requests not cached")
    else:
        print("   ⚠️  WARNING: POST request was cached!")

    print("\n" + "=" * 60)
    print("Cache middleware tests complete!")
    print("=" * 60)

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)

    issues = []
    if speedup < 2:
        issues.append("Cache not providing speedup")
    if not is_cached:
        issues.append("Responses not being cached")
    if not timestamps_different:
        issues.append("Non-cacheable endpoints being cached")

    if issues:
        print("⚠️  ISSUES FOUND:")
        for issue in issues:
            print(f"   - {issue}")
    else:
        print("✓ All tests passed - cache is working correctly!")

    return len(issues) == 0


if __name__ == "__main__":
    success = test_cache_middleware()
    sys.exit(0 if success else 1)
