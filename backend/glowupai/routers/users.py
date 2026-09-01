"""User management router."""

from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, ConfigDict, Field

from ..auth import verify_id_token, AuthError

logger = logging.getLogger(__name__)


class UserCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    name: Optional[str] = Field(default=None, max_length=80)
    focus: Optional[str] = None
    skin_type: Optional[str] = None


class ConsentCreate(BaseModel):
    facial_data: bool
    policy_version: Optional[str] = None


class ExperienceProfileUpdate(BaseModel):
    display_name: Optional[str] = Field(default=None, max_length=80)
    skin_type: Optional[str] = Field(default=None, max_length=40)
    focus_vertical: Optional[str] = None
    goals: Optional[List[str]] = None
    experience_level: Optional[str] = Field(default=None, max_length=80)
    onboarding_complete: Optional[bool] = None


def setup_users_router(service, analytics, run_handler, require_owner) -> APIRouter:
    """Setup user routes with dependencies."""

    # Create a fresh router for each app instance to properly capture the service
    router = APIRouter(prefix="/api", tags=["users"])

    @router.post("/users")
    def create_user(payload: UserCreate) -> Dict[str, Any]:
        # Create user - frontend will update profile separately
        return run_handler(service.create_user, payload.skin_type)

    @router.post("/auth/session")
    def auth_session(authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        if not authorization or not authorization.lower().startswith("bearer "):
            raise HTTPException(status_code=401, detail="missing bearer token")
        token = authorization.split(" ", 1)[1].strip()
        try:
            identity = verify_id_token(token, service.settings.firebase_project_id)
        except AuthError as exc:
            raise HTTPException(status_code=401, detail=str(exc)) from exc

        result = run_handler(
            service.session_for_identity,
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

    @router.get("/users/{id}")
    def get_user(id: str, authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        require_owner(id, authorization)
        return run_handler(service.profile, id)

    @router.get("/users/{user_id}/profile")
    def profile(user_id: str, authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.profile, user_id)

    @router.patch("/users/{user_id}/profile", tags=["profile"])
    def update_experience_profile(
        user_id: str,
        payload: ExperienceProfileUpdate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.update_profile, user_id, **payload.model_dump())

    @router.post("/users/{user_id}/consent")
    def consent(
        user_id: str,
        payload: ConsentCreate,
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(
            service.grant_consent, user_id, payload.facial_data, payload.policy_version
        )

    @router.post("/users/{user_id}/consent/data-collection")
    def consent_data_collection(
        user_id: str,
        payload: Dict[str, Any],
        authorization: Optional[str] = Header(default=None)
    ) -> Dict[str, Any]:
        """Record user consent for data collection."""
        require_owner(user_id, authorization)
        return run_handler(
            service.record_data_collection_consent,
            user_id,
            payload.get("granted", False),
            payload.get("policy_version", "1.0")
        )

    @router.get("/users/{user_id}/export")
    def export_user(user_id: str, authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        require_owner(user_id, authorization)
        return run_handler(service.export_user, user_id)

    @router.delete("/users/{user_id}", status_code=204)
    def delete_user(user_id: str, authorization: Optional[str] = Header(default=None)) -> None:
        require_owner(user_id, authorization)
        run_handler(service.delete_user, user_id)

    return router
