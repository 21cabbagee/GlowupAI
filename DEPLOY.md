# Deploying the SkinProof backend

Scope: `backend/` only (`skinproof.api:app`, i.e. `skinproof.complete_api.app`).
This is Task 1.3 of `ANDROID_PLAN.md`. It does not touch `backend/skinproof/`
Python source, `backend/tests/`, or `app/` — only the deploy surface
(`Dockerfile`, `docker-compose.yml`, `.env.example`, `.dockerignore`,
`railway.json`, this file, and `backend/docs/operations.md`).

Everything marked **[HUMAN]** requires an account, a login session, or a
secret this agent does not have and must not be given. Everything else was
verified statically by reading the code that runs it; nothing below was
exercised against a live Railway/Render project or a live Postgres instance
(Docker was not available in this environment — see "What was and wasn't
verified" at the end).

---

## 0. Provider choice: Railway over Render

Recommendation: **Railway**, with `backend/railway.json` (already provided).
Reasons, briefly:

- Railway's `railway.json` maps directly onto what already exists: a
  `Dockerfile` build, a `healthcheckPath`, and a restart policy — no
  translation into a YAML service DSL.
- A managed Postgres plugin is one `railway add` away, in the same project,
  wired to the app service via a reference variable — you never hand-copy a
  connection string.
- Railway injects `PORT` automatically and routes public traffic to it,
  which is exactly what the Dockerfile's `CMD` already expects
  (`--port ${PORT}`).

Render is a fine alternative (`render.yaml` would look almost identical —
Dockerfile build, `/api/health` health check path, a managed Postgres
add-on) if the human already has a Render account; the runbook below is
Railway-specific but the same env-var matrix and verification steps apply
unchanged.

---

## 1. Environment variable matrix

Source of truth: `backend/skinproof/config.py`. Full annotated copy also
lives in `backend/.env.example`.

| Variable | Required | Default | Breaks without it |
| --- | --- | --- | --- |
| `DATABASE_URL` (or `SKINPROOF_DATABASE_URL` / `POSTGRES_URL`) | **Yes**, in production | none — falls back to a local SQLite file | Without any of the three, the app silently runs on ephemeral SQLite. On Railway/Render that file is lost on every redeploy/restart — all users, captures, and history disappear. |
| `SKINPROOF_PHOTO_DIR` | No | unset (in-memory store) | Unset: photos live only in process memory and vanish on **every restart**, not just redeploy. Set: photos go to local disk instead, which is still wiped on redeploy unless a persistent volume is attached (see §4). |
| `SKINPROOF_PHOTO_KEY` | No, but required to persist `SKINPROOF_PHOTO_DIR` meaningfully | unset | Without it, setting `SKINPROOF_PHOTO_DIR` alone has no effect — `build_photo_store()` only switches off the in-memory store when **both** are set. Must be base64 for exactly 32 raw bytes. |
| `GEMINI_API_KEY` (or `SKINPROOF_GEMINI_API_KEY`) | No, but required for shelf-scan OCR and cited Q&A | unset | Without it, shelf-scan returns no candidates (manual-add fallback only) and Q&A/citations degrade. Nothing crashes. |
| `SKINPROOF_GEMINI_ENABLED` | No | `1` | Set to `0` to force those features off even if a key is present (useful for a Gemini-free staging deploy). |
| `SKINPROOF_FIREBASE_PROJECT_ID` | **Yes**, once auth is turned on | unset | `POST /api/auth/session` and any request the client expects to be checked will 401 with "SKINPROOF_FIREBASE_PROJECT_ID is not configured on this server". No service-account JSON is needed — verification is JWKS-based against Google's public signing keys (`skinproof/auth.py`), so this is the **only** Firebase-side secret the backend needs. |
| `SKINPROOF_AUTH_REQUIRED` | No | `0` (off) | `0`: every user-scoped route trusts the `{user_id}` path parameter with no ownership check — this is the pre-Phase-1 behaviour, kept as the default so the existing test suite and any not-yet-updated client keep working. `1`: `_require_owner` starts enforcing that the bearer token's Firebase `uid` maps to the `user_id` in the path, 403 on mismatch. **Flip to `1` before shipping the Android app**, once it sends bearer tokens on every call. |
| `SKINPROOF_ADMIN_TOKEN` | No, but strongly recommended | unset | Unset: `/api/admin/*` (including `GET /api/admin/audit`, previously an unauthenticated global leak per `frontend-api-map.md`) is hard-disabled with 403 — safe by default. Set: those routes accept `Authorization: Bearer <token>` matching this value via constant-time compare. Use a long random value; never reuse it elsewhere. |
| `SKINPROOF_ENV` | **Yes** — set to `production` | `development` | See the security note in §2 below. This is not optional. |
| `SKINPROOF_DISABLE_LEGACY_KEY_FILE` | **Yes** — set to `1` | unset (`0`) | Belt-and-suspenders alongside `SKINPROOF_ENV=production`; see §2. |
| `SKINPROOF_MODEL_VERSION` | No | `deterministic-3.0` | Only affects the version string stamped on new metric snapshots / used to filter reprocessing. Changing it later does not retroactively relabel existing data. |
| `SKINPROOF_POLICY_VERSION` | No | `2026-01` | Stamped on new consent grants. Bump when the consent copy/policy changes; existing consent rows keep their original version. |
| `SKINPROOF_DB_PATH` | No | `.data/skinproof.sqlite3` | Only read when no `DATABASE_URL`/etc. is set. Irrelevant once Postgres is configured. |
| `SKINPROOF_DB_POOL_MIN_SIZE` / `SKINPROOF_DB_POOL_MAX_SIZE` / `SKINPROOF_DB_CONNECT_TIMEOUT` | No | `1` / `10` / `10` | Postgres connection pool sizing. Set `MAX_SIZE` to respect the provider's connection cap (Railway's starter Postgres plugin and Neon free tier both cap concurrent connections). |
| `SKINPROOF_RAW_RETENTION_DAYS` | No | `730` | Retention-policy input for a future cleanup job; not currently enforced by a running worker (see `docs/operations.md`). |
| `PORT` | Set by the platform, not by you | `8000` (Dockerfile `ENV`) | Railway/Render inject this at runtime and route traffic to it. **Never hardcode 8000** — the Dockerfile's `CMD` already reads `$PORT`; do not override the start command with a literal port. |

