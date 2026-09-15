"""Production operations primitives.

The application is intentionally usable without an operations vendor in local
development, but production still needs observable and reversible controls.
This module keeps those controls small, dependency-light, and safe to call
from multiple FastAPI workers:

* :class:`AlertManager` sends deduplicated, payload-free alerts.
* :class:`RuntimeControls` stores distributed kill switches in Redis when it is
  available and falls back to an atomically-written local file.
* :class:`BackupManager` creates and verifies SQLite or PostgreSQL database
  backups. Object storage is deliberately reported as a separate dependency;
  copying a database cannot restore image blobs.
"""

from __future__ import annotations

import asyncio
import hashlib
import json
import logging
import os
import shutil
import sqlite3
import subprocess  # nosec B404
import tempfile
import time
import urllib.error
import urllib.request
import uuid
from collections.abc import Callable, Mapping
from datetime import UTC, datetime
from pathlib import Path
from threading import RLock
from typing import Any
from urllib.parse import quote, unquote, urlsplit, urlunsplit

from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

logger = logging.getLogger(__name__)

KILL_SWITCHES = frozenset(
    {
        "maintenance",
        "signups",
        "captures",
        "ai",
        "billing",
        "reprocessing",
    }
)
_HEALTH_PATHS = {
    "/api/health",
    "/api/ready",
    "/api/live",
    "/api/metrics",
    "/internal/metrics",
}


def _truthy(value: str | None, default: bool = False) -> bool:
    if value is None:
        return default
    return value.strip().casefold() in {"1", "true", "yes", "on"}


def _utc_now() -> str:
    return datetime.now(UTC).isoformat().replace("+00:00", "Z")


