"""Graceful shutdown handling for production deployments."""

from __future__ import annotations

import asyncio
import logging
import signal
import sys
from collections.abc import Callable

logger = logging.getLogger(__name__)


class GracefulShutdown:
    """Handle graceful shutdown on SIGTERM/SIGINT."""

    def __init__(self, cleanup_handlers: list[Callable] | None = None):
        """Initialize shutdown handler.

        Args:
            cleanup_handlers: List of cleanup functions to call on shutdown
        """
        self.cleanup_handlers = cleanup_handlers or []
        self.is_shutting_down = False

    def register_cleanup(self, handler: Callable):
        """Register a cleanup handler.

        Args:
            handler: Cleanup function to call on shutdown
        """
        self.cleanup_handlers.append(handler)

    async def shutdown(self):
        """Execute graceful shutdown."""
        if self.is_shutting_down:
            logger.warning("Shutdown already in progress")
            return

        self.is_shutting_down = True
        logger.info("Starting graceful shutdown...")

        # Run cleanup handlers
        for handler in self.cleanup_handlers:
            try:
                if asyncio.iscoroutinefunction(handler):
                    await handler()
                else:
                    handler()
                logger.info(f"Cleanup handler completed: {handler.__name__}")
            except Exception as exc:
                logger.error(f"Error in cleanup handler {handler.__name__}: {exc}")

        logger.info("Graceful shutdown complete")

    def setup_signal_handlers(self):
        """Setup signal handlers for SIGTERM and SIGINT."""

        def signal_handler(signum, frame):
            logger.info(f"Received signal {signum}, initiating shutdown")
            # Create task for async shutdown
            asyncio.create_task(self.shutdown())

        # Register signal handlers
        signal.signal(signal.SIGTERM, signal_handler)
        signal.signal(signal.SIGINT, signal_handler)

        logger.info("Signal handlers registered for graceful shutdown")


def create_shutdown_handler(
    app, db_close_func: Callable | None = None,
) -> GracefulShutdown:
    """Create shutdown handler for the application.

    Args:
        app: FastAPI application instance
        db_close_func: Optional database close function

    Returns:
        Configured GracefulShutdown instance
    """
    shutdown_handler = GracefulShutdown()

    # Register database cleanup
    if db_close_func:
        shutdown_handler.register_cleanup(db_close_func)

    # Register app cleanup
    @app.on_event("shutdown")
    async def app_shutdown():
        """FastAPI shutdown event handler."""
        await shutdown_handler.shutdown()

    return shutdown_handler
