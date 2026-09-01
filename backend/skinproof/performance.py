"""Performance optimization utilities for production."""

from __future__ import annotations

import hashlib
import io
import json
import logging
import time
from typing import Any, Callable

from PIL import Image
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

logger = logging.getLogger(__name__)


class ImageCompressor:
    """Compress images before storage to reduce disk usage and improve performance."""

    @staticmethod
    def compress_image(
        image_bytes: bytes,
        max_dimension: int = 1024,
        quality: int = 85,
        format: str = "JPEG",
    ) -> bytes:
        """Compress an image by resizing and reducing quality.

        Args:
            image_bytes: Original image bytes
            max_dimension: Maximum width or height (default 1024px)
            quality: JPEG quality 0-100 (default 85)
            format: Output format (JPEG or PNG)

        Returns:
            Compressed image bytes
        """
        try:
            # Open image
            img = Image.open(io.BytesIO(image_bytes))

            # Convert RGBA to RGB if saving as JPEG
            if format.upper() == "JPEG" and img.mode in ("RGBA", "LA", "P"):
                # Create white background
                background = Image.new("RGB", img.size, (255, 255, 255))
                if img.mode == "P":
                    img = img.convert("RGBA")
                background.paste(img, mask=img.split()[-1] if img.mode == "RGBA" else None)
                img = background

            # Resize if needed
            original_size = img.size
            if max(img.size) > max_dimension:
                ratio = max_dimension / max(img.size)
                new_size = tuple(int(dim * ratio) for dim in img.size)
                img = img.resize(new_size, Image.Resampling.LANCZOS)
                logger.debug(f"Image resized from {original_size} to {new_size}")

            # Compress
            output = io.BytesIO()
            if format.upper() == "JPEG":
                img.save(output, format="JPEG", quality=quality, optimize=True)
            else:
                img.save(output, format=format, optimize=True)

            compressed_bytes = output.getvalue()

            # Log compression ratio
            original_size_kb = len(image_bytes) / 1024
            compressed_size_kb = len(compressed_bytes) / 1024
            compression_ratio = (1 - compressed_size_kb / original_size_kb) * 100

            logger.info(
                f"Image compressed: {original_size_kb:.1f}KB → {compressed_size_kb:.1f}KB "
                f"({compression_ratio:.1f}% reduction)"
            )

            return compressed_bytes

        except Exception as exc:
            logger.error(f"Image compression failed: {exc}")
            # Return original if compression fails
            return image_bytes


class RedisCache:
    """Redis-backed cache with fallback to in-memory cache."""

    def __init__(self, redis_url: str | None = None, default_ttl: int = 300):
        self.redis_url = redis_url
        self.redis_client = None
        self.memory_cache = {}  # Fallback memory cache
        self.cache_times = {}  # Track expiration for memory cache
        self.default_ttl = default_ttl

        if redis_url:
            try:
                import redis
                self.redis_client = redis.from_url(
                    redis_url,
                    decode_responses=True,
                    socket_connect_timeout=5,
                    socket_timeout=5,
                )
                self.redis_client.ping()
                logger.info("Redis cache initialized successfully")
            except (ImportError, Exception) as exc:
                logger.warning(
                    f"Redis unavailable, falling back to memory cache: {exc}"
                )
                self.redis_client = None

    def get(self, key: str) -> Any | None:
        """Get value from cache.

        Args:
            key: Cache key

        Returns:
            Cached value or None if not found
        """
        if self.redis_client:
            try:
                value = self.redis_client.get(key)
                if value:
                    return json.loads(value)
                return None
            except Exception as exc:
                logger.error(f"Redis cache get failed: {exc}")
                return None
        else:
            # Memory cache with expiration
            if key in self.memory_cache:
                if time.time() < self.cache_times.get(key, 0):
                    return self.memory_cache[key]
                else:
                    # Expired
                    del self.memory_cache[key]
                    del self.cache_times[key]
            return None

    def set(self, key: str, value: Any, ttl: int | None = None) -> bool:
        """Set value in cache.

        Args:
            key: Cache key
            value: Value to cache (must be JSON serializable)
            ttl: Time to live in seconds (default: use default_ttl)

        Returns:
            True if set successfully, False otherwise
        """
        ttl = ttl or self.default_ttl

        if self.redis_client:
            try:
                self.redis_client.setex(key, ttl, json.dumps(value))
                return True
            except Exception as exc:
                logger.error(f"Redis cache set failed: {exc}")
                return False
        else:
            # Memory cache
            self.memory_cache[key] = value
            self.cache_times[key] = time.time() + ttl
            return True

    def delete(self, key: str) -> bool:
        """Delete value from cache.

        Args:
            key: Cache key

        Returns:
            True if deleted, False otherwise
        """
        if self.redis_client:
            try:
                self.redis_client.delete(key)
                return True
            except Exception as exc:
                logger.error(f"Redis cache delete failed: {exc}")
                return False
        else:
            if key in self.memory_cache:
                del self.memory_cache[key]
                if key in self.cache_times:
                    del self.cache_times[key]
                return True
            return False

    def clear(self) -> bool:
        """Clear all cache entries.

        Returns:
            True if cleared successfully
        """
        if self.redis_client:
            try:
                self.redis_client.flushdb()
                return True
            except Exception as exc:
                logger.error(f"Redis cache clear failed: {exc}")
                return False
        else:
            self.memory_cache.clear()
            self.cache_times.clear()
            return True


