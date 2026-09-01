"""
Performance test suite for GlowupAI API.

Tests:
1. Response times on key endpoints (dashboard, captures, history)
2. Load test with 50 concurrent users
3. Memory usage monitoring
4. Database query performance

Run with: python performance_test.py
"""

import os
import sys
import time
import json
import asyncio
import statistics
import tracemalloc
from typing import Any
from pathlib import Path
from datetime import datetime

import requests
import psutil

# Add backend to path
backend_dir = Path(__file__).parent
sys.path.insert(0, str(backend_dir))

# Environment setup
os.environ.setdefault("DATABASE_URL", "sqlite:///test_performance.db")
os.environ.setdefault("FIREBASE_PROJECT_ID", "test-project")
os.environ.setdefault("JWT_SECRET", "test-secret-key-for-performance-testing")


class PerformanceTestResult:
    """Store performance test results."""

    def __init__(self):
        self.endpoint_times = {}
        self.load_test_results = {}
        self.memory_stats = {}
        self.database_stats = {}

    def add_endpoint_time(self, endpoint: str, duration_ms: float):
        """Add endpoint response time."""
        if endpoint not in self.endpoint_times:
            self.endpoint_times[endpoint] = []
        self.endpoint_times[endpoint].append(duration_ms)

    def get_endpoint_stats(self, endpoint: str) -> dict[str, float]:
        """Get statistics for an endpoint."""
        times = self.endpoint_times.get(endpoint, [])
        if not times:
            return {}

        return {
            "mean": statistics.mean(times),
            "median": statistics.median(times),
            "p95": self._percentile(times, 95),
            "p99": self._percentile(times, 99),
            "min": min(times),
            "max": max(times),
            "count": len(times),
        }

    @staticmethod
    def _percentile(data: list[float], percentile: float) -> float:
        """Calculate percentile."""
        sorted_data = sorted(data)
        index = int(len(sorted_data) * percentile / 100)
        return sorted_data[min(index, len(sorted_data) - 1)]

    def to_dict(self) -> dict[str, Any]:
        """Convert to dictionary."""
        return {
            "endpoint_stats": {
                endpoint: self.get_endpoint_stats(endpoint)
                for endpoint in self.endpoint_times
            },
            "load_test": self.load_test_results,
            "memory": self.memory_stats,
            "database": self.database_stats,
        }

    def print_report(self):
        """Print human-readable report."""
        print("\n" + "=" * 80)
        print("PERFORMANCE TEST REPORT")
        print("=" * 80)

        # Endpoint response times
        print("\n1. ENDPOINT RESPONSE TIMES")
        print("-" * 80)
        for endpoint, stats in self.to_dict()["endpoint_stats"].items():
            print(f"\n{endpoint}:")
            print(f"  Mean:     {stats.get('mean', 0):.2f}ms")
            print(f"  Median:   {stats.get('median', 0):.2f}ms")
            print(f"  P95:      {stats.get('p95', 0):.2f}ms")
            print(f"  P99:      {stats.get('p99', 0):.2f}ms")
            print(f"  Min/Max:  {stats.get('min', 0):.2f}ms / {stats.get('max', 0):.2f}ms")
            print(f"  Count:    {stats.get('count', 0)}")

        # Load test results
        if self.load_test_results:
            print("\n2. LOAD TEST RESULTS (50 concurrent users)")
            print("-" * 80)
            for key, value in self.load_test_results.items():
                print(f"  {key}: {value}")

        # Memory stats
        if self.memory_stats:
            print("\n3. MEMORY USAGE")
            print("-" * 80)
            for key, value in self.memory_stats.items():
                print(f"  {key}: {value}")

        # Database stats
        if self.database_stats:
            print("\n4. DATABASE PERFORMANCE")
            print("-" * 80)
            for key, value in self.database_stats.items():
                print(f"  {key}: {value}")

        print("\n" + "=" * 80)


