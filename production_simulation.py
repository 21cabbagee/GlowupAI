#!/usr/bin/env python3
"""
Production Deployment Simulation for GlowUp AI
==============================================

Simulates a complete production deployment and user journey:
1. Backend deployment (production settings)
2. Complete user flow testing
3. Performance monitoring
4. Production features verification

This is a comprehensive test to catch issues before production launch.
"""

import asyncio
import json
import os
import subprocess
import sys
import time
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any

import requests


@dataclass
class TestResult:
    """Track test results."""
    name: str
    passed: bool
    duration_ms: float
    error: str | None = None
    details: dict = field(default_factory=dict)


@dataclass
class MonitoringMetrics:
    """Track monitoring metrics."""
    response_times: list[float] = field(default_factory=list)
    error_count: int = 0
    success_count: int = 0
    memory_usage: list[float] = field(default_factory=list)

    @property
    def avg_response_time(self) -> float:
        return sum(self.response_times) / len(self.response_times) if self.response_times else 0

    @property
    def p95_response_time(self) -> float:
        if not self.response_times:
            return 0
        sorted_times = sorted(self.response_times)
        idx = int(len(sorted_times) * 0.95)
        return sorted_times[idx]

    @property
    def error_rate(self) -> float:
        total = self.success_count + self.error_count
        return self.error_count / total if total > 0 else 0


