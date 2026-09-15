CREATE TABLE IF NOT EXISTS play_purchases (
    token_hash TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_ciphertext TEXT NOT NULL,
    active INTEGER NOT NULL,
    expires_at TEXT NOT NULL,
    verified_at TEXT NOT NULL
);
