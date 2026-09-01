"""Subscription, products, experiments, and commerce router."""

from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional, Union

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field

logger = logging.getLogger(__name__)


class UpgradeCreate(BaseModel):
    source: str = "local_checkout"


class ProductCreate(BaseModel):
    name: str = Field(min_length=1, max_length=160)
    barcode: Optional[str] = None
    category: str = "other"
    ingredients: Optional[Union[List[str], str]] = None
    stabilization_days: int = Field(default=14, ge=0, le=180)


class RoutineEventCreate(BaseModel):
    user_id: str
    product_id: str
    action: str
    timestamp: Optional[str] = None
    slot: str = "unspecified"
    dose: Optional[str] = None
    frequency: Optional[str] = None
    notes: Optional[str] = None
    experiment_id: Optional[str] = None


class PurchaseGuidanceCreate(BaseModel):
    name: Optional[str] = Field(default=None, max_length=160)
    barcode: Optional[str] = Field(default=None, max_length=80)
    category: str = Field(default="other", max_length=40)
    ingredients: Optional[Union[List[str], str]] = None
    price_cents: Optional[int] = Field(default=None, ge=0)
    currency: str = Field(default="INR", min_length=3, max_length=3)


class ExperimentCreate(BaseModel):
    user_id: str
    name: str = Field(min_length=1, max_length=160)
    hypothesis: Optional[str] = None
    product_id: str
    primary_metric: str = "redness_score"
    target_days: int = Field(default=14, ge=1, le=180)


class ExperimentStatus(BaseModel):
    user_id: str
    status: str


class QnaCreate(BaseModel):
    question: str = Field(min_length=1, max_length=2000)
    thread_id: Optional[str] = None


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
        limit: int = 100,
        authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        """List all subscriptions (returns list of entitlements)."""
        # This endpoint could be used by admins or for listing user's own subscriptions
        # For now, making it open to match the POST endpoint pattern
        subscriptions = run_handler(service.list_subscriptions, limit)
        return {"subscriptions": subscriptions, "count": len(subscriptions)}

    @router.post("/subscriptions")
    def create_subscription(
        payload: SubscriptionCreate,
        authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        """Create or upgrade a subscription."""
        require_owner(payload.user_id, authorization)
        return run_handler(service.create_subscription, payload.user_id, payload.plan, payload.source)

    @router.get("/users/{user_id}/subscription")
    def subscription(user_id: str, authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.entitlement, user_id)

    @router.post("/users/{user_id}/subscription/upgrade")
    def upgrade(
        user_id: str,
        payload: UpgradeCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.upgrade, user_id, payload.source)

    @router.post("/users/{user_id}/subscription/cancel")
    def cancel_subscription(
        user_id: str, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.downgrade, user_id)

    @router.post("/products")
    def create_product(payload: ProductCreate) -> Dict[str, Any]:
        return run_handler(
            service.create_product,
            payload.name,
            payload.barcode,
            payload.category,
            payload.ingredients,
            payload.stabilization_days,
        )

    @router.get("/products/search")
    def search_products(q: str = "") -> Dict[str, Any]:
        return run_handler(service.search_products, q)

    @router.get("/products/lookup")
    def lookup_product(barcode: str) -> Dict[str, Any]:
        return run_handler(service.lookup_product, barcode)

    @router.get("/products/{product_id}")
    def product_detail(
        product_id: str, user_id: str, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.product_detail, user_id, product_id)

    @router.get("/products/{product_id}/ingredient-explainer")
    def ingredient_explainer(
        product_id: str, user_id: str, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.ingredient_explainer, user_id, product_id)

    @router.get("/products/{product_id}/predict")
    def predict_product(
        product_id: str, user_id: str, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.predict_product, user_id, product_id)

    @router.post("/users/{user_id}/purchase-guidance")
    def purchase_guidance(
        user_id: str,
        payload: PurchaseGuidanceCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.purchase_guidance, user_id, **payload.model_dump())

    @router.post("/routine-events")
    def routine_event(
        payload: RoutineEventCreate, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(payload.user_id, authorization)
        return run_handler(service.add_routine_event, **payload.model_dump())

    @router.get("/users/{user_id}/confound-check")
    def confound_check(
        user_id: str,
        exclude_product_id: Optional[str] = None,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.confound_check, user_id, exclude_product_id)

    @router.post("/experiments")
    def experiment(
        payload: ExperimentCreate, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(payload.user_id, authorization)
        return run_handler(
            service.create_experiment,
            payload.user_id,
            payload.name,
            payload.hypothesis,
            payload.product_id,
            payload.primary_metric,
            payload.target_days,
        )

    @router.get("/users/{user_id}/experiments")
    def experiments(user_id: str, authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.experiments, user_id)

    @router.get("/users/{user_id}/experiments/{experiment_id}")
    def experiment_detail(
        user_id: str,
        experiment_id: str,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.experiment, experiment_id, user_id)

    @router.post("/users/{user_id}/experiments/{experiment_id}/status")
    def experiment_status(
        user_id: str,
        experiment_id: str,
        payload: ExperimentStatus,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        if payload.user_id != user_id:
            raise HTTPException(status_code=400, detail="user_id mismatch")
        return run_handler(service.set_experiment_status, user_id, experiment_id, payload.status)

    @router.post("/users/{user_id}/qna")
    def qna(
        user_id: str,
        payload: QnaCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.ask, user_id, payload.question, payload.thread_id)

    @router.get("/users/{user_id}/qna")
    def qna_history(user_id: str, authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.qna_history, user_id)

    @router.get("/users/{user_id}/discover")
    def discover(user_id: str, authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.discover, user_id)

    @router.get("/users/{user_id}/commerce/offers")
    def offers(
        user_id: str,
        product_id: Optional[str] = None,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.offers, user_id, product_id)

    @router.post("/users/{user_id}/commerce/offers/{offer_id}/click")
    def click_offer(
        user_id: str, offer_id: str, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.click_offer, user_id, offer_id)

    return router
