-- Android talks only to FastAPI and Supabase Auth. It never needs direct table access through
-- PostgREST. Keep the database/API boundary fail-closed even if the public anon key is extracted
-- from the APK. The backend's direct database owner connection continues to bypass RLS, while
-- anon/authenticated Supabase roles have neither grants nor permissive policies.
DO $$
DECLARE
    item RECORD;
BEGIN
    FOR item IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename <> 'schema_migrations'
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', item.tablename);
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM anon', item.tablename);
        END IF;
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
            EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM authenticated', item.tablename);
        END IF;
    END LOOP;
END
$$;

-- Future tables created by this migration owner start private too. RLS must still be enabled in
-- the migration that creates each future table; the revoked grants prevent accidental exposure
-- in the meantime.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated';
    END IF;
END
$$;
