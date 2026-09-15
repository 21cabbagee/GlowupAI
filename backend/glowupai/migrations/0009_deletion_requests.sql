CREATE TABLE IF NOT EXISTS deletion_requests (
    user_id TEXT PRIMARY KEY,
    requested_at TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0
);
