CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    skin_type TEXT,
    baseline_date TEXT,
    consent_state TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
    deleted_at TEXT
);

CREATE TABLE IF NOT EXISTS consent_events (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type TEXT NOT NULL,
    granted INTEGER NOT NULL,
    policy_version TEXT NOT NULL,
    recorded_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS products (
    id TEXT PRIMARY KEY,
    barcode TEXT UNIQUE,
    name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'other',
    ingredients_json TEXT NOT NULL DEFAULT '[]',
    stabilization_days INTEGER NOT NULL DEFAULT 14,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS metric_snapshots (
    id TEXT PRIMARY KEY,
    photo_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    model_version TEXT NOT NULL,
    blemish_count DOUBLE PRECISION NOT NULL,
    redness_score DOUBLE PRECISION NOT NULL,
    redness_delta DOUBLE PRECISION,
    darkspot_area DOUBLE PRECISION NOT NULL,
    texture_score DOUBLE PRECISION NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    noise_floor_json TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS analysis_jobs (
    id TEXT PRIMARY KEY,
    capture_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'queued',
    error TEXT,
    queued_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
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
    generated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS appearance_profiles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vertical TEXT NOT NULL CHECK (vertical IN ('skin')),
    baseline_capture_id TEXT REFERENCES photo_captures(id) ON DELETE SET NULL,
    goal TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
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
    confidence DOUBLE PRECISION NOT NULL,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS reminders (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind TEXT NOT NULL,
    next_at TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    cadence_days INTEGER NOT NULL DEFAULT 4,
    last_sent_at TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS engagement_events (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    reference_id TEXT,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    occurred_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS entitlements (
    user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    plan TEXT NOT NULL DEFAULT 'free' CHECK (plan IN ('free','premium')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','past_due','cancelled')),
    started_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS qna_threads (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS qna_messages (
    id TEXT PRIMARY KEY,
    thread_id TEXT NOT NULL REFERENCES qna_threads(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user','assistant')),
    content TEXT NOT NULL,
    citations_json TEXT NOT NULL DEFAULT '[]',
    scope TEXT NOT NULL DEFAULT 'cosmetic_tracking',
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS affiliate_clicks (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    offer_id TEXT NOT NULL REFERENCES affiliate_offers(id),
    clicked_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS cohort_insights (
    id TEXT PRIMARY KEY,
    category TEXT NOT NULL,
    metric TEXT NOT NULL,
    sample_size INTEGER NOT NULL,
    summary TEXT NOT NULL,
    evidence_json TEXT NOT NULL DEFAULT '{}',
    generated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS labels (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    photo_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    label_type TEXT NOT NULL,
    value TEXT NOT NULL,
    confidence DOUBLE PRECISION,
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS reprocess_jobs (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
    model_version TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'queued' CHECK (status IN ('queued','running','completed','failed')),
    processed_count INTEGER NOT NULL DEFAULT 0,
    error TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
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
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS experience_profiles (
    user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name TEXT,
    focus_vertical TEXT NOT NULL DEFAULT 'skin' CHECK (focus_vertical IN ('skin')),
    goals_json TEXT NOT NULL DEFAULT '[]',
    experience_level TEXT,
    onboarding_completed_at TEXT,
    updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE INDEX IF NOT EXISTS idx_events_user_time ON routine_events(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_captures_user_time ON photo_captures(user_id, captured_at);
CREATE INDEX IF NOT EXISTS idx_metrics_user_time ON metric_snapshots(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_verdicts_user_product ON verdicts(user_id, product_id, generated_at);
CREATE INDEX IF NOT EXISTS idx_appearance_user_time ON appearance_captures(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_engagement_user_time ON engagement_events(user_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_qna_thread_time ON qna_messages(thread_id, created_at);
CREATE INDEX IF NOT EXISTS idx_labels_user_photo ON labels(user_id, photo_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at);
