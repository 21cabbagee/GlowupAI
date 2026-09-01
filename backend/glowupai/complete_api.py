from __future__ import annotations

import asyncio
import base64
import binascii
import json
import logging
import os
import secrets
from pathlib import Path

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

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
from .shutdown import create_shutdown_handler

logger = logging.getLogger(__name__)


class UserCreate(BaseModel):
    skin_type: str | None = None


class ConsentCreate(BaseModel):
    facial_data: bool
    policy_version: str | None = None


class ProductCreate(BaseModel):
    name: str = Field(min_length=1, max_length=160)
    barcode: str | None = None
    category: str = "other"
    ingredients: list[str] | str | None = None
    stabilization_days: int = Field(default=14, ge=0, le=180)


class RoutineEventCreate(BaseModel):
    user_id: str
    product_id: str
    action: str
    timestamp: str | None = None
    slot: str = "unspecified"
    dose: str | None = None
    frequency: str | None = None
    notes: str | None = None
    experiment_id: str | None = None


class CaptureCreate(BaseModel):
    user_id: str
    image_base64: str
    quality: dict | None = None
    captured_at: str | None = None
    device_meta: dict | None = None
    is_baseline: bool = False
    vertical: str = "skin"
    experiment_id: str | None = None


class ExperimentCreate(BaseModel):
    user_id: str
    name: str = Field(min_length=1, max_length=160)
    hypothesis: str | None = None
    product_id: str
    primary_metric: str = "redness_score"
    target_days: int = Field(default=14, ge=1, le=180)


class ExperimentStatus(BaseModel):
    user_id: str
    status: str


class QnaCreate(BaseModel):
    question: str = Field(min_length=1, max_length=2000)
    thread_id: str | None = None


class UpgradeCreate(BaseModel):
    source: str = "local_checkout"


class EngagementCreate(BaseModel):
    event_type: str
    reference_id: str | None = None
    metadata: dict | None = None


class OfferCreate(BaseModel):
    product_id: str
    merchant: str
    url: str
    price_cents: int | None = Field(default=None, ge=0)
    currency: str = "USD"


class LabelCreate(BaseModel):
    photo_id: str
    label_type: str
    value: str
    confidence: float | None = Field(default=None, ge=0, le=1)
    notes: str | None = None


class ReprocessCreate(BaseModel):
    model_version: str = Field(min_length=1, max_length=80)


class TriageCreate(BaseModel):
    text: str = Field(min_length=1, max_length=2000)


class ExperienceProfileUpdate(BaseModel):
    display_name: str | None = Field(default=None, max_length=80)
    skin_type: str | None = Field(default=None, max_length=40)
    focus_vertical: str | None = None
    goals: list[str] | None = None
    experience_level: str | None = Field(default=None, max_length=80)
    onboarding_complete: bool | None = None


class ShelfScanCreate(BaseModel):
    image_base64: str


class ShelfScanConfirm(BaseModel):
    selections: list[dict]


class ContextEventCreate(BaseModel):
    event_type: str
    value: str | None = None
    occurred_at: str | None = None
    notes: str | None = None


class CheckInCreate(BaseModel):
    routine_state: str = Field(
        default="steady", pattern="^(steady|changed|missed|not_sure)$",
    )
    skin_feel: str = Field(default="not_sure", pattern="^(better|same|worse|not_sure)$")
    note: str | None = Field(default=None, max_length=400)
    occurred_at: str | None = None


class MeasurementFeedbackCreate(BaseModel):
    capture_id: str
    agreement: str = Field(pattern="^(fair|uncertain|off)$")
    note: str | None = Field(default=None, max_length=400)


class PurchaseGuidanceCreate(BaseModel):
    name: str | None = Field(default=None, max_length=160)
    barcode: str | None = Field(default=None, max_length=80)
    category: str = Field(default="other", max_length=40)
    ingredients: list[str] | str | None = None
    price_cents: int | None = Field(default=None, ge=0)
    currency: str = Field(default="INR", min_length=3, max_length=3)


