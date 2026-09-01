"""Redis-backed rate limiting for production API abuse prevention."""

from __future__ import annotations

import logging
import time
from typing import Callable

from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

logger = logging.getLogger(__name__)


class RedisRateLimiter:
    """Redis-backed rate limiter with sliding window algorithm.

    Falls back to in-memory rate limiting if Redis is not available.
    """

    def __init__(self, redis_url: str | None = None):
        self.redis_url = redis_url
        self.redis_client = None
        self.fallback_memory = {}  # Fallback for when Redis is unavailable

        # Rate limits: (requests_per_minute, window_seconds)
        self.limits = {
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
            except (ImportError, Exception) as exc:
                logger.warning(
                    f"Redis unavailable, falling back to memory-based rate limiting: {exc}"
                )
                self.redis_client = None

    def _get_limit_type(self, path: str, method: str) -> str:
        """Determine rate limit type from path and method."""
        if "/api/captures/analyze" in path or "/api/captures" in path and method == "POST":
            return "capture_analyze"
        elif "/api/auth/" in path:
            return "auth"
        elif "/api/dashboard" in path or "/api/users/" in path and "/dashboard" in path:
            return "dashboard"
        else:
            return "api"

    async def check_rate_limit(
        self, client_id: str, path: str, method: str
    ) -> tuple[bool, int | None]:
        """Check if request is within rate limit.

        Args:
            client_id: Client identifier (IP or user ID)
            path: Request path
            method: HTTP method

        Returns:
            Tuple of (allowed: bool, retry_after: int | None)
        """
        limit_type = self._get_limit_type(path, method)
        max_requests, window_seconds = self.limits[limit_type]

        if self.redis_client:
            return await self._check_redis_limit(
                client_id, limit_type, max_requests, window_seconds
            )
        else:
            return await self._check_memory_limit(
                client_id, limit_type, max_requests, window_seconds
            )

    async def _check_redis_limit(
        self, client_id: str, limit_type: str, max_requests: int, window_seconds: int
    ) -> tuple[bool, int | None]:
        """Check rate limit using Redis sliding window."""
        try:
            key = f"ratelimit:{limit_type}:{client_id}"
            now = time.time()
            window_start = now - window_seconds

            # Remove old entries
            self.redis_client.zremrangebyscore(key, 0, window_start)

            # Count requests in current window
            request_count = self.redis_client.zcard(key)

            if request_count < max_requests:
                # Add current request
                self.redis_client.zadd(key, {str(now): now})
                self.redis_client.expire(key, window_seconds)
                return True, None
            else:
                # Calculate retry after
                oldest = self.redis_client.zrange(key, 0, 0, withscores=True)
                if oldest:
                    retry_after = int(oldest[0][1] + window_seconds - now) + 1
                else:
                    retry_after = window_seconds
                return False, retry_after

        except Exception as exc:
            logger.error(f"Redis rate limit check failed: {exc}")
            # Fall back to allowing request on Redis failure
            return True, None

    async def _check_memory_limit(
        self, client_id: str, limit_type: str, max_requests: int, window_seconds: int
    ) -> tuple[bool, int | None]:
        """Check rate limit using in-memory sliding window."""
        key = f"{limit_type}:{client_id}"
        now = time.time()
        window_start = now - window_seconds

        # Initialize if not exists
        if key not in self.fallback_memory:
            self.fallback_memory[key] = []

        # Remove old entries
        self.fallback_memory[key] = [
            ts for ts in self.fallback_memory[key] if ts > window_start
        ]

        if len(self.fallback_memory[key]) < max_requests:
            self.fallback_memory[key].append(now)
            return True, None
        else:
            oldest = min(self.fallback_memory[key])
            retry_after = int(oldest + window_seconds - now) + 1
            return False, retry_after


class ProductionRateLimitMiddleware(BaseHTTPMiddleware):
    """Production-ready rate limiting middleware with Redis backend."""

    def __init__(self, app, redis_url: str | None = None, enabled: bool = True):
        super().__init__(app)
        self.limiter = RedisRateLimiter(redis_url)
        self.enabled = enabled

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        """Apply rate limiting to requests."""
        if not self.enabled:
            return await call_next(request)

        # Skip rate limiting for health and metrics endpoints
        if request.url.path in ["/api/health", "/api/metrics"]:
            return await call_next(request)

        # Get client identifier (prefer user ID from auth, fall back to IP)
        client_id = request.client.host if request.client else "unknown"

        # Try to extract user ID from authorization header for better tracking
        auth_header = request.headers.get("authorization", "")
        if auth_header and "." in auth_header:
            # Simple extraction of sub from JWT (without full verification)
            try:
                import base64
                import json
                token = auth_header.replace("Bearer ", "").strip()
                payload = token.split(".")[1]
                # Add padding if needed
                payload += "=" * (4 - len(payload) % 4)
                decoded = json.loads(base64.urlsafe_b64decode(payload))
                if "sub" in decoded:
                    client_id = f"user:{decoded['sub']}"
            except Exception:
                pass  # Fall back to IP

        # Check rate limit
        allowed, retry_after = await self.limiter.check_rate_limit(
            client_id, request.url.path, request.method
        )

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
                "X-RateLimit-Limit": str(self.limiter.limits.get(
                    self.limiter._get_limit_type(request.url.path, request.method),
                    (60, 60)
                )[0]),
                "X-RateLimit-Remaining": "0",
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
        limit_type = self.limiter._get_limit_type(request.url.path, request.method)
        max_requests = self.limiter.limits[limit_type][0]

        response.headers["X-RateLimit-Limit"] = str(max_requests)

        return response
