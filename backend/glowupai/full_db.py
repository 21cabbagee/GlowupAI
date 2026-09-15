from __future__ import annotations

import sqlite3
from pathlib import Path

from .db import Database

FULL_SCHEMA = """
CREATE TABLE IF NOT EXISTS appearance_profiles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vertical TEXT NOT NULL CHECK (vertical IN ('skin')),
    baseline_capture_id TEXT,
    goal TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(user_id, vertical)
);

CREATE TABLE IF NOT EXISTS experiments (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    hypothesis TEXT,
    primary_metric TEXT NOT NULL DEFAULT 'redness_score',
    status TEXT NOT NULL DEFAULT 'planned' CHECK (status IN ('planned','running','paused','completed','cancelled')),
    start_at TEXT,
    end_at TEXT,
    target_days INTEGER NOT NULL DEFAULT 14,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS experiment_products (
    experiment_id TEXT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    product_id TEXT NOT NULL REFERENCES products(id),
    role TEXT NOT NULL DEFAULT 'test',
    PRIMARY KEY(experiment_id, product_id)
);

CREATE TABLE IF NOT EXISTS experiment_events (
    experiment_id TEXT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    routine_event_id TEXT NOT NULL REFERENCES routine_events(id) ON DELETE CASCADE,
    PRIMARY KEY(experiment_id, routine_event_id)
);

CREATE TABLE IF NOT EXISTS appearance_captures (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    photo_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    vertical TEXT NOT NULL CHECK (vertical IN ('skin')),
    metrics_json TEXT NOT NULL,
    model_version TEXT NOT NULL,
    confidence REAL NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS reminders (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind TEXT NOT NULL,
    next_at TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    cadence_days INTEGER NOT NULL DEFAULT 4,
    last_sent_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS engagement_events (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    reference_id TEXT,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    occurred_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS entitlements (
    user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    plan TEXT NOT NULL DEFAULT 'free' CHECK (plan IN ('free','premium')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','past_due','cancelled')),
    started_at TEXT NOT NULL DEFAULT (datetime('now')),
    renews_at TEXT,
    source TEXT NOT NULL DEFAULT 'local'
);

CREATE TABLE IF NOT EXISTS billing_events (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    provider TEXT NOT NULL DEFAULT 'local',
    provider_event_id TEXT,
    payload_json TEXT NOT NULL DEFAULT '{}',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS play_purchases (
    token_hash TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_ciphertext TEXT NOT NULL,
    active INTEGER NOT NULL,
    expires_at TEXT NOT NULL,
    verified_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS play_accounts (
    account_hash TEXT PRIMARY KEY,
    user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS play_notification_events (
    message_id TEXT PRIMARY KEY,
    received_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS deletion_requests (
    user_id TEXT PRIMARY KEY,
    requested_at TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS qna_threads (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS qna_messages (
    id TEXT PRIMARY KEY,
    thread_id TEXT NOT NULL REFERENCES qna_threads(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user','assistant')),
    content TEXT NOT NULL,
    citations_json TEXT NOT NULL DEFAULT '[]',
    scope TEXT NOT NULL DEFAULT 'cosmetic_tracking',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS qna_reports (
    message_id TEXT PRIMARY KEY REFERENCES qna_messages(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE IF NOT EXISTS affiliate_offers (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    merchant TEXT NOT NULL,
    url TEXT NOT NULL,
    price_cents INTEGER,
    currency TEXT NOT NULL DEFAULT 'USD',
    disclosed INTEGER NOT NULL DEFAULT 1,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS affiliate_clicks (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    offer_id TEXT NOT NULL REFERENCES affiliate_offers(id),
    clicked_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS cohort_insights (
    id TEXT PRIMARY KEY,
    category TEXT NOT NULL,
    metric TEXT NOT NULL,
    sample_size INTEGER NOT NULL,
    summary TEXT NOT NULL,
    evidence_json TEXT NOT NULL DEFAULT '{}',
    generated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS labels (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    photo_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    label_type TEXT NOT NULL,
    value TEXT NOT NULL,
    confidence REAL,
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS reprocess_jobs (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
    model_version TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'queued' CHECK (status IN ('queued','running','completed','failed')),
    processed_count INTEGER NOT NULL DEFAULT 0,
    error TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    completed_at TEXT
);

CREATE TABLE IF NOT EXISTS audit_log (
    id TEXT PRIMARY KEY,
    actor_type TEXT NOT NULL,
    actor_id TEXT,
    action TEXT NOT NULL,
    subject_type TEXT,
    subject_id TEXT,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS experience_profiles (
    user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name TEXT,
    focus_vertical TEXT NOT NULL DEFAULT 'skin',
    goals_json TEXT NOT NULL DEFAULT '[]',
    experience_level TEXT,
    onboarding_completed_at TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS entitlement_usage (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature TEXT NOT NULL,
    used_count INTEGER NOT NULL DEFAULT 0,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (user_id, feature)
);

CREATE TABLE IF NOT EXISTS context_events (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL CHECK (event_type IN ('sleep','travel','weather','cycle','stress','diet','custom')),
    value TEXT,
    notes TEXT,
    occurred_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS check_ins (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    routine_state TEXT NOT NULL CHECK (routine_state IN ('steady','changed','missed','not_sure')),
    skin_feel TEXT NOT NULL CHECK (skin_feel IN ('better','same','worse','not_sure')),
    note TEXT,
    occurred_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS measurement_feedback (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    capture_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    agreement TEXT NOT NULL CHECK (agreement IN ('fair','uncertain','off')),
    note TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(user_id, capture_id)
);
CREATE TABLE IF NOT EXISTS jobs (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
    job_type TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'queued' CHECK (status IN ('queued','running','completed','failed')),
    payload_json TEXT NOT NULL DEFAULT '{}',
    result_json TEXT,
    error TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    started_at TEXT,
    completed_at TEXT,
    next_attempt_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_appearance_user_time ON appearance_captures(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_engagement_user_time ON engagement_events(user_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_qna_thread_time ON qna_messages(thread_id, created_at);
CREATE INDEX IF NOT EXISTS idx_labels_user_photo ON labels(user_id, photo_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_context_user_time ON context_events(user_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_jobs_user_status ON jobs(user_id, status);
CREATE TABLE IF NOT EXISTS job_leases (
    job_id TEXT PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
    token TEXT,
    expires_at REAL NOT NULL DEFAULT 0,
    attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_check_ins_user_time ON check_ins(user_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_measurement_feedback_capture ON measurement_feedback(capture_id);

-- Versioned AI evidence and provider usage. Provider payloads, keys and image
-- bytes are never persisted; only validated evidence and safe provenance are.
CREATE TABLE IF NOT EXISTS ai_analyses (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    capture_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    task_type TEXT NOT NULL,
    request_key TEXT NOT NULL UNIQUE,
    schema_version TEXT NOT NULL,
    vision_provider TEXT NOT NULL,
    vision_model_id TEXT NOT NULL,
    vision_reasoning_effort TEXT NOT NULL CHECK (vision_reasoning_effort = 'medium'),
    language_provider TEXT,
    language_model_id TEXT,
    language_reasoning_effort TEXT CHECK (language_reasoning_effort IS NULL OR language_reasoning_effort = 'medium'),
    language_fallback_provider TEXT,
    language_fallback_model_id TEXT,
    language_fallback_reasoning_effort TEXT CHECK (language_fallback_reasoning_effort IS NULL OR language_fallback_reasoning_effort = 'medium'),
    prompt_version TEXT NOT NULL,
    preprocessing_version TEXT NOT NULL,
    input_digest TEXT NOT NULL,
    image_input_json TEXT NOT NULL DEFAULT '{}',
    data_class TEXT NOT NULL,
    policy_decision TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'queued',
    language_mode TEXT NOT NULL DEFAULT 'unavailable',
    validated_vision_json TEXT,
    validated_language_json TEXT,
    safe_error_code TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    completed_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_ai_analyses_user_created ON ai_analyses(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analyses_capture ON ai_analyses(capture_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analyses_status ON ai_analyses(status, created_at);

CREATE TABLE IF NOT EXISTS ai_comparisons (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    earlier_analysis_id TEXT NOT NULL REFERENCES ai_analyses(id) ON DELETE CASCADE,
    later_analysis_id TEXT NOT NULL REFERENCES ai_analyses(id) ON DELETE CASCADE,
    request_key TEXT NOT NULL UNIQUE,
    schema_version TEXT NOT NULL,
    vision_provider TEXT NOT NULL,
    vision_model_id TEXT NOT NULL,
    vision_reasoning_effort TEXT NOT NULL CHECK (vision_reasoning_effort = 'medium'),
    language_provider TEXT,
    language_model_id TEXT,
    language_mode TEXT NOT NULL DEFAULT 'unavailable',
    validated_result_json TEXT,
    validated_language_json TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    completed_at TEXT,
    UNIQUE(user_id, earlier_analysis_id, later_analysis_id, schema_version, vision_model_id)
);
CREATE INDEX IF NOT EXISTS idx_ai_comparisons_user_created ON ai_comparisons(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_usage_buckets (
    provider TEXT NOT NULL,
    project_alias TEXT NOT NULL,
    model_id TEXT NOT NULL,
    quota_window TEXT NOT NULL,
    reserved_tokens INTEGER NOT NULL DEFAULT 0 CHECK (reserved_tokens >= 0),
    reserved_requests INTEGER NOT NULL DEFAULT 0 CHECK (reserved_requests >= 0),
    reserved_cost_usd REAL NOT NULL DEFAULT 0 CHECK (reserved_cost_usd >= 0),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY(provider, project_alias, model_id, quota_window)
);

CREATE TABLE IF NOT EXISTS ai_usage_reservations (
    id TEXT PRIMARY KEY,
    provider TEXT NOT NULL CHECK (provider IN ('openai', 'gemini')),
    project_alias TEXT NOT NULL,
    model_id TEXT NOT NULL,
    user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
    task TEXT NOT NULL,
    request_identity TEXT NOT NULL,
    quota_window TEXT NOT NULL,
    reserved_tokens INTEGER NOT NULL CHECK (reserved_tokens > 0),
    actual_input_tokens INTEGER,
    actual_output_tokens INTEGER,
    estimated_cost_usd REAL NOT NULL DEFAULT 0 CHECK (estimated_cost_usd >= 0),
    actual_cost_usd REAL,
    attempt INTEGER NOT NULL CHECK (attempt > 0),
    state TEXT NOT NULL CHECK (state IN ('reserved', 'reconciled', 'released', 'ambiguous', 'failed')),
    provider_request_id TEXT,
    error_code TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    reconciled_at TEXT,
    UNIQUE(provider, request_identity, attempt)
);
CREATE INDEX IF NOT EXISTS idx_ai_reservations_bucket ON ai_usage_reservations(provider, project_alias, model_id, quota_window, state);
CREATE INDEX IF NOT EXISTS idx_ai_reservations_user_window ON ai_usage_reservations(user_id, provider, quota_window, state);

CREATE TABLE IF NOT EXISTS ai_policy_consents (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    policy_version TEXT NOT NULL,
    scope TEXT NOT NULL,
    granted_at TEXT,
    revoked_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY(user_id, policy_version, scope)
);
CREATE INDEX IF NOT EXISTS idx_ai_policy_consents_active ON ai_policy_consents(user_id, scope, revoked_at);
"""


