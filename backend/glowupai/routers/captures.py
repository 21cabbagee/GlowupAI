"""Capture and photo management router."""

from __future__ import annotations

import base64
import binascii
import logging
import os
import io
from typing import Any

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import Response
from PIL import Image, ImageOps
from pydantic import BaseModel, Field

from ..auth import AuthError, verify_access_token

logger = logging.getLogger(__name__)

# Base64 expands binary data by roughly one third. Bound it in the request
# model so hostile clients cannot force an unbounded allocation before image
# decoding and compression begin.
MAX_IMAGE_BASE64_CHARS = 16_000_000
MAX_IMAGE_BYTES = 12_000_000


class CaptureCreate(BaseModel):
    user_id: str
    image_base64: str = Field(min_length=4, max_length=MAX_IMAGE_BASE64_CHARS)
    quality: dict[str, Any] | None = None
    captured_at: str | None = None
    device_meta: dict[str, Any] | None = None
    is_baseline: bool = False
    vertical: str = "skin"
    experiment_id: str | None = None
    idempotency_key: str | None = Field(default=None, min_length=1, max_length=100)


class CheckInCreate(BaseModel):
    routine_state: str = Field(
        default="steady", pattern="^(steady|changed|missed|not_sure)$"
    )
    skin_feel: str = Field(default="not_sure", pattern="^(better|same|worse|not_sure)$")
    note: str | None = Field(default=None, max_length=400)
    occurred_at: str | None = None


class MeasurementFeedbackCreate(BaseModel):
    capture_id: str
    agreement: str = Field(pattern="^(fair|uncertain|off)$")
    note: str | None = Field(default=None, max_length=400)


class LabelCreate(BaseModel):
    photo_id: str
    label_type: str
    value: str
    confidence: float | None = Field(default=None, ge=0, le=1)
    notes: str | None = None


class ReprocessCreate(BaseModel):
    model_version: str = Field(min_length=1, max_length=80)


class ShelfScanCreate(BaseModel):
    image_base64: str = Field(min_length=4, max_length=MAX_IMAGE_BASE64_CHARS)


class ShelfScanConfirm(BaseModel):
    selections: list[dict[str, Any]]


class ComparisonCreate(BaseModel):
    earlier_capture_id: str = Field(min_length=1, max_length=100)
    later_capture_id: str = Field(min_length=1, max_length=100)
    vertical: str = Field(default="skin", pattern="^skin$")


