from __future__ import annotations

import asyncio
import json
import sqlite3
from pathlib import Path

import pytest
from starlette.applications import Starlette
from starlette.responses import JSONResponse
from starlette.routing import Route
from starlette.testclient import TestClient

from glowupai.observability import MetricsCollector
from glowupai.production_ops import (
    AlertManager,
    BackupError,
    BackupManager,
    KillSwitchMiddleware,
    RuntimeControls,
)
from glowupai.rate_limiter import RedisRateLimiter


def test_metrics_expose_rolling_alert_data_and_prometheus():
    metrics = MetricsCollector()
    metrics.record_request("GET", "/api/users/secret-id", 200, 10)
    metrics.record_request("POST", "/api/captures", 500, 100)

    snapshot = metrics.window_snapshot()
    assert snapshot["requests"] == 2
    assert snapshot["server_errors"] == 1
    assert snapshot["server_error_rate"] == 0.5
    exposition = metrics.prometheus()
    assert "glowupai_http_requests_total 2" in exposition
    assert 'status_code="500"' in exposition


def test_alerts_are_deduplicated_and_payload_is_metrics_only():
    sent: list[dict] = []

    def sender(_url: str, body: bytes, _timeout: int) -> None:
        sent.append(json.loads(body))

    alerts = AlertManager(
        "https://alerts.example.test",
        minimum_requests=2,
        error_rate_threshold=0.5,
        sender=sender,
    )
    snapshot = {"requests": 2, "server_error_rate": 0.5, "p95_duration_ms": 1}
    assert alerts.observe(snapshot) == ["http.server_error_rate"]
    assert alerts.observe(snapshot) == []
    assert len(sent) == 1
    assert "details" in sent[0]
    assert "user_id" not in json.dumps(sent[0])


def test_runtime_controls_are_atomic_and_persistent(tmp_path: Path):
    path = tmp_path / "controls.json"
    controls = RuntimeControls(path)
    assert not controls.is_active("captures")
    value = controls.set("captures", True, updated_by="test")
    assert value["active"] is True
    assert controls.is_active("captures")
    assert (path.stat().st_mode & 0o777) == 0o600
    assert RuntimeControls(path).is_active("captures")


def test_kill_switch_blocks_feature_but_not_health_or_admin(tmp_path: Path):
    controls = RuntimeControls(tmp_path / "controls.json")
    controls.set("captures", True)

    async def captures(_request):
        return JSONResponse({"ok": True})

    async def health(_request):
        return JSONResponse({"status": "alive"})

    app = Starlette(
        routes=[
            Route("/api/captures", captures, methods=["POST"]),
            Route("/api/live", health),
        ]
    )
    app.add_middleware(KillSwitchMiddleware, controls=controls)
    with TestClient(app) as client:
        blocked = client.post("/api/captures")
        assert blocked.status_code == 503
        assert blocked.json()["error_code"] == "KILL_SWITCH_ACTIVE"
        assert client.get("/api/live").status_code == 200


def test_sqlite_backup_is_verified_and_tamper_is_rejected(tmp_path: Path):
    source = tmp_path / "source.sqlite3"
    connection = sqlite3.connect(source)
    connection.execute("CREATE TABLE sample (id INTEGER PRIMARY KEY)")
    connection.execute("INSERT INTO sample VALUES (1)")
    connection.commit()
    connection.close()

    manager = BackupManager(
        database_url=None,
        sqlite_path=source,
        directory=tmp_path / "backups",
    )
    manifest = manager.create_backup("test")
    artifact = tmp_path / "backups" / manifest["filename"]
    assert manager.verify_backup(artifact)["valid"] is True

    artifact.write_bytes(artifact.read_bytes() + b"tampered")
    with pytest.raises(BackupError, match="checksum"):
        manager.verify_backup(artifact)


def test_memory_rate_limiter_enforces_configured_limit():
    limiter = RedisRateLimiter(
        limits={
            "capture_analyze": (2, 60),
            "auth": (2, 60),
            "dashboard": (2, 60),
            "api": (2, 60),
        }
    )
    results = [
        asyncio.run(limiter.check_rate_limit("test", "/api/roadmap", "GET"))
        for _ in range(3)
    ]
    assert [result[0] for result in results] == [True, True, False]
    assert results[-1][1] is not None
