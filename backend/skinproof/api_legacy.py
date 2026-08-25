from __future__ import annotations

import base64
import binascii
import json
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

from .config import Settings
from .complete_db import build_full_database
from .photos import build_photo_store
from .service import SkinProofService


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


class CaptureCreate(BaseModel):
    user_id: str
    image_base64: str
    quality: dict | None = None
    captured_at: str | None = None
    device_meta: dict | None = None
    is_baseline: bool = False


class TriageCreate(BaseModel):
    text: str = Field(min_length=1, max_length=2000)


def create_app(service: SkinProofService | None = None) -> FastAPI:
    settings = Settings.from_env()
    settings.prepare()
    active_service = service or SkinProofService(build_full_database(settings), settings, build_photo_store(settings.photo_dir))
    app = FastAPI(title="SkinProof API", version="0.1.0", description="Cosmetic tracking, not diagnosis.")
    app.state.skinproof = active_service

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

    @app.get("/api/health")
    def health():
        return {"status": "ok", "version": "0.1.0", "scope": "cosmetic_tracking"}

    @app.post("/api/users")
    def create_user(payload: UserCreate):
        return active_service.create_user(payload.skin_type)

    @app.post("/api/users/{user_id}/consent")
    def consent(user_id: str, payload: ConsentCreate):
        return run(active_service.grant_consent, user_id, payload.facial_data, payload.policy_version)

    @app.post("/api/products")
    def create_product(payload: ProductCreate):
        return run(active_service.create_product, payload.name, payload.barcode, payload.category, payload.ingredients, payload.stabilization_days)

    @app.get("/api/products/search")
    def search_products(q: str = ""):
        return active_service.search_products(q)

    @app.post("/api/routine-events")
    def routine_event(payload: RoutineEventCreate):
        return run(active_service.add_routine_event, **payload.model_dump())

    @app.post("/api/captures")
    def capture(payload: CaptureCreate):
        try:
            image = base64.b64decode(payload.image_base64, validate=True)
        except (binascii.Error, ValueError) as exc:
            raise HTTPException(status_code=400, detail="image_base64 must be valid base64") from exc
        return run(active_service.create_capture, payload.user_id, image, payload.quality, payload.captured_at, payload.device_meta, payload.is_baseline)

    @app.get("/api/users/{user_id}/dashboard")
    def dashboard(user_id: str):
        return run(active_service.dashboard, user_id)

    @app.get("/api/users/{user_id}/history")
    def history(user_id: str):
        return run(active_service.history, user_id)

    @app.get("/api/users/{user_id}/export")
    def export_user(user_id: str):
        return run(active_service.export_user, user_id)

    @app.delete("/api/users/{user_id}", status_code=204)
    def delete_user(user_id: str):
        run(active_service.delete_user, user_id)

    @app.get("/api/products/{product_id}/ingredient-explainer")
    def ingredient_explainer(product_id: str):
        return run(active_service.ingredient_explainer, product_id)

    @app.post("/api/triage")
    def triage(payload: TriageCreate):
        return active_service.triage_question(payload.text)

    static_file = Path(__file__).parent / "static" / "index.html"

    @app.get("/", include_in_schema=False)
    def index():
        return FileResponse(static_file)

    return app


app = create_app()