class AlertManager:
    """Send deduplicated operational alerts without request payloads.

    The webhook format is intentionally Slack-compatible (``text``) while
    also including ``content`` for Discord-compatible generic webhooks. A
    missing webhook is not an application error; Sentry or log-based alerts
    can still be used.
    """

    def __init__(
        self,
        webhook_url: str | None = None,
        *,
        cooldown_seconds: int = 900,
        timeout_seconds: int = 5,
        error_rate_threshold: float = 0.05,
        p95_latency_threshold_ms: float = 2000.0,
        minimum_requests: int = 20,
        sender: Callable[[str, bytes, int], None] | None = None,
    ) -> None:
        candidate_url = webhook_url.strip() if webhook_url else None
        if candidate_url and urlsplit(candidate_url).scheme != "https":
            logger.warning("Ignoring non-HTTPS operational alert webhook")
            candidate_url = None
        self.webhook_url = candidate_url
        self.cooldown_seconds = max(0, cooldown_seconds)
        self.timeout_seconds = max(1, timeout_seconds)
        self.error_rate_threshold = max(0.0, error_rate_threshold)
        self.p95_latency_threshold_ms = max(0.0, p95_latency_threshold_ms)
        self.minimum_requests = max(1, minimum_requests)
        self._last_sent: dict[str, float] = {}
        self._active_alerts: set[str] = set()
        self._lock = RLock()
        self._sender = sender

    @classmethod
    def from_env(cls) -> "AlertManager":
        return cls(
            os.getenv("GLOWUPAI_ALERT_WEBHOOK_URL", "").strip() or None,
            cooldown_seconds=int(os.getenv("GLOWUPAI_ALERT_COOLDOWN_SECONDS", "900")),
            timeout_seconds=int(os.getenv("GLOWUPAI_ALERT_TIMEOUT_SECONDS", "5")),
            error_rate_threshold=float(
                os.getenv("GLOWUPAI_ALERT_ERROR_RATE_THRESHOLD", "0.05")
            ),
            p95_latency_threshold_ms=float(
                os.getenv("GLOWUPAI_ALERT_P95_LATENCY_MS", "2000")
            ),
            minimum_requests=int(os.getenv("GLOWUPAI_ALERT_MIN_REQUESTS", "20")),
        )

    @property
    def configured(self) -> bool:
        return bool(self.webhook_url)

    def status(self) -> dict[str, Any]:
        with self._lock:
            return {
                "configured": self.configured,
                "cooldown_seconds": self.cooldown_seconds,
                "thresholds": {
                    "server_error_rate": self.error_rate_threshold,
                    "p95_latency_ms": self.p95_latency_threshold_ms,
                    "minimum_requests": self.minimum_requests,
                },
                "active_alerts": sorted(self._active_alerts),
            }

    def _should_send(self, key: str) -> bool:
        now = time.monotonic()
        with self._lock:
            last = self._last_sent.get(key)
            if last is not None and now - last < self.cooldown_seconds:
                return False
            self._last_sent[key] = now
            return True

    def notify(
        self,
        key: str,
        *,
        severity: str,
        message: str,
        details: Mapping[str, Any] | None = None,
        force: bool = False,
    ) -> bool:
        """Send one alert, returning whether a webhook request was attempted."""
        if not force and not self._should_send(key):
            return False
        if not self.configured:
            logger.warning(
                "Operational alert has no configured webhook",
                extra={"alert_key": key, "severity": severity},
            )
            return False

        payload = {
            "text": f"[{severity.upper()}] GlowupAI: {message}",
            "content": f"[{severity.upper()}] GlowupAI: {message}",
            "severity": severity,
            "alert_key": key,
            "timestamp": _utc_now(),
            # Details are metrics/configuration only. Callers must not put
            # user IDs, image data, tokens, or request bodies here.
            "details": dict(details or {}),
        }
        body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        try:
            if self._sender:
                self._sender(self.webhook_url or "", body, self.timeout_seconds)
            else:
                request = urllib.request.Request(
                    self.webhook_url or "",
                    data=body,
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                # The constructor rejects every non-HTTPS webhook scheme.
                with urllib.request.urlopen(  # nosec B310
                    request, timeout=self.timeout_seconds
                ) as response:
                    if response.status >= 400:
                        raise urllib.error.HTTPError(
                            request.full_url,
                            response.status,
                            "alert webhook returned an error",
                            response.headers,
                            None,
                        )
            logger.info("Operational alert sent", extra={"alert_key": key})
            return True
        except (OSError, urllib.error.URLError, ValueError) as exc:
            logger.error(
                "Operational alert delivery failed",
                extra={"alert_key": key, "error_type": type(exc).__name__},
            )
            return False

    def observe(self, snapshot: Mapping[str, Any]) -> list[str]:
        """Evaluate rolling request metrics and send threshold alerts.

        This method is cheap when no webhook is configured and is safe to call
        after every request. Alerts are deduplicated by ``_should_send``.
        """
        requests = int(snapshot.get("requests", 0))
        if requests < self.minimum_requests:
            return []
        triggered: list[str] = []
        error_rate = float(snapshot.get("server_error_rate", 0.0))
        p95 = float(snapshot.get("p95_duration_ms", 0.0))
        if error_rate >= self.error_rate_threshold:
            key = "http.server_error_rate"
            if key not in self._active_alerts:
                if self.notify(
                    key,
                    severity="critical",
                    message="5xx error rate exceeded threshold",
                    details={
                        "window_requests": requests,
                        "server_error_rate": round(error_rate, 4),
                        "threshold": self.error_rate_threshold,
                    },
                ):
                    with self._lock:
                        self._active_alerts.add(key)
                    triggered.append(key)
        else:
            with self._lock:
                self._active_alerts.discard("http.server_error_rate")

        if p95 >= self.p95_latency_threshold_ms:
            key = "http.p95_latency"
            if key not in self._active_alerts:
                if self.notify(
                    key,
                    severity="warning",
                    message="HTTP p95 latency exceeded threshold",
                    details={
                        "window_requests": requests,
                        "p95_duration_ms": round(p95, 2),
                        "threshold_ms": self.p95_latency_threshold_ms,
                    },
                ):
                    with self._lock:
                        self._active_alerts.add(key)
                    triggered.append(key)
        else:
            with self._lock:
                self._active_alerts.discard("http.p95_latency")
        return triggered

    async def observe_async(self, snapshot: Mapping[str, Any]) -> list[str]:
        """Run alert delivery off the event loop when a webhook is configured."""
        if not self.configured:
            return self.observe(snapshot)
        return await asyncio.to_thread(self.observe, snapshot)


class RuntimeControls:
    """Runtime kill switches with atomic local and optional Redis storage."""

    redis_key = "glowupai:ops:kill_switches"

    def __init__(
        self,
        path: str | Path = ".data/runtime_controls.json",
        *,
        redis_url: str | None = None,
        fail_closed: bool = False,
    ) -> None:
        self.path = Path(path)
        self.fail_closed = fail_closed
        self._lock = RLock()
        self._redis: Any = None
        self._redis_error: str | None = None
        if redis_url:
            try:
                import redis

                self._redis = redis.from_url(
                    redis_url,
                    decode_responses=True,
                    socket_connect_timeout=2,
                    socket_timeout=2,
                )
                self._redis.ping()
            except (ImportError, OSError, ValueError, RuntimeError) as exc:
                self._redis_error = type(exc).__name__
                logger.warning(
                    "Runtime controls Redis is unavailable; using local fallback",
                    extra={"error_type": self._redis_error},
                )
                self._redis = None

    @classmethod
    def from_env(cls, redis_url: str | None = None) -> "RuntimeControls":
        path = os.getenv(
            "GLOWUPAI_CONTROLS_FILE", ".data/runtime_controls.json"
        ).strip()
        environment = os.getenv("GLOWUPAI_ENV", "development").casefold()
        return cls(
            path or ".data/runtime_controls.json",
            redis_url=redis_url,
            fail_closed=_truthy(
                os.getenv("GLOWUPAI_KILL_SWITCH_FAIL_CLOSED"),
                default=environment in {"prod", "production"},
            ),
        )

    def _defaults(self) -> dict[str, dict[str, Any]]:
        return {
            name: {
                "active": _truthy(
                    os.getenv(f"GLOWUPAI_KILL_SWITCH_{name.upper()}"), False
                ),
                "updated_at": None,
                "updated_by": "environment",
            }
            for name in sorted(KILL_SWITCHES)
        }

    def _read_file(self) -> dict[str, dict[str, Any]]:
        switches = self._defaults()
        try:
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            values = raw.get("switches", {})
            if isinstance(values, dict):
                for name in KILL_SWITCHES:
                    value = values.get(name)
                    if isinstance(value, dict):
                        switches[name].update(value)
                        switches[name]["active"] = bool(value.get("active", False))
        except FileNotFoundError:
            pass
        except (OSError, UnicodeError, json.JSONDecodeError, AttributeError) as exc:
            logger.error(
                "Runtime controls file could not be read",
                extra={"error_type": type(exc).__name__},
            )
            if self.fail_closed:
                for value in switches.values():
                    value["active"] = True
        return switches

    def _read_redis(self) -> dict[str, dict[str, Any]] | None:
        if self._redis is None:
            return None
        try:
            values = self._redis.hgetall(self.redis_key)
            if not values:
                return None
            result = self._defaults()
            for name, value in values.items():
                if name in KILL_SWITCHES:
                    parsed = json.loads(value)
                    if isinstance(parsed, dict):
                        result[name].update(parsed)
            return result
        except (OSError, ValueError, TypeError, json.JSONDecodeError) as exc:
            self._redis_error = type(exc).__name__
            logger.error(
                "Runtime controls Redis read failed",
                extra={"error_type": self._redis_error},
            )
            return None

    def snapshot(self) -> dict[str, dict[str, Any]]:
        with self._lock:
            redis_snapshot = self._read_redis()
            if redis_snapshot is not None:
                return redis_snapshot
            if self._redis is not None and self._redis_error and self.fail_closed:
                switches = self._defaults()
                for value in switches.values():
                    value["active"] = True
                return switches
            return self._read_file()

    def is_active(self, name: str) -> bool:
        if name not in KILL_SWITCHES:
            raise ValueError(f"unknown kill switch: {name}")
        return bool(self.snapshot()[name]["active"])

    def set(self, name: str, active: bool, updated_by: str = "admin") -> dict[str, Any]:
        if name not in KILL_SWITCHES:
            raise ValueError(f"unknown kill switch: {name}")
        value = {
            "active": bool(active),
            "updated_at": _utc_now(),
            "updated_by": updated_by[:120],
        }
        with self._lock:
            current = self.snapshot()
            current[name] = value
            if self._redis is not None:
                try:
                    self._redis.hset(self.redis_key, name, json.dumps(value))
                except (OSError, ValueError, TypeError) as exc:
                    self._redis_error = type(exc).__name__
                    if self.fail_closed:
                        raise RuntimeError(
                            "runtime controls storage unavailable"
                        ) from exc
                    logger.error(
                        "Runtime controls Redis write failed; writing local fallback",
                        extra={"error_type": self._redis_error},
                    )
            self._write_file(current)
        return value

    def _write_file(self, switches: dict[str, dict[str, Any]]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "version": 1,
            "updated_at": _utc_now(),
            "switches": switches,
        }
        temp_path: str | None = None
        try:
            with tempfile.NamedTemporaryFile(
                mode="w",
                encoding="utf-8",
                dir=self.path.parent,
                prefix=f".{self.path.name}.",
                delete=False,
            ) as temporary:
                temp_path = temporary.name
                json.dump(payload, temporary, indent=2, sort_keys=True)
                temporary.write("\n")
                temporary.flush()
                os.fsync(temporary.fileno())
            os.chmod(temp_path, 0o600)
            os.replace(temp_path, self.path)
        except OSError:
            if temp_path:
                try:
                    os.unlink(temp_path)
                except OSError:
                    pass
            raise

    def status(self) -> dict[str, Any]:
        return {
            "switches": self.snapshot(),
            "storage": "redis" if self._redis is not None else "file",
            "path": str(self.path) if self._redis is None else None,
            "fail_closed": self.fail_closed,
            "storage_error": self._redis_error,
        }


def control_for_request(request: Request) -> str | None:
    """Return the kill switch protecting a request, if any."""
    path = request.url.path
    method = request.method.upper()
    if path == "/api/users" and method == "POST":
        return "signups"
    if (path.startswith("/api/captures") or "/captures" in path) and method in {
        "POST",
        "PUT",
        "PATCH",
    }:
        return "captures"
    if "/reprocess" in path:
        return "reprocessing"
    if path == "/api/triage" or any(
        marker in path
        for marker in (
            "/qna",
            "/ai/",
            "/shelf-scan",
            "/ingredient-explainer",
            "/predict",
            "/root-cause",
        )
    ):
        return "ai"
    if (
        path.startswith("/api/subscriptions")
        or "/subscription/" in path
        or path in {"/api/products", "/api/routine-events"}
    ) and method in {"POST", "PUT", "PATCH", "DELETE"}:
        return "billing" if "subscription" in path else None
    return None


class KillSwitchMiddleware(BaseHTTPMiddleware):
    """Stop selected features immediately without a process restart."""

    def __init__(self, app, controls: RuntimeControls):
        super().__init__(app)
        self.controls = controls

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint,
    ) -> Response:
        path = request.url.path
        if path in _HEALTH_PATHS or path.startswith("/api/admin"):
            return await call_next(request)

        active_control: str | None = None
        if self.controls.is_active("maintenance"):
            active_control = "maintenance"
        else:
            feature = control_for_request(request)
            if feature and self.controls.is_active(feature):
                active_control = feature

        if active_control:
            return JSONResponse(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                content={
                    "detail": "This feature is temporarily unavailable",
                    "error_code": "KILL_SWITCH_ACTIVE",
                    "control": active_control,
                },
                headers={"Retry-After": "60", "Cache-Control": "no-store"},
            )
        return await call_next(request)


