ALTER TABLE users ADD COLUMN IF NOT EXISTS supabase_uid TEXT UNIQUE;

CREATE INDEX IF NOT EXISTS idx_users_supabase_uid ON users(supabase_uid);
