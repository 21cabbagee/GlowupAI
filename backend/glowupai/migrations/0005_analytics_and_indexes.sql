-- Analytics events table for tracking user behavior
CREATE TABLE IF NOT EXISTS analytics_events (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    event_data TEXT NOT NULL DEFAULT '{}',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Indexes for analytics queries
CREATE INDEX IF NOT EXISTS idx_analytics_user_type
ON analytics_events(user_id, event_type, created_at);

CREATE INDEX IF NOT EXISTS idx_analytics_type_time
ON analytics_events(event_type, created_at);

-- Performance optimization indexes
CREATE INDEX IF NOT EXISTS idx_captures_user_created
ON photo_captures(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_routine_events_user_time
ON routine_events(user_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_users_firebase_uid
ON users(firebase_uid);
