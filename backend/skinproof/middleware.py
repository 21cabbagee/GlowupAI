"""Production middleware: rate limiting, error handling, timeouts."""
from __future__ import annotations

import asyncio
import logging
import time
from collections import defaultdict
from typing import Callable

from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

logger = logging.getLogger(__name__)


class RateLimitExceeded(Exception):
    """Exception raised when rate limit is exceeded."""
    pass


class RateLimiter:
    """Token bucket rate limiter with different limits per endpoint type."""

    def __init__(self):
        # Store last request time and token count per client
        self.clients: dict[str, dict[str, float | int]] = defaultdict(
            lambda: {"tokens": 0, "last_update": time.time()}
        )
        self.lock = asyncio.Lock()

        # Rate limits: (requests_per_minute, burst_size)
        self.limits = {
            "auth": (10, 20),  # Auth endpoints: 10/min, burst 20
            "upload": (5, 10),  # Upload endpoints: 5/min, burst 10
            "api": (60, 100),  # Standard API: 60/min, burst 100
            "admin": (100, 200),  # Admin endpoints: higher limits
        }

    def _get_endpoint_type(self, path: str) -> str:
        """Determine endpoint type from path."""
        if "/api/auth/" in path or "/api/users" in path and path.endswith("/users"):
            return "auth"
        elif "/api/captures" in path or "/api/shelf-scan" in path:
            return "upload"
        elif "/api/admin/" in path:
            return "admin"
        else:
            return "api"

    async def check_rate_limit(self, client_id: str, endpoint: str) -> bool:
        """Check if request is within rate limit.

        Args:
            client_id: Client identifier (IP or user ID)
            endpoint: Request endpoint path

        Returns:
            True if within limit, False if exceeded

        Raises:
            RateLimitExceeded: If rate limit is exceeded
        """
        endpoint_type = self._get_endpoint_type(endpoint)
        requests_per_minute, burst_size = self.limits[endpoint_type]

        async with self.lock:
            now = time.time()
            client = self.clients[client_id]

            # Refill tokens based on time elapsed
            time_passed = now - client["last_update"]
            client["tokens"] = min(
                burst_size,
                client["tokens"] + (time_passed * requests_per_minute / 60.0)
            )
            client["last_update"] = now

            # Check if we have tokens
            if client["tokens"] >= 1:
                client["tokens"] -= 1
                return True
            else:
                # Calculate retry after time
                retry_after = int((1 - client["tokens"]) * 60.0 / requests_per_minute) + 1
                raise RateLimitExceeded(f"Rate limit exceeded. Retry after {retry_after} seconds.")


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Middleware for rate limiting requests."""

    def __init__(self, app, enabled: bool = True):
        super().__init__(app)
        self.limiter = RateLimiter()
        self.enabled = enabled

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        """Apply rate limiting to requests."""
        if not self.enabled:
            return await call_next(request)

        # Skip rate limiting for health check
        if request.url.path == "/api/health":
            return await call_next(request)

        # Get client identifier (IP address or user from auth)
        client_id = request.client.host if request.client else "unknown"

        try:
            await self.limiter.check_rate_limit(client_id, request.url.path)
            return await call_next(request)
        except RateLimitExceeded as exc:
            logger.warning(
                f"Rate limit exceeded for {client_id} on {request.url.path}",
                extra={"client_id": client_id, "endpoint": request.url.path}
            )
            return JSONResponse(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                content={"detail": str(exc), "error_code": "RATE_LIMIT_EXCEEDED"},
                headers={"Retry-After": "60"},
            )


class TimeoutMiddleware(BaseHTTPMiddleware):
    """Middleware to enforce request timeouts."""

    def __init__(self, app, timeout_seconds: int = 30):
        super().__init__(app)
        self.timeout_seconds = timeout_seconds

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        """Apply timeout to requests."""
        # Skip timeout for health check
        if request.url.path == "/api/health":
            return await call_next(request)

        try:
            return await asyncio.wait_for(
                call_next(request), timeout=self.timeout_seconds
            )
        except asyncio.TimeoutError:
            logger.error(
                f"Request timeout after {self.timeout_seconds}s: {request.method} {request.url.path}",
                extra={"endpoint": f"{request.method} {request.url.path}"}
            )
            return JSONResponse(
                status_code=status.HTTP_504_GATEWAY_TIMEOUT,
                content={
                    "detail": f"Request timeout after {self.timeout_seconds} seconds",
                    "error_code": "REQUEST_TIMEOUT",
                },
            )


class ErrorHandlingMiddleware(BaseHTTPMiddleware):
    """Middleware for global error handling."""

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        """Catch and format all unhandled exceptions."""
        try:
            return await call_next(request)
        except Exception as exc:
            # Log the error with full context
            logger.exception(
                f"Unhandled exception in {request.method} {request.url.path}",
                extra={
                    "endpoint": f"{request.method} {request.url.path}",
                    "exception_type": type(exc).__name__,
                },
            )

            # Return user-friendly error (no stack traces to client)
            return JSONResponse(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                content={
                    "detail": "An internal server error occurred",
                    "error_code": "INTERNAL_SERVER_ERROR",
                    # Include exception type in development mode only
                },
            )


def create_health_checker(db_check: Callable, settings) -> Callable:
    """Create a comprehensive health check function.

    Args:
        db_check: Function to check database health
        settings: Application settings

    Returns:
        Health check function
    """
    async def health_check() -> dict:
        """Comprehensive health check."""
        checks = {
            "status": "healthy",
            "checks": {}
        }

        # Database check
        try:
            db_healthy = await asyncio.get_event_loop().run_in_executor(None, db_check)
            checks["checks"]["database"] = {
                "status": "healthy" if db_healthy else "unhealthy",
                "backend": getattr(settings, "database_url", None) and "postgres" or "sqlite"
            }
        except Exception as exc:
            checks["checks"]["database"] = {
                "status": "unhealthy",
                "error": str(exc)
            }
            checks["status"] = "unhealthy"

        # Disk check (for photo storage)
        try:
            import shutil
            if settings.photo_dir:
                stat = shutil.disk_usage(str(settings.photo_dir.parent))
                free_gb = stat.free / (1024 ** 3)
                checks["checks"]["disk"] = {
                    "status": "healthy" if free_gb > 1.0 else "warning",
                    "free_gb": round(free_gb, 2)
                }
        except Exception as exc:
            checks["checks"]["disk"] = {
                "status": "unknown",
                "error": str(exc)
            }

        # Gemini API check (if enabled)
        if settings.gemini_enabled and settings.gemini_api_key:
            checks["checks"]["gemini_api"] = {
                "status": "configured"
            }

        return checks

    return health_check
