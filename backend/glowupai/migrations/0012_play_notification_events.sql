CREATE TABLE IF NOT EXISTS play_accounts (
    account_hash TEXT PRIMARY KEY,
    user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS play_notification_events (
    message_id TEXT PRIMARY KEY,
    received_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);
