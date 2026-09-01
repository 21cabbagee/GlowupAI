"""FastAPI routers for SkinProof API."""

from .admin import router as admin_router
from .analytics import router as analytics_router
from .captures import router as captures_router
from .subscriptions import router as subscriptions_router
from .users import router as users_router

__all__ = [
    "users_router",
    "captures_router",
    "analytics_router",
    "subscriptions_router",
    "admin_router",
]
