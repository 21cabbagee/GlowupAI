"""OpenTelemetry instrumentation and metrics for production observability."""

from __future__ import annotations

import logging
import math
import re
import time
from collections import deque
from threading import RLock
from typing import Any

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

logger = logging.getLogger(__name__)


class MetricsCollector:
    """In-memory metrics collector for basic observability.

    For production, replace with OpenTelemetry + external backend (Prometheus, DataDog, etc).
    This provides a lightweight fallback for environments without full observability setup.
    """

    def __init__(self, rolling_window_seconds: int = 300):
        self.rolling_window_seconds = max(60, rolling_window_seconds)
        self._lock = RLock()
        self._recent: deque[tuple[float, int, float]] = deque(maxlen=20_000)
        self.metrics: dict[str, Any] = {
            "request_count": 0,
            "error_count": 0,
            "request_duration_sum": 0.0,
            "request_duration_count": 0,
            "endpoint_counts": {},
            "status_code_counts": {},
        }

    def record_request(
        self,
        method: str,
        path: str,
        status_code: int,
        duration_ms: float,
    ):
        """Record a request metric."""
        with self._lock:
            self.metrics["request_count"] += 1
            self.metrics["request_duration_sum"] += duration_ms
            self.metrics["request_duration_count"] += 1
            self._recent.append((time.time(), status_code, duration_ms))

            # Track by endpoint
            endpoint_key = f"{method} {_metric_path(path)}"
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
        with self._lock:
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
                "status_codes": dict(self.metrics["status_code_counts"]),
                "rolling_window": self.window_snapshot(),
            }

    def window_snapshot(self, now: float | None = None) -> dict[str, Any]:
        """Return bounded five-minute metrics for alert evaluation."""
        with self._lock:
            current = now if now is not None else time.time()
            cutoff = current - self.rolling_window_seconds
            while self._recent and self._recent[0][0] < cutoff:
                self._recent.popleft()
            events = list(self._recent)

        durations = sorted(event[2] for event in events)
        requests = len(events)
        server_errors = sum(1 for event in events if event[1] >= 500)
        p95 = 0.0
        if durations:
            p95 = durations[
                min(len(durations) - 1, math.ceil(len(durations) * 0.95) - 1)
            ]
        return {
            "window_seconds": self.rolling_window_seconds,
            "requests": requests,
            "server_errors": server_errors,
            "server_error_rate": server_errors / requests if requests else 0.0,
            "p95_duration_ms": round(p95, 2),
        }

    def prometheus(self) -> str:
        """Render a vendor-neutral Prometheus text exposition."""
        with self._lock:
            request_count = int(self.metrics["request_count"])
            error_count = int(self.metrics["error_count"])
            duration_sum = float(self.metrics["request_duration_sum"])
            duration_count = int(self.metrics["request_duration_count"])
            status_codes = dict(self.metrics["status_code_counts"])
        lines = [
            "# HELP glowupai_http_requests_total Total HTTP requests handled.",
            "# TYPE glowupai_http_requests_total counter",
            f"glowupai_http_requests_total {request_count}",
            "# HELP glowupai_http_errors_total Total HTTP 4xx and 5xx responses.",
            "# TYPE glowupai_http_errors_total counter",
            f"glowupai_http_errors_total {error_count}",
            "# HELP glowupai_http_request_duration_ms_sum Total request duration in milliseconds.",
            "# TYPE glowupai_http_request_duration_ms_sum counter",
            f"glowupai_http_request_duration_ms_sum {duration_sum:.3f}",
            "# HELP glowupai_http_request_duration_ms_count Number of observed request durations.",
            "# TYPE glowupai_http_request_duration_ms_count counter",
            f"glowupai_http_request_duration_ms_count {duration_count}",
            "# HELP glowupai_http_responses_total HTTP responses by status code.",
            "# TYPE glowupai_http_responses_total counter",
        ]
        for code, count in sorted(status_codes.items(), key=lambda item: str(item[0])):
            lines.append(
                f'glowupai_http_responses_total{{status_code="{_escape_label(str(code))}"}} {count}'
            )
        rolling = self.window_snapshot()
        lines.extend(
            [
                "# HELP glowupai_http_window_server_error_rate Five-minute HTTP 5xx rate.",
                "# TYPE glowupai_http_window_server_error_rate gauge",
                f"glowupai_http_window_server_error_rate {rolling['server_error_rate']:.6f}",
                "# HELP glowupai_http_window_p95_duration_ms Five-minute HTTP p95 duration.",
                "# TYPE glowupai_http_window_p95_duration_ms gauge",
                f"glowupai_http_window_p95_duration_ms {rolling['p95_duration_ms']:.3f}",
            ]
        )
        return "\n".join(lines) + "\n"

    def reset(self):
        """Reset all metrics (useful for testing)."""
        with self._lock:
            self.metrics = {
                "request_count": 0,
                "error_count": 0,
                "request_duration_sum": 0.0,
                "request_duration_count": 0,
                "endpoint_counts": {},
                "status_code_counts": {},
            }
            self._recent.clear()


class MetricsMiddleware(BaseHTTPMiddleware):
    """Middleware to collect request metrics."""

    def __init__(self, app, collector: MetricsCollector, alert_manager=None):
        super().__init__(app)
        self.collector = collector
        self.alert_manager = alert_manager

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint,
    ) -> Response:
        """Collect metrics for each request."""
        start_time = time.time()

        try:
            response = await call_next(request)
            duration_ms = (time.time() - start_time) * 1000

            # Record metrics
            self.collector.record_request(
                request.method,
                request.url.path,
                response.status_code,
                duration_ms,
            )

            if self.alert_manager:
                # Delivery is offloaded so an unavailable webhook cannot add
                # latency to a user request. The manager deduplicates alerts.
                import asyncio

                asyncio.create_task(
                    self.alert_manager.observe_async(self.collector.window_snapshot())
                )

            return response

        except Exception as exc:  # noqa: BLE001 - record every unhandled 5xx
            duration_ms = (time.time() - start_time) * 1000
            # Record as 500 error
            logger.error(f"Request processing failed: {exc}")
            self.collector.record_request(
                request.method,
                request.url.path,
                500,
                duration_ms,
            )
            if self.alert_manager:
                import asyncio

                asyncio.create_task(
                    self.alert_manager.observe_async(self.collector.window_snapshot())
                )
            raise


def setup_opentelemetry(
    service_name: str = "glowupai",
    enabled: bool = False,
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


def _escape_label(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


def _metric_path(path: str) -> str:
    """Avoid putting owner/resource IDs into in-process operator metrics."""
    path = re.sub(
        r"/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}(?=/|$)",
        "/:id",
        path,
        flags=re.IGNORECASE,
    )
    return re.sub(r"/[^/]{20,}(?=/|$)", "/:id", path)
