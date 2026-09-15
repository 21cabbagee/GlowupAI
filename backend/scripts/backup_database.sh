#!/usr/bin/env bash
# Create and immediately verify a portable database backup.
# Configure DATABASE_URL/SUPABASE_DB_URL and GLOWUPAI_BACKUP_DIR in the
# scheduler/secret manager. Do not send backup artifacts to the application
# container's ephemeral filesystem as the sole copy.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_DIR"

GLOWUPAI_ENV="${GLOWUPAI_ENV:-production}" \
  python -m glowupai.cli backup --label "scheduled"

latest_manifest="$(find "${GLOWUPAI_BACKUP_DIR:-.data/backups}" -maxdepth 1 \
  -type f -name '*.manifest.json' -print | sort | tail -n 1)"
if [[ -z "$latest_manifest" ]]; then
  echo "Backup completed without a manifest" >&2
  exit 1
fi

artifact="${latest_manifest%.manifest.json}"
python -m glowupai.cli verify-backup --path "$artifact"
