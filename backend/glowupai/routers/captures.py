"""Capture and photo management router."""

from __future__ import annotations

import base64
import binascii
import logging
import os
from typing import Any, Dict, List, Optional

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field

from ..auth import verify_id_token

logger = logging.getLogger(__name__)


class CaptureCreate(BaseModel):
    user_id: str
    image_base64: str
    quality: Optional[Dict[str, Any]] = None
    captured_at: Optional[str] = None
    device_meta: Optional[Dict[str, Any]] = None
    is_baseline: bool = False
    vertical: str = "skin"
    experiment_id: Optional[str] = None


class CheckInCreate(BaseModel):
    routine_state: str = Field(
        default="steady", pattern="^(steady|changed|missed|not_sure)$"
    )
    skin_feel: str = Field(default="not_sure", pattern="^(better|same|worse|not_sure)$")
    note: Optional[str] = Field(default=None, max_length=400)
    occurred_at: Optional[str] = None


class MeasurementFeedbackCreate(BaseModel):
    capture_id: str
    agreement: str = Field(pattern="^(fair|uncertain|off)$")
    note: Optional[str] = Field(default=None, max_length=400)


class LabelCreate(BaseModel):
    photo_id: str
    label_type: str
    value: str
    confidence: Optional[float] = Field(default=None, ge=0, le=1)
    notes: Optional[str] = None


class ReprocessCreate(BaseModel):
    model_version: str = Field(min_length=1, max_length=80)


class ShelfScanCreate(BaseModel):
    image_base64: str


class ShelfScanConfirm(BaseModel):
    selections: List[Dict[str, Any]]


def setup_captures_router(service, analytics, compressor, run_handler, require_owner) -> APIRouter:
    """Setup capture routes with dependencies."""

    # Create a fresh router for each app instance
    router = APIRouter(prefix="/api", tags=["captures"])

    @router.post("/captures")
    def capture(
        payload: CaptureCreate, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(payload.user_id, authorization)
        try:
            image = base64.b64decode(payload.image_base64, validate=True)
        except (binascii.Error, ValueError) as exc:
            raise HTTPException(
                status_code=400, detail="image_base64 must be valid base64"
            ) from exc

        # Compress image before processing
        compressed_image = compressor.compress_image(
            image,
            max_dimension=int(os.getenv("GLOWUPAI_MAX_IMAGE_DIMENSION", "1024")),
            quality=int(os.getenv("GLOWUPAI_IMAGE_QUALITY", "85")),
        )

        result = run_handler(
            service.create_capture,
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

    @router.post("/captures/{capture_id}/feedback")
    def submit_feedback(
        capture_id: str,
        payload: Dict[str, Any],
        authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        """Submit feedback for a capture."""
        # Extract user_id from authorization
        uid = verify_id_token(authorization.replace("Bearer ", ""), service.settings.firebase_project_id)["uid"] if authorization else None
        if not uid:
            raise HTTPException(status_code=401, detail="Unauthorized")

        return run_handler(
            service.submit_capture_feedback,
            capture_id,
            uid,
            payload.get("feedback_type"),
            payload.get("issues"),
            payload.get("corrections"),
            payload.get("comment")
        )

    @router.get("/users/{user_id}/capture-guide")
    def capture_guide(
        user_id: str,
        vertical: str = "skin",
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.capture_guide, user_id, vertical)

    @router.get("/users/{user_id}/dashboard")
    def dashboard(
        user_id: str,
        vertical: str = "skin",
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.dashboard, user_id, vertical)

    @router.get("/users/{user_id}/history")
    def history(
        user_id: str,
        vertical: str = "skin",
        authorization: Optional[str] = Header(default=None),
    ) -> List[Dict[str, Any]]:
        require_owner(user_id, authorization)
        result = run_handler(service.history, user_id, vertical)

        # Track comparison viewed
        if result and len(result.get("captures", [])) > 1:
            capture_ids = [c.get("id") for c in result.get("captures", [])]
            analytics.track_comparison_viewed(user_id, capture_ids)

        return result

    @router.get("/users/{user_id}/check-ins")
    def check_ins(
        user_id: str, limit: int = 30, authorization: Optional[str] = Header(default=None)
    ) -> List[Dict[str, Any]]:
        require_owner(user_id, authorization)
        return run_handler(service.check_ins, user_id, limit)

    @router.post("/users/{user_id}/check-ins")
    def create_check_in(
        user_id: str,
        payload: CheckInCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.create_check_in, user_id, **payload.model_dump())

    @router.get("/users/{user_id}/weekly-recap")
    def weekly_recap(
        user_id: str,
        vertical: str = "skin",
        as_of: Optional[str] = None,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.weekly_recap, user_id, vertical, as_of)

    @router.post("/users/{user_id}/measurement-feedback")
    def measurement_feedback(
        user_id: str,
        payload: MeasurementFeedbackCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(
            service.add_measurement_feedback,
            user_id,
            payload.capture_id,
            payload.agreement,
            payload.note,
        )

    @router.get("/users/{user_id}/labels")
    def labels(user_id: str, authorization: Optional[str] = Header(default=None)) -> List[Dict[str, Any]]:
        require_owner(user_id, authorization)
        return run_handler(service.labels, user_id)

    @router.post("/users/{user_id}/labels")
    def add_label(
        user_id: str,
        payload: LabelCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(
            service.add_label,
            user_id,
            payload.photo_id,
            payload.label_type,
            payload.value,
            payload.confidence,
            payload.notes,
        )

    @router.post("/users/{user_id}/reprocess")
    def reprocess(
        user_id: str,
        payload: ReprocessCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.reprocess, user_id, payload.model_version)

    @router.get("/users/{user_id}/reprocess/{job_id}")
    def reprocess_status(
        user_id: str, job_id: str, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.reprocess_status, user_id, job_id)

    @router.post("/users/{user_id}/shelf-scan")
    def shelf_scan(
        user_id: str,
        payload: ShelfScanCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        try:
            image = base64.b64decode(payload.image_base64, validate=True)
        except (binascii.Error, ValueError) as exc:
            raise HTTPException(
                status_code=400, detail="image_base64 must be valid base64"
            ) from exc
        return run_handler(service.scan_shelf, user_id, image)

    @router.get("/users/{user_id}/shelf-scan/{job_id}")
    def shelf_scan_status(
        user_id: str, job_id: str, authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.shelf_scan_status, user_id, job_id)

    @router.post("/users/{user_id}/shelf-scan/{job_id}/confirm")
    def shelf_scan_confirm(
        user_id: str,
        job_id: str,
        payload: ShelfScanConfirm,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.confirm_shelf_scan, user_id, job_id, payload.selections)

    return router
