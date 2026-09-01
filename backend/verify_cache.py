#!/usr/bin/env python3
"""Verify cache is working in production.

Usage:
    python3 verify_cache.py [--url http://localhost:8000] [--admin-token TOKEN]
"""

import argparse
import sys
import time
from pathlib import Path

try:
    import requests
except ImportError:
    print("Error: requests library not installed")
    print("Install with: pip install requests")
    sys.exit(1)


def test_cache_headers(base_url: str):
    """Test cache by checking response headers."""
    print("\n" + "=" * 60)
    print("Testing Cache via Response Headers")
    print("=" * 60)

    # Use health endpoint as a test (it's not cached, but we can check headers exist)
    test_url = f"{base_url}/api/health"

    print(f"\nTesting endpoint: {test_url}")

    try:
        response = requests.get(test_url, timeout=10)
        print(f"Status: {response.status_code}")
        print("\nResponse headers:")

        # Check for timing header
        response_time = response.headers.get("X-Response-Time", "N/A")
        print(f"  X-Response-Time: {response_time}")

        # Check for cache status (won't be present for health endpoint)
        cache_status = response.headers.get("X-Cache-Status", "N/A")
        print(f"  X-Cache-Status: {cache_status}")

        print("\n✓ Server is responding")

        # Check if middleware is installed (timing header should be present)
        if response_time != "N/A":
            print("✓ RequestTimingMiddleware is working")
        else:
            print("⚠️  RequestTimingMiddleware may not be installed")

        return True

    except requests.exceptions.RequestException as exc:
        print(f"✗ Error connecting to server: {exc}")
        return False


def test_cache_diagnostics(base_url: str, admin_token: str | None):
    """Test cache via admin diagnostics endpoint."""
    print("\n" + "=" * 60)
    print("Testing Cache Diagnostics Endpoint")
    print("=" * 60)

    if not admin_token:
        print("⚠️  No admin token provided, skipping diagnostics")
        print("   Use --admin-token TOKEN to test admin endpoint")
        return False

    diagnostics_url = f"{base_url}/api/admin/cache/diagnostics"

    print(f"\nFetching: {diagnostics_url}")

    try:
        headers = {"Authorization": f"Bearer {admin_token}"}
        response = requests.get(diagnostics_url, headers=headers, timeout=10)

        if response.status_code == 403:
            print("✗ Access denied - invalid admin token")
            return False
        elif response.status_code == 404:
            print("⚠️  Diagnostics endpoint not found")
            print("   Make sure the cache diagnostics endpoint is deployed")
            return False
        elif response.status_code != 200:
            print(f"✗ Unexpected status code: {response.status_code}")
            return False

        data = response.json()

        # Display health
        print("\nCache Health:")
        health = data.get("health", {})
        print(f"  Backend: {health.get('backend')}")
        print(f"  Enabled: {health.get('enabled')}")
        print(f"  Default TTL: {health.get('default_ttl')}s")

        if health.get('backend') == 'redis':
            print(f"  Redis Connected: {health.get('redis_connected')}")
            if not health.get('redis_connected'):
                print(f"  Redis Error: {health.get('redis_error', 'Unknown')}")

        # Display operations test
        print("\nCache Operations Test:")
        ops = data.get("operations_test", {})
        print(f"  Set: {'✓' if ops.get('set') else '✗'}")
        print(f"  Get: {'✓' if ops.get('get') else '✗'}")
        print(f"  Delete: {'✓' if ops.get('delete') else '✗'}")
        print(f"  Overall: {'✓ PASS' if ops.get('success') else '✗ FAIL'}")

        if ops.get('errors'):
            print("  Errors:")
            for error in ops['errors']:
                print(f"    - {error}")

        # Display stats
        print("\nCache Statistics:")
        stats = data.get("stats", {})
        print(f"  Backend: {stats.get('backend')}")

        if stats.get('backend') == 'redis':
            redis_stats = stats.get('redis_stats', {})
            if redis_stats != 'unavailable':
                print(f"  Commands: {redis_stats.get('total_commands_processed', 'N/A')}")
                print(f"  Hit Rate: {stats.get('hit_rate', 'N/A')}")

        # Overall assessment
        print("\n" + "=" * 60)
        if ops.get('success'):
            print("✓ Cache is configured and working correctly!")
            if health.get('backend') == 'memory':
                print("⚠️  Using in-memory cache (Redis not configured)")
                print("   Consider configuring Redis for production")
            return True
        else:
            print("✗ Cache has issues!")
            return False

    except requests.exceptions.RequestException as exc:
        print(f"✗ Error: {exc}")
        return False


