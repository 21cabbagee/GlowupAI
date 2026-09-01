# Local operations and production handoff

For the full provider runbook (account setup, secrets, first deploy,
smoke tests, rollback, logs), see `DEPLOY.md` at the repo root. This
document covers day-to-day local and operational commands; DEPLOY.md
covers the one-time and per-deploy provider steps.

## Local run

```powershell
python -m pip install -e .
python -m glowupai.cli serve
```

The browser surface is `/`; interactive API documentation is `/docs`.

For development, omit `GLOWUPAI_PHOTO_DIR` and the server uses memory-only
photo storage. For encrypted local object storage, set a 32-byte base64 key:

```powershell
$env:GLOWUPAI_PHOTO_DIR = ".data/photos"
$env:GLOWUPAI_PHOTO_KEY = "<base64-encoded-32-byte-key>"
python -m glowupai.cli serve
```

The `cryptography` dependency is declared in `pyproject.toml`; a deployment
must install project dependencies before enabling that store.

## PostgreSQL setup

Set DATABASE_URL in the process environment. The API opens a bounded
connection pool and applies every unapplied file in glowupai/migrations
before serving requests. Startup fails fast if the initial connection is
unavailable; /api/health reports database, database_ready, and returns 503
if a later health check fails.

For a reproducible local database and API, use:

docker compose up --build

For Neon, replace the local DATABASE_URL with the Neon pooled connection
string and keep the same application image; no service code changes are
required.

## Deployment checks

- provide DATABASE_URL and verify /api/health reports database_ready=true;
- use a managed PostgreSQL backup/restore policy and keep migration files in
  the release artifact;
- set GLOWUPAI_DB_POOL_MAX_SIZE to match the host worker count and the
  provider connection limit;
- keep GLOWUPAI_ENV=production and disable legacy local key-file lookup.

## Before accepting real users

- replace the memory/local store with S3/GCS + KMS and access audit logs;
- run a bounded raw-photo retention worker (the configured default is 730
  days) and keep derived metrics only after deletion policy review;
- put MediaPipe/ARKit capture guidance in the mobile client and validate one
  face, pose, distance, exposure, and reference-card state on-device;
- add a real queue worker and retry/dead-letter handling for analysis jobs;
- establish a diverse, consented longitudinal labeling workflow before
  training a custom blemish detector;
- add authenticated export packaging for raw objects and verify complete
  deletion across database, object storage, backups, and analytics copies;
- obtain dermatology/cosmetic-science and privacy/legal review before exposing
  new metrics or marketing language;
- add billing provider webhooks and entitlements without allowing commerce to
  influence verdict labels or placement.

## Health and evidence checks

- `GET /api/health` confirms the API process is live.
- `GET /api/users/{id}/dashboard` is the primary operator/user smoke check.
- Inspect `analysis_jobs` for failed processing and compare `model_version`
  before reprocessing historical photos.
- Treat a rise in `evidence_unclear` as a data-quality signal first: check
  cadence, capture quality, and simultaneous routine changes before changing
  thresholds.

## Container runtime notes (Railway / Render)

- The container binds to `0.0.0.0:$PORT`. `PORT` is injected by the
  platform at runtime; never hardcode `8000` in a start command override.
- The image's `HEALTHCHECK` and the platform's own healthcheck
  (`railway.json`'s `deploy.healthcheckPath`) both poll `/api/health`, so a
  failing database connection is visible to both container orchestration
  and the platform's routing layer at the same time.
- Restart policy is `ON_FAILURE` with a bounded retry count
  (`railway.json`); a crash-looping deploy stops retrying rather than
  burning build minutes indefinitely — check `railway logs` before
  re-triggering manually.
- The container runs as a non-root user (`glowupai`, uid 10001). If a
  future change needs to write outside `/app`, it must be an explicitly
  writable, pre-created, owned directory — not assumed root access.
- Photo storage is not durable across redeploys unless a persistent volume
  is attached and `GLOWUPAI_PHOTO_DIR`/`GLOWUPAI_PHOTO_KEY` point at it;
  see `DEPLOY.md` §4 for the full tradeoff and the interim recommendation.
