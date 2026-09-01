"""OpenTelemetry instrumentation and metrics for production observability."""

from __future__ import annotations

import logging
import time
from collections.abc import Callable

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

logger = logging.getLogger(__name__)


class MetricsCollector:
    """In-memory metrics collector for basic observability.

    For production, replace with OpenTelemetry + external backend (Prometheus, DataDog, etc).
    This provides a lightweight fallback for environments without full observability setup.
    """

    def __init__(self):
        self.metrics = {
            "request_count": 0,
            "error_count": 0,
            "request_duration_sum": 0.0,
            "request_duration_count": 0,
            "endpoint_counts": {},
            "status_code_counts": {},
        }

    def record_request(
        self, method: str, path: str, status_code: int, duration_ms: float,
    ):
        """Record a request metric."""
        self.metrics["request_count"] += 1
        self.metrics["request_duration_sum"] += duration_ms
        self.metrics["request_duration_count"] += 1

        # Track by endpoint
        endpoint_key = f"{method} {path}"
        self.metrics["endpoint_counts"][endpoint_key] = (
            self.metrics["endpoint_counts"].get(endpoint_key, 0) + 1
        )

        # Track by status code
        self.metrics["status_code_counts"][status_code] = (
            self.metrics["status_code_counts"].get(status_code, 0) + 1
        )

        # Track errors (4xx and 5xx)
        if status_code >= 400:
            self.metrics["error_count"] += 1

    def get_metrics(self) -> dict:
        """Get current metrics summary."""
        avg_duration = 0.0
        if self.metrics["request_duration_count"] > 0:
            avg_duration = (
                self.metrics["request_duration_sum"]
                / self.metrics["request_duration_count"]
            )

        error_rate = 0.0
        if self.metrics["request_count"] > 0:
            error_rate = self.metrics["error_count"] / self.metrics["request_count"]

        return {
            "requests": self.metrics["request_count"],
            "errors": self.metrics["error_count"],
            "error_rate": round(error_rate * 100, 2),
            "avg_duration_ms": round(avg_duration, 2),
            "top_endpoints": sorted(
                self.metrics["endpoint_counts"].items(),
                key=lambda x: x[1],
                reverse=True,
            )[:10],
            "status_codes": self.metrics["status_code_counts"],
        }

    def reset(self):
        """Reset all metrics (useful for testing)."""
        self.__init__()


class MetricsMiddleware(BaseHTTPMiddleware):
    """Middleware to collect request metrics."""

    def __init__(self, app, collector: MetricsCollector):
        super().__init__(app)
        self.collector = collector

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint,
    ) -> Response:
        """Collect metrics for each request."""
        start_time = time.time()

        try:
            response = await call_next(request)
            duration_ms = (time.time() - start_time) * 1000

            # Record metrics
            self.collector.record_request(
                request.method, request.url.path, response.status_code, duration_ms,
            )

            return response

        except (RuntimeError, ValueError, OSError, IOError) as exc:
            duration_ms = (time.time() - start_time) * 1000
            # Record as 500 error
            logger.error(f"Request processing failed: {exc}")
            self.collector.record_request(
                request.method, request.url.path, 500, duration_ms,
            )
            raise


def setup_opentelemetry(
    service_name: str = "glowupai", enabled: bool = False,
) -> dict | None:
    """Setup OpenTelemetry instrumentation.

    This is a placeholder for full OpenTelemetry setup. To enable:
    1. Install: pip install opentelemetry-api opentelemetry-sdk opentelemetry-instrumentation-fastapi
    2. Set environment variables:
       - OTEL_EXPORTER_OTLP_ENDPOINT (e.g., http://collector:4317)
       - OTEL_SERVICE_NAME=glowupai
    3. Set enabled=True

    Args:
        service_name: Service name for traces
        enabled: Whether to enable OpenTelemetry

    Returns:
        OpenTelemetry configuration dict or None if disabled
    """
    if not enabled:
        logger.info(
            "OpenTelemetry instrumentation disabled (set OTEL_ENABLED=1 to enable)",
        )
        return None

    try:
        # Import OpenTelemetry modules
        from opentelemetry import trace
        from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import (
            OTLPSpanExporter,
        )
        from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
        from opentelemetry.sdk.resources import Resource
        from opentelemetry.sdk.trace import TracerProvider
        from opentelemetry.sdk.trace.export import BatchSpanProcessor

        # Create resource
        resource = Resource.create({"service.name": service_name})

        # Setup tracer provider
        provider = TracerProvider(resource=resource)
        trace.set_tracer_provider(provider)

        # Setup OTLP exporter
        otlp_exporter = OTLPSpanExporter()
        span_processor = BatchSpanProcessor(otlp_exporter)
        provider.add_span_processor(span_processor)

        logger.info(f"OpenTelemetry initialized for service: {service_name}")

        return {
            "provider": provider,
            "instrumentor": FastAPIInstrumentor,
        }

    except ImportError:
        logger.warning(
            "OpenTelemetry packages not installed. "
            "Install with: pip install opentelemetry-api opentelemetry-sdk "
            "opentelemetry-instrumentation-fastapi opentelemetry-exporter-otlp",
        )
        return None
    except (RuntimeError, ValueError, OSError) as exc:
        logger.error(f"Failed to initialize OpenTelemetry: {exc}")
        return None


def instrument_fastapi(app, otel_config: dict | None):
    """Instrument FastAPI app with OpenTelemetry.

    Args:
        app: FastAPI application instance
        otel_config: OpenTelemetry configuration from setup_opentelemetry
    """
    if otel_config and "instrumentor" in otel_config:
        try:
            instrumentor = otel_config["instrumentor"]
            instrumentor.instrument_app(app)
            logger.info("FastAPI instrumented with OpenTelemetry")
        except (RuntimeError, ValueError, AttributeError) as exc:
            logger.error(f"Failed to instrument FastAPI: {exc}")