---

## 2. Security item: the legacy Gemini-key file bridge

`backend/skinproof/config.py::_legacy_gemini_key()` will, in development,
read an API key out of a local `first.py` file via regex if no
`GEMINI_API_KEY`/`SKINPROOF_GEMINI_API_KEY` is set. It is a convenience
bridge for the existing local workspace, not a secrets mechanism.

It disables itself automatically when `SKINPROOF_ENV` is `production` or
`prod` — but **every production deploy must set `SKINPROOF_ENV=production`
explicitly** in the platform's environment variables. Do not rely on this
being anyone's implicit default; Railway/Render do not set it for you, and
if it is left unset the deployed container defaults to `development` and
the bridge stays live (though harmless if `first.py` is never present in
the image — see below).

Two independent controls are set in `.env.example` and should both be
present in production:

- `SKINPROOF_ENV=production`
- `SKINPROOF_DISABLE_LEGACY_KEY_FILE=1` (works regardless of `SKINPROOF_ENV`)

Belt-and-suspenders note: `backend/.dockerignore` also excludes `first.py`
from the build context, and the Dockerfile never `COPY`s the repo root
(only `pyproject.toml`, `README.md`, and `skinproof/`), so the file cannot
end up inside the production image regardless of these two env vars. The
env vars are still required because the same code path is reachable if
someone ever changes the Dockerfile to a broader `COPY . .`, or runs the
app outside Docker on a host where `first.py` happens to exist.

---

## 3. Postgres migration on boot — verified status

**Claim to verify:** a fresh, empty Postgres database gets the full schema,
including `users.firebase_uid`, and re-running the app against an
already-migrated database is safe (idempotent).

**Verified by static code reading. Not verified by running a real Postgres
instance** — Docker was not available in this environment (`docker
--version` fails with "command not found" in both the bash and PowerShell
tool). If Docker is available where you run this, `docker compose up
--build` (using the updated `backend/docker-compose.yml`) followed by
`curl http://localhost:8000/api/health` and a `psql` inspection of
`schema_migrations` and `\d users` would be the empirical confirmation;
that step is left to a human or a later agent with Docker access.

What the code reading established, precisely:

1. `backend/skinproof/complete_db.py::build_full_database()` picks
   `PostgresDatabase` whenever `settings.database_url` is set (i.e.
   `DATABASE_URL`/`SKINPROOF_DATABASE_URL`/`POSTGRES_URL`), never
   `FullDatabase` (SQLite) in that case.
