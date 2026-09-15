-- Migrate databases that applied the pre-Supabase identity migration.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'firebase_uid'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'supabase_uid'
    ) THEN
        ALTER TABLE users RENAME COLUMN firebase_uid TO supabase_uid;
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'firebase_uid'
    ) THEN
        UPDATE users SET supabase_uid = COALESCE(supabase_uid, firebase_uid);
        ALTER TABLE users DROP COLUMN firebase_uid;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_supabase_uid ON users(supabase_uid);
