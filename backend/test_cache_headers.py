#!/usr/bin/env python3
"""Test cache headers and logging."""

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from fastapi import FastAPI
from fastapi.testclient import TestClient

from glowupai.performance import CacheMiddleware, RedisCache


def create_test_app():
    """Create a test FastAPI app with cache middleware."""
    app = FastAPI()

    # Create cache
    cache = RedisCache(redis_url=None, default_ttl=300)

    # Add cache middleware
    app.add_middleware(CacheMiddleware, cache=cache, cacheable_paths=["/api/users/"])

    @app.get("/api/users/{user_id}/dashboard")
    def dashboard(user_id: str):
        """Simulate dashboard endpoint."""
        time.sleep(0.05)  # 50ms
        return {"user_id": user_id, "data": "test"}

    return app


def test_cache_headers():
    """Test cache headers."""
    print("=" * 60)
    print("Testing Cache Headers")
    print("=" * 60)

    app = create_test_app()
    client = TestClient(app)

    # First request - should be MISS
    print("\n1. First request (Cache MISS):")
    response1 = client.get("/api/users/user123/dashboard")

    print(f"   Status: {response1.status_code}")
    print(f"   X-Cache-Status: {response1.headers.get('X-Cache-Status', 'N/A')}")
    print(f"   Cache-Control: {response1.headers.get('Cache-Control', 'N/A')}")

    # Second request - should be HIT
    print("\n2. Second request (Cache HIT):")
    response2 = client.get("/api/users/user123/dashboard")

    print(f"   Status: {response2.status_code}")
    print(f"   X-Cache-Status: {response2.headers.get('X-Cache-Status', 'N/A')}")
    print(f"   Cache-Control: {response2.headers.get('Cache-Control', 'N/A')}")

    # Verify
    print("\n3. Verification:")
    miss_header = response1.headers.get('X-Cache-Status')
    hit_header = response2.headers.get('X-Cache-Status')

    print(f"   First response has MISS header: {miss_header == 'MISS'}")
    print(f"   Second response has HIT header: {hit_header == 'HIT'}")
    print(f"   Cache-Control header present: {'Cache-Control' in response2.headers}")

    if miss_header == 'MISS' and hit_header == 'HIT':
        print("\n✓ Cache headers working correctly!")
        return True
    else:
        print("\n✗ Cache headers not working correctly!")
        return False


if __name__ == "__main__":
    success = test_cache_headers()
    sys.exit(0 if success else 1)
