from __future__ import annotations

import json
import logging
import os
import secrets
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles

from .analytics import AnalyticsTracker
from .auth import AuthError, verify_id_token
from .complete_db import build_full_database
from .complete_service import CompleteGlowupAIService
from .config import Settings
from .logging_config import RequestLoggingMiddleware, setup_logging
from .middleware import (
    ErrorHandlingMiddleware,
    TimeoutMiddleware,
    create_health_checker,
)
from .monitoring import setup_sentry
from .observability import (
    MetricsCollector,
    MetricsMiddleware,
    instrument_fastapi,
    setup_opentelemetry,
)
from .performance import (
    CacheMiddleware,
    ImageCompressor,
    RedisCache,
    RequestTimingMiddleware,
)
from .photos import build_photo_store
from .rate_limiter import ProductionRateLimitMiddleware
from .routers import (
    setup_admin_router,
    setup_analytics_router,
    setup_captures_router,
    setup_subscriptions_router,
    setup_users_router,
)
from .shutdown import create_shutdown_handler

logger = logging.getLogger(__name__)


def setup_monitoring(
    app: FastAPI,
    active: CompleteGlowupAIService,
) -> tuple[AnalyticsTracker, MetricsCollector, RedisCache, ImageCompressor, str | None]:
    """Setup monitoring, analytics, and performance tools.

    Returns analytics, metrics_collector, cache, compressor, and redis_url.
    """
    # Setup Sentry error monitoring
    sentry_dsn = os.getenv("SENTRY_DSN", "").strip() or None
    sentry_environment = os.getenv("GLOWUPAI_ENV", "production")
    if sentry_dsn:
        setup_sentry(
            dsn=sentry_dsn,
            environment=sentry_environment,
            traces_sample_rate=float(os.getenv("SENTRY_TRACES_SAMPLE_RATE", "0.1")),
            profiles_sample_rate=float(os.getenv("SENTRY_PROFILES_SAMPLE_RATE", "0.1")),
        )
        logger.info("Sentry error monitoring enabled")

    # Initialize analytics tracker
    analytics = AnalyticsTracker(active.db)
    app.state.analytics = analytics

    # Initialize metrics collector
    metrics_collector = MetricsCollector()
    app.state.metrics = metrics_collector

    # Initialize Redis cache
    redis_url = (
        os.getenv("REDIS_URL", "").strip()
        or os.getenv("REDIS_PRIVATE_URL", "").strip()
        or None
    )
    cache = RedisCache(redis_url, default_ttl=300)  # 5-minute TTL
    app.state.cache = cache

    # Initialize image compressor
    compressor = ImageCompressor()
    app.state.compressor = compressor

    # Setup OpenTelemetry (if enabled)
    otel_enabled = os.getenv("OTEL_ENABLED", "0") == "1"
    otel_config = setup_opentelemetry("glowupai", enabled=otel_enabled)
    if otel_config:
        instrument_fastapi(app, otel_config)

    return analytics, metrics_collector, cache, compressor, redis_url


def setup_cors(app: FastAPI, settings: Settings) -> None:
    """Configure CORS middleware.

    Security: allow_credentials=False because we use Bearer token auth (not cookies).
    This prevents CSRF attacks. Explicit headers list instead of wildcard.
    """
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.allowed_origins,
        allow_credentials=False,  # Bearer tokens don't require credentials
        allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH"],
        allow_headers=["Authorization", "Content-Type"],  # Explicit headers only
    )


def configure_error_handlers(app: FastAPI) -> None:
    """Configure error handling middleware."""
    app.add_middleware(ErrorHandlingMiddleware)


