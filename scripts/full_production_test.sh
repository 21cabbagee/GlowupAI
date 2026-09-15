#!/bin/bash
# Verify the real Vercel production deployment and build an installable APK.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
PRODUCTION_ORIGIN="${PRODUCTION_ORIGIN:-https://backend-piyushcapitals-4171.vercel.app}"
API_BASE_URL="${PRODUCTION_ORIGIN%/}/api/"

cd "$PROJECT_ROOT"

echo "[1/6] Checking Vercel production health"
curl --fail --silent --show-error --max-time 30 "${API_BASE_URL}health" >/dev/null

echo "[2/6] Checking Vercel dependencies and runtime controls"
READINESS="$(curl --fail --silent --show-error --max-time 45 "${API_BASE_URL}ready")"
python3 -c '
import json, sys
payload = json.loads(sys.stdin.read())
if payload.get("status") != "healthy":
    raise SystemExit("production readiness is not healthy")
checks = payload.get("checks", {})
if checks.get("database", {}).get("status") != "healthy":
    raise SystemExit("production database is not healthy")
switches = payload.get("ops", {}).get("runtime_controls", {}).get("switches", {})
active = sorted(name for name, value in switches.items() if value.get("active"))
if active:
    raise SystemExit("active production kill switches: " + ", ".join(active))
print("Vercel, database, and runtime controls are healthy")
' <<<"$READINESS"

echo "[3/6] Running backend regression suite"
PYTHON_BIN="${PYTHON_BIN:-python3}"
(
    cd "$BACKEND_DIR"
    GLOWUPAI_DISABLE_LOCAL_ENV=1 GLOWUPAI_RATE_LIMIT_ENABLED=0 \
        "$PYTHON_BIN" -m pytest tests -q --tb=short
    "$PYTHON_BIN" -m black --check glowupai tests
    "$PYTHON_BIN" -m mypy glowupai --ignore-missing-imports --no-strict-optional
    "$PYTHON_BIN" -m bandit -r glowupai -q
)

echo "[4/6] Running Android unit tests and lint"
./gradlew testDebugUnitTest lintDebug --stacktrace

if [[ -f "$BACKEND_DIR/.env" ]]; then
    set -a
    # Local secrets stay local; only Supabase's public client configuration is
    # forwarded into BuildConfig.
    source "$BACKEND_DIR/.env"
    set +a
fi
DEBUG_SUPABASE_URL="${DEBUG_SUPABASE_URL:-${SUPABASE_URL:-}}"
DEBUG_SUPABASE_ANON_KEY="${DEBUG_SUPABASE_ANON_KEY:-${SUPABASE_PUBLISHABLE_KEY:-}}"
if [[ -z "$DEBUG_SUPABASE_URL" || -z "$DEBUG_SUPABASE_ANON_KEY" ]]; then
    echo "DEBUG_SUPABASE_URL and DEBUG_SUPABASE_ANON_KEY are required to build an installable APK" >&2
    exit 1
fi

echo "[5/6] Building installable APK against Vercel"
DEBUG_API_BASE_URL="$API_BASE_URL" \
DEBUG_SUPABASE_URL="$DEBUG_SUPABASE_URL" \
DEBUG_SUPABASE_ANON_KEY="$DEBUG_SUPABASE_ANON_KEY" \
    ./gradlew assembleDebug --stacktrace

APK="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"

echo "[6/6] Production verification complete"
echo "APK: $APK"
shasum -a 256 "$APK"
echo "Authenticated capture and Google Play purchase checks still require real test-user/Play credentials."
