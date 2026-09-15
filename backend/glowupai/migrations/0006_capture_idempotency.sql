ALTER TABLE photo_captures
    ADD COLUMN IF NOT EXISTS idempotency_key TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_captures_user_idempotency
    ON photo_captures(user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
