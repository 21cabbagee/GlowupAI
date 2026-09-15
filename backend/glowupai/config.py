from __future__ import annotations

import base64
import math
import os
import re
from dataclasses import dataclass, field
from pathlib import Path


def _load_local_env() -> None:
    """Load the ignored backend ``.env`` file for local development.

    Deployment environments inject their variables before startup. This
    small loader keeps the documented local workflow working without adding a
    runtime dependency, and never overrides an explicitly exported variable.
    Production processes do not read a local dotenv file when their
    environment is already marked as production.
    """

    if os.getenv("GLOWUPAI_DISABLE_LOCAL_ENV", "").strip().casefold() in {
        "1",
        "true",
        "yes",
        "on",
    }:
        return
    if os.getenv("GLOWUPAI_ENV", "").strip().casefold() in {"prod", "production"}:
        return

    candidates = (
        Path(__file__).resolve().parents[1] / ".env",
        Path.cwd() / ".env",
    )
    env_path = next((path for path in candidates if path.is_file()), None)
    if env_path is None:
        return

    try:
        lines = env_path.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeError):
        return

    for raw_line in lines:
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        name, separator, value = line.partition("=")
        name = name.strip()
        if not separator or not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", name):
            continue
        if name in os.environ:
            continue
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]
        os.environ[name] = value


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


