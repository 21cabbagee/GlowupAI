#!/usr/bin/env python3
"""Test cache invalidation."""

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from fastapi import FastAPI
from fastapi.testclient import TestClient

from glowupai.performance import CacheMiddleware, RedisCache


call_count = 0


def create_app():
    """Create test app."""
    global call_count

    app = FastAPI()
    cache = RedisCache(redis_url=None, default_ttl=300)
    app.state.cache = cache

    app.add_middleware(CacheMiddleware, cache=cache, cacheable_paths=["/api/users/"])

    @app.get("/api/users/{user_id}/dashboard")
    def dashboard(user_id: str):
        global call_count
        call_count += 1
        time.sleep(0.05)
        return {
            "user_id": user_id,
            "data": f"result {call_count}",
            "call_number": call_count
        }

    @app.post("/api/users/{user_id}/captures")
    def create_capture(user_id: str):
        """Create capture and invalidate cache."""
        # Invalidate user's cache
        deleted = CacheMiddleware.invalidate_user_cache(cache, user_id)
        return {
            "capture_id": "new_123",
            "cache_invalidated": deleted
        }

    return app


def test_cache_invalidation():
    """Test cache invalidation."""
    global call_count
    call_count = 0

    print("=" * 60)
    print("Cache Invalidation Test")
    print("=" * 60)

    app = create_app()
    client = TestClient(app)

    # 1. Load dashboard (cache miss)
    print("\n1. Initial dashboard load:")
    r1 = client.get("/api/users/user123/dashboard")
    print(f"   X-Cache-Status: {r1.headers.get('X-Cache-Status')}")
    print(f"   Data: {r1.json()['data']}")
    print(f"   Total calls: {call_count}")

    # 2. Load again (cache hit)
    print("\n2. Load again (should be cached):")
    r2 = client.get("/api/users/user123/dashboard")
    print(f"   X-Cache-Status: {r2.headers.get('X-Cache-Status')}")
    print(f"   Data: {r2.json()['data']}")
    print(f"   Total calls: {call_count}")

    # 3. Create capture (invalidates cache)
    print("\n3. Create capture (invalidates cache):")
    r3 = client.post("/api/users/user123/captures")
    print(f"   Cache entries invalidated: {r3.json()['cache_invalidated']}")

    # 4. Load dashboard again (should be cache miss due to invalidation)
    print("\n4. Load dashboard after invalidation:")
    r4 = client.get("/api/users/user123/dashboard")
    print(f"   X-Cache-Status: {r4.headers.get('X-Cache-Status')}")
    print(f"   Data: {r4.json()['data']}")
    print(f"   Total calls: {call_count}")

    # 5. Load again (should be cached again)
    print("\n5. Load again (should be cached again):")
    r5 = client.get("/api/users/user123/dashboard")
    print(f"   X-Cache-Status: {r5.headers.get('X-Cache-Status')}")
    print(f"   Data: {r5.json()['data']}")
    print(f"   Total calls: {call_count}")

    # 6. Test that other user's cache is not affected
    print("\n6. Different user (independent cache):")
    r6 = client.get("/api/users/user456/dashboard")
    print(f"   X-Cache-Status: {r6.headers.get('X-Cache-Status')}")
    print(f"   Data: {r6.json()['data']}")
    print(f"   Total calls: {call_count}")

    # Verify
    print("\n" + "=" * 60)
    print("Verification")
    print("=" * 60)

    issues = []

    # Should have 3 calls: user123 (1st), user123 (after invalidation), user456
    expected_calls = 3
    if call_count != expected_calls:
        issues.append(f"Expected {expected_calls} calls, got {call_count}")
        print(f"✗ Expected {expected_calls} calls, got {call_count}")
    else:
        print(f"✓ Correct number of calls ({expected_calls})")

    # Check cache statuses
    statuses = [
        (r1, "MISS", "Initial load"),
        (r2, "HIT", "Second load"),
        (r4, "MISS", "After invalidation"),
        (r5, "HIT", "After re-cache"),
        (r6, "MISS", "Different user"),
    ]

    for response, expected_status, description in statuses:
        actual_status = response.headers.get('X-Cache-Status')
        if actual_status != expected_status:
            issues.append(f"{description}: expected {expected_status}, got {actual_status}")
            print(f"✗ {description}: expected {expected_status}, got {actual_status}")
        else:
            print(f"✓ {description}: {expected_status}")

    # Check that cache was actually invalidated
    if r3.json()['cache_invalidated'] < 1:
        issues.append("Cache invalidation didn't delete any entries")
        print("✗ Cache invalidation didn't delete any entries")
    else:
        print(f"✓ Cache invalidation deleted {r3.json()['cache_invalidated']} entries")

    # Summary
    print("\n" + "=" * 60)
    if issues:
        print("FAILED - Issues:")
        for issue in issues:
            print(f"  - {issue}")
        return False
    else:
        print("SUCCESS - Cache invalidation working correctly!")
        return True


if __name__ == "__main__":
    success = test_cache_invalidation()
    sys.exit(0 if success else 1)
