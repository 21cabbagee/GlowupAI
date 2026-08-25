CREATE TABLE IF NOT EXISTS check_ins (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    routine_state TEXT NOT NULL CHECK (routine_state IN ('steady','changed','missed','not_sure')),
    skin_feel TEXT NOT NULL CHECK (skin_feel IN ('better','same','worse','not_sure')),
    note TEXT,
    occurred_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
);

CREATE TABLE IF NOT EXISTS measurement_feedback (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    capture_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    agreement TEXT NOT NULL CHECK (agreement IN ('fair','uncertain','off')),
    note TEXT,
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text),
    UNIQUE(user_id, capture_id)
);

CREATE INDEX IF NOT EXISTS idx_check_ins_user_time ON check_ins(user_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_measurement_feedback_capture ON measurement_feedback(capture_id);
