"""FastAPI routers for GlowUpAI API."""

from .admin import setup_admin_router
from .analytics import setup_analytics_router
from .captures import setup_captures_router
from .subscriptions import setup_subscriptions_router
from .users import setup_users_router

__all__ = [
    "setup_users_router",
    "setup_captures_router",
    "setup_analytics_router",
    "setup_subscriptions_router",
    "setup_admin_router",
]
