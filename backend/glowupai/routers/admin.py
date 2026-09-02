"""Admin endpoints router."""

from __future__ import annotations

import logging
from typing import Any

from fastapi import APIRouter, Header
from pydantic import BaseModel, Field

logger = logging.getLogger(__name__)


class OfferCreate(BaseModel):
    product_id: str
    merchant: str
    url: str
    price_cents: int | None = Field(default=None, ge=0)
    currency: str = "USD"


class TriageCreate(BaseModel):
    text: str = Field(min_length=1, max_length=2000)


def setup_admin_router(
    service, analytics, metrics_collector, run_handler, require_admin, cache=None
) -> APIRouter:
    """Setup admin routes with dependencies."""

    # Create a fresh router for each app instance
    router = APIRouter(prefix="/api", tags=["admin"])

    @router.get("/metrics", tags=["monitoring"])
    def metrics(authorization: str | None = Header(default=None)) -> dict[str, Any]:
        """Get application metrics (admin only)."""
        require_admin(authorization)
        result: dict[str, Any] = metrics_collector.get_metrics()
        return result

    @router.get("/cache/diagnostics", tags=["monitoring"])
    def cache_diagnostics(
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Get cache health and diagnostics (admin only)."""
        require_admin(authorization)
        if cache is None:
            return {"error": "Cache not configured"}

        from ..cache_diagnostics import (
            check_cache_health,
            get_cache_stats,
            test_cache_operations,
        )

        return {
            "health": check_cache_health(cache),
            "operations_test": test_cache_operations(cache),
            "stats": get_cache_stats(cache),
        }

    @router.post("/admin/offers")
    def add_offer(
        payload: OfferCreate, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_admin(authorization)
        result: dict[str, Any] = run_handler(
            service.add_offer,
            payload.product_id,
            payload.merchant,
            payload.url,
            payload.price_cents,
            payload.currency,
        )
        return result

    @router.post("/triage")
    def triage_question(
        payload: TriageCreate, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """AI triage endpoint - requires authentication to prevent abuse."""
        require_admin(authorization)  # Protect against AI service abuse
        result: dict[str, Any] = service.triage_question(payload.text)
        return result

    @router.get("/admin/audit")
    def audit(
        limit: int = 100, authorization: str | None = Header(default=None)
    ) -> list[dict[str, Any]]:
        require_admin(authorization)
        result: list[dict[str, Any]] = service.admin_audit(limit)
        return result

    @router.get("/admin/measurement-feedback")
    def measurement_feedback_summary(
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_admin(authorization)
        result: dict[str, Any] = service.measurement_feedback_summary()
        return result

    @router.get("/admin/analytics")
    def admin_analytics(
        days: int = 7, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """Get analytics summary for admins."""
        require_admin(authorization)
        result: dict[str, Any] = analytics.get_analytics_summary(days)
        return result

    @router.get("/admin/analytics/daily")
    def admin_analytics_daily(
        days: int = 30, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """Get daily analytics stats for admins."""
        require_admin(authorization)
        result: dict[str, Any] = analytics.get_daily_stats(days)
        return result

    @router.get("/admin/analytics/events")
    def admin_analytics_events(
        event_type: str | None = None,
        days: int = 7,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Get event counts by type for admins."""
        require_admin(authorization)
        result: dict[str, Any] = analytics.get_event_counts(days, event_type)
        return result

    @router.get("/admin/feedback")
    def admin_feedback(
        limit: int = 100, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """Get feedback statistics for admin dashboard."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(service.get_feedback_stats)
        return result

    @router.get("/admin/feedback/corrections")
    def admin_feedback_corrections(
        limit: int = 100, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """Get feedback corrections for retraining."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(service.get_feedback_corrections, limit)
        return result

    @router.get("/admin/feedback/accuracy")
    def admin_feedback_accuracy(
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Get metric accuracy analysis."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(service.get_metric_accuracy_analysis)
        return result

    @router.get("/admin/monitoring")
    def admin_monitoring(
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Get model health monitoring status."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(service.get_model_health_status)
        return result

    @router.get("/admin/monitoring/daily-report")
    def admin_monitoring_daily(
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Get daily monitoring report."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(service.generate_monitoring_daily_report)
        return result

    @router.get("/admin/data-collection/stats")
    def admin_data_collection_stats(
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Get data collection statistics."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(service.get_collection_stats)
        return result

    @router.post("/admin/data-collection/export")
    def admin_data_collection_export(
        payload: dict[str, Any], authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """Export collected data as training dataset."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(
            service.export_training_dataset,
            payload.get("output_dir"),
            payload.get("min_quality", 0.75),
            payload.get("max_samples"),
        )
        return result

    @router.post("/admin/data-collection/cleanup")
    def admin_data_collection_cleanup(
        payload: dict[str, Any], authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """Cleanup old collected data (GDPR/CCPA compliance)."""
        require_admin(authorization)
        result: dict[str, Any] = run_handler(
            service.cleanup_old_data, payload.get("retention_days", 365)
        )
        return result

    return router
