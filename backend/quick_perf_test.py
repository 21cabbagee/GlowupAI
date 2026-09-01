"""
Quick performance test with real data.

This test:
1. Creates a test user with actual data
2. Measures endpoint response times
3. Compares with baseline expectations
"""

import time
import statistics
import requests
import jwt
import base64
import os
from pathlib import Path

# Configuration
BASE_URL = "http://localhost:8000"
JWT_SECRET = os.environ.get("JWT_SECRET", "test-secret")
TEST_USER_ID = f"perf_test_user_{int(time.time())}"


def generate_token(user_id: str) -> str:
    """Generate JWT token."""
    payload = {
        "sub": user_id,
        "email": f"{user_id}@test.com",
        "email_verified": True,
        "name": "Performance Test User",
        "iat": int(time.time()),
        "exp": int(time.time()) + 3600,
    }
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def get_headers(user_id: str) -> dict:
    """Get request headers."""
    return {
        "Authorization": f"Bearer {generate_token(user_id)}",
        "Content-Type": "application/json",
    }


def create_test_user() -> str:
    """Create a test user and capture."""
    print(f"Creating test user: {TEST_USER_ID}")

    # Create user via auth/session endpoint
    headers = get_headers(TEST_USER_ID)
    response = requests.post(
        f"{BASE_URL}/api/auth/session",
        headers=headers,
        json={},
        timeout=10
    )

    if response.status_code in [200, 201]:
        print(f"✓ User created: {response.status_code}")
        return response.json().get("user_id", TEST_USER_ID)
    else:
        print(f"✗ User creation failed: {response.status_code} - {response.text[:200]}")
        return TEST_USER_ID


def measure_endpoint(name: str, method: str, url: str, headers: dict, runs: int = 10) -> dict:
    """Measure endpoint performance."""
    print(f"\nTesting {name}...")
    times = []

    # Warmup
    for _ in range(3):
        try:
            requests.request(method, url, headers=headers, timeout=10)
        except Exception:
            pass

    # Measure
    for i in range(runs):
        start = time.perf_counter()
        try:
            response = requests.request(method, url, headers=headers, timeout=10)
            duration_ms = (time.perf_counter() - start) * 1000
            times.append(duration_ms)

            status = "✓" if response.status_code in [200, 404] else "✗"
            print(f"  Run {i+1}: {duration_ms:.2f}ms [{response.status_code}] {status}")
        except Exception as e:
            duration_ms = (time.perf_counter() - start) * 1000
            times.append(duration_ms)
            print(f"  Run {i+1}: {duration_ms:.2f}ms [ERROR] ✗")

    if not times:
        return {}

    return {
        "mean": statistics.mean(times),
        "median": statistics.median(times),
        "min": min(times),
        "max": max(times),
        "p95": sorted(times)[int(len(times) * 0.95)],
        "count": len(times),
    }


def print_results(results: dict):
    """Print performance results."""
    print("\n" + "=" * 80)
    print("PERFORMANCE TEST RESULTS")
    print("=" * 80)

    # Define baseline expectations
    baselines = {
        "dashboard": {"expected": 100, "baseline": 5},
        "history": {"expected": 100, "baseline": 8},
        "profile": {"expected": 100, "baseline": 5},
    }

    for endpoint, stats in results.items():
        if not stats:
            continue

        print(f"\n{endpoint.upper()}:")
        print(f"  Mean:     {stats['mean']:.2f}ms")
        print(f"  Median:   {stats['median']:.2f}ms")
        print(f"  P95:      {stats['p95']:.2f}ms")
        print(f"  Min/Max:  {stats['min']:.2f}ms / {stats['max']:.2f}ms")

        # Check against baseline
        if endpoint in baselines:
            expected = baselines[endpoint]["expected"]
            baseline = baselines[endpoint]["baseline"]

            mean_ms = stats["mean"]

            # Performance assessment
            if mean_ms < expected:
                status = "✓ EXCELLENT"
                symbol = "🟢"
            elif mean_ms < expected * 2:
                status = "⚠ ACCEPTABLE"
                symbol = "🟡"
            else:
                status = "✗ SLOW"
                symbol = "🔴"

            print(f"\n  Expected:  <{expected}ms")
            print(f"  Baseline:  {baseline}ms (from Agent #16)")
            print(f"  Status:    {status} {symbol}")

            # Calculate improvement/regression
            if baseline > 0:
                change_pct = ((mean_ms - baseline) / baseline) * 100
                if change_pct < 0:
                    print(f"  Change:    {abs(change_pct):.1f}% FASTER ✓")
                elif change_pct < 10:
                    print(f"  Change:    +{change_pct:.1f}% (acceptable)")
                else:
                    print(f"  Change:    +{change_pct:.1f}% SLOWER ✗")

    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)

    total_tests = len([r for r in results.values() if r])
    passing = sum(1 for endpoint, stats in results.items()
                  if stats and endpoint in baselines and
                  stats["mean"] < baselines[endpoint]["expected"])

    print(f"Tests Run: {total_tests}")
    print(f"Passing:   {passing}/{total_tests}")
    print(f"Status:    {'✓ ALL TESTS PASSED' if passing == total_tests else '✗ SOME TESTS FAILED'}")
    print("=" * 80)


def main():
    """Run performance tests."""
    print("=" * 80)
    print("QUICK PERFORMANCE TEST")
    print("=" * 80)
    print(f"Base URL: {BASE_URL}")
    print(f"Test User: {TEST_USER_ID}")

    # Check server
    try:
        response = requests.get(f"{BASE_URL}/", timeout=5)
        print(f"✓ Server is running")
    except Exception as e:
        print(f"✗ Cannot connect to server: {e}")
        print("\nStart the server with:")
        print("  cd /Users/21cabbage/GlowupAI/backend")
        print("  source venv/bin/activate")
        print("  ENABLE_RATE_LIMITING=false uvicorn glowupai.complete_api:app")
        return

    # Create test user
    user_id = create_test_user()
    headers = get_headers(user_id)

    # Run tests
    results = {}

    endpoints = [
        ("dashboard", "GET", f"/api/users/{user_id}/dashboard"),
        ("history", "GET", f"/api/users/{user_id}/history"),
        ("profile", "GET", f"/api/users/{user_id}/profile"),
    ]

    for name, method, path in endpoints:
        url = f"{BASE_URL}{path}"
        results[name] = measure_endpoint(name, method, url, headers, runs=20)

    # Print results
    print_results(results)

    # Save results
    import json
    results_file = Path(__file__).parent / "performance_results.json"
    with open(results_file, "w") as f:
        json.dump(results, f, indent=2)
    print(f"\n✓ Results saved to: {results_file}")


if __name__ == "__main__":
    main()
