#!/usr/bin/env bash
# Promote a verified Vercel deployment back to production.
# Usage: VERCEL_TOKEN=... ./scripts/rollback.sh <deployment-url>

set -euo pipefail

DEPLOYMENT_URL="${1:-}"
if [[ ! "$DEPLOYMENT_URL" =~ ^https://[A-Za-z0-9-]+\.vercel\.app/?$ ]]; then
  echo "Usage: VERCEL_TOKEN=... $0 <verified-vercel-deployment-url>" >&2
  exit 2
fi
: "${VERCEL_TOKEN:?VERCEL_TOKEN is required}"

if [[ "${CONFIRM_ROLLBACK:-}" != "YES" ]]; then
  echo "Refusing rollback without CONFIRM_ROLLBACK=YES" >&2
  echo "This must target a verified Vercel deployment: $DEPLOYMENT_URL" >&2
  exit 2
fi

BACKEND_URL="${BACKEND_URL:-https://backend-piyushcapitals-4171.vercel.app}"

echo "Promoting $DEPLOYMENT_URL to Vercel production"
npx vercel promote "$DEPLOYMENT_URL" --yes --token="$VERCEL_TOKEN"

echo "Rollback queued. Waiting for readiness at $BACKEND_URL/api/ready"
for attempt in {1..18}; do
  status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --max-time 10 "$BACKEND_URL/api/ready" || true)"
  if [[ "$status" == "200" ]]; then
    echo "Rollback is ready (HTTP 200)"
    exit 0
  fi
  echo "Readiness returned HTTP $status (attempt $attempt/18)"
  sleep 5
done

echo "Vercel promotion was requested but readiness did not recover within 90 seconds" >&2
exit 1
