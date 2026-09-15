"""Redis-backed rate limiting for production API abuse prevention."""

from __future__ import annotations

import logging
import os
import time
from threading import RLock
from typing import Any

from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

logger = logging.getLogger(__name__)

_SLIDING_WINDOW_SCRIPT = """
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local window_start = now - window
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
local count = redis.call('ZCARD', key)
local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
local reset = now + window
if #oldest > 0 then
  reset = tonumber(oldest[2]) + window
end
if count < limit then
  local member = string.format('%.6f-%s', now, redis.call('INCR', key .. ':sequence'))
  redis.call('ZADD', key, now, member)
  redis.call('EXPIRE', key, window)
  redis.call('EXPIRE', key .. ':sequence', window)
  return {1, limit - count - 1, 0, math.floor(reset)}
end
local retry = math.max(1, math.floor(reset - now) + 1)
return {0, 0, retry, math.floor(reset)}
"""


class RedisRateLimiter:
    """Redis-backed rate limiter with sliding window algorithm.

    Falls back to in-memory rate limiting if Redis is not available.
    """

    def __init__(
        self,
        redis_url: str | None = None,
        *,
        limits: dict[str, tuple[int, int]] | None = None,
        fail_closed: bool = False,
    ):
        self.redis_url = redis_url
        self.redis_client = None
        self.fallback_memory: dict[str, list[float]] = (
            {}
        )  # Fallback for when Redis is unavailable
        self.memory_lock = RLock()
        self.fail_closed = fail_closed
        self.redis_error: str | None = None

        # Rate limits: (requests_per_minute, window_seconds)
        self.limits = limits or {
            "capture_analyze": (10, 60),  # 10 per minute
            "auth": (5, 60),  # 5 per minute
            "dashboard": (30, 60),  # 30 per minute
            "api": (60, 60),  # 60 per minute default
        }

        if redis_url:
            try:
                import redis

                self.redis_client = redis.from_url(
                    redis_url,
                    decode_responses=True,
                    socket_connect_timeout=5,
                    socket_timeout=5,
                )
                # Test connection
                self.redis_client.ping()
                logger.info("Redis rate limiter initialized successfully")
            except (ImportError, OSError, ValueError, RuntimeError) as exc:
                # Catch-all for Redis initialization with fallback
                self.redis_error = type(exc).__name__
                logger.warning(
                    "Redis unavailable, falling back to memory-based rate limiting",
                    extra={"error_type": self.redis_error},
                )
                self.redis_client = None

    def _get_limit_type(self, path: str, method: str) -> str:
        """Determine rate limit type from path and method."""
        if (
            "/api/captures/analyze" in path
            or "/api/captures" in path
            and method == "POST"
        ):
            return "capture_analyze"
        elif "/api/auth/" in path:
            return "auth"
        elif "/api/dashboard" in path or "/api/users/" in path and "/dashboard" in path:
            return "dashboard"
        else:
            return "api"

    async def check_rate_limit(
        self,
        client_id: str,
        path: str,
        method: str,
    ) -> tuple[bool, int | None, int, int]:
        """Check if request is within rate limit.

        Args:
            client_id: Client identifier (IP or user ID)
            path: Request path
            method: HTTP method

        Returns:
            Tuple of (allowed: bool, retry_after: int | None, remaining: int, reset: int)
        """
        limit_type = self._get_limit_type(path, method)
        max_requests, window_seconds = self.limits[limit_type]

        if self.redis_client:
            return await self._check_redis_limit(
                client_id,
                limit_type,
                max_requests,
                window_seconds,
            )
        elif self.fail_closed:
            return False, 5, 0, int(time.time() + 5)
        else:
            return await self._check_memory_limit(
                client_id,
                limit_type,
                max_requests,
                window_seconds,
            )

    async def _check_redis_limit(
        self,
        client_id: str,
        limit_type: str,
        max_requests: int,
        window_seconds: int,
    ) -> tuple[bool, int | None, int, int]:
        """Check rate limit using Redis sliding window."""
        redis_client = self.redis_client
        if redis_client is None:
            raise RuntimeError("Redis client must be initialized")

        try:
            key = f"ratelimit:{limit_type}:{client_id}"
            now = time.time()
            result = redis_client.eval(
                _SLIDING_WINDOW_SCRIPT,
                1,
                key,
                now,
                window_seconds,
                max_requests,
            )
            allowed, remaining, retry_after, reset_timestamp = (
                int(value) for value in result
            )
            return bool(allowed), (retry_after or None), remaining, reset_timestamp

        # Redis clients expose vendor-specific exceptions.
        except Exception as exc:  # noqa: BLE001
            self.redis_error = type(exc).__name__
            logger.error(
                "Redis rate limit check failed; using memory fallback",
                extra={"error_type": self.redis_error},
            )
            if self.fail_closed:
                return False, 5, 0, int(time.time() + 5)
            return await self._check_memory_limit(
                client_id, limit_type, max_requests, window_seconds
            )

    async def _check_memory_limit(
        self,
        client_id: str,
        limit_type: str,
        max_requests: int,
        window_seconds: int,
    ) -> tuple[bool, int | None, int, int]:
        """Check rate limit using in-memory sliding window."""
        key = f"{limit_type}:{client_id}"
        now = time.time()
        window_start = now - window_seconds

        with self.memory_lock:
            # Initialize if not exists
            if key not in self.fallback_memory:
                self.fallback_memory[key] = []

            # Remove old entries
            self.fallback_memory[key] = [
                ts for ts in self.fallback_memory[key] if ts > window_start
            ]

            current_count = len(self.fallback_memory[key])
            oldest = (
                min(self.fallback_memory[key]) if self.fallback_memory[key] else now
            )
            reset_timestamp = int(oldest + window_seconds)

            if current_count < max_requests:
                self.fallback_memory[key].append(now)
                remaining = max_requests - current_count - 1
                return True, None, remaining, reset_timestamp
            retry_after = int(oldest + window_seconds - now) + 1
            return False, retry_after, 0, reset_timestamp

    def status(self) -> dict[str, Any]:
        return {
            "backend": "redis" if self.redis_client is not None else "memory",
            "configured_redis": bool(self.redis_url),
            "fail_closed": self.fail_closed,
            "limits": {
                name: {"requests": value[0], "window_seconds": value[1]}
                for name, value in self.limits.items()
            },
            "error": self.redis_error,
        }


