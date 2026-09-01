from __future__ import annotations

import json
import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path
from threading import RLock
from typing import Any

SCHEMA = """
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    skin_type TEXT,
    baseline_date TEXT,
    consent_state TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    deleted_at TEXT,
    firebase_uid TEXT UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid);

CREATE TABLE IF NOT EXISTS consent_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type TEXT NOT NULL,
    granted INTEGER NOT NULL,
    policy_version TEXT NOT NULL,
    recorded_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS products (
    id TEXT PRIMARY KEY,
    barcode TEXT UNIQUE,
    name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'other',
    ingredients_json TEXT NOT NULL DEFAULT '[]',
    stabilization_days INTEGER NOT NULL DEFAULT 14,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS routine_events (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id TEXT NOT NULL REFERENCES products(id),
    action TEXT NOT NULL CHECK (action IN ('start', 'stop', 'change')),
    timestamp TEXT NOT NULL,
    slot TEXT NOT NULL DEFAULT 'unspecified',
    dose TEXT,
    frequency TEXT,
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS photo_captures (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    captured_at TEXT NOT NULL,
    raw_ref TEXT NOT NULL,
    aligned_ref TEXT,
    capture_quality_json TEXT NOT NULL,
    device_meta_json TEXT NOT NULL DEFAULT '{}',
    is_baseline INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'accepted',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS metric_snapshots (
    id TEXT PRIMARY KEY,
    photo_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    model_version TEXT NOT NULL,
    blemish_count REAL NOT NULL,
    redness_score REAL NOT NULL,
    redness_delta REAL,
    darkspot_area REAL NOT NULL,
    texture_score REAL NOT NULL,
    confidence REAL NOT NULL,
    noise_floor_json TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS analysis_jobs (
    id TEXT PRIMARY KEY,
    capture_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'queued',
    error TEXT,
    queued_at TEXT NOT NULL DEFAULT (datetime('now')),
    started_at TEXT,
    completed_at TEXT
);

CREATE TABLE IF NOT EXISTS verdicts (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id TEXT NOT NULL REFERENCES products(id),
    label TEXT NOT NULL CHECK (label IN ('keep', 'likely_useful', 'evidence_unclear', 'investigate')),
    evidence_window_start TEXT,
    evidence_window_end TEXT,
    generated_text TEXT NOT NULL,
    evidence_json TEXT NOT NULL,
    generated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_events_user_time ON routine_events(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_captures_user_time ON photo_captures(user_id, captured_at);
CREATE INDEX IF NOT EXISTS idx_metrics_user_time ON metric_snapshots(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_verdicts_user_product ON verdicts(user_id, product_id, generated_at);

CREATE TABLE IF NOT EXISTS collection_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    face_id TEXT NOT NULL,
    anonymous_capture_id TEXT NOT NULL,
    collected_at TEXT NOT NULL,
    quality_score REAL NOT NULL,
    model_version TEXT NOT NULL,
    UNIQUE(face_id, anonymous_capture_id)
);

CREATE INDEX IF NOT EXISTS idx_collection_log_collected_at ON collection_log(collected_at);
CREATE INDEX IF NOT EXISTS idx_collection_log_face_id ON collection_log(face_id);

CREATE TABLE IF NOT EXISTS capture_feedback (
    id TEXT PRIMARY KEY,
    capture_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feedback_type TEXT NOT NULL CHECK (feedback_type IN ('accurate', 'inaccurate')),
    issues_json TEXT NOT NULL DEFAULT '[]',
    corrections_json TEXT NOT NULL DEFAULT '{}',
    original_metrics_json TEXT NOT NULL DEFAULT '{}',
    comment TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_feedback_capture ON capture_feedback(capture_id);
CREATE INDEX IF NOT EXISTS idx_feedback_user ON capture_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_feedback_type ON capture_feedback(feedback_type);
CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON capture_feedback(created_at);

CREATE TABLE IF NOT EXISTS model_predictions (
    id TEXT PRIMARY KEY,
    capture_id TEXT NOT NULL,
    predictions_json TEXT NOT NULL,
    processing_time_ms REAL NOT NULL,
    error TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_predictions_capture ON model_predictions(capture_id);
CREATE INDEX IF NOT EXISTS idx_predictions_created_at ON model_predictions(created_at);
CREATE INDEX IF NOT EXISTS idx_predictions_error ON model_predictions(error);

CREATE TABLE IF NOT EXISTS model_health_log (
    id TEXT PRIMARY KEY,
    status TEXT NOT NULL CHECK (status IN ('healthy', 'degraded', 'critical')),
    variance_json TEXT NOT NULL,
    error_rate REAL NOT NULL,
    drift_json TEXT NOT NULL,
    issues_json TEXT NOT NULL DEFAULT '[]',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_health_log_created_at ON model_health_log(created_at);
CREATE INDEX IF NOT EXISTS idx_health_log_status ON model_health_log(status);
"""


def json_dumps(value: Any) -> str:
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


class Database:
    backend = "sqlite"

    def __init__(self, path: str | Path = ".data/glowupai.sqlite3") -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        # The API and background job runner intentionally share one SQLite connection. SQLite
        # allows that connection across threads only with check_same_thread=False; it still does
        # not make interleaved statements/commits safe. A re-entrant lock keeps each operation
        # atomic and also lets transaction callers perform nested connection work.
        self._lock = RLock()
        self.connection = sqlite3.connect(self.path, check_same_thread=False)
        self.connection.row_factory = sqlite3.Row
        with self._lock:
            self.connection.execute("PRAGMA foreign_keys = ON")
            self.connection.execute("PRAGMA journal_mode = WAL")
            self.connection.executescript(SCHEMA)
            self.connection.commit()

    @contextmanager
    def transaction(self) -> Iterator[sqlite3.Connection]:
        with self._lock:
            try:
                yield self.connection
                self.connection.commit()
            except Exception:
                self.connection.rollback()
                raise

    def execute(self, sql: str, params: tuple[Any, ...] = ()) -> sqlite3.Cursor:
        with self._lock:
            cursor = self.connection.execute(sql, params)
            self.connection.commit()
            return cursor

    def fetchone(self, sql: str, params: tuple[Any, ...] = ()) -> sqlite3.Row | None:
        with self._lock:
            result: sqlite3.Row | None = self.connection.execute(sql, params).fetchone()
            return result

    def fetchall(self, sql: str, params: tuple[Any, ...] = ()) -> list[sqlite3.Row]:
        with self._lock:
            return list(self.connection.execute(sql, params).fetchall())

    def healthcheck(self) -> bool:
        result = self.fetchone("SELECT 1 AS ok")
        return result is not None and result["ok"] == 1

    def count_tables(self) -> int:
        """Count the number of user-created tables in the database."""
        result = self.fetchone(
            "SELECT COUNT(*) as count FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
        )
        return result["count"] if result else 0

    def close(self) -> None:
        with self._lock:
            self.connection.close()