def setup_captures_router(
    service, analytics, compressor, run_handler, require_owner
) -> APIRouter:
    """Setup capture routes with dependencies."""

    # Create a fresh router for each app instance
    router = APIRouter(prefix="/api", tags=["captures"])

    @router.post("/captures")
    def capture(
        payload: CaptureCreate, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(payload.user_id, authorization)
        try:
            image = base64.b64decode(payload.image_base64, validate=True)
        except (binascii.Error, ValueError) as exc:
            raise HTTPException(
                status_code=400, detail="image_base64 must be valid base64"
            ) from exc
        if len(image) > MAX_IMAGE_BYTES:
            raise HTTPException(status_code=413, detail="image is too large")

        # Compress image before processing
        compressed_image = compressor.compress_image(
            image,
            max_dimension=int(os.getenv("GLOWUPAI_MAX_IMAGE_DIMENSION", "1024")),
            quality=int(os.getenv("GLOWUPAI_IMAGE_QUALITY", "85")),
        )

        result: dict[str, Any] = run_handler(
            service.create_capture,
            payload.user_id,
            compressed_image,
            payload.quality,
            payload.captured_at,
            payload.device_meta,
            payload.is_baseline,
            payload.vertical,
            payload.experiment_id,
            payload.idempotency_key,
        )

        # An idempotency replay returns the original capture, but must not
        # repeat side effects such as analytics events or streak milestones.
        # `_capture_created` is internal metadata set by CaptureService and is
        # removed so the public response remains exactly the capture payload.
        capture_was_created = result.pop("_capture_created", True)

        # Track analytics only for a newly persisted capture.
        capture_id = result.get("id") or result.get("capture_id")
        if capture_was_created and capture_id:
            analytics.track_capture_created(
                user_id=payload.user_id,
                capture_id=capture_id,
                is_baseline=payload.is_baseline,
                metrics=result.get("metric") or result.get("metrics"),
            )

            # Check for streak milestone
            streak = analytics.get_user_streak(payload.user_id)
            if streak in [3, 7, 14, 30, 60, 90]:
                analytics.track_streak_milestone(payload.user_id, streak)

        return result

    @router.post("/captures/{capture_id}/feedback")
    def submit_feedback(
        capture_id: str,
        payload: dict[str, Any],
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Submit feedback for a capture."""
        # Extract user_id from authorization
        if not authorization or not authorization.lower().startswith("bearer "):
            raise HTTPException(status_code=401, detail="missing bearer token")
        try:
            token_result = verify_access_token(
                authorization.split(" ", 1)[1].strip(),
                service.settings.supabase_url,
                service.settings.supabase_jwt_secret,
                jwks_url=service.settings.supabase_jwks_url,
            )
        except AuthError as exc:
            raise HTTPException(status_code=401, detail=str(exc)) from exc
        owner = service.db.fetchone(
            "SELECT id FROM users WHERE supabase_uid = ? AND deleted_at IS NULL",
            (token_result.uid,),
        )
        if not owner:
            raise HTTPException(
                status_code=403, detail="authenticated user is not registered"
            )
        owner_id = owner["id"]

        result: dict[str, Any] = run_handler(
            service.submit_capture_feedback,
            capture_id,
            owner_id,
            payload.get("feedback_type"),
            payload.get("issues"),
            payload.get("corrections"),
            payload.get("comment"),
        )
        return result

    @router.get("/users/{user_id}/capture-guide")
    def capture_guide(
        user_id: str,
        vertical: str = "skin",
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.capture_guide, user_id, vertical)
        return result

    @router.get("/users/{user_id}/dashboard")
    def dashboard(
        user_id: str,
        vertical: str = "skin",
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.dashboard, user_id, vertical)
        return result

    @router.get("/users/{user_id}/history")
    def history(
        user_id: str,
        vertical: str = "skin",
        authorization: str | None = Header(default=None),
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        result: list[dict[str, Any]] = run_handler(service.history, user_id, vertical)

        # Track comparison viewed
        if len(result) > 1:
            capture_ids = [c.get("id") for c in result]
            analytics.track_comparison_viewed(user_id, capture_ids)

        return result

    @router.get("/users/{user_id}/captures/{capture_id}/photo")
    def capture_photo(
        user_id: str, capture_id: str, authorization: str | None = Header(default=None)
    ):
        require_owner(user_id, authorization)
        run_handler(service.require_user, user_id)
        capture = service.db.fetchone(
            "SELECT raw_ref FROM photo_captures WHERE id=? AND user_id=?",
            (capture_id, user_id),
        )
        if not capture:
            raise HTTPException(404, "Photo not found")
        try:
            raw = service.photos.read(capture["raw_ref"])
        except (KeyError, FileNotFoundError):
            raise HTTPException(404, "Photo is no longer available") from None
        with Image.open(io.BytesIO(raw)) as source:
            image = ImageOps.exif_transpose(source).convert("RGB")
            image.thumbnail((1600, 1600))
            output = io.BytesIO()
            image.save(output, format="JPEG", quality=85)
        return Response(
            output.getvalue(),
            media_type="image/jpeg",
            headers={
                "Cache-Control": "private, no-store",
                "X-Content-Type-Options": "nosniff",
            },
        )

    @router.get("/users/{user_id}/captures/{capture_id}")
    def capture_detail(
        user_id: str,
        capture_id: str,
        vertical: str = "skin",
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Owner-scoped capture detail for result recovery and deep links."""
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.capture_detail, user_id, capture_id, vertical
        )
        return result

    @router.post("/users/{user_id}/comparisons")
    @router.post("/users/{user_id}/compare")
    def compare_captures(
        user_id: str,
        payload: ComparisonCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        """Compare two owner-scoped captures using the Luna vision stage."""
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.compare_captures,
            user_id,
            payload.earlier_capture_id,
            payload.later_capture_id,
            payload.vertical,
        )
        return result

    @router.get("/users/{user_id}/comparisons/{comparison_id}")
    def comparison_detail(
        user_id: str,
        comparison_id: str,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.comparison_detail, user_id, comparison_id
        )
        return result

    @router.get("/users/{user_id}/check-ins")
    def check_ins(
        user_id: str, limit: int = 30, authorization: str | None = Header(default=None)
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        result: list[dict[str, Any]] = run_handler(service.check_ins, user_id, limit)
        return result

    @router.post("/users/{user_id}/check-ins")
    def create_check_in(
        user_id: str,
        payload: CheckInCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.create_check_in, user_id, **payload.model_dump()
        )
        return result

    @router.get("/users/{user_id}/weekly-recap")
    def weekly_recap(
        user_id: str,
        vertical: str = "skin",
        as_of: str | None = None,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.weekly_recap, user_id, vertical, as_of
        )
        return result

    @router.post("/users/{user_id}/measurement-feedback")
    def measurement_feedback(
        user_id: str,
        payload: MeasurementFeedbackCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.add_measurement_feedback,
            user_id,
            payload.capture_id,
            payload.agreement,
            payload.note,
        )
        return result

    @router.get("/users/{user_id}/labels")
    def labels(
        user_id: str, authorization: str | None = Header(default=None)
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        result: list[dict[str, Any]] = run_handler(service.labels, user_id)
        return result

    @router.post("/users/{user_id}/labels")
    def add_label(
        user_id: str,
        payload: LabelCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.add_label,
            user_id,
            payload.photo_id,
            payload.label_type,
            payload.value,
            payload.confidence,
            payload.notes,
        )
        return result

    @router.post("/users/{user_id}/reprocess")
    def reprocess(
        user_id: str,
        payload: ReprocessCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(
            service.reprocess, user_id, payload.model_version
        )
        return result

    @router.get("/users/{user_id}/reprocess/{job_id}")
    def reprocess_status(
        user_id: str, job_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.reprocess_status, user_id, job_id)
        return result

    @router.post("/users/{user_id}/shelf-scan")
    def shelf_scan(
        user_id: str,
        payload: ShelfScanCreate,
        authorization: str | None = Header(default=None),
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        try:
            image = base64.b64decode(payload.image_base64, validate=True)
        except (binascii.Error, ValueError) as exc:
            raise HTTPException(
                status_code=400, detail="image_base64 must be valid base64"
            ) from exc
        if len(image) > MAX_IMAGE_BYTES:
            raise HTTPException(status_code=413, detail="image is too large")
        result: dict[str, Any] = run_handler(service.scan_shelf, user_id, image)
        return result

    @router.get("/users/{user_id}/shelf-scan/{job_id}")
    def shelf_scan_status(
        user_id: str, job_id: str, authorization: str | None = Header(default=None)
    ) -> dict[str, Any]:
        require_owner(user_id, authorization)
        result: dict[str, Any] = run_handler(service.shelf_scan_status, user_id, job_id)
        return result

    @router.post("/users/{user_id}/shelf-scan/{job_id}/confirm")
    def shelf_scan_confirm(
        user_id: str,
        job_id: str,
        payload: ShelfScanConfirm,
        authorization: str | None = Header(default=None),
    ) -> list[dict[str, Any]]:
        require_owner(user_id, authorization)
        result: list[dict[str, Any]] = run_handler(
            service.confirm_shelf_scan, user_id, job_id, payload.selections
        )
        return result

    return router
