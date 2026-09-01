from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from pathlib import Path


def _legacy_gemini_key() -> str | None:
    """Read the existing local key file without executing or logging it.

    This is a migration bridge for the current workspace only. Production
    deployments should use GEMINI_API_KEY or GLOWUPAI_GEMINI_API_KEY and can
    disable this bridge with GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1.
    """

    if os.getenv("GLOWUPAI_DISABLE_LEGACY_KEY_FILE", "").strip() == "1":
        return None
    if os.getenv("GLOWUPAI_ENV", "development").strip().casefold() in {
        "prod",
        "production",
    }:
        return None
    path = Path(os.getenv("GLOWUPAI_LEGACY_KEY_FILE", "first.py"))
    try:
        source = path.read_text(encoding="utf-8")
    except (OSError, UnicodeError):
        return None
    # Parse a quoted value only; never eval/import the file.
    match = re.search(r"['\"]([^'\"]{20,})['\"]", source)
    return match.group(1).strip() if match else None


@dataclass(frozen=True)
class Settings:
    db_path: Path
    photo_dir: Path | None
    database_url: str | None = None
    database_pool_min_size: int = 1
    database_pool_max_size: int = 10
    database_connect_timeout: int = 10
    database_statement_timeout: int = 30000  # milliseconds
    database_pool_timeout: int = 30  # seconds
    raw_photo_retention_days: int = 730
    model_version: str = "deterministic-3.0"
    policy_version: str = "2026-01"
    gemini_api_key: str | None = None
    gemini_model: str = "gemini-3.5-flash-lite"
    gemini_enabled: bool = True
    firebase_project_id: str | None = None
    auth_required: bool = False
    admin_token: str | None = None
    allowed_origins: list[str] = field(default_factory=list)  # CORS allowed origins
    # Production settings
    log_level: str = "INFO"
    json_logs: bool = True
    rate_limit_enabled: bool = True
    request_timeout: int = 30  # seconds
    otel_enabled: bool = False

    @classmethod
    def from_env(cls) -> Settings:
        db_path = Path(os.getenv("GLOWUPAI_DB_PATH", ".data/glowupai.sqlite3"))
        database_url = (
            os.getenv("GLOWUPAI_DATABASE_URL", "").strip()
            or os.getenv("DATABASE_URL", "").strip()
            or os.getenv("POSTGRES_URL", "").strip()
            or None
        )
        photo_dir_value = os.getenv("GLOWUPAI_PHOTO_DIR", "").strip()
        api_key = (
            os.getenv("GLOWUPAI_GEMINI_API_KEY", "").strip()
            or os.getenv("GEMINI_API_KEY", "").strip()
            or _legacy_gemini_key()
        )
        enabled_value = os.getenv("GLOWUPAI_GEMINI_ENABLED", "1").strip().casefold()
        auth_required_value = (
            os.getenv("GLOWUPAI_AUTH_REQUIRED", "0").strip().casefold()
        )
        # CORS allowed origins - comma-separated list or single origin
        allowed_origins_env = os.getenv("GLOWUPAI_ALLOWED_ORIGINS", "").strip()
        env_name = os.getenv("GLOWUPAI_ENV", "development").strip().casefold()

        if allowed_origins_env:
            allowed_origins = [
                origin.strip() for origin in allowed_origins_env.split(",")
            ]
        else:
            # Production must explicitly set CORS origins - fail fast if not configured
            if env_name in {"prod", "production"}:
                raise RuntimeError(
                    "GLOWUPAI_ALLOWED_ORIGINS must be explicitly configured in production. "
                    "Set comma-separated origins (e.g., GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.ai)",
                )
            # Development default: allow localhost and emulator
            allowed_origins = [
                "http://localhost:3000",
                "http://localhost:8000",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8000",
                "http://10.0.2.2:8000",  # Android emulator
            ]
        return cls(
            db_path=db_path,
            photo_dir=Path(photo_dir_value) if photo_dir_value else None,
            database_url=database_url,
            database_pool_min_size=max(
                1,
                int(os.getenv("GLOWUPAI_DB_POOL_MIN_SIZE", "1")),
            ),
            database_pool_max_size=max(
                1,
                int(os.getenv("GLOWUPAI_DB_POOL_MAX_SIZE", "10")),
            ),
            database_connect_timeout=max(
                1,
                int(os.getenv("GLOWUPAI_DB_CONNECT_TIMEOUT", "10")),
            ),
            database_statement_timeout=max(
                1000,
                int(os.getenv("GLOWUPAI_DB_STATEMENT_TIMEOUT", "30000")),
            ),
            database_pool_timeout=max(
                5,
                int(os.getenv("GLOWUPAI_DB_POOL_TIMEOUT", "30")),
            ),
            raw_photo_retention_days=int(
                os.getenv("GLOWUPAI_RAW_RETENTION_DAYS", "730"),
            ),
            model_version=os.getenv("GLOWUPAI_MODEL_VERSION", "deterministic-3.0"),
            policy_version=os.getenv("GLOWUPAI_POLICY_VERSION", "2026-01"),
            gemini_api_key=api_key or None,
            gemini_model=os.getenv("GLOWUPAI_GEMINI_MODEL", "gemini-3.5-flash-lite"),
            gemini_enabled=enabled_value not in {"0", "false", "no", "off"},
            firebase_project_id=os.getenv("GLOWUPAI_FIREBASE_PROJECT_ID", "").strip()
            or None,
            # Auth defaults OFF: the existing test suite and the unauthenticated
            # web client carry no bearer tokens and must keep passing/working.
            auth_required=auth_required_value in {"1", "true", "yes", "on"},
            admin_token=os.getenv("GLOWUPAI_ADMIN_TOKEN", "").strip() or None,
            allowed_origins=allowed_origins,
            # Production settings
            log_level=os.getenv("GLOWUPAI_LOG_LEVEL", "INFO").upper(),
            json_logs=os.getenv("GLOWUPAI_JSON_LOGS", "1")
            in {"1", "true", "yes", "on"},
            rate_limit_enabled=os.getenv("GLOWUPAI_RATE_LIMIT_ENABLED", "1")
            in {"1", "true", "yes", "on"},
            request_timeout=max(5, int(os.getenv("GLOWUPAI_REQUEST_TIMEOUT", "30"))),
            otel_enabled=os.getenv("OTEL_ENABLED", "0") in {"1", "true", "yes", "on"},
        )

    def prepare(self) -> None:
        if not self.database_url:
            self.db_path.parent.mkdir(parents=True, exist_ok=True)
        if self.photo_dir:
            self.photo_dir.mkdir(parents=True, exist_ok=True)
