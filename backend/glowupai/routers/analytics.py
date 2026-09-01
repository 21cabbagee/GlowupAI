"""Analytics and engagement router."""

from __future__ import annotations

import logging
from typing import Any

from fastapi import APIRouter, Header
from pydantic import BaseModel

logger = logging.getLogger(__name__)


class EngagementCreate(BaseModel):
    event_type: str
    reference_id: str | None = None
    metadata: dict[str, Any] | None = None


class ContextEventCreate(BaseModel):
    event_type: str
    value: str | None = None
    occurred_at: str | None = None
    notes: str | None = None


def setup_analytics_router(service, run_handler, require_owner) -> APIRouter:
    """Setup analytics routes with dependencies."""

    # Create a fresh router for each app instance
    router = APIRouter(prefix="/api", tags=["analytics"])

    @router.get("/analytics/summary")
    def analytics_summary(user_id: str) -> dict[str, Any]:
        return run_handler(service.summary, user_id)

    @router.get("/analytics/trends")
    def analytics_trends(user_id: str, vertical: str = "skin") -> dict[str, Any]:
        return run_handler(service.trends, user_id, vertical)

    @router.get("/users/{user_id}/analytics")
    def analytics(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.analytics, user_id)

    @router.get("/users/{user_id}/engagement")
    def engagement(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.engagement, user_id)

    @router.post("/users/{user_id}/engagement")
    def engagement_event(
        user_id: str,
        payload: EngagementCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(
            service.record_engagement,
            user_id,
            payload.event_type,
            payload.reference_id,
            payload.metadata,
        )

    @router.get("/users/{user_id}/context-events")
    def context_events(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        return run_handler(service.context_events, user_id)

    @router.post("/users/{user_id}/context-events")
    def add_context_event(
        user_id: str,
        payload: ContextEventCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(
            service.add_context_event,
            user_id,
            payload.event_type,
            payload.value,
            payload.occurred_at,
            payload.notes,
        )

    @router.get("/users/{user_id}/root-cause")
    def root_cause(
        user_id: str,
        metric: str = "texture_score",
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.root_cause_search, user_id, metric)

    @router.get("/users/{user_id}/budget-optimizer")
    def budget_optimizer(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.budget_optimizer, user_id)

    @router.get("/users/{user_id}/derm-export")
    def derm_export(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.dermatologist_report, user_id)

    return router
