"""Subscription, products, experiments, and commerce router."""

from __future__ import annotations

import base64
import json
import logging
import os
from typing import Any

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field
from ..config import play_billing_enabled
from ..play_billing import verify_notification_identity

logger = logging.getLogger(__name__)


class UpgradeCreate(BaseModel):
    source: str = "local_checkout"


class PlayPurchaseCreate(BaseModel):
    purchase_token: str = Field(min_length=1, max_length=4096)


class PlayNotificationMessage(BaseModel):
    data: str = Field(min_length=1, max_length=32768)
    message_id: str | None = Field(default=None, alias="messageId", max_length=256)


class PlayNotification(BaseModel):
    message: PlayNotificationMessage


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


class PurchaseGuidanceCreate(BaseModel):
    name: str | None = Field(default=None, max_length=160)
    barcode: str | None = Field(default=None, max_length=80)
    category: str = Field(default="other", max_length=40)
    ingredients: list[str] | str | None = None
    price_cents: int | None = Field(default=None, ge=0)
    currency: str = Field(default="INR", min_length=3, max_length=3)


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


class SubscriptionCreate(BaseModel):
    user_id: str
    plan: str = "premium"
    source: str = "api"


def setup_subscriptions_router(
    service, run_handler, require_owner, require_admin, require_authenticated
) -> APIRouter:
    """Setup subscription and product routes with dependencies."""

    # Create a fresh router for each app instance
    router = APIRouter(prefix="/api", tags=["subscriptions"])

    @router.get("/billing/config")
    def billing_config():
        if not play_billing_enabled():
            return {"enabled": False, "product_ids": []}
        return {
            "enabled": True,
            "product_ids": [
                p.strip()
                for p in os.getenv("GLOWUPAI_PLAY_PRODUCT_IDS", "").split(",")
                if p.strip()
            ],
        }

    @router.post("/billing/play/notifications")
    def play_notification(
        payload: PlayNotification, authorization: str | None = Header(default=None)
    ):
        if not play_billing_enabled():
            raise HTTPException(404, "Google Play billing is not enabled")
        try:
            verify_notification_identity(authorization)
        except PermissionError as exc:
            raise HTTPException(401, str(exc)) from None
        message_id = payload.message.message_id
        if message_id and service.db.fetchone(
            "SELECT message_id FROM play_notification_events WHERE message_id=?",
            (message_id,),
        ):
            return {"received": True, "duplicate": True}
        try:
            notification = json.loads(
                base64.b64decode(payload.message.data, validate=True).decode("utf-8")
            )
            if notification.get("packageName") != os.getenv(
                "GLOWUPAI_PLAY_PACKAGE_NAME"
            ):
                raise ValueError("Unexpected package")
            event = (
                notification.get("subscriptionNotification")
                or notification.get("voidedPurchaseNotification")
                or {}
            )
            token = event.get("purchaseToken")
            if token is not None and (
                not isinstance(token, str) or not 1 <= len(token) <= 4096
            ):
                raise ValueError("Invalid purchase token")
        except (ValueError, TypeError, AttributeError):
            raise HTTPException(400, "Invalid Play notification") from None
        linked = False
        if token:
            try:
                linked = service.subscription_svc.refresh_play_purchase(token)
            except Exception:
                # Non-2xx keeps delivery retryable when Google or storage is unavailable.
                raise HTTPException(
                    503, "Purchase refresh failed; retry notification"
                ) from None
        if message_id:
            service.db.execute(
                "INSERT INTO play_notification_events (message_id) VALUES (?) ON CONFLICT (message_id) DO NOTHING",
                (message_id,),
            )
        return {"received": True, "known_purchase": linked}

    @router.post("/users/{user_id}/subscription/play")
    def verify_play_purchase(
        user_id: str,
        payload: PlayPurchaseCreate,
        authorization: str | None = Header(default=None),
    ):
        if not play_billing_enabled():
            raise HTTPException(404, "Google Play billing is not enabled")
        require_owner(user_id, authorization)
        try:
            return run_handler(
                service.subscription_svc.verify_play_purchase,
                user_id,
                payload.purchase_token,
            )
        except RuntimeError:
            logger.exception("Google Play purchase verification failed")
            raise HTTPException(
                503, "Google Play verification is temporarily unavailable"
            ) from None

    @router.get("/subscriptions")
    def list_subscriptions(
        limit: int = 100, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """List all subscriptions (returns list of entitlements)."""
        require_admin(authorization)
        subscriptions = run_handler(service.list_subscriptions, limit)
        return {"subscriptions": subscriptions, "count": len(subscriptions)}

    @router.post("/subscriptions")
    def create_subscription(
        payload: SubscriptionCreate, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """Create or upgrade a subscription."""
        require_owner(payload.user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.create_subscription, payload.user_id, payload.plan, payload.source
        )
        return result

    @router.get("/users/{user_id}/subscription")
    def subscription(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.entitlement, user_id)
        return result

    @router.post("/users/{user_id}/subscription/upgrade")
    def upgrade(
        user_id: str,
        payload: UpgradeCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.upgrade, user_id, payload.source)
        return result

    @router.post("/users/{user_id}/subscription/cancel")
    def cancel_subscription(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.downgrade, user_id)
        return result

    @router.post("/products")
    def create_product(
        payload: ProductCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_authenticated(authorization)
        result: dict[str, Any] = run_handler(
            service.create_product,
            payload.name,
            payload.barcode,
            payload.category,
            payload.ingredients,
            payload.stabilization_days,
        )
        return result

    @router.get("/products/search")
    def search_products(q: str = "") -> list[dict[str, Any]]:
        result: list[dict[str, Any]] = run_handler(service.search_products, q)
        return result

    @router.get("/products/lookup")
    def lookup_product(barcode: str) -> dict[str, Any]:
        result: dict[str, Any] = run_handler(service.lookup_product, barcode)
        return result

    @router.get("/products/{product_id}")
    def product_detail(
        product_id: str, user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.product_detail, user_id, product_id
        )
        return result

    @router.get("/products/{product_id}/ingredient-explainer")
    def ingredient_explainer(
        product_id: str, user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.ingredient_explainer, user_id, product_id
        )
        return result

    @router.get("/products/{product_id}/predict")
    def predict_product(
        product_id: str, user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.predict_product, user_id, product_id
        )
        return result

    @router.post("/users/{user_id}/purchase-guidance")
    def purchase_guidance(
        user_id: str,
        payload: PurchaseGuidanceCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.purchase_guidance, user_id, **payload.model_dump()
        )
        return result

    @router.post("/routine-events")
    def routine_event(
        payload: RoutineEventCreate, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(payload.user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.add_routine_event, **payload.model_dump()
        )
        return result

    @router.get("/users/{user_id}/confound-check")
    def confound_check(
        user_id: str,
        exclude_product_id: str | None = None,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.confound_check, user_id, exclude_product_id
        )
        return result

    @router.post("/experiments")
    def experiment(
        payload: ExperimentCreate, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(payload.user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.create_experiment,
            payload.user_id,
            payload.name,
            payload.hypothesis,
            payload.product_id,
            payload.primary_metric,
            payload.target_days,
        )
        return result

    @router.get("/users/{user_id}/experiments")
    def experiments(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        result: list[dict[str, Any]] = run_handler(service.experiments, user_id)
        return result

    @router.get("/users/{user_id}/experiments/{experiment_id}")
    def experiment_detail(
        user_id: str,
        experiment_id: str,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.experiment, experiment_id, user_id)
        return result

    @router.post("/users/{user_id}/experiments/{experiment_id}/status")
    def experiment_status(
        user_id: str,
        experiment_id: str,
        payload: ExperimentStatus,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        if payload.user_id != user_id:
            raise HTTPException(status_code=400, detail="user_id mismatch")
        result: dict[str, Any] = run_handler(
            service.set_experiment_status, user_id, experiment_id, payload.status
        )
        return result

    @router.post("/users/{user_id}/qna")
    def qna(
        user_id: str,
        payload: QnaCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.ask, user_id, payload.question, payload.thread_id
        )
        return result

    @router.get("/users/{user_id}/qna")
    def qna_history(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        result: list[dict[str, Any]] = run_handler(service.qna_history, user_id)
        return result

    @router.post("/users/{user_id}/qna/{message_id}/report")
    def report_answer(
        user_id: str,
        message_id: str,
        authorization: str | None = Header(default=None),
    ) -> dict[str, bool]:
        require_owner(user_id, authorization)
        message = service.db.fetchone(
            "SELECT m.id FROM qna_messages m JOIN qna_threads t ON t.id=m.thread_id "
            "WHERE m.id=? AND t.user_id=? AND m.role='assistant'",
            (message_id, user_id),
        )
        if message is None:
            raise HTTPException(404, "Answer not found")
        service.db.execute(
            "INSERT INTO qna_reports (message_id,user_id) VALUES (?,?) "
            "ON CONFLICT(message_id) DO NOTHING",
            (message_id, user_id),
        )
        return {"reported": True}

    @router.get("/admin/qna-reports")
    def reported_answers(
        authorization: str | None = Header(default=None),
    ) -> list[dict[str, Any]]:
        require_admin(authorization)
        return [
            dict(row)
            for row in service.db.fetchall(
                "SELECT r.message_id,r.created_at,m.content FROM qna_reports r "
                "JOIN qna_messages m ON m.id=r.message_id "
                "ORDER BY r.created_at DESC LIMIT 100"
            )
        ]

    @router.get("/users/{user_id}/discover")
    def discover(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.discover, user_id)
        return result

    @router.get("/users/{user_id}/commerce/offers")
    def offers(
        user_id: str,
        product_id: str | None = None,
        authorization: str | None = Header(default=None),
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        result: list[dict[str, Any]] = run_handler(service.offers, user_id, product_id)
        return result

    @router.post("/users/{user_id}/commerce/offers/{offer_id}/click")
    def click_offer(
        user_id: str, offer_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.click_offer, user_id, offer_id)
        return result

    return router