class ProductionRateLimitMiddleware(BaseHTTPMiddleware):
    """Production-ready rate limiting middleware with Redis backend."""

    def __init__(
        self,
        app,
        redis_url: str | None = None,
        enabled: bool = True,
        fail_closed: bool = False,
    ):
        super().__init__(app)
        self.limiter = RedisRateLimiter(
            redis_url,
            limits=_limits_from_env(),
            fail_closed=fail_closed,
        )
        self.enabled = enabled
        # Expose redacted backend status to health/admin diagnostics without
        # making the Redis client itself part of the public API.
        target = app
        while getattr(target, "app", None) is not None:
            target = target.app
        if hasattr(target, "state"):
            target.state.rate_limiter = self.limiter

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint,
    ) -> Response:
        """Apply rate limiting to requests."""
        if not self.enabled:
            return await call_next(request)

        # Skip rate limiting for health and metrics endpoints
        if request.url.path in [
            "/api/health",
            "/api/ready",
            "/api/live",
            "/api/metrics",
            "/internal/metrics",
        ]:
            return await call_next(request)

        # Use the source IP as the stable abuse key. Do not use an unverified
        # JWT claim here: a caller could otherwise rotate `sub` values to
        # bypass a per-IP bucket before authentication runs.
        client_id = f"ip:{request.client.host if request.client else 'unknown'}"

        # Check rate limit
        (
            allowed,
            retry_after,
            remaining,
            reset_timestamp,
        ) = await self.limiter.check_rate_limit(
            client_id,
            request.url.path,
            request.method,
        )

        limit_type = self.limiter._get_limit_type(request.url.path, request.method)
        max_requests = self.limiter.limits[limit_type][0]

        if not allowed:
            logger.warning(
                f"Rate limit exceeded for {client_id} on {request.method} {request.url.path}",
                extra={
                    "client_id": client_id,
                    "endpoint": f"{request.method} {request.url.path}",
                    "retry_after": retry_after,
                },
            )

            headers = {
                "Retry-After": str(retry_after or 60),
                "X-RateLimit-Limit": str(max_requests),
                "X-RateLimit-Remaining": "0",
                "X-RateLimit-Reset": str(reset_timestamp),
            }

            return JSONResponse(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                content={
                    "detail": f"Rate limit exceeded. Try again in {retry_after} seconds.",
                    "error_code": "RATE_LIMIT_EXCEEDED",
                    "retry_after": retry_after,
                },
                headers=headers,
            )

        # Add rate limit info to response headers
        response = await call_next(request)
        response.headers["X-RateLimit-Limit"] = str(max_requests)
        response.headers["X-RateLimit-Remaining"] = str(remaining)
        response.headers["X-RateLimit-Reset"] = str(reset_timestamp)

        return response


def _limits_from_env() -> dict[str, tuple[int, int]]:
    """Load bounded route limits without allowing a client to configure them."""

    def limit(name: str, default: int) -> int:
        try:
            return max(1, int(os.getenv(name, str(default))))
        except ValueError:
            logger.warning(
                "Invalid rate limit configuration; using default", extra={"name": name}
            )
            return default

    return {
        "capture_analyze": (limit("GLOWUPAI_RATE_LIMIT_CAPTURE_PER_MINUTE", 10), 60),
        "auth": (limit("GLOWUPAI_RATE_LIMIT_AUTH_PER_MINUTE", 5), 60),
        "dashboard": (limit("GLOWUPAI_RATE_LIMIT_DASHBOARD_PER_MINUTE", 30), 60),
        "api": (limit("GLOWUPAI_RATE_LIMIT_API_PER_MINUTE", 60), 60),
    }