def create_middleware_stack(
    app: FastAPI,
    metrics_collector: MetricsCollector,
    cache: RedisCache,
    redis_url: str | None,
) -> None:
    """Add middleware stack in correct order.

    Middleware is added in reverse order (last added = first executed).
    """
    # 1. Request logging (log all requests)
    app.add_middleware(RequestLoggingMiddleware)

    # 2. Request timing (track slow endpoints)
    slow_threshold_ms = float(os.getenv("GLOWUPAI_SLOW_THRESHOLD_MS", "1000"))
    app.add_middleware(RequestTimingMiddleware, slow_threshold_ms=slow_threshold_ms)

    # 3. Metrics collection
    app.add_middleware(MetricsMiddleware, collector=metrics_collector)

    # 4. Response caching (for dashboard and other GET endpoints)
    cache_enabled = os.getenv("GLOWUPAI_CACHE_ENABLED", "1") == "1"
    if cache_enabled:
        app.add_middleware(CacheMiddleware, cache=cache)

    # 5. Rate limiting (Redis-backed)
    rate_limit_enabled = os.getenv("GLOWUPAI_RATE_LIMIT_ENABLED", "1") == "1"
    app.add_middleware(
        ProductionRateLimitMiddleware, redis_url=redis_url, enabled=rate_limit_enabled
    )

    # 6. Request timeout (innermost, closest to handlers)
    timeout_seconds = int(os.getenv("GLOWUPAI_REQUEST_TIMEOUT", "30"))
    app.add_middleware(TimeoutMiddleware, timeout_seconds=timeout_seconds)

    logger.info(
        f"Middleware configured: rate_limit={rate_limit_enabled}, cache={cache_enabled}, "
        f"timeout={timeout_seconds}s, slow_threshold={slow_threshold_ms}ms",
    )


def register_routes(
    app: FastAPI,
    active: CompleteGlowupAIService,
    settings: Settings,
    analytics: AnalyticsTracker,
    metrics_collector: MetricsCollector,
    compressor: ImageCompressor,
    run,
    _require_owner,
    _require_admin,
) -> None:
    """Register all API routes via routers.

    Args:
        app: FastAPI application instance
        active: Service instance
        settings: Application settings
        analytics: Analytics tracker
        metrics_collector: Metrics collector
        compressor: Image compressor
        run: Helper function for running service methods with error handling
        _require_owner: Auth helper for owner verification
        _require_admin: Auth helper for admin verification
    """

    # Health check endpoint (kept in main file)
    @app.get("/api/health")
    async def health():
        """Enhanced health check with database, disk, and dependency checks."""
        health_checker = create_health_checker(active.db, settings)
        try:
            health_status = await health_checker()

            # Add version and feature info
            health_status["version"] = "3.0.0"
            health_status["scope"] = "cosmetic_tracking"
            health_status["features"] = [
                "experiments",
                "qna",
                "discover",
                "commerce",
                "reprocessing",
                "shelf_scan",
                "product_prediction",
                "root_cause_search",
                "budget_optimizer",
                "derm_export",
            ]

            # Return 503 if unhealthy, 200 if healthy
            status_code = 200 if health_status["status"] == "healthy" else 503
            return JSONResponse(content=health_status, status_code=status_code)

        except (OSError, RuntimeError) as exc:
            logger.exception("Health check failed: %s", exc)
            return JSONResponse(
                content={
                    "status": "unhealthy",
                    "error": "Health check failed",
                    "version": "3.0.0",
                },
                status_code=503,
            )

    # Register routers
    users_router = setup_users_router(active, analytics, run, _require_owner)
    app.include_router(users_router)
    logger.info("Users router registered")

    captures_router = setup_captures_router(
        active, analytics, compressor, run, _require_owner
    )
    app.include_router(captures_router)
    logger.info("Captures router registered")

    analytics_router = setup_analytics_router(active, run, _require_owner)
    app.include_router(analytics_router)
    logger.info("Analytics router registered")

    subscriptions_router = setup_subscriptions_router(active, run, _require_owner)
    app.include_router(subscriptions_router)
    logger.info("Subscriptions router registered")

    admin_router = setup_admin_router(
        active, analytics, metrics_collector, run, _require_admin
    )
    app.include_router(admin_router)
    logger.info("Admin router registered")

    # Static files
    static_dir = Path(__file__).parent / "static"
    app.mount("/assets", StaticFiles(directory=static_dir), name="assets")
    static_file = static_dir / "index.html"

    @app.get("/", include_in_schema=False)
    def index():
        return FileResponse(static_file)