class FullDatabase(Database):
    """Base schema plus the complete product schema."""

    def __init__(self, path: str | Path = ".data/glowupai.sqlite3") -> None:
        super().__init__(path)
        with self._lock:
            self.connection.executescript(FULL_SCHEMA)
            try:
                self.connection.execute(
                    "ALTER TABLE photo_captures ADD COLUMN idempotency_key TEXT",
                )
            except sqlite3.OperationalError as exc:
                if "duplicate column name" not in str(exc).lower():
                    raise
            self.connection.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_captures_user_idempotency "
                "ON photo_captures(user_id, idempotency_key) "
                "WHERE idempotency_key IS NOT NULL",
            )
            try:
                self.connection.execute(
                    "ALTER TABLE jobs ADD COLUMN next_attempt_at TEXT",
                )
            except sqlite3.OperationalError as exc:
                if "duplicate column name" not in str(exc).lower():
                    raise
            try:
                self.connection.execute(
                    "ALTER TABLE ai_analyses ADD COLUMN image_input_json TEXT NOT NULL DEFAULT '{}'",
                )
            except sqlite3.OperationalError as exc:
                if "duplicate column name" not in str(exc).lower():
                    raise
            self.connection.execute(
                "CREATE INDEX IF NOT EXISTS idx_jobs_due "
                "ON jobs(status, next_attempt_at, created_at)",
            )
            self.connection.commit()