class CacheMiddleware(BaseHTTPMiddleware):
    """Middleware to cache GET responses."""

    def __init__(self, app, cache: RedisCache, cacheable_paths: list[str] | None = None):
        super().__init__(app)
        self.cache = cache
        self.cacheable_paths = cacheable_paths or ["/api/dashboard", "/api/users/"]

    def _is_cacheable(self, path: str, method: str) -> bool:
        """Check if request is cacheable."""
        if method != "GET":
            return False
        return any(cacheable in path for cacheable in self.cacheable_paths)

    def _generate_cache_key(self, request: Request) -> str:
        """Generate cache key from request."""
        # Include user ID from auth header if available
        auth_header = request.headers.get("authorization", "")
        user_id = "anonymous"

        if auth_header and "." in auth_header:
            try:
                import base64
                token = auth_header.replace("Bearer ", "").strip()
                payload = token.split(".")[1]
                payload += "=" * (4 - len(payload) % 4)
                decoded = json.loads(base64.urlsafe_b64decode(payload))
                user_id = decoded.get("sub", "anonymous")
            except Exception:
                pass

        # Create cache key from path, query params, and user
        key_parts = [
            request.url.path,
            str(request.query_params),
            user_id,
        ]
        key_string = "|".join(key_parts)
        key_hash = hashlib.sha256(key_string.encode()).hexdigest()[:16]

        return f"cache:{key_hash}"

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        """Cache GET responses."""
        # Check if request is cacheable
        if not self._is_cacheable(request.url.path, request.method):
            return await call_next(request)

        # Try to get from cache
        cache_key = self._generate_cache_key(request)
        cached = self.cache.get(cache_key)

        if cached:
            logger.debug(f"Cache hit: {cache_key}")
            return Response(
                content=cached["content"],
                status_code=cached["status_code"],
                headers=dict(cached.get("headers", {})),
                media_type=cached.get("media_type"),
            )

        # Call endpoint
        response = await call_next(request)

        # Cache successful responses
        if response.status_code == 200:
            # Read response body
            body = b""
            async for chunk in response.body_iterator:
                body += chunk

            # Store in cache
            cache_data = {
                "content": body.decode("utf-8") if body else "",
                "status_code": response.status_code,
                "headers": dict(response.headers),
                "media_type": response.media_type,
            }
            self.cache.set(cache_key, cache_data)

            logger.debug(f"Cache miss, stored: {cache_key}")

            # Return new response with body
            return Response(
                content=body,
                status_code=response.status_code,
                headers=dict(response.headers),
                media_type=response.media_type,
            )

        return response


class RequestTimingMiddleware(BaseHTTPMiddleware):
    """Middleware to track request timing and slow endpoints."""

    def __init__(self, app, slow_threshold_ms: float = 1000.0):
        super().__init__(app)
        self.slow_threshold_ms = slow_threshold_ms

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        """Track request timing."""
        start_time = time.time()

        response = await call_next(request)

        duration_ms = (time.time() - start_time) * 1000

        # Add timing header
        response.headers["X-Response-Time"] = f"{duration_ms:.2f}ms"

        # Log slow requests
        if duration_ms > self.slow_threshold_ms:
            logger.warning(
                f"Slow request: {request.method} {request.url.path} took {duration_ms:.2f}ms",
                extra={
                    "endpoint": f"{request.method} {request.url.path}",
                    "duration_ms": duration_ms,
                    "threshold_ms": self.slow_threshold_ms,
                },
            )

        return response
