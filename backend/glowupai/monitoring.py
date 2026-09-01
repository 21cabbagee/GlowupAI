"""Sentry error monitoring and performance tracking for production."""

from __future__ import annotations

import logging
import os
from typing import Any

logger = logging.getLogger(__name__)


def setup_sentry(
    dsn: str | None = None,
    environment: str = "production",
    traces_sample_rate: float = 0.1,
    profiles_sample_rate: float = 0.1,
) -> bool:
    """Initialize Sentry SDK for error monitoring and performance tracking.

    Args:
        dsn: Sentry DSN (Data Source Name)
        environment: Environment name (production/staging/development)
        traces_sample_rate: Percentage of transactions to trace (0.0-1.0)
        profiles_sample_rate: Percentage of transactions to profile (0.0-1.0)

    Returns:
        True if Sentry was initialized successfully, False otherwise
    """
    if not dsn:
        logger.info("Sentry DSN not provided, error monitoring disabled")
        return False

    try:
        import sentry_sdk
        from sentry_sdk.integrations.fastapi import FastApiIntegration
        from sentry_sdk.integrations.logging import LoggingIntegration
        from sentry_sdk.integrations.starlette import StarletteIntegration

        # Configure logging integration
        logging_integration = LoggingIntegration(
            level=logging.INFO,  # Capture info and above as breadcrumbs
            event_level=logging.ERROR,  # Send errors and above as events
        )

        sentry_sdk.init(
            dsn=dsn,
            environment=environment,
            traces_sample_rate=traces_sample_rate,
            profiles_sample_rate=profiles_sample_rate,
            integrations=[
                FastApiIntegration(),
                StarletteIntegration(),
                logging_integration,
            ],
            # Set release version if available
            release=os.getenv("GIT_COMMIT", os.getenv("RAILWAY_GIT_COMMIT_SHA", None)),
            # Send default PII (Personally Identifiable Information)
            send_default_pii=False,  # Don't send user IP addresses
            # Performance monitoring
            enable_tracing=True,
            # Additional context
            attach_stacktrace=True,
            # Filter out health check transactions
            before_send_transaction=_filter_transactions,
        )

        logger.info(
            f"Sentry initialized: environment={environment}, "
            f"traces_sample_rate={traces_sample_rate}",
        )
        return True

    except ImportError:
        logger.warning(
            "Sentry SDK not installed. Install with: pip install sentry-sdk[fastapi]",
        )
        return False
    except Exception as exc:
        logger.error(f"Failed to initialize Sentry: {exc}")
        return False


def _filter_transactions(event: dict, hint: dict) -> dict | None:
    """Filter out health check and metrics transactions from Sentry."""
    url = event.get("request", {}).get("url", "")

    # Don't send health checks and metrics to Sentry
    if any(
        path in url
        for path in ["/api/health", "/api/metrics", "/health", "/healthz"]
    ):
        return None

    return event


def capture_exception(exc: Exception, context: dict[str, Any] | None = None) -> None:
    """Capture an exception and send it to Sentry with additional context.

    Args:
        exc: Exception to capture
        context: Additional context to attach to the event
    """
    try:
        import sentry_sdk

        if context:
            with sentry_sdk.push_scope() as scope:
                for key, value in context.items():
                    scope.set_context(key, value)
                sentry_sdk.capture_exception(exc)
        else:
            sentry_sdk.capture_exception(exc)

    except ImportError:
        logger.debug("Sentry SDK not available, skipping exception capture")
    except Exception as capture_exc:
        logger.error(f"Failed to capture exception in Sentry: {capture_exc}")


def capture_message(
    message: str, level: str = "info", context: dict[str, Any] | None = None,
) -> None:
    """Capture a message and send it to Sentry with additional context.

    Args:
        message: Message to capture
        level: Severity level (debug/info/warning/error/fatal)
        context: Additional context to attach to the event
    """
    try:
        import sentry_sdk

        if context:
            with sentry_sdk.push_scope() as scope:
                for key, value in context.items():
                    scope.set_context(key, value)
                sentry_sdk.capture_message(message, level=level)
        else:
            sentry_sdk.capture_message(message, level=level)

    except ImportError:
        logger.debug("Sentry SDK not available, skipping message capture")
    except Exception as capture_exc:
        logger.error(f"Failed to capture message in Sentry: {capture_exc}")


def set_user(user_id: str, email: str | None = None) -> None:
    """Set user context for Sentry events.

    Args:
        user_id: User ID
        email: User email (optional)
    """
    try:
        import sentry_sdk

        sentry_sdk.set_user({"id": user_id, "email": email})

    except ImportError:
        pass
    except Exception as exc:
        logger.error(f"Failed to set Sentry user context: {exc}")


def set_tag(key: str, value: str) -> None:
    """Set a tag for Sentry events.

    Args:
        key: Tag key
        value: Tag value
    """
    try:
        import sentry_sdk

        sentry_sdk.set_tag(key, value)

    except ImportError:
        pass
    except Exception as exc:
        logger.error(f"Failed to set Sentry tag: {exc}")


def start_transaction(name: str, op: str) -> Any:
    """Start a Sentry performance transaction.

    Args:
        name: Transaction name
        op: Operation type (e.g., 'http.server', 'db.query')

    Returns:
        Transaction object or None
    """
    try:
        import sentry_sdk

        return sentry_sdk.start_transaction(name=name, op=op)

    except ImportError:
        return None
    except Exception as exc:
        logger.error(f"Failed to start Sentry transaction: {exc}")
        return None


class SentryMiddleware:
    """Middleware to add Sentry context to requests."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        """Add request context to Sentry."""
        if scope["type"] == "http":
            try:
                import sentry_sdk

                with sentry_sdk.configure_scope() as sentry_scope:
                    # Add request ID if available
                    headers = dict(scope.get("headers", []))
                    request_id = headers.get(b"x-request-id", b"").decode()
                    if request_id:
                        sentry_scope.set_tag("request_id", request_id)

                    # Add endpoint info
                    path = scope.get("path", "")
                    method = scope.get("method", "")
                    sentry_scope.set_tag("endpoint", f"{method} {path}")

            except ImportError:
                pass
            except Exception as exc:
                logger.error(f"Failed to set Sentry context: {exc}")

        await self.app(scope, receive, send)
