"""Simple in-memory rate limiting for SkinProof API.

For production, consider using Redis-backed rate limiting (slowapi with Redis)
or a cloud-native solution (Cloud Armor, AWS WAF, Cloudflare).
"""

from __future__ import annotations

import time
from collections import defaultdict
from collections.abc import Callable
from dataclasses import dataclass, field
from threading import Lock

from fastapi import HTTPException, Request
from starlette.middleware.base import BaseHTTPMiddleware


@dataclass
class RateLimitConfig:
    """Rate limit configuration for an endpoint pattern."""

    requests: int
    window_seconds: int
    endpoint_pattern: str


@dataclass
class RateLimitEntry:
    """Track request timestamps for a client."""

    timestamps: list[float] = field(default_factory=list)


class InMemoryRateLimiter:
    """In-memory rate limiter using sliding window algorithm.

    WARNING: This is NOT suitable for multi-process deployments (e.g., Gunicorn
    with multiple workers). For production, use Redis-backed rate limiting.
    """

    def __init__(self) -> None:
        self._store: dict[str, RateLimitEntry] = defaultdict(RateLimitEntry)
        self._lock = Lock()

    def is_allowed(
        self, key: str, requests: int, window_seconds: int,
    ) -> tuple[bool, int]:
        """Check if request is allowed under rate limit.

        Returns (is_allowed, retry_after_seconds).
        """
        now = time.time()
        cutoff = now - window_seconds

        with self._lock:
            entry = self._store[key]
            # Remove expired timestamps
            entry.timestamps = [ts for ts in entry.timestamps if ts > cutoff]

            if len(entry.timestamps) < requests:
                entry.timestamps.append(now)
                return True, 0

            # Rate limit exceeded - calculate retry_after
            oldest_timestamp = entry.timestamps[0]
            retry_after = int(oldest_timestamp + window_seconds - now) + 1
            return False, retry_after

    def cleanup_old_entries(self, max_age_seconds: int = 3600) -> None:
        """Remove entries that haven't been accessed recently."""
        now = time.time()
        cutoff = now - max_age_seconds

        with self._lock:
            keys_to_remove = [
                key
                for key, entry in self._store.items()
                if not entry.timestamps or entry.timestamps[-1] < cutoff
            ]
            for key in keys_to_remove:
                del self._store[key]


class RateLimitMiddleware(BaseHTTPMiddleware):
    """FastAPI middleware for rate limiting.

    Usage:
        limiter = InMemoryRateLimiter()
        app.add_middleware(RateLimitMiddleware, limiter=limiter, get_client_id=get_remote_address)

        # Configure limits per endpoint
        rate_limits = {
            "/api/auth/session": RateLimitConfig(10, 60, "/api/auth/session"),
            "/api/users/*/captures": RateLimitConfig(30, 3600, "/api/users/.*/captures"),
        }
    """

    def __init__(
        self,
        app,
        limiter: InMemoryRateLimiter,
        get_client_id: Callable[[Request], str],
        rate_limits: dict[str, RateLimitConfig] | None = None,
        enabled: bool = True,
    ) -> None:
        super().__init__(app)
        self.limiter = limiter
        self.get_client_id = get_client_id
        self.rate_limits = rate_limits or self._default_rate_limits()
        self.enabled = enabled

    def _default_rate_limits(self) -> dict[str, RateLimitConfig]:
        """Default rate limits for sensitive endpoints."""
        return {
            "/api/auth/session": RateLimitConfig(10, 60, "/api/auth/session"),
            "/api/users": RateLimitConfig(5, 60, "/api/users"),
        }

    def _get_rate_limit(self, path: str) -> RateLimitConfig | None:
        """Find matching rate limit config for path."""
        for config in self.rate_limits.values():
            if path.startswith(config.endpoint_pattern.replace("*", "")):
                return config
        return None

    async def dispatch(self, request: Request, call_next):
        if not self.enabled:
            return await call_next(request)

        # Skip rate limiting for health checks and static files
        if request.url.path in {"/api/health", "/api/roadmap", "/"}:
            return await call_next(request)

        rate_limit = self._get_rate_limit(request.url.path)
        if not rate_limit:
            # No rate limit configured for this endpoint
            return await call_next(request)

        client_id = self.get_client_id(request)
        key = f"{client_id}:{rate_limit.endpoint_pattern}"

        is_allowed, retry_after = self.limiter.is_allowed(
            key, rate_limit.requests, rate_limit.window_seconds,
        )

        if not is_allowed:
            raise HTTPException(
                status_code=429,
                detail=f"Rate limit exceeded. Try again in {retry_after} seconds.",
                headers={"Retry-After": str(retry_after)},
            )

        response = await call_next(request)
        return response


def get_remote_address(request: Request) -> str:
    """Extract client IP address from request.

    Handles X-Forwarded-For header for proxied requests.
    """
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        # X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
        # Take the first one (original client)
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "unknown"


def create_rate_limiter(
    enabled: bool = True,
) -> tuple[InMemoryRateLimiter, dict[str, RateLimitConfig]]:
    """Factory function to create rate limiter with default configuration.

    Returns (limiter, rate_limits_config).
    """
    limiter = InMemoryRateLimiter()

    # Configure rate limits for sensitive endpoints
    rate_limits = {
        # Authentication: 10 attempts per minute
        "/api/auth/session": RateLimitConfig(10, 60, "/api/auth/session"),
        # User creation: 5 per minute (prevent spam)
        "/api/users": RateLimitConfig(5, 60, "/api/users"),
        # Captures: 30 per hour (reasonable for daily usage)
        "/api/users/.*/captures": RateLimitConfig(30, 3600, "/api/users"),
        # Gemini-powered features: 10 per hour (expensive)
        "/api/users/.*/qna": RateLimitConfig(10, 3600, "/api/users/.*/qna"),
        "/api/users/.*/shelf-scan": RateLimitConfig(
            10, 3600, "/api/users/.*/shelf-scan",
        ),
        # General API: 100 requests per minute
        "/api": RateLimitConfig(100, 60, "/api"),
    }

    return limiter, rate_limits