def play_billing_enabled() -> bool:
    """Whether Google Play purchase verification is enabled for this deployment.

    Billing is deliberately opt-in for production launches that are not yet in
    a Play testing track.  When disabled, the API exposes no purchasable
    products and does not require Play service-account or RTDN credentials.
    """

    return os.getenv("GLOWUPAI_PLAY_BILLING_ENABLED", "1").strip().casefold() not in {
        "0",
        "false",
        "no",
        "off",
    }


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
    # The paid OpenAI vision/language provider is deliberately opt-in.  A
    # missing key or zero spend cap disables it rather than making requests
    # with an implicit/unbounded budget.
    luna_api_key: str | None = None
    luna_model: str = "gpt-5.6-luna"
    luna_endpoint: str = "https://api.openai.com/v1/responses"
    luna_enabled: bool = False
    luna_reasoning_effort: str = "medium"
    luna_monthly_spend_cap_usd: float = 0.0
    luna_reasoning_fallback: bool = True
    luna_image_max_edge: int = 768
    luna_image_jpeg_quality: int = 82
    luna_image_detail: str = "low"
    luna_label_detail: str = "auto"
    luna_escalate_detail: bool = False
    # Conservative reservation estimates used before the provider returns
    # usage. They make the explicit monthly cap enforceable even when a
    # provider response omits cost metadata; tune only from measured billing
    # data, never from a client request.
    luna_image_estimated_tokens: int = 3500
    luna_image_estimated_cost_usd: float = 0.001
    luna_fallback_estimated_tokens: int = 1200
    luna_fallback_estimated_cost_usd: float = 0.0005
    luna_max_requests_per_window: int | None = None
    luna_max_tokens_per_window: int | None = None
    luna_max_cost_usd_per_window: float | None = None
    luna_user_daily_limit: int | None = None
    gemini_personal_data_enabled: bool = False
    gemini_eligibility_review_id: str | None = None
    ai_max_attempts: int = 2
    # Captures are synchronous today, so keep provider work well under the
    # API's 30-second request deadline. A timed-out AI result is safely stored
    # as unavailable; it must never make a valid selfie upload fail.
    ai_timeout_seconds: int = 12
    ai_daily_user_limit: int = 0
    supabase_url: str | None = None
    supabase_jwks_url: str | None = None
    supabase_jwt_secret: str | None = None
    supabase_service_role_key: str | None = None
    supabase_storage_bucket: str = "user-images"
    auth_required: bool = False
    admin_token: str | None = None
    allowed_origins: list[str] = field(default_factory=list)  # CORS allowed origins
    # Production settings
    log_level: str = "INFO"
    json_logs: bool = True
    rate_limit_enabled: bool = True
    request_timeout: int = 30  # seconds
    otel_enabled: bool = False

    @property
    def is_production(self) -> bool:
        return os.getenv("GLOWUPAI_ENV", "development").strip().casefold() in {
            "prod",
            "production",
        }

    def validate_for_production(self) -> None:
        """Fail closed before serving any production request.

        These checks intentionally validate configuration, not connectivity to
        external providers. Provider smoke tests belong to deployment checks.
        """
        if not self.is_production:
            return
        failures: list[str] = []
        if not self.auth_required:
            failures.append("GLOWUPAI_AUTH_REQUIRED must be enabled")
        if not self.supabase_url or not self.supabase_url.startswith("https://"):
            failures.append("SUPABASE_URL must be an HTTPS project URL")
        if not self.supabase_jwt_secret and not self.supabase_jwks_url:
            failures.append(
                "SUPABASE_JWT_SECRET or SUPABASE_JWKS_URL is required for token verification"
            )
        if self.supabase_jwks_url and not self.supabase_jwks_url.startswith("https://"):
            failures.append("SUPABASE_JWKS_URL must use HTTPS")
        if not self.database_url or not self.database_url.lower().startswith(
            ("postgresql://", "postgres://")
        ):
            failures.append("SUPABASE_DB_URL or DATABASE_URL must point to PostgreSQL")
        if not self.allowed_origins or any(
            not origin.startswith("https://") for origin in self.allowed_origins
        ):
            failures.append(
                "GLOWUPAI_ALLOWED_ORIGINS must contain only approved HTTPS origins"
            )
        if not self.supabase_service_role_key:
            failures.append(
                "SUPABASE_SERVICE_ROLE_KEY or SUPABASE_SECRET_KEY is required for private image storage"
            )
        if not self.supabase_storage_bucket:
            failures.append("SUPABASE_STORAGE_BUCKET is required")
        if play_billing_enabled():
            billing_key = os.getenv("GLOWUPAI_BILLING_KEY", "").strip()
            try:
                if len(base64.b64decode(billing_key, validate=True)) != 32:
                    raise ValueError
            except (ValueError, TypeError):
                failures.append(
                    "GLOWUPAI_BILLING_KEY must be base64 for exactly 32 bytes"
                )
            if not os.getenv("GLOWUPAI_PLAY_PACKAGE_NAME", "").strip():
                failures.append("GLOWUPAI_PLAY_PACKAGE_NAME is required")
            if not os.getenv("GLOWUPAI_PLAY_PRODUCT_IDS", "").strip():
                failures.append("GLOWUPAI_PLAY_PRODUCT_IDS is required")
            rtdn_audience = os.getenv("GLOWUPAI_PLAY_RTDN_AUDIENCE", "").strip()
            if not rtdn_audience or not rtdn_audience.startswith("https://"):
                failures.append(
                    "GLOWUPAI_PLAY_RTDN_AUDIENCE must be an HTTPS push endpoint"
                )
            if not os.getenv("GLOWUPAI_PLAY_RTDN_EMAIL", "").strip():
                failures.append("GLOWUPAI_PLAY_RTDN_EMAIL is required")
            if not (
                os.getenv("GLOWUPAI_PLAY_SERVICE_ACCOUNT_JSON_B64", "").strip()
                or os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "").strip()
            ):
                failures.append(
                    "GLOWUPAI_PLAY_SERVICE_ACCOUNT_JSON_B64 or GOOGLE_APPLICATION_CREDENTIALS is required"
                )
        if os.getenv("GLOWUPAI_SKIP_QUALITY_CHECKS", "0").strip().casefold() in {
            "1",
            "true",
            "yes",
            "on",
        }:
            failures.append("GLOWUPAI_SKIP_QUALITY_CHECKS cannot be enabled")
        if not self.admin_token or len(self.admin_token) < 32:
            failures.append("GLOWUPAI_ADMIN_TOKEN must be at least 32 characters")
        if self.raw_photo_retention_days <= 0:
            failures.append("GLOWUPAI_RAW_RETENTION_DAYS must be positive")
        if self.luna_enabled:
            if not self.luna_api_key:
                failures.append("OPENAI_API_KEY is required when Luna is enabled")
            if self.luna_model != "gpt-5.6-luna":
                failures.append("GLOWUPAI_LUNA_MODEL must be gpt-5.6-luna")
            if self.luna_reasoning_effort != "medium":
                failures.append("GLOWUPAI_LUNA_REASONING_EFFORT must be medium")
            if (
                not math.isfinite(self.luna_monthly_spend_cap_usd)
                or self.luna_monthly_spend_cap_usd <= 0
            ):
                failures.append(
                    "GLOWUPAI_LUNA_MONTHLY_SPEND_CAP_USD must be positive when Luna is enabled"
                )
            for name, value in (
                (
                    "GLOWUPAI_LUNA_IMAGE_ESTIMATED_COST_USD",
                    self.luna_image_estimated_cost_usd,
                ),
                (
                    "GLOWUPAI_LUNA_FALLBACK_ESTIMATED_COST_USD",
                    self.luna_fallback_estimated_cost_usd,
                ),
            ):
                if not math.isfinite(value) or value <= 0:
                    failures.append(f"{name} must be a finite positive number")
            if self.luna_max_cost_usd_per_window is not None and (
                not math.isfinite(self.luna_max_cost_usd_per_window)
                or self.luna_max_cost_usd_per_window <= 0
            ):
                failures.append(
                    "GLOWUPAI_LUNA_MAX_COST_USD_PER_WINDOW must be finite and positive"
                )
        if self.gemini_personal_data_enabled and not self.gemini_eligibility_review_id:
            failures.append("personal Gemini data requires an eligibility review ID")
        if failures:
            raise RuntimeError(
                "Invalid production configuration: " + "; ".join(failures)
            )

    @classmethod
    def from_env(cls) -> Settings:
        _load_local_env()
        db_path = Path(os.getenv("GLOWUPAI_DB_PATH", ".data/glowupai.sqlite3"))
        database_url = (
            os.getenv("SUPABASE_DB_URL", "").strip()
            or os.getenv("DATABASE_URL", "").strip()
            or None
        )
        photo_dir_value = os.getenv("GLOWUPAI_PHOTO_DIR", "").strip()
        gemini_api_key = (
            os.getenv("GLOWUPAI_GEMINI_API_KEY", "").strip()
            or os.getenv("GEMINI_API_KEY", "").strip()
            or _legacy_gemini_key()
        )
        luna_api_key = (
            os.getenv("GLOWUPAI_LUNA_API_KEY", "").strip()
            or os.getenv("OPENAI_API_KEY", "").strip()
        )
        enabled_value = os.getenv("GLOWUPAI_GEMINI_ENABLED", "1").strip().casefold()
        luna_enabled_value = os.getenv("GLOWUPAI_LUNA_ENABLED", "0").strip().casefold()
        luna_fallback_value = (
            os.getenv("GLOWUPAI_LUNA_REASONING_FALLBACK", "1").strip().casefold()
        )
        luna_escalate_value = (
            os.getenv("GLOWUPAI_LUNA_ESCALATE_DETAIL", "0").strip().casefold()
        )
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
            if env_name in {"prod", "production"}:
                # Production must declare its consumer origins explicitly.
                allowed_origins = []
            else:
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
            gemini_api_key=gemini_api_key or None,
            gemini_model=os.getenv("GLOWUPAI_GEMINI_MODEL", "gemini-3.5-flash-lite"),
            gemini_enabled=enabled_value not in {"0", "false", "no", "off"},
            luna_api_key=luna_api_key or None,
            luna_model=os.getenv("GLOWUPAI_LUNA_MODEL", "gpt-5.6-luna").strip()
            or "gpt-5.6-luna",
            luna_endpoint=os.getenv(
                "GLOWUPAI_LUNA_ENDPOINT", "https://api.openai.com/v1/responses"
            ).strip()
            or "https://api.openai.com/v1/responses",
            luna_enabled=luna_enabled_value in {"1", "true", "yes", "on"},
            luna_reasoning_effort=os.getenv(
                "GLOWUPAI_LUNA_REASONING_EFFORT", "medium"
            ).strip()
            or "medium",
            luna_monthly_spend_cap_usd=max(
                0.0,
                float(os.getenv("GLOWUPAI_LUNA_MONTHLY_SPEND_CAP_USD", "0")),
            ),
            luna_reasoning_fallback=luna_fallback_value in {"1", "true", "yes", "on"},
            luna_image_max_edge=max(
                256, int(os.getenv("GLOWUPAI_LUNA_IMAGE_MAX_EDGE", "768"))
            ),
            luna_image_jpeg_quality=min(
                95,
                max(50, int(os.getenv("GLOWUPAI_LUNA_IMAGE_JPEG_QUALITY", "82"))),
            ),
            luna_image_detail=os.getenv("GLOWUPAI_LUNA_IMAGE_DETAIL", "low").strip()
            or "low",
            luna_label_detail=os.getenv("GLOWUPAI_LUNA_LABEL_DETAIL", "auto").strip()
            or "auto",
            luna_escalate_detail=luna_escalate_value in {"1", "true", "yes", "on"},
            luna_image_estimated_tokens=max(
                1, int(os.getenv("GLOWUPAI_LUNA_IMAGE_ESTIMATED_TOKENS", "3500"))
            ),
            luna_image_estimated_cost_usd=max(
                0.0001,
                float(os.getenv("GLOWUPAI_LUNA_IMAGE_ESTIMATED_COST_USD", "0.001")),
            ),
            luna_fallback_estimated_tokens=max(
                1, int(os.getenv("GLOWUPAI_LUNA_FALLBACK_ESTIMATED_TOKENS", "1200"))
            ),
            luna_fallback_estimated_cost_usd=max(
                0.0001,
                float(os.getenv("GLOWUPAI_LUNA_FALLBACK_ESTIMATED_COST_USD", "0.0005")),
            ),
            luna_max_requests_per_window=(
                max(1, int(os.environ["GLOWUPAI_LUNA_MAX_REQUESTS_PER_WINDOW"]))
                if os.getenv("GLOWUPAI_LUNA_MAX_REQUESTS_PER_WINDOW")
                else None
            ),
            luna_max_tokens_per_window=(
                max(1, int(os.environ["GLOWUPAI_LUNA_MAX_TOKENS_PER_WINDOW"]))
                if os.getenv("GLOWUPAI_LUNA_MAX_TOKENS_PER_WINDOW")
                else None
            ),
            luna_max_cost_usd_per_window=(
                max(0.0001, float(os.environ["GLOWUPAI_LUNA_MAX_COST_USD_PER_WINDOW"]))
                if os.getenv("GLOWUPAI_LUNA_MAX_COST_USD_PER_WINDOW")
                else None
            ),
            luna_user_daily_limit=(
                max(1, int(os.environ["GLOWUPAI_LUNA_USER_DAILY_LIMIT"]))
                if os.getenv("GLOWUPAI_LUNA_USER_DAILY_LIMIT")
                else None
            ),
            gemini_personal_data_enabled=os.getenv(
                "GLOWUPAI_GEMINI_PERSONAL_DATA_ENABLED", "0"
            )
            .strip()
            .casefold()
            in {"1", "true", "yes", "on"},
            gemini_eligibility_review_id=os.getenv(
                "GLOWUPAI_GEMINI_ELIGIBILITY_REVIEW_ID", ""
            ).strip()
            or None,
            ai_max_attempts=max(1, int(os.getenv("GLOWUPAI_AI_MAX_ATTEMPTS", "2"))),
            ai_timeout_seconds=max(
                5, int(os.getenv("GLOWUPAI_AI_TIMEOUT_SECONDS", "12"))
            ),
            ai_daily_user_limit=max(
                0, int(os.getenv("GLOWUPAI_AI_DAILY_USER_LIMIT", "0"))
            ),
            supabase_url=(supabase_url := os.getenv("SUPABASE_URL", "").strip())
            or None,
            supabase_jwks_url=(
                os.getenv("SUPABASE_JWKS_URL", "").strip()
                or (
                    f"{supabase_url.rstrip('/')}/auth/v1/.well-known/jwks.json"
                    if supabase_url
                    else ""
                )
                or None
            ),
            supabase_jwt_secret=os.getenv("SUPABASE_JWT_SECRET", "").strip() or None,
            supabase_service_role_key=(
                os.getenv("SUPABASE_SERVICE_ROLE_KEY", "").strip()
                or os.getenv("SUPABASE_SECRET_KEY", "").strip()
                or None
            ),
            supabase_storage_bucket=os.getenv(
                "SUPABASE_STORAGE_BUCKET", "user-images"
            ).strip()
            or "user-images",
            auth_required=(
                auth_required_value in {"1", "true", "yes", "on"}
                if "GLOWUPAI_AUTH_REQUIRED" in os.environ
                else env_name in {"prod", "production"}
            ),
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