def create_complete_app(service: CompleteGlowupAIService | None = None) -> FastAPI:
    """Create and configure the complete FastAPI application.

    Args:
        service: Optional pre-configured service instance (for testing)

    Returns:
        Configured FastAPI application instance
    """
    # Setup structured logging
    log_level = os.getenv("GLOWUPAI_LOG_LEVEL", "INFO")
    use_json_logs = os.getenv("GLOWUPAI_JSON_LOGS", "1") == "1"
    setup_logging(log_level, use_json_logs)
    logger.info("Initializing GlowupAI application")

    # Initialize settings and service
    if service:
        # Use the provided service and its settings (for testing)
        active = service
        settings = service.settings
    else:
        # Production: create service from environment settings
        settings = Settings.from_env()
        settings.prepare()
        active = CompleteGlowupAIService(
            build_full_database(settings),
            settings,
            build_photo_store(settings.photo_dir),
        )

    # Create FastAPI app
    app = FastAPI(
        title="GlowUpAI",
        version="3.0.0",
        description="A complete personal appearance measurement system. Cosmetic tracking, never diagnosis.",
    )
    app.state.glowupai = active

    # Setup monitoring and performance tools
    analytics, metrics_collector, cache, compressor, redis_url = setup_monitoring(
        app, active
    )

    # Setup graceful shutdown
    def cleanup_db():
        """Close database connections on shutdown."""
        logger.info("Closing database connections")
        try:
            active.db.close()
        except (OSError, RuntimeError) as exc:
            logger.error(f"Error closing database: {exc}")

    shutdown_handler = create_shutdown_handler(app, cleanup_db)
    shutdown_handler.setup_signal_handlers()

    # Configure middleware (order matters: last added = first executed)
    setup_cors(app, settings)
    configure_error_handlers(app)
    create_middleware_stack(app, metrics_collector, cache, redis_url)

    # Helper function for running service methods with error handling
    def run(callable_, *args, **kwargs):
        try:
            return callable_(*args, **kwargs)
        except PermissionError as exc:
            raise HTTPException(status_code=403, detail=str(exc)) from exc
        except ValueError as exc:
            detail = str(exc)
            try:
                detail = json.loads(detail)
            except json.JSONDecodeError:
                pass
            raise HTTPException(status_code=400, detail=detail) from exc

    # Authentication helper functions
    def _bearer_identity(authorization: str | None):
        if not authorization or not authorization.lower().startswith("bearer "):
            raise HTTPException(status_code=401, detail="missing bearer token")
        token = authorization.split(" ", 1)[1].strip()
        try:
            return verify_id_token(token, active.settings.firebase_project_id)
        except AuthError as exc:
            raise HTTPException(status_code=401, detail=str(exc)) from exc

    def _require_owner(user_id: str, authorization: str | None) -> None:
        """No-op unless GLOWUPAI_AUTH_REQUIRED is set; then 401/403 on mismatch."""
        if not active.settings.auth_required:
            return
        identity = _bearer_identity(authorization)
        row = active.db.fetchone(
            "SELECT id FROM users WHERE firebase_uid = ?",
            (identity.uid,),
        )
        if not row or row["id"] != user_id:
            raise HTTPException(
                status_code=403,
                detail="the authenticated account does not own this user_id",
            )

    def _require_admin(authorization: str | None) -> None:
        if not active.settings.admin_token:
            raise HTTPException(
                status_code=403,
                detail="admin endpoints are disabled: GLOWUPAI_ADMIN_TOKEN is not configured",
            )
        if not authorization or not authorization.lower().startswith("bearer "):
            raise HTTPException(status_code=403, detail="missing admin bearer token")
        token = authorization.split(" ", 1)[1].strip()
        if not secrets.compare_digest(token, active.settings.admin_token):
            raise HTTPException(status_code=403, detail="invalid admin token")

    # Register all routes
    register_routes(
        app,
        active,
        settings,
        analytics,
        metrics_collector,
        compressor,
        run,
        _require_owner,
        _require_admin,
    )

    return app


app = create_complete_app()
