"""Structured logging configuration with request ID tracking."""

from __future__ import annotations

import json
import logging
import sys
import time
import uuid
from contextvars import ContextVar
from typing import Any

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

# Context variable to track request ID across async boundaries
request_id_ctx: ContextVar[str] = ContextVar("request_id", default="")


class StructuredFormatter(logging.Formatter):
    """JSON formatter for structured logging."""

    def format(self, record: logging.LogRecord) -> str:
        """Format log record as JSON with request context."""
        log_data = {
            "timestamp": self.formatTime(record),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "request_id": request_id_ctx.get("") or None,
        }

        # Add exception info if present
        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        # Add extra fields
        if hasattr(record, "user_id"):
            log_data["user_id"] = record.user_id
        if hasattr(record, "endpoint"):
            log_data["endpoint"] = record.endpoint
        if hasattr(record, "duration_ms"):
            log_data["duration_ms"] = record.duration_ms
        if hasattr(record, "status_code"):
            log_data["status_code"] = record.status_code

        return json.dumps(log_data)


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Middleware to log requests with timing and request IDs."""

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        """Process request with logging."""
        # Generate and set request ID
        req_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
        request_id_ctx.set(req_id)

        # Start timing
        start_time = time.time()

        # Log request
        logger = logging.getLogger("skinproof.access")
        logger.info(
            f"{request.method} {request.url.path}",
            extra={
                "endpoint": f"{request.method} {request.url.path}",
                "client_ip": request.client.host if request.client else None,
            },
        )

        # Process request
        try:
            response = await call_next(request)
            duration_ms = round((time.time() - start_time) * 1000, 2)

            # Log response
            logger.info(
                f"Response {response.status_code}",
                extra={
                    "status_code": response.status_code,
                    "duration_ms": duration_ms,
                    "endpoint": f"{request.method} {request.url.path}",
                },
            )

            # Add request ID to response headers
            response.headers["X-Request-ID"] = req_id
            return response

        except Exception as exc:
            duration_ms = round((time.time() - start_time) * 1000, 2)
            logger.exception(
                f"Request failed: {exc}",
                extra={
                    "duration_ms": duration_ms,
                    "endpoint": f"{request.method} {request.url.path}",
                },
            )
            raise


def setup_logging(log_level: str = "INFO", use_json: bool = True) -> None:
    """Configure application logging.

    Args:
        log_level: Logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
        use_json: Whether to use JSON structured logging (True for production)
    """
    level = getattr(logging, log_level.upper(), logging.INFO)

    # Configure root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(level)

    # Remove existing handlers
    root_logger.handlers.clear()

    # Create console handler
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(level)

    # Set formatter
    if use_json:
        formatter = StructuredFormatter()
    else:
        formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        )

    handler.setFormatter(formatter)
    root_logger.addHandler(handler)

    # Set specific logger levels
    logging.getLogger("skinproof").setLevel(level)
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)  # Reduce noise
    logging.getLogger("uvicorn.error").setLevel(logging.INFO)


def get_logger(name: str) -> logging.Logger:
    """Get a logger instance.

    Args:
        name: Logger name (typically __name__)

    Returns:
        Configured logger instance
    """
    return logging.getLogger(name)
