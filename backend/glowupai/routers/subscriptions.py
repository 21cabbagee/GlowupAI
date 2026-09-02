"""Subscription, products, experiments, and commerce router."""

from __future__ import annotations

import logging
from typing import Any

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field

logger = logging.getLogger(__name__)


class UpgradeCreate(BaseModel):
    source: str = "local_checkout"


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


def setup_subscriptions_router(service, run_handler, require_owner) -> APIRouter:
    """Setup subscription and product routes with dependencies."""

    # Create a fresh router for each app instance
    router = APIRouter(prefix="/api", tags=["subscriptions"])

    @router.get("/subscriptions")
    def list_subscriptions(
        limit: int = 100, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        """List all subscriptions (returns list of entitlements)."""
        # This endpoint could be used by admins or for listing user's own subscriptions
        # For now, making it open to match the POST endpoint pattern
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
    def create_product(payload: ProductCreate) -> dict[str, Any]:
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
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.experiments, user_id)
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
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.offers, user_id, product_id)
        return result

    @router.post("/users/{user_id}/commerce/offers/{offer_id}/click")
    def click_offer(
        user_id: str, offer_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.click_offer, user_id, offer_id)
        return result

    return router