class BackupError(RuntimeError):
    """Raised when a database backup cannot be created or verified."""


class BackupManager:
    """Create and verify database-only backups with retention metadata."""

    def __init__(
        self,
        *,
        database_url: str | None,
        sqlite_path: str | Path,
        directory: str | Path = ".data/backups",
        retention_days: int = 30,
        command_timeout_seconds: int = 600,
    ) -> None:
        self.database_url = database_url
        self.sqlite_path = Path(sqlite_path)
        self.directory = Path(directory)
        self.retention_days = max(1, retention_days)
        self.command_timeout_seconds = max(10, command_timeout_seconds)

    @classmethod
    def from_env(cls, settings: Any) -> "BackupManager":
        return cls(
            database_url=getattr(settings, "database_url", None),
            sqlite_path=getattr(settings, "db_path", ".data/glowupai.sqlite3"),
            directory=os.getenv("GLOWUPAI_BACKUP_DIR", ".data/backups"),
            retention_days=int(os.getenv("GLOWUPAI_BACKUP_RETENTION_DAYS", "30")),
            command_timeout_seconds=int(
                os.getenv("GLOWUPAI_BACKUP_TIMEOUT_SECONDS", "600")
            ),
        )

    @property
    def backend(self) -> str:
        return "postgresql" if self.database_url else "sqlite"

    def status(self) -> dict[str, Any]:
        artifacts = []
        if self.directory.is_dir():
            for manifest in sorted(
                self.directory.glob("*.manifest.json"), reverse=True
            ):
                try:
                    artifacts.append(json.loads(manifest.read_text(encoding="utf-8")))
                except (OSError, UnicodeError, json.JSONDecodeError):
                    artifacts.append({"manifest": manifest.name, "status": "invalid"})
        return {
            "configured": True,
            "backend": self.backend,
            "directory": str(self.directory),
            "retention_days": self.retention_days,
            "artifact_count": len(artifacts),
            "latest": artifacts[0] if artifacts else None,
            "scope": "database_only",
            "storage_backup_required": True,
        }

    def create_backup(self, label: str = "scheduled") -> dict[str, Any]:
        self.directory.mkdir(parents=True, exist_ok=True)
        os.chmod(self.directory, 0o700)
        stamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
        safe_label = (
            "".join(
                character if character.isalnum() or character in "-_" else "_"
                for character in label[:40]
            ).strip("_")
            or "backup"
        )
        suffix = ".dump" if self.database_url else ".sqlite3"
        destination = self.directory / f"{stamp}-{safe_label}{suffix}"
        temporary = self.directory / f".{destination.name}.{uuid.uuid4().hex}.tmp"
        try:
            if self.database_url:
                self._dump_postgres(temporary)
            else:
                self._dump_sqlite(temporary)
            os.replace(temporary, destination)
            os.chmod(destination, 0o600)
            checksum = _sha256(destination)
            manifest = {
                "version": 1,
                "created_at": _utc_now(),
                "label": safe_label,
                "backend": self.backend,
                "filename": destination.name,
                "sha256": checksum,
                "size_bytes": destination.stat().st_size,
                "scope": "database_only",
                "storage_backup_required": True,
            }
            manifest_path = destination.with_name(f"{destination.name}.manifest.json")
            manifest_path.write_text(
                json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8"
            )
            os.chmod(manifest_path, 0o600)
            self._prune()
            return manifest
        except (OSError, ValueError, sqlite3.Error, subprocess.SubprocessError) as exc:
            try:
                temporary.unlink(missing_ok=True)
            except OSError:
                pass
            raise BackupError("database backup failed") from exc

    def _dump_sqlite(self, destination: Path) -> None:
        if not self.sqlite_path.is_file():
            raise BackupError(f"SQLite database does not exist: {self.sqlite_path}")
        source = sqlite3.connect(self.sqlite_path)
        target = sqlite3.connect(destination)
        try:
            source.backup(target)
            result = target.execute("PRAGMA integrity_check").fetchone()
            if not result or result[0] != "ok":
                raise BackupError("SQLite integrity check failed")
            target.commit()
        finally:
            target.close()
            source.close()

    def _dump_postgres(self, destination: Path) -> None:
        if not self.database_url:
            raise BackupError("PostgreSQL URL is not configured")
        # No shell is used and the password is moved to PGPASSWORD so it does
        # not appear in the pg_dump process arguments. Prefer a managed
        # provider's native PITR for the primary RPO; this dump is the portable
        # restore artifact and verification point.
        parsed = urlsplit(self.database_url)
        password = unquote(parsed.password) if parsed.password is not None else None
        username = quote(parsed.username, safe="") if parsed.username else ""
        hostname = parsed.hostname or ""
        if ":" in hostname and not hostname.startswith("["):
            hostname = f"[{hostname}]"
        netloc = f"{username}@" if username else ""
        netloc += hostname
        if parsed.port:
            netloc += f":{parsed.port}"
        safe_url = urlunsplit(
            (parsed.scheme, netloc, parsed.path, parsed.query, parsed.fragment)
        )
        command_environment = os.environ.copy()
        if password is not None:
            command_environment["PGPASSWORD"] = password
        pg_dump = shutil.which("pg_dump")
        if not pg_dump:
            raise BackupError("pg_dump is required for PostgreSQL backups")
        with destination.open("wb") as output:
            subprocess.run(  # nosec B603
                [
                    pg_dump,
                    "--format=custom",
                    "--no-owner",
                    "--no-acl",
                    "--file",
                    str(destination),
                    safe_url,
                ],
                check=True,
                stdout=output,
                stderr=subprocess.PIPE,
                timeout=self.command_timeout_seconds,
                text=False,
                env=command_environment,
            )

    def verify_backup(self, artifact: str | Path) -> dict[str, Any]:
        path = Path(artifact).resolve()
        directory = self.directory.resolve()
        if directory not in path.parents:
            raise BackupError("backup must be inside the configured backup directory")
        if not path.is_file() or path.name.endswith(".manifest.json"):
            raise BackupError("backup artifact does not exist")
        manifest_path = path.with_name(f"{path.name}.manifest.json")
        if not manifest_path.is_file():
            raise BackupError("backup manifest is missing")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        expected = str(manifest.get("sha256", ""))
        actual = _sha256(path)
        if not expected or expected != actual:
            raise BackupError("backup checksum does not match its manifest")
        if path.suffix == ".sqlite3":
            connection = sqlite3.connect(path)
            try:
                result = connection.execute("PRAGMA integrity_check").fetchone()
            finally:
                connection.close()
            if not result or result[0] != "ok":
                raise BackupError("SQLite backup failed integrity check")
        elif path.suffix == ".dump":
            pg_restore = shutil.which("pg_restore")
            if not pg_restore:
                raise BackupError("pg_restore is required to verify PostgreSQL dumps")
            try:
                subprocess.run(  # nosec B603
                    [pg_restore, "--list", str(path)],
                    check=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    timeout=self.command_timeout_seconds,
                    text=False,
                )
            except subprocess.SubprocessError as exc:
                raise BackupError("PostgreSQL backup archive is invalid") from exc
        return {
            "valid": True,
            "filename": path.name,
            "sha256": actual,
            "backend": manifest.get("backend", self.backend),
        }

    def _prune(self) -> None:
        cutoff = time.time() - self.retention_days * 86400
        for artifact in self.directory.iterdir():
            if not artifact.is_file() or artifact.name.startswith("."):
                continue
            if artifact.stat().st_mtime < cutoff:
                artifact.unlink()


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


__all__ = [
    "AlertManager",
    "BackupError",
    "BackupManager",
    "KILL_SWITCHES",
    "KillSwitchMiddleware",
    "RuntimeControls",
    "control_for_request",
]