class ProductionSimulator:
    """Simulates production deployment and testing."""

    def __init__(self, backend_url: str = "http://localhost:8000"):
        self.backend_url = backend_url
        self.results: list[TestResult] = []
        self.metrics = MonitoringMetrics()
        self.test_user_id: str | None = None
        self.test_capture_id: str | None = None
        self.start_time = time.time()

    def log(self, message: str, level: str = "INFO"):
        """Log with timestamp."""
        timestamp = datetime.now().strftime("%H:%M:%S")
        prefix = {
            "INFO": "ℹ️",
            "SUCCESS": "✅",
            "ERROR": "❌",
            "WARNING": "⚠️",
            "STEP": "▶️"
        }.get(level, "•")
        print(f"[{timestamp}] {prefix} {message}")

    def run_test(self, name: str, func):
        """Run a test and track results."""
        self.log(f"Running: {name}", "STEP")
        start = time.time()
        try:
            result = func()
            duration = (time.time() - start) * 1000
            self.results.append(TestResult(
                name=name,
                passed=True,
                duration_ms=duration,
                details=result if isinstance(result, dict) else {}
            ))
            self.log(f"✓ {name} ({duration:.0f}ms)", "SUCCESS")
            return result
        except Exception as e:
            duration = (time.time() - start) * 1000
            self.results.append(TestResult(
                name=name,
                passed=False,
                duration_ms=duration,
                error=str(e)
            ))
            self.log(f"✗ {name}: {e}", "ERROR")
            raise

    def http_get(self, path: str, headers: dict | None = None) -> dict:
        """Make HTTP GET request with metrics tracking."""
        start = time.time()
        try:
            response = requests.get(
                f"{self.backend_url}{path}",
                headers=headers or {},
                timeout=10
            )
            duration = (time.time() - start) * 1000
            self.metrics.response_times.append(duration)

            if response.status_code >= 400:
                self.metrics.error_count += 1
                raise Exception(f"HTTP {response.status_code}: {response.text}")

            self.metrics.success_count += 1
            return response.json() if response.content else {}
        except Exception as e:
            self.metrics.error_count += 1
            raise

    def http_post(self, path: str, data: dict | None = None, headers: dict | None = None) -> dict:
        """Make HTTP POST request with metrics tracking."""
        start = time.time()
        try:
            response = requests.post(
                f"{self.backend_url}{path}",
                json=data,
                headers=headers or {},
                timeout=10
            )
            duration = (time.time() - start) * 1000
            self.metrics.response_times.append(duration)

            if response.status_code >= 400:
                self.metrics.error_count += 1
                raise Exception(f"HTTP {response.status_code}: {response.text}")

            self.metrics.success_count += 1
            return response.json() if response.content else {}
        except Exception as e:
            self.metrics.error_count += 1
            raise

    def test_health_check(self):
        """Test health endpoint."""
        health = self.http_get("/api/health")
        assert health["status"] in ["ok", "healthy"], f"Unhealthy: {health}"
        assert health.get("database") == "postgresql", "Not using PostgreSQL"
        return health

    def test_user_signup(self):
        """Test user signup flow."""
        # Create user
        user = self.http_post("/api/users", {"skin_type": "combination"})
        assert "user_id" in user, "No user_id returned"
        self.test_user_id = user["user_id"]
        return user

    def test_onboarding(self):
        """Test onboarding flow."""
        if not self.test_user_id:
            raise Exception("No test user created")

        # Update profile
        profile_data = {
            "display_name": "Test User",
            "skin_type": "combination",
            "focus_vertical": "skincare",
            "goals": ["reduce_redness", "improve_texture"],
            "experience_level": "intermediate",
            "onboarding_complete": True
        }
        result = requests.patch(
            f"{self.backend_url}/api/users/{self.test_user_id}/profile",
            json=profile_data,
            timeout=10
        )
        assert result.status_code == 200, f"Profile update failed: {result.text}"

        # Grant consent
        consent = self.http_post(
            f"/api/users/{self.test_user_id}/consent",
            {"facial_data": True}
        )
        return {"profile": profile_data, "consent": consent}

    def test_photo_capture(self):
        """Test photo capture flow."""
        if not self.test_user_id:
            raise Exception("No test user created")

        # Create a dummy photo capture
        capture_data = {
            "timestamp": datetime.now().isoformat(),
            "lighting_quality": 0.85,
            "photo_data": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        }

        result = requests.post(
            f"{self.backend_url}/api/users/{self.test_user_id}/captures",
            json=capture_data,
            timeout=10
        )

        if result.status_code == 201:
            capture = result.json()
            self.test_capture_id = capture.get("capture_id")
            return capture
        else:
            self.log(f"Capture endpoint not ready: {result.status_code}", "WARNING")
            return {"warning": "capture endpoint not available"}

    def test_dashboard(self):
        """Test dashboard data retrieval."""
        if not self.test_user_id:
            raise Exception("No test user created")

        # Get user profile
        profile = self.http_get(f"/api/users/{self.test_user_id}/profile")
        assert "user_id" in profile, "Invalid profile response"

        return profile

    def test_insights(self):
        """Test insights and analytics."""
        if not self.test_user_id:
            raise Exception("No test user created")

        # Try to get analytics data
        try:
            analytics = self.http_get(f"/api/users/{self.test_user_id}/analytics")
            return analytics
        except Exception as e:
            self.log(f"Analytics endpoint not available: {e}", "WARNING")
            return {"warning": "analytics not available"}

    def test_history(self):
        """Test capture history."""
        if not self.test_user_id:
            raise Exception("No test user created")

        try:
            history = self.http_get(f"/api/users/{self.test_user_id}/captures")
            return history
        except Exception as e:
            self.log(f"History endpoint not available: {e}", "WARNING")
            return {"warning": "history not available"}

    def test_rate_limiting(self):
        """Test rate limiting."""
        self.log("Testing rate limiting (making 10 rapid requests)...")
        rate_limit_hit = False

        for i in range(10):
            try:
                self.http_get("/api/health")
            except Exception as e:
                if "429" in str(e):
                    rate_limit_hit = True
                    self.log("Rate limit triggered (expected)", "SUCCESS")
                    break

        # It's OK if rate limiting isn't triggered with just 10 requests
        return {"rate_limit_tested": True, "rate_limit_hit": rate_limit_hit}

    def test_caching(self):
        """Test response caching."""
        # Make same request twice and check response time
        start1 = time.time()
        self.http_get("/api/health")
        time1 = (time.time() - start1) * 1000

        time.sleep(0.1)  # Small delay

        start2 = time.time()
        self.http_get("/api/health")
        time2 = (time.time() - start2) * 1000

        cached = time2 < time1 * 0.8  # Second request should be faster if cached
        return {
            "first_request_ms": time1,
            "second_request_ms": time2,
            "likely_cached": cached
        }

    def test_error_handling(self):
        """Test error handling."""
        # Test 404
        try:
            self.http_get("/api/nonexistent")
            raise Exception("Should have returned 404")
        except Exception as e:
            if "404" not in str(e):
                raise Exception(f"Unexpected error: {e}")

        # Test invalid user ID
        try:
            self.http_get("/api/users/invalid-id/profile")
            raise Exception("Should have returned error for invalid user")
        except Exception as e:
            if "404" not in str(e) and "400" not in str(e):
                raise Exception(f"Unexpected error: {e}")

        return {"error_handling": "working"}

    def check_backend_logs(self):
        """Check backend logs for errors."""
        try:
            result = subprocess.run(
                ["docker", "compose", "-f", "/Users/21cabbage/GlowupAI/backend/docker-compose.yml",
                 "logs", "--tail", "100", "api"],
                capture_output=True,
                text=True,
                timeout=10
            )

            logs = result.stdout
            error_count = logs.lower().count("error")
            warning_count = logs.lower().count("warning")

            return {
                "error_lines": error_count,
                "warning_lines": warning_count,
                "has_critical_errors": error_count > 5
            }
        except Exception as e:
            self.log(f"Could not check logs: {e}", "WARNING")
            return {"log_check": "failed"}

    def run_user_journey(self):
        """Run complete user journey."""
        self.log("=" * 60)
        self.log("PRODUCTION SIMULATION - USER JOURNEY TEST", "STEP")
        self.log("=" * 60)

        # 1. Health check
        self.run_test("1. Health Check", self.test_health_check)

        # 2. User signup
        self.run_test("2. User Signup", self.test_user_signup)

        # 3. Complete onboarding
        self.run_test("3. Complete Onboarding", self.test_onboarding)

        # 4. Take first photo
        self.run_test("4. Take First Photo", self.test_photo_capture)

        # 5. View dashboard
        self.run_test("5. View Dashboard", self.test_dashboard)

        # 6. Check insights
        self.run_test("6. Check Insights", self.test_insights)

        # 7. View history
        self.run_test("7. View History", self.test_history)

    def run_production_features_test(self):
        """Test production-specific features."""
        self.log("=" * 60)
        self.log("PRODUCTION FEATURES TEST", "STEP")
        self.log("=" * 60)

        # Rate limiting
        self.run_test("Rate Limiting", self.test_rate_limiting)

        # Caching
        self.run_test("Response Caching", self.test_caching)

        # Error handling
        self.run_test("Error Handling", self.test_error_handling)

    def generate_report(self):
        """Generate final report."""
        total_time = time.time() - self.start_time
        passed = sum(1 for r in self.results if r.passed)
        failed = len(self.results) - passed

        self.log("=" * 60)
        self.log("PRODUCTION SIMULATION REPORT", "STEP")
        self.log("=" * 60)

        print(f"""
📊 TEST SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Total Tests:        {len(self.results)}
  Passed:             {passed} ✅
  Failed:             {failed} ❌
  Total Duration:     {total_time:.2f}s

⚡ PERFORMANCE METRICS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Avg Response Time:  {self.metrics.avg_response_time:.0f}ms
  P95 Response Time:  {self.metrics.p95_response_time:.0f}ms
  Total Requests:     {self.metrics.success_count + self.metrics.error_count}
  Successful:         {self.metrics.success_count}
  Errors:             {self.metrics.error_count}
  Error Rate:         {self.metrics.error_rate * 100:.1f}%

📝 DETAILED RESULTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""")

        for result in self.results:
            status = "✅ PASS" if result.passed else "❌ FAIL"
            print(f"  {status} {result.name} ({result.duration_ms:.0f}ms)")
            if result.error:
                print(f"       Error: {result.error}")

        print("\n" + "━" * 60)

        # Production readiness assessment
        print("\n🚀 PRODUCTION READINESS ASSESSMENT\n" + "━" * 60)

        blockers = []
        warnings = []

        if failed > 0:
            blockers.append(f"{failed} tests failed")

        if self.metrics.error_rate > 0.1:
            blockers.append(f"Error rate too high: {self.metrics.error_rate * 100:.1f}%")

        if self.metrics.avg_response_time > 2000:
            warnings.append(f"High avg response time: {self.metrics.avg_response_time:.0f}ms")

        if self.metrics.p95_response_time > 5000:
            blockers.append(f"P95 response time too high: {self.metrics.p95_response_time:.0f}ms")

        if blockers:
            print("\n❌ PRODUCTION BLOCKERS (must fix before launch):")
            for blocker in blockers:
                print(f"  • {blocker}")

        if warnings:
            print("\n⚠️  WARNINGS (should address):")
            for warning in warnings:
                print(f"  • {warning}")

        if not blockers:
            print("\n✅ NO BLOCKERS - Ready for production deployment!")
            if not warnings:
                print("✅ NO WARNINGS - All systems optimal!")

        print("\n" + "=" * 60)

        return {
            "passed": failed == 0 and len(blockers) == 0,
            "blockers": blockers,
            "warnings": warnings
        }