2. `PostgresDatabase.__init__` (`backend/skinproof/postgres_db.py:26-52`)
   calls `self._migrate()` unconditionally, synchronously, before the
   constructor returns — so migration runs on every process boot, before
   any request can be served.
3. `_migrate()` (lines 99-127):
   - Creates `schema_migrations(version PRIMARY KEY, applied_at)` if absent.
   - Reads the set of already-applied filenames.
   - Iterates `sorted(migration_dir.glob("*.sql"))` — the four files sort as
     `0001_initial.sql`, `0002_growth_features.sql`,
     `0003_checkins_measurement_feedback.sql`, `0004_firebase_identity.sql`,
     which is the correct dependency order (0001 creates `users` before
     0004 alters it).
   - Skips any file already recorded in `schema_migrations`.
   - For a new file, splits on `;`, executes each statement, then inserts
     a `schema_migrations` row for it.
   - The entire loop runs inside one `pool.connection()` context, with a
     single `connection.commit()` after the loop — so a crash mid-migration
     rolls back everything for that boot (no partially-applied file is ever
     recorded as applied).
4. Confirmed by reading `backend/skinproof/migrations/0001_initial.sql`:
   it creates `users` (without `firebase_uid`) and every other table
   (`consent_events`, `products`, `routine_events`, `photo_captures`,
   `metric_snapshots`, `analysis_jobs`, `verdicts`,
   `appearance_profiles`, `experiments`, `experiment_products`,
   `experiment_events`, `appearance_captures`, `reminders`,
   `engagement_events`, `entitlements`, `billing_events`, `qna_threads`,
   `qna_messages`, `affiliate_offers`, `affiliate_clicks`,
   `cohort_insights`, `labels`, `reprocess_jobs`, `audit_log`,
   `experience_profiles`) plus their indexes. `0002` adds
   `entitlement_usage`, `context_events`, `jobs`. `0003` adds `check_ins`,
   `measurement_feedback`. `0004` is exactly:
   ```sql
   ALTER TABLE users ADD COLUMN IF NOT EXISTS firebase_uid TEXT UNIQUE;
   CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid);
   ```
   So a fresh database that boots the app once ends up with all four files
   applied in order and `users.firebase_uid` present with a unique
   constraint and an index — the same shape the SQLite path bakes directly
   into its `CREATE TABLE users (...)` in `backend/skinproof/db.py:13-23`.
