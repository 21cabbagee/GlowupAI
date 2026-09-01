#!/usr/bin/env python3
"""Test script to verify dashboard cache performance."""

import json
import time
from pathlib import Path

# Add parent to path for imports
import sys
sys.path.insert(0, str(Path(__file__).parent))

from glowupai.performance import RedisCache, CacheMiddleware

def test_redis_cache():
    """Test Redis cache basic operations."""
    print("=" * 60)
    print("Testing RedisCache")
    print("=" * 60)

    # Test with no Redis (memory cache fallback)
    cache = RedisCache(redis_url=None, default_ttl=5)

    # Test set/get
    print("\n1. Testing set/get operations...")
    test_data = {"user": "test123", "data": [1, 2, 3]}
    result = cache.set("test:key", test_data, ttl=10)
    print(f"   Set result: {result}")

    retrieved = cache.get("test:key")
    print(f"   Get result: {retrieved}")
    print(f"   Data matches: {retrieved == test_data}")

    # Test cache miss
    print("\n2. Testing cache miss...")
    missing = cache.get("nonexistent:key")
    print(f"   Missing key result: {missing}")

    # Test expiration
    print("\n3. Testing expiration...")
    cache.set("expire:test", {"temp": "data"}, ttl=1)
    print(f"   Immediate get: {cache.get('expire:test')}")
    time.sleep(2)
    print(f"   After expiration: {cache.get('expire:test')}")

    # Test delete
    print("\n4. Testing delete...")
    cache.set("delete:test", {"data": "value"})
    print(f"   Before delete: {cache.get('delete:test')}")
    cache.delete("delete:test")
    print(f"   After delete: {cache.get('delete:test')}")

    # Test pattern delete
    print("\n5. Testing pattern delete...")
    cache.set("pattern:test:1", {"id": 1})
    cache.set("pattern:test:2", {"id": 2})
    cache.set("pattern:other:3", {"id": 3})
    print(f"   Before pattern delete:")
    print(f"     pattern:test:1 = {cache.get('pattern:test:1')}")
    print(f"     pattern:test:2 = {cache.get('pattern:test:2')}")
    print(f"     pattern:other:3 = {cache.get('pattern:other:3')}")

    deleted = cache.delete_pattern("pattern:test:*")
    print(f"   Deleted {deleted} keys")
    print(f"   After pattern delete:")
    print(f"     pattern:test:1 = {cache.get('pattern:test:1')}")
    print(f"     pattern:test:2 = {cache.get('pattern:test:2')}")
    print(f"     pattern:other:3 = {cache.get('pattern:other:3')}")

    print("\n" + "=" * 60)
    print("RedisCache tests complete!")
    print("=" * 60)


def test_cache_key_generation():
    """Test cache key generation."""
    print("\n" + "=" * 60)
    print("Testing Cache Key Generation")
    print("=" * 60)

    # Test key generation
    user_id = "user123"
    path = "/api/users/user123/dashboard"
    query_params = "vertical=skin"

    key = CacheMiddleware.generate_cache_key_for_user(user_id, path, query_params)
    print(f"\nGenerated cache key:")
    print(f"  User ID: {user_id}")
    print(f"  Path: {path}")
    print(f"  Query: {query_params}")
    print(f"  Key: {key}")

    # Test that same inputs produce same key
    key2 = CacheMiddleware.generate_cache_key_for_user(user_id, path, query_params)
    print(f"\nConsistency check: {key == key2}")

    # Test that different inputs produce different keys
    key3 = CacheMiddleware.generate_cache_key_for_user(user_id, path, "vertical=hair")
    print(f"Different query produces different key: {key != key3}")

    print("\n" + "=" * 60)
    print("Cache key generation tests complete!")
    print("=" * 60)


def test_cacheable_path_matching():
    """Test the cacheable path matching logic."""
    print("\n" + "=" * 60)
    print("Testing Cacheable Path Matching")
    print("=" * 60)

    cache = RedisCache(redis_url=None)
    middleware = CacheMiddleware(None, cache=cache)

    test_cases = [
        ("/api/users/user123/dashboard", "GET", True, "Dashboard endpoint"),
        ("/api/users/", "GET", True, "Users list"),
        ("/api/dashboard", "GET", True, "Dashboard root"),
        ("/api/captures", "GET", False, "Captures not cacheable"),
        ("/api/users/user123/dashboard", "POST", False, "POST not cacheable"),
        ("/health", "GET", False, "Health check not cacheable"),
    ]

    print("\nPath matching results:")
    for path, method, expected, description in test_cases:
        result = middleware._is_cacheable(path, method)
        status = "✓" if result == expected else "✗"
        print(f"  {status} {description}")
        print(f"     Path: {path}, Method: {method}")
        print(f"     Expected: {expected}, Got: {result}")

    print("\n" + "=" * 60)
    print("Path matching tests complete!")
    print("=" * 60)


def simulate_cache_performance():
    """Simulate cache performance impact."""
    print("\n" + "=" * 60)
    print("Simulating Cache Performance Impact")
    print("=" * 60)

    cache = RedisCache(redis_url=None, default_ttl=300)

    # Simulate a slow dashboard query
    def slow_dashboard_query():
        """Simulate database query."""
        time.sleep(0.1)  # Simulate 100ms query
        return {
            "user_id": "user123",
            "captures": [{"id": f"cap{i}", "date": f"2024-{i:02d}-01"} for i in range(1, 13)],
            "metrics": {"total": 12, "baseline": 1},
        }

    # Test without cache
    print("\n1. Without cache (3 requests):")
    times_no_cache = []
    for i in range(3):
        start = time.time()
        result = slow_dashboard_query()
        duration = time.time() - start
        times_no_cache.append(duration)
        print(f"   Request {i+1}: {duration*1000:.2f}ms")

    avg_no_cache = sum(times_no_cache) / len(times_no_cache)
    print(f"   Average: {avg_no_cache*1000:.2f}ms")

    # Test with cache
    print("\n2. With cache (3 requests):")
    cache_key = "cache:dashboard:user123"
    times_with_cache = []

    for i in range(3):
        start = time.time()

        # Try to get from cache
        cached = cache.get(cache_key)
        if cached:
            result = cached
        else:
            result = slow_dashboard_query()
            cache.set(cache_key, result)

        duration = time.time() - start
        times_with_cache.append(duration)
        hit_status = "HIT" if cached else "MISS"
        print(f"   Request {i+1}: {duration*1000:.2f}ms [{hit_status}]")

    avg_with_cache = sum(times_with_cache) / len(times_with_cache)
    print(f"   Average: {avg_with_cache*1000:.2f}ms")

    # Calculate improvement
    speedup = avg_no_cache / avg_with_cache
    print(f"\n3. Performance improvement:")
    print(f"   Speedup: {speedup:.2f}x faster")
    print(f"   Time saved: {(avg_no_cache - avg_with_cache)*1000:.2f}ms per request")

    print("\n" + "=" * 60)
    print("Performance simulation complete!")
    print("=" * 60)


if __name__ == "__main__":
    test_redis_cache()
    test_cache_key_generation()
    test_cacheable_path_matching()
    simulate_cache_performance()

    print("\n" + "=" * 60)
    print("ALL TESTS COMPLETE")
    print("=" * 60)
