-- Versioned persistence for the Luna vision and Gemini language pipeline.
-- Provider payloads, keys and image bytes are intentionally never stored here.

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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
    completed_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_ai_analyses_user_created
    ON ai_analyses(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analyses_capture
    ON ai_analyses(capture_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analyses_status
    ON ai_analyses(status, created_at);

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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
    completed_at TEXT,
    UNIQUE(user_id, earlier_analysis_id, later_analysis_id, schema_version, vision_model_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_comparisons_user_created
    ON ai_comparisons(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_usage_buckets (
    provider TEXT NOT NULL,
    project_alias TEXT NOT NULL,
    model_id TEXT NOT NULL,
    quota_window TEXT NOT NULL,
    reserved_tokens BIGINT NOT NULL DEFAULT 0 CHECK (reserved_tokens >= 0),
    reserved_requests INTEGER NOT NULL DEFAULT 0 CHECK (reserved_requests >= 0),
    reserved_cost_usd NUMERIC(20, 8) NOT NULL DEFAULT 0 CHECK (reserved_cost_usd >= 0),
    updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
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
    reserved_tokens BIGINT NOT NULL CHECK (reserved_tokens > 0),
    actual_input_tokens BIGINT,
    actual_output_tokens BIGINT,
    estimated_cost_usd NUMERIC(20, 8) NOT NULL DEFAULT 0 CHECK (estimated_cost_usd >= 0),
    actual_cost_usd NUMERIC(20, 8),
    attempt INTEGER NOT NULL CHECK (attempt > 0),
    state TEXT NOT NULL CHECK (state IN ('reserved', 'reconciled', 'released', 'ambiguous', 'failed')),
    provider_request_id TEXT,
    error_code TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
    reconciled_at TEXT,
    UNIQUE(provider, request_identity, attempt)
);

CREATE INDEX IF NOT EXISTS idx_ai_reservations_bucket
    ON ai_usage_reservations(provider, project_alias, model_id, quota_window, state);
CREATE INDEX IF NOT EXISTS idx_ai_reservations_user_window
    ON ai_usage_reservations(user_id, provider, quota_window, state);

CREATE TABLE IF NOT EXISTS ai_policy_consents (
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    policy_version TEXT NOT NULL,
    scope TEXT NOT NULL,
    granted_at TEXT,
    revoked_at TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
    PRIMARY KEY(user_id, policy_version, scope)
);

CREATE INDEX IF NOT EXISTS idx_ai_policy_consents_active
    ON ai_policy_consents(user_id, scope, revoked_at);

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS next_attempt_at TEXT;
CREATE INDEX IF NOT EXISTS idx_jobs_due ON jobs(status, next_attempt_at, created_at);