class PerformanceTester:
    """Run performance tests."""

    def __init__(self, base_url: str = "http://localhost:8000"):
        self.base_url = base_url
        self.result = PerformanceTestResult()
        self.test_user_id = "test_user_123"
        self.auth_token = self._generate_test_token()

    def _generate_test_token(self) -> str:
        """Generate a test JWT token."""
        import jwt
        payload = {
            "sub": self.test_user_id,
            "email": "test@example.com",
            "email_verified": True,
            "name": "Test User",
            "iat": int(time.time()),
            "exp": int(time.time()) + 3600,
        }
        return jwt.encode(payload, os.environ["JWT_SECRET"], algorithm="HS256")

    def _get_headers(self) -> dict[str, str]:
        """Get request headers with auth."""
        return {
            "Authorization": f"Bearer {self.auth_token}",
            "Content-Type": "application/json",
        }

    def _measure_request(self, method: str, url: str, **kwargs) -> tuple[float, Any]:
        """Measure request duration and return response."""
        start = time.perf_counter()
        try:
            response = requests.request(method, url, **kwargs)
            duration_ms = (time.perf_counter() - start) * 1000
            return duration_ms, response
        except Exception as e:
            duration_ms = (time.perf_counter() - start) * 1000
            print(f"Request failed: {e}")
            return duration_ms, None

    def test_endpoint_performance(self, warmup_runs: int = 5, test_runs: int = 20):
        """Test response times on key endpoints."""
        print("\nTesting endpoint performance...")

        endpoints = [
            ("dashboard", "GET", f"/api/users/{self.test_user_id}/dashboard"),
            ("history", "GET", f"/api/users/{self.test_user_id}/history"),
            ("profile", "GET", f"/api/users/{self.test_user_id}/profile"),
        ]

        for name, method, path in endpoints:
            url = f"{self.base_url}{path}"

            # Warmup
            print(f"  Warming up {name}...")
            for _ in range(warmup_runs):
                self._measure_request(method, url, headers=self._get_headers(), timeout=10)

            # Test runs
            print(f"  Testing {name} ({test_runs} runs)...")
            for i in range(test_runs):
                duration_ms, response = self._measure_request(
                    method, url, headers=self._get_headers(), timeout=10
                )
                self.result.add_endpoint_time(name, duration_ms)

                if response and response.status_code == 200:
                    print(f"    Run {i+1}: {duration_ms:.2f}ms")
                else:
                    status = response.status_code if response else "ERROR"
                    print(f"    Run {i+1}: {duration_ms:.2f}ms (status: {status})")

    def test_load_concurrent(self, num_users: int = 50, duration_sec: int = 30):
        """Test with concurrent users."""
        print(f"\nLoad testing with {num_users} concurrent users for {duration_sec}s...")

        import concurrent.futures

        request_times = []
        errors = 0
        success = 0

        def make_request():
            """Make a single request."""
            nonlocal errors, success
            url = f"{self.base_url}/api/users/{self.test_user_id}/dashboard"
            duration_ms, response = self._measure_request(
                "GET", url, headers=self._get_headers(), timeout=10
            )

            if response and response.status_code == 200:
                success += 1
            else:
                errors += 1

            return duration_ms

        start_time = time.perf_counter()
        end_time = start_time + duration_sec

        with concurrent.futures.ThreadPoolExecutor(max_workers=num_users) as executor:
            futures_list = []

            while time.perf_counter() < end_time:
                # Submit requests to maintain num_users concurrent
                while len(futures_list) < num_users and time.perf_counter() < end_time:
                    futures_list.append(executor.submit(make_request))

                # Collect completed futures
                done, pending = concurrent.futures.wait(
                    futures_list, timeout=0.1, return_when=concurrent.futures.FIRST_COMPLETED
                )

                futures_list = list(pending)

                for future in done:
                    try:
                        duration = future.result()
                        request_times.append(duration)
                    except Exception as e:
                        print(f"Request error: {e}")
                        errors += 1

        total_duration = time.perf_counter() - start_time
        total_requests = len(request_times)
        throughput = total_requests / total_duration if total_duration > 0 else 0

        self.result.load_test_results = {
            "total_requests": total_requests,
            "successful": success,
            "errors": errors,
            "duration_sec": f"{total_duration:.2f}s",
            "throughput": f"{throughput:.2f} req/s",
            "mean_latency": f"{statistics.mean(request_times):.2f}ms" if request_times else "N/A",
            "p95_latency": f"{self.result._percentile(request_times, 95):.2f}ms" if request_times else "N/A",
            "error_rate": f"{(errors / total_requests * 100):.1f}%" if total_requests > 0 else "N/A",
        }

    def test_memory_usage(self):
        """Monitor memory usage during requests."""
        print("\nTesting memory usage...")

        tracemalloc.start()
        process = psutil.Process()

        # Baseline
        baseline_mem = process.memory_info().rss / 1024 / 1024  # MB

        # Make requests
        for i in range(100):
            url = f"{self.base_url}/api/users/{self.test_user_id}/dashboard"
            self._measure_request("GET", url, headers=self._get_headers(), timeout=10)

        # Check memory
        current_mem = process.memory_info().rss / 1024 / 1024  # MB
        current, peak = tracemalloc.get_traced_memory()
        tracemalloc.stop()

        self.result.memory_stats = {
            "baseline_mb": f"{baseline_mem:.2f} MB",
            "after_100_requests_mb": f"{current_mem:.2f} MB",
            "memory_increase_mb": f"{current_mem - baseline_mem:.2f} MB",
            "traced_current_mb": f"{current / 1024 / 1024:.2f} MB",
            "traced_peak_mb": f"{peak / 1024 / 1024:.2f} MB",
            "potential_leak": "YES" if (current_mem - baseline_mem) > 50 else "NO",
        }

    def compare_with_baseline(self, baseline_file: str = "performance_baseline.json"):
        """Compare results with baseline."""
        baseline_path = Path(__file__).parent / baseline_file

        if not baseline_path.exists():
            print(f"\nNo baseline found at {baseline_path}")
            print("Saving current results as baseline...")
            self.save_baseline(baseline_file)
            return

        with open(baseline_path) as f:
            baseline = json.load(f)

        print("\n" + "=" * 80)
        print("COMPARISON WITH BASELINE")
        print("=" * 80)

        # Compare endpoint times
        current = self.result.to_dict()

        for endpoint in ["dashboard", "history", "profile"]:
            if endpoint in current["endpoint_stats"] and endpoint in baseline.get("endpoint_stats", {}):
                current_mean = current["endpoint_stats"][endpoint]["mean"]
                baseline_mean = baseline["endpoint_stats"][endpoint]["mean"]
                diff_pct = ((current_mean - baseline_mean) / baseline_mean) * 100

                status = "FASTER" if diff_pct < 0 else "SLOWER"
                symbol = "✓" if diff_pct < 10 else "✗"

                print(f"\n{endpoint}:")
                print(f"  Baseline: {baseline_mean:.2f}ms")
                print(f"  Current:  {current_mean:.2f}ms")
                print(f"  Change:   {diff_pct:+.1f}% {status} {symbol}")

        print("\n" + "=" * 80)

    def save_baseline(self, filename: str = "performance_baseline.json"):
        """Save current results as baseline."""
        baseline_path = Path(__file__).parent / filename

        with open(baseline_path, "w") as f:
            json.dump(self.result.to_dict(), f, indent=2)

        print(f"Baseline saved to {baseline_path}")

    def run_all_tests(self):
        """Run all performance tests."""
        print("=" * 80)
        print("STARTING PERFORMANCE TESTS")
        print("=" * 80)
        print(f"Base URL: {self.base_url}")
        print(f"Test User ID: {self.test_user_id}")

        # Check if server is running
        try:
            response = requests.get(f"{self.base_url}/", timeout=5)
            if response.status_code not in [200, 404]:
                print(f"\nERROR: Server returned status {response.status_code}")
                print("Make sure the server is running: uvicorn glowupai.complete_api:app")
                return
        except requests.exceptions.RequestException as e:
            print(f"\nERROR: Cannot connect to server at {self.base_url}")
            print(f"Error: {e}")
            print("\nMake sure the server is running:")
            print("  cd /Users/21cabbage/GlowupAI/backend")
            print("  source venv/bin/activate")
            print("  uvicorn glowupai.complete_api:app --reload")
            return

        try:
            # Test 1: Endpoint performance
            self.test_endpoint_performance(warmup_runs=5, test_runs=20)

            # Test 2: Load test
            self.test_load_concurrent(num_users=50, duration_sec=30)

            # Test 3: Memory usage
            self.test_memory_usage()

            # Print report
            self.result.print_report()

            # Compare with baseline
            self.compare_with_baseline()

        except KeyboardInterrupt:
            print("\n\nTest interrupted by user")
        except Exception as e:
            print(f"\n\nTest failed with error: {e}")
            import traceback
            traceback.print_exc()


def main():
    """Main entry point."""
    import argparse

    parser = argparse.ArgumentParser(description="Run performance tests")
    parser.add_argument(
        "--url",
        default="http://localhost:8000",
        help="Base URL for the API (default: http://localhost:8000)",
    )
    parser.add_argument(
        "--save-baseline",
        action="store_true",
        help="Save results as new baseline",
    )

    args = parser.parse_args()

    tester = PerformanceTester(base_url=args.url)
    tester.run_all_tests()

    if args.save_baseline:
        tester.save_baseline()


if __name__ == "__main__":
    main()
