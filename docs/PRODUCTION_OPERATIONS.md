# Production operations

This is the operational handoff for the backend. The app exposes safe
diagnostics, but secrets and destructive infrastructure actions stay outside
the public client.

## Monitoring and alerts

- `GET /api/live` is a liveness probe and does not touch the database.
- `GET /api/ready` is the readiness probe and checks the database/dependencies.
- `GET /internal/metrics` returns Prometheus text. Protect it with
  `GLOWUPAI_METRICS_TOKEN`; if that is unset, use the admin bearer token.
- `GET /api/admin/ops` shows redacted alert, backup, and kill-switch status.
- `POST /api/admin/ops/alerts/test` verifies the configured webhook.

Metrics retain only bounded counters and a five-minute rolling window. Alerts
are generated for server-error rate and p95 latency after the configured
minimum request count. Alert details contain thresholds and counts only; they
must never contain image data, request bodies, access tokens, or user IDs.

Set `GLOWUPAI_ALERT_WEBHOOK_URL` for Slack/Discord-compatible generic webhook
delivery. Set `SENTRY_DSN` separately when stack traces and traces are needed.

## Rate limits

Rate limits are applied by client IP. The middleware deliberately does not
trust an unverified JWT subject as a bucket key; owner authentication happens
inside the protected route. Capture, auth, dashboard, and general API buckets
are configurable with the `GLOWUPAI_RATE_LIMIT_*_PER_MINUTE` variables.

Redis uses an atomic sliding-window script and is the recommended production
backend. If Redis is unavailable, the bounded in-memory fallback continues to
enforce limits per process and reports the degraded backend in operator
status. Set `GLOWUPAI_RATE_LIMIT_FAIL_CLOSED=1` when an outage should reject
traffic instead of using that fallback.

## Kill switches

The following controls are available: `maintenance`, `signups`, `captures`,
`ai`, `billing`, and `reprocessing`.

```sh
curl -X PUT "$BACKEND_URL/api/admin/ops/kill-switches/captures" \
  -H "Authorization: Bearer $GLOWUPAI_ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"active":true,"reason":"capture provider incident"}'
```

Read the state with `GET /api/admin/ops/kill-switches`. Redis is the shared
store when `REDIS_URL` is configured; otherwise the backend writes an atomic,
mode-600 JSON file. `GLOWUPAI_KILL_SWITCH_FAIL_CLOSED=1` makes a controls-store
read failure block traffic behind the switch until the store recovers. The
admin operations endpoints and health probes remain available so an operator
can recover the service.

## Backups and restore

`glowupai backup` creates a timestamped, checksum-manifested database backup;
`glowupai verify-backup --path ...` verifies the checksum and SQLite integrity
or runs `pg_restore --list` for a PostgreSQL custom dump. The same flow is
available through `backend/scripts/backup_database.sh` and the admin backup
endpoint for an explicitly authorized operator.

The artifact is database-only. Production must also enable the managed
PostgreSQL provider's PITR and versioning/retention for the private image
bucket. Store at least one verified copy outside the application container.
The initial recovery objectives are RPO 24 hours and RTO 4 hours until an
isolated restore drill demonstrates tighter numbers.

## Rollback

Record the deployed backend revision and Vercel deployment URL in the release
change. Before rollback, activate the affected feature kill switch when
possible, capture `/api/admin/ops` and `/api/metrics`, and identify a verified
healthy target deploy. Then run from `backend/`:

```sh
VERCEL_TOKEN=... \
../scripts/rollback.sh https://known-good-deployment.vercel.app
```

The script promotes the known-good Vercel deployment and waits for `/api/ready`.
Keep Git deployment behavior in mind after the rollback; pause the triggering
pipeline if the bad revision would immediately be redeployed.
After readiness recovers, run the production smoke journey, verify billing
webhooks/jobs, and leave the kill switch active until the incident owner signs
off.

## Required scheduling

Run `backend/scripts/backup_database.sh` at least daily from a scheduler with
access to the database and an external backup destination. Alert if the last
verified manifest is older than 24 hours. Perform a quarterly isolated
restore, including a deletion replay, and record the measured RPO/RTO.