def create_complete_app(service: CompleteGlowupAIService | None = None) -> FastAPI:
    # Setup structured logging
    log_level = os.getenv("GLOWUPAI_LOG_LEVEL", "INFO")
    use_json_logs = os.getenv("GLOWUPAI_JSON_LOGS", "1") == "1"
    setup_logging(log_level, use_json_logs)
    logger.info("Initializing SkinProof application")

    settings = Settings.from_env()
    settings.prepare()
    active = service or CompleteGlowupAIService(
        build_full_database(settings), settings, build_photo_store(settings.photo_dir),
    )

    app = FastAPI(
        title="SkinProof",
        version="3.0.0",
        description="A complete personal appearance measurement system. Cosmetic tracking, never diagnosis.",
    )
    app.state.skinproof = active

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
    redis_url = os.getenv("REDIS_URL", "").strip() or os.getenv("REDIS_PRIVATE_URL", "").strip() or None
    cache = RedisCache(redis_url, default_ttl=300)  # 5-minute TTL
    app.state.cache = cache

    # Initialize image compressor
    compressor = ImageCompressor()
    app.state.compressor = compressor

    # Setup OpenTelemetry (if enabled)
    otel_enabled = os.getenv("OTEL_ENABLED", "0") == "1"
    otel_config = setup_opentelemetry("skinproof", enabled=otel_enabled)
    if otel_config:
        instrument_fastapi(app, otel_config)

    # Setup graceful shutdown
    def cleanup_db():
        """Close database connections on shutdown."""
        logger.info("Closing database connections")
        try:
            active.db.close()
        except Exception as exc:
            logger.error(f"Error closing database: {exc}")

    shutdown_handler = create_shutdown_handler(app, cleanup_db)
    shutdown_handler.setup_signal_handlers()

    # Add middleware in correct order (last added = first executed)
    # 1. CORS (outermost)
    # Security: allow_credentials=False because we use Bearer token auth (not cookies)
    # This prevents CSRF attacks. Explicit headers list instead of wildcard.
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.allowed_origins,
        allow_credentials=False,  # Bearer tokens don't require credentials
        allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH"],
        allow_headers=["Authorization", "Content-Type"],  # Explicit headers only
    )

    # 2. Error handling (catch all errors)
    app.add_middleware(ErrorHandlingMiddleware)

    # 3. Request logging (log all requests)
    app.add_middleware(RequestLoggingMiddleware)

    # 4. Request timing (track slow endpoints)
    slow_threshold_ms = float(os.getenv("GLOWUPAI_SLOW_THRESHOLD_MS", "1000"))
    app.add_middleware(RequestTimingMiddleware, slow_threshold_ms=slow_threshold_ms)

    # 5. Metrics collection
    app.add_middleware(MetricsMiddleware, collector=metrics_collector)

    # 6. Response caching (for dashboard and other GET endpoints)
    cache_enabled = os.getenv("GLOWUPAI_CACHE_ENABLED", "1") == "1"
    if cache_enabled:
        app.add_middleware(CacheMiddleware, cache=cache)

    # 7. Rate limiting (Redis-backed)
    rate_limit_enabled = os.getenv("GLOWUPAI_RATE_LIMIT_ENABLED", "1") == "1"
    app.add_middleware(ProductionRateLimitMiddleware, redis_url=redis_url, enabled=rate_limit_enabled)

    # 8. Request timeout (innermost, closest to handlers)
    timeout_seconds = int(os.getenv("GLOWUPAI_REQUEST_TIMEOUT", "30"))
    app.add_middleware(TimeoutMiddleware, timeout_seconds=timeout_seconds)

    logger.info(
        f"Middleware configured: rate_limit={rate_limit_enabled}, cache={cache_enabled}, "
        f"timeout={timeout_seconds}s, slow_threshold={slow_threshold_ms}ms",
    )

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

    # -- auth boundary --------------------------------------------------------
    #
    # When `GLOWUPAI_AUTH_REQUIRED` is off (the default), `_require_owner` is
    # a no-op and every route behaves exactly as it did before this module
    # existed. That default is deliberate: the existing test suite and the
    # unauthenticated static/web client carry no bearer token and must keep
    # working unchanged. Flip the flag on only once a deployment has a real
    # Firebase project configured and every client sends a token.

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
            "SELECT id FROM users WHERE firebase_uid = ?", (identity.uid,),
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

    @app.get("/api/health")
    async def health():
        """Enhanced health check with database, disk, and dependency checks."""
        health_checker = create_health_checker(active.db.healthcheck, settings)
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

        except Exception as exc:
            logger.exception("Health check failed")
            return JSONResponse(
                content={
                    "status": "unhealthy",
                    "error": "Health check failed",
                    "version": "3.0.0",
                },
                status_code=503,
            )

    @app.get("/api/metrics", tags=["monitoring"])
    def metrics(authorization: str | None = Header(default=None)):
        """Get application metrics (admin only)."""
        _require_admin(authorization)
        return metrics_collector.get_metrics()

    @app.post("/api/users")
    def create_user(payload: UserCreate):
        return active.create_user(payload.skin_type)

    @app.post("/api/auth/session")
    def auth_session(authorization: str | None = Header(default=None)):
        identity = _bearer_identity(authorization)
        result = run(
            active.session_for_identity,
            identity.uid,
            identity.email,
            identity.email_verified,
            identity.name,
        )

        # Track user signup if new user
        if result.get("created"):
            analytics.track_user_signup(
                user_id=result["user_id"],
                method="google" if identity.email else "anonymous",
                email=identity.email,
            )

        return result

    @app.get("/api/users/{user_id}/profile")
    def profile(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.profile, user_id)

    @app.patch("/api/users/{user_id}/profile", tags=["profile"])
    def update_experience_profile(
        user_id: str,
        payload: ExperienceProfileUpdate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.update_profile, user_id, **payload.model_dump())

    @app.post("/api/users/{user_id}/consent")
    def consent(
        user_id: str,
        payload: ConsentCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(
            active.grant_consent, user_id, payload.facial_data, payload.policy_version,
        )

    @app.get("/api/users/{user_id}/subscription")
    def subscription(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.entitlement, user_id)

    @app.post("/api/users/{user_id}/subscription/upgrade")
    def upgrade(
        user_id: str,
        payload: UpgradeCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.upgrade, user_id, payload.source)

    @app.post("/api/users/{user_id}/subscription/cancel")
    def cancel_subscription(
        user_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.downgrade, user_id)

    @app.post("/api/products")
    def create_product(payload: ProductCreate):
        return run(
            active.create_product,
            payload.name,
            payload.barcode,
            payload.category,
            payload.ingredients,
            payload.stabilization_days,
        )

    @app.get("/api/products/search")
    def search_products(q: str = ""):
        return active.search_products(q)

    @app.get("/api/products/lookup")
    def lookup_product(barcode: str):
        return run(active.lookup_product, barcode)

    @app.get("/api/products/{product_id}")
    def product_detail(
        product_id: str, user_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.product_detail, user_id, product_id)

    @app.get("/api/products/{product_id}/ingredient-explainer")
    def ingredient_explainer(
        product_id: str, user_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.ingredient_explainer, user_id, product_id)

    @app.get("/api/products/{product_id}/predict")
    def predict_product(
        product_id: str, user_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.predict_product, user_id, product_id)

    @app.post("/api/users/{user_id}/purchase-guidance")
    def purchase_guidance(
        user_id: str,
        payload: PurchaseGuidanceCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.purchase_guidance, user_id, **payload.model_dump())

    @app.post("/api/routine-events")
    def routine_event(
        payload: RoutineEventCreate, authorization: str | None = Header(default=None),
    ):
        _require_owner(payload.user_id, authorization)
        return run(active.add_routine_event, **payload.model_dump())

    @app.get("/api/users/{user_id}/confound-check")
    def confound_check(
        user_id: str,
        exclude_product_id: str | None = None,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.confound_check, user_id, exclude_product_id)

    @app.post("/api/captures")
    def capture(
        payload: CaptureCreate, authorization: str | None = Header(default=None),
    ):
        _require_owner(payload.user_id, authorization)
        try:
            image = base64.b64decode(payload.image_base64, validate=True)
        except (binascii.Error, ValueError) as exc:
            raise HTTPException(
                status_code=400, detail="image_base64 must be valid base64",
            ) from exc

        # Compress image before processing
        compressed_image = compressor.compress_image(
            image,
            max_dimension=int(os.getenv("GLOWUPAI_MAX_IMAGE_DIMENSION", "1024")),
            quality=int(os.getenv("GLOWUPAI_IMAGE_QUALITY", "85")),
        )

        result = run(
            active.create_capture,
            payload.user_id,
            compressed_image,
            payload.quality,
            payload.captured_at,
            payload.device_meta,
            payload.is_baseline,
            payload.vertical,
            payload.experiment_id,
        )

        # Track analytics
        capture_id = result.get("capture_id")
        if capture_id:
            analytics.track_capture_created(
                user_id=payload.user_id,
                capture_id=capture_id,
                is_baseline=payload.is_baseline,
                metrics=result.get("metrics"),
            )

            # Check for streak milestone
            streak = analytics.get_user_streak(payload.user_id)
            if streak in [3, 7, 14, 30, 60, 90]:
                analytics.track_streak_milestone(payload.user_id, streak)

        return result

    @app.get("/api/users/{user_id}/capture-guide")
    def capture_guide(
        user_id: str,
        vertical: str = "skin",
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.capture_guide, user_id, vertical)

    @app.get("/api/users/{user_id}/dashboard")
    def dashboard(
        user_id: str,
        vertical: str = "skin",
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.dashboard, user_id, vertical)

    @app.get("/api/users/{user_id}/history")
    def history(
        user_id: str,
        vertical: str = "skin",
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        result = run(active.history, user_id, vertical)

        # Track comparison viewed
        if result and len(result) > 1:
            capture_ids = [c.get("id") for c in result]
            analytics.track_comparison_viewed(user_id, capture_ids)

        return result

    @app.get("/api/users/{user_id}/engagement")
    def engagement(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.engagement, user_id)

    @app.post("/api/users/{user_id}/engagement")
    def engagement_event(
        user_id: str,
        payload: EngagementCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(
            active.record_engagement,
            user_id,
            payload.event_type,
            payload.reference_id,
            payload.metadata,
        )

    @app.get("/api/users/{user_id}/check-ins")
    def check_ins(
        user_id: str, limit: int = 30, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.check_ins, user_id, limit)

    @app.post("/api/users/{user_id}/check-ins")
    def create_check_in(
        user_id: str,
        payload: CheckInCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.create_check_in, user_id, **payload.model_dump())

    @app.get("/api/users/{user_id}/weekly-recap")
    def weekly_recap(
        user_id: str,
        vertical: str = "skin",
        as_of: str | None = None,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.weekly_recap, user_id, vertical, as_of)

    @app.post("/api/users/{user_id}/measurement-feedback")
    def measurement_feedback(
        user_id: str,
        payload: MeasurementFeedbackCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(
            active.add_measurement_feedback,
            user_id,
            payload.capture_id,
            payload.agreement,
            payload.note,
        )

    @app.get("/api/users/{user_id}/analytics")
    def analytics(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.analytics, user_id)

    @app.post("/api/experiments")
    def experiment(
        payload: ExperimentCreate, authorization: str | None = Header(default=None),
    ):
        _require_owner(payload.user_id, authorization)
        return run(
            active.create_experiment,
            payload.user_id,
            payload.name,
            payload.hypothesis,
            payload.product_id,
            payload.primary_metric,
            payload.target_days,
        )

    @app.get("/api/users/{user_id}/experiments")
    def experiments(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.experiments, user_id)

    @app.get("/api/users/{user_id}/experiments/{experiment_id}")
    def experiment_detail(
        user_id: str,
        experiment_id: str,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.experiment, experiment_id, user_id)

    @app.post("/api/users/{user_id}/experiments/{experiment_id}/status")
    def experiment_status(
        user_id: str,
        experiment_id: str,
        payload: ExperimentStatus,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        if payload.user_id != user_id:
            raise HTTPException(status_code=400, detail="user_id mismatch")
        return run(active.set_experiment_status, user_id, experiment_id, payload.status)

    @app.post("/api/users/{user_id}/qna")
    def qna(
        user_id: str,
        payload: QnaCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.ask, user_id, payload.question, payload.thread_id)

    @app.get("/api/users/{user_id}/qna")
    def qna_history(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.qna_history, user_id)

    @app.get("/api/users/{user_id}/discover")
    def discover(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.discover, user_id)

    @app.get("/api/users/{user_id}/commerce/offers")
    def offers(
        user_id: str,
        product_id: str | None = None,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.offers, user_id, product_id)

    @app.post("/api/users/{user_id}/commerce/offers/{offer_id}/click")
    def click_offer(
        user_id: str, offer_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.click_offer, user_id, offer_id)

    @app.post("/api/admin/offers")
    def add_offer(
        payload: OfferCreate, authorization: str | None = Header(default=None),
    ):
        _require_admin(authorization)
        return run(
            active.add_offer,
            payload.product_id,
            payload.merchant,
            payload.url,
            payload.price_cents,
            payload.currency,
        )

    @app.get("/api/users/{user_id}/labels")
    def labels(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.labels, user_id)

    @app.post("/api/users/{user_id}/labels")
    def add_label(
        user_id: str,
        payload: LabelCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(
            active.add_label,
            user_id,
            payload.photo_id,
            payload.label_type,
            payload.value,
            payload.confidence,
            payload.notes,
        )

    @app.post("/api/users/{user_id}/reprocess")
    def reprocess(
        user_id: str,
        payload: ReprocessCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.reprocess, user_id, payload.model_version)

    @app.get("/api/users/{user_id}/reprocess/{job_id}")
    def reprocess_status(
        user_id: str, job_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.reprocess_status, user_id, job_id)

    @app.post("/api/users/{user_id}/shelf-scan")
    def shelf_scan(
        user_id: str,
        payload: ShelfScanCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        try:
            image = base64.b64decode(payload.image_base64, validate=True)
        except (binascii.Error, ValueError) as exc:
            raise HTTPException(
                status_code=400, detail="image_base64 must be valid base64",
            ) from exc
        return run(active.scan_shelf, user_id, image)

    @app.get("/api/users/{user_id}/shelf-scan/{job_id}")
    def shelf_scan_status(
        user_id: str, job_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.shelf_scan_status, user_id, job_id)

    @app.post("/api/users/{user_id}/shelf-scan/{job_id}/confirm")
    def shelf_scan_confirm(
        user_id: str,
        job_id: str,
        payload: ShelfScanConfirm,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.confirm_shelf_scan, user_id, job_id, payload.selections)

    @app.get("/api/users/{user_id}/context-events")
    def context_events(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.context_events, user_id)

    @app.post("/api/users/{user_id}/context-events")
    def add_context_event(
        user_id: str,
        payload: ContextEventCreate,
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(
            active.add_context_event,
            user_id,
            payload.event_type,
            payload.value,
            payload.occurred_at,
            payload.notes,
        )

    @app.get("/api/users/{user_id}/root-cause")
    def root_cause(
        user_id: str,
        metric: str = "texture_score",
        authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.root_cause_search, user_id, metric)

    @app.get("/api/users/{user_id}/budget-optimizer")
    def budget_optimizer(
        user_id: str, authorization: str | None = Header(default=None),
    ):
        _require_owner(user_id, authorization)
        return run(active.budget_optimizer, user_id)

    @app.get("/api/users/{user_id}/derm-export")
    def derm_export(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.dermatologist_report, user_id)

    @app.get("/api/users/{user_id}/export")
    def export_user(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        return run(active.export_user, user_id)

    @app.delete("/api/users/{user_id}", status_code=204)
    def delete_user(user_id: str, authorization: str | None = Header(default=None)):
        _require_owner(user_id, authorization)
        run(active.delete_user, user_id)

    @app.post("/api/triage")
    def triage_question(payload: TriageCreate):
        return active.triage_question(payload.text)

    @app.get("/api/admin/audit")
    def audit(limit: int = 100, authorization: str | None = Header(default=None)):
        _require_admin(authorization)
        return active.admin_audit(limit)

    @app.get("/api/admin/measurement-feedback")
    def measurement_feedback_summary(authorization: str | None = Header(default=None)):
        _require_admin(authorization)
        return active.measurement_feedback_summary()

    @app.get("/api/admin/analytics")
    def admin_analytics(
        days: int = 7,
        authorization: str | None = Header(default=None),
    ):
        """Get analytics summary for admins."""
        _require_admin(authorization)
        return analytics.get_analytics_summary(days)

    @app.get("/api/admin/analytics/daily")
    def admin_analytics_daily(
        days: int = 30,
        authorization: str | None = Header(default=None),
    ):
        """Get daily analytics stats for admins."""
        _require_admin(authorization)
        return analytics.get_daily_stats(days)

    @app.get("/api/admin/analytics/events")
    def admin_analytics_events(
        event_type: str | None = None,
        days: int = 7,
        authorization: str | None = Header(default=None),
    ):
        """Get event counts by type for admins."""
        _require_admin(authorization)
        return analytics.get_event_counts(days, event_type)

    @app.post("/api/captures/{capture_id}/feedback")
    def submit_feedback(
        capture_id: str,
        payload: dict,
        authorization: str | None = Header(default=None),
    ):
        """Submit feedback for a capture."""
        # Extract user_id from authorization
        uid = verify_id_token(authorization.replace("Bearer ", ""))["uid"] if authorization else None
        if not uid:
            raise HTTPException(status_code=401, detail="Unauthorized")

        return run(
            active.submit_capture_feedback,
            capture_id,
            uid,
            payload.get("feedback_type"),
            payload.get("issues"),
            payload.get("corrections"),
            payload.get("comment"),
        )

    @app.get("/api/admin/feedback")
    def admin_feedback(
        limit: int = 100,
        authorization: str | None = Header(default=None),
    ):
        """Get feedback statistics for admin dashboard."""
        _require_admin(authorization)
        return run(active.get_feedback_stats)

    @app.get("/api/admin/feedback/corrections")
    def admin_feedback_corrections(
        limit: int = 100,
        authorization: str | None = Header(default=None),
    ):
        """Get feedback corrections for retraining."""
        _require_admin(authorization)
        return run(active.get_feedback_corrections, limit)

    @app.get("/api/admin/feedback/accuracy")
    def admin_feedback_accuracy(
        authorization: str | None = Header(default=None),
    ):
        """Get metric accuracy analysis."""
        _require_admin(authorization)
        return run(active.get_metric_accuracy_analysis)

    @app.get("/api/admin/monitoring")
    def admin_monitoring(
        authorization: str | None = Header(default=None),
    ):
        """Get model health monitoring status."""
        _require_admin(authorization)
        return run(active.get_model_health_status)

    @app.get("/api/admin/monitoring/daily-report")
    def admin_monitoring_daily(
        authorization: str | None = Header(default=None),
    ):
        """Get daily monitoring report."""
        _require_admin(authorization)
        return run(active.generate_monitoring_daily_report)

    @app.get("/api/admin/data-collection/stats")
    def admin_data_collection_stats(
        authorization: str | None = Header(default=None),
    ):
        """Get data collection statistics."""
        _require_admin(authorization)
        return run(active.get_collection_stats)

    @app.post("/api/admin/data-collection/export")
    def admin_data_collection_export(
        payload: dict,
        authorization: str | None = Header(default=None),
    ):
        """Export collected data as training dataset."""
        _require_admin(authorization)
        return run(
            active.export_training_dataset,
            payload.get("output_dir"),
            payload.get("min_quality", 0.75),
            payload.get("max_samples"),
        )

    @app.post("/api/admin/data-collection/cleanup")
    def admin_data_collection_cleanup(
        payload: dict,
        authorization: str | None = Header(default=None),
    ):
        """Cleanup old collected data (GDPR/CCPA compliance)."""
        _require_admin(authorization)
        return run(active.cleanup_old_data, payload.get("retention_days", 365))

    @app.post("/api/users/{user_id}/consent/data-collection")
    def consent_data_collection(
        user_id: str,
        payload: dict,
        authorization: str | None = Header(default=None),
    ):
        """Record user consent for data collection."""
        _require_owner(user_id, authorization)
        return run(
            active.record_data_collection_consent,
            user_id,
            payload.get("granted", False),
            payload.get("policy_version", "1.0"),
        )

    static_dir = Path(__file__).parent / "static"
    app.mount("/assets", StaticFiles(directory=static_dir), name="assets")
    static_file = static_dir / "index.html"

    @app.get("/", include_in_schema=False)
    def index():
        return FileResponse(static_file)

    return app


app = create_complete_app()