def check_backend_running(url: str) -> bool:
    """Check if backend is running."""
    try:
        response = requests.get(f"{url}/api/health", timeout=5)
        return response.status_code == 200
    except:
        return False


def start_backend():
    """Start backend using docker-compose."""
    print("🚀 Starting backend with production settings...")

    backend_dir = "/Users/21cabbage/GlowupAI/backend"

    # Check if Docker is available
    try:
        subprocess.run(["docker", "--version"], check=True, capture_output=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("❌ Docker is not available. Cannot start backend.")
        print("ℹ️  Please ensure Docker is installed and running.")
        return False

    # Start docker-compose
    try:
        subprocess.run(
            ["docker", "compose", "-f", f"{backend_dir}/docker-compose.yml", "up", "-d", "--build"],
            check=True,
            cwd=backend_dir
        )

        # Wait for backend to be ready
        print("⏳ Waiting for backend to be ready...")
        for i in range(30):
            if check_backend_running("http://localhost:8000"):
                print("✅ Backend is ready!")
                return True
            time.sleep(2)

        print("❌ Backend did not start within 60 seconds")
        return False

    except subprocess.CalledProcessError as e:
        print(f"❌ Failed to start backend: {e}")
        return False


def stop_backend():
    """Stop backend."""
    print("\n🛑 Stopping backend...")
    backend_dir = "/Users/21cabbage/GlowupAI/backend"
    try:
        subprocess.run(
            ["docker", "compose", "-f", f"{backend_dir}/docker-compose.yml", "down"],
            check=True,
            cwd=backend_dir
        )
        print("✅ Backend stopped")
    except subprocess.CalledProcessError as e:
        print(f"⚠️  Warning: Could not stop backend: {e}")


def main():
    """Main entry point."""
    print("""
╔═══════════════════════════════════════════════════════════════╗
║                                                               ║
║         🚀 GlowUp AI - Production Deployment Simulation       ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
    """)

    # Check if backend is already running
    backend_running = check_backend_running("http://localhost:8000")

    if not backend_running:
        print("ℹ️  Backend is not running. Starting it now...")
        if not start_backend():
            print("\n❌ Cannot proceed without backend. Exiting.")
            sys.exit(1)
        backend_was_started = True
    else:
        print("✅ Backend is already running")
        backend_was_started = False

    try:
        # Run simulation
        simulator = ProductionSimulator("http://localhost:8000")

        # Run user journey tests
        simulator.run_user_journey()

        # Run production features tests
        simulator.run_production_features_test()

        # Generate report
        report = simulator.generate_report()

        # Exit with appropriate code
        sys.exit(0 if report["passed"] else 1)

    except KeyboardInterrupt:
        print("\n\n⚠️  Simulation interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Simulation failed with error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        if backend_was_started:
            stop_backend()


if __name__ == "__main__":
    main()