def test_dashboard_cache(base_url: str):
    """Test dashboard caching by making multiple requests."""
    print("\n" + "=" * 60)
    print("Testing Dashboard Cache (Live Test)")
    print("=" * 60)

    # Test with a non-existent user (should fail gracefully or return empty)
    test_user_id = "cache_test_user_999"
    dashboard_url = f"{base_url}/api/users/{test_user_id}/dashboard"

    print(f"\nTesting: {dashboard_url}")
    print("Note: This user likely doesn't exist, we're just testing cache headers\n")

    times = []
    statuses = []

    for i in range(3):
        try:
            start = time.time()
            response = requests.get(dashboard_url, timeout=10)
            duration = (time.time() - start) * 1000

            times.append(duration)
            cache_status = response.headers.get("X-Cache-Status", "N/A")
            statuses.append(cache_status)

            print(f"Request {i+1}:")
            print(f"  Duration: {duration:.2f}ms")
            print(f"  Status Code: {response.status_code}")
            print(f"  X-Cache-Status: {cache_status}")

        except requests.exceptions.RequestException as exc:
            print(f"Request {i+1}: Error - {exc}")
            return False

        time.sleep(0.1)  # Brief pause between requests

    # Analyze results
    print("\n" + "-" * 60)
    print("Analysis:")

    # Check if cache headers are present
    if all(s == "N/A" for s in statuses):
        print("⚠️  No X-Cache-Status headers found")
        print("   Cache middleware may not be enabled")
        return False

    # Check if we got a cache hit
    hits = sum(1 for s in statuses if s == "HIT")
    misses = sum(1 for s in statuses if s == "MISS")

    print(f"  Cache MISS: {misses}")
    print(f"  Cache HIT: {hits}")

    if hits > 0:
        print("\n✓ Cache is working!")

        # Check speedup
        if len(times) >= 2 and times[0] > 0:
            first_time = times[0]
            avg_cached = sum(times[1:]) / len(times[1:]) if len(times) > 1 else times[1]
            speedup = first_time / avg_cached if avg_cached > 0 else 1

            print(f"  First request: {first_time:.2f}ms")
            print(f"  Avg cached: {avg_cached:.2f}ms")
            print(f"  Speedup: {speedup:.1f}x")

            if speedup < 1.5:
                print("⚠️  Speedup is low - cache may not be providing expected benefit")

        return True
    else:
        print("\n⚠️  No cache hits detected")
        print("   Possible reasons:")
        print("   - Dashboard endpoint may not be in cacheable_paths")
        print("   - Cache may be disabled via GLOWUPAI_CACHE_ENABLED=0")
        print("   - Caching may require authentication")
        return False


def main():
    parser = argparse.ArgumentParser(description="Verify cache is working")
    parser.add_argument(
        "--url",
        default="http://localhost:8000",
        help="Base URL of the API (default: http://localhost:8000)"
    )
    parser.add_argument(
        "--admin-token",
        help="Admin token for diagnostics endpoint"
    )

    args = parser.parse_args()

    print("=" * 60)
    print("GlowupAI Cache Verification")
    print("=" * 60)
    print(f"Testing server at: {args.url}")

    # Test 1: Basic connectivity and headers
    test1 = test_cache_headers(args.url)

    # Test 2: Admin diagnostics (if token provided)
    test2 = test_cache_diagnostics(args.url, args.admin_token)

    # Test 3: Live dashboard cache test
    test3 = test_dashboard_cache(args.url)

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)

    passed = sum([test1, test2 if args.admin_token else True, test3])
    total = 3 if args.admin_token else 2

    if passed == total:
        print("✓ All tests passed!")
        print("\nCache is working correctly.")
        return 0
    else:
        print(f"⚠️  {passed}/{total} tests passed")
        print("\nSome issues detected. Check output above for details.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