5. **Idempotency** is defended twice over: (a) the `schema_migrations`
   ledger means an already-applied file is never re-executed on a later
   boot, and (b) even the SQL text itself is idempotent
   (`CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, `ADD COLUMN
   IF NOT EXISTS`), so nothing breaks even in the hypothetical case of a
   ledger row being lost or a statement being re-run by hand.

**Conclusion:** the production (Postgres) path is code-verified to deliver
the same schema as the SQLite path, including `firebase_uid`, and to be
safe to boot repeatedly against the same database. This has **not** been
empirically exercised against a running Postgres server in this session.
Recommend a human (or an agent with Docker) run `docker compose up --build`
from `backend/` once, hit `/api/health`, and spot-check with
`psql "$DATABASE_URL" -c '\d users'` before trusting this in production.

---

## 4. Persistent photo storage — known limitation

Read: `backend/skinproof/photos.py`.

- Default (`SKINPROOF_PHOTO_DIR` unset): `MemoryPhotoStore` — photo bytes
  never touch disk at all. They are lost on **every process restart**
  (crash, redeploy, scale-to-zero), not only on redeploy.
- `SKINPROOF_PHOTO_DIR` **and** `SKINPROOF_PHOTO_KEY` both set:
  `EncryptedFilePhotoStore` — AES-GCM encrypted files under that directory,
  keyed per-user via HMAC derivation from the root key. This is real
  storage, but it still lives on the **container's local filesystem**.

Railway and Render containers both run on an ephemeral, per-deploy
filesystem: anything written to local disk is discarded on every redeploy,
crash-restart, or horizontal scale event. So even with the encrypted local
store configured, **photos vanish on the next deploy** unless a persistent
volume is attached.

Options, concretely:

1. **Attach a persistent volume** (Railway: "Volumes" per service, mount at
   e.g. `/data`; Render: "Disks", same idea) and point
   `SKINPROOF_PHOTO_DIR` at the mount path. Minimal code change (zero, in
   fact — `EncryptedFilePhotoStore` already exists and just needs a
   directory that survives). Downsides: volumes are single-instance (no
   horizontal scaling of the API without a shared filesystem), and neither
   platform's volumes are as durable/available as an object store's SLA.
2. **Move to object storage** (S3/GCS/R2 + KMS or server-side encryption),
   which is what `backend/docs/operations.md` already lists under "Before
   accepting real users". This is the architecturally correct answer for a
   product that must survive scale-out and provider migration, but it is a
   **backend code change** (a new `PhotoStore` implementation) — out of
   scope for this deployment-prep task per the task boundary above.

**Recommendation:** attach a Railway volume and set `SKINPROOF_PHOTO_DIR`
(+ `SKINPROOF_PHOTO_KEY`) now, to get past the "photos are actively deleted
every deploy" failure mode for the alpha/closed-testing period the plan
targets. Treat the S3/GCS migration as a tracked follow-up before any real
user's face photos are at stake beyond that alpha — the volume path is a
stopgap, not the end state, and `operations.md` already says so.

---

## 5. Runbook

Ordered, exact. **[HUMAN]** marks every step this agent cannot perform —
it requires an account, a browser login, or a secret it must not hold.

1. **[HUMAN]** Create a Railway account at railway.app if one does not
   exist, and a payment method if required for the plan you want.
2. **[HUMAN]** Install the Railway CLI:
   ```powershell
   npm install -g @railway/cli
   # or: scoop install railway
   ```
3. **[HUMAN]** `railway login` — opens a browser to authenticate. This
   session cannot do this; it has no browser and no credentials.
4. **[HUMAN]** From `backend/`, create the project:
   ```powershell
   cd backend
   railway init
   ```
   Choose "Empty Project" (or link to a new one), name it e.g.
   `glowup-ai-backend`.
5. **[HUMAN]** Provision Postgres:
   ```powershell
   railway add --database postgres
   ```
   This creates a Postgres plugin in the same project and exposes
   `DATABASE_URL` (and related `PG*` vars) as a reference variable other
   services in the project can consume.
6. **[HUMAN]** Attach a persistent volume for photos (see §4):
   in the Railway dashboard, select the API service → **Volumes** → add a
   volume, mount path e.g. `/data/photos`.
7. **[HUMAN]** Set the app service's environment variables (dashboard
   "Variables" tab, or CLI). At minimum:
   ```powershell
   railway variables --set "SKINPROOF_ENV=production" `
     --set "SKINPROOF_DISABLE_LEGACY_KEY_FILE=1" `
     --set "SKINPROOF_FIREBASE_PROJECT_ID=<the glowup-ai Firebase project id>" `
     --set "SKINPROOF_AUTH_REQUIRED=0" `
     --set "SKINPROOF_ADMIN_TOKEN=<a long random value>" `
     --set "SKINPROOF_PHOTO_DIR=/data/photos" `
     --set "SKINPROOF_PHOTO_KEY=<base64 32-byte key>" `
     --set "GEMINI_API_KEY=<production key>"
   ```
   Reference `DATABASE_URL` from the Postgres plugin rather than typing it
   in by hand (Railway's dashboard offers this as a variable reference).
   Leave `SKINPROOF_AUTH_REQUIRED=0` until the Android app is actually
   sending bearer tokens on every call (Phase 3.1), then flip it to `1` in
   a follow-up deploy — flipping it early with no client wired up would
   403 every user-scoped request from the current unauthenticated web
   client and from Phase 2/3 work in progress.
8. **[HUMAN]** First deploy:
   ```powershell
   railway up
   ```
   This builds `backend/Dockerfile` remotely and deploys it. Because
   `backend/railway.json` sets `"builder": "DOCKERFILE"`, Railway will not
   try to auto-detect a buildpack.
9. **[HUMAN]** Get the public URL:
   ```powershell
   railway domain
   ```
   (generates a `*.up.railway.app` HTTPS domain if one is not already
   assigned; a custom domain can be attached later in the dashboard).
10. **Migration verification** — **[HUMAN]**, since it needs the deployed
    URL and/or `DATABASE_URL`:
    ```bash
    curl -s https://<your-app>.up.railway.app/api/health | jq
    # Expect: {"status":"ok","database":"postgresql","database_ready":true,...}
    ```
    If `database_ready` is `false` or the request 503s, check
    `railway logs` for a migration or connection error before doing
    anything else — do not proceed to smoke tests against a database that
    isn't ready.
    Optional deeper check, from a machine with `psql` and the Postgres
    connection string (Railway dashboard → Postgres plugin → "Connect"):
    ```bash
    psql "$DATABASE_URL" -c "select version, applied_at from schema_migrations order by version;"
    psql "$DATABASE_URL" -c "\d users"   # confirm firebase_uid column + unique index
    ```
11. **Smoke test `/api/health`** — **[HUMAN]** (needs the live URL):
    ```bash
    curl -i https://<your-app>.up.railway.app/api/health
    ```
    Expect `200` and the JSON shape above.
12. **Smoke test `POST /api/auth/session`** — **[HUMAN]** (needs a real
    Firebase ID token, which requires the Firebase project from Task 1.1
    and a signed-in test user — this agent has neither):
    ```bash
    curl -i -X POST https://<your-app>.up.railway.app/api/auth/session \
      -H "Authorization: Bearer <a real Firebase ID token>"
    ```
    Expect `200` and a profile payload (`session_for_identity` creates the
    user + appearance profile on first sight, per `complete_api.py:230-233`,
    and is idempotent on repeat calls with the same `uid`). Expect `401`
    with an explicit "SKINPROOF_FIREBASE_PROJECT_ID is not configured on
    this server" if that variable was skipped in step 7, and `401` with
    "token audience/issuer does not match this project" if the token comes
    from a different Firebase project than `SKINPROOF_FIREBASE_PROJECT_ID`.
13. **Rollback** — **[HUMAN]**:
    - Fastest: Railway dashboard → Deployments → pick the previous
      successful deployment → "Redeploy". Railway keeps deployment history
      per service; this does not require a new build.
    - CLI equivalent: `railway rollback` (interactive picker) if available
      in your CLI version, otherwise use the dashboard.
    - Database rollback is **not automatic** — the migration files never
      `DROP`, so rolling the app back to a pre-`0004` image still leaves
      `firebase_uid` in the schema (harmless: older app code simply never
      reads/writes that column). There is no destructive migration in this
      set to roll back.
14. **Log access** — **[HUMAN]** (or scriptable once logged in):
    ```powershell
    railway logs            # tail current deployment
    railway logs --deployment <id>   # a specific past deployment
    ```
    or the dashboard's "Deployments" → "View Logs" for the same, with
    search/filter.

---

## 6. What was and wasn't verified in this session

Verified by reading code (no execution needed / no credentials required):

- Dockerfile binds to `0.0.0.0:$PORT`, not a hardcoded port.
- `HEALTHCHECK` hits the real `/api/health` route the app actually exposes.
- Non-root user, no dev dependencies (`dev` extra never installed), layer
  ordering separates dependency install from source copy.
- The Postgres migration path applies all four migration files, in the
  right order, and idempotently — see §3 for the full chain of reasoning.
- The legacy Gemini-key bridge is genuinely disabled by
  `SKINPROOF_ENV=production` and cannot reach into the Docker image
  regardless (it is not `COPY`'d in).
- `POST /api/auth/session` requires only `SKINPROOF_FIREBASE_PROJECT_ID`
  (JWKS verification, no service-account secret) per `skinproof/auth.py`.
- Photo persistence: confirmed in-memory-only by default, confirmed
  encrypted-local-disk when both photo env vars are set, confirmed that
  disk is not durable across redeploys on either target platform.

**Not verified — explicitly, not by omission:**

- No live Postgres instance was started (`docker --version` failed:
  Docker is not installed/available in this environment), so the migration
  behavior above is a static-analysis conclusion, not an observed one.
- No Railway or Render account was created or logged into, no real deploy
  was performed, and no live HTTPS URL exists yet to run the `/api/health`
  or `/api/auth/session` curl smoke tests against.
- No real Firebase ID token was available, so the `auth/session` 200 path
  is confirmed by reading `complete_api.py`/`auth.py`, not by an actual
  round trip.
- `railway.json`'s exact accepted schema was not validated against a real
  Railway CLI/build (no CLI login available); the shape used here follows
  Railway's documented v2 config keys (`build.builder`,
  `build.dockerfilePath`, `deploy.healthcheckPath`,
  `deploy.restartPolicyType`/`restartPolicyMaxRetries`).
