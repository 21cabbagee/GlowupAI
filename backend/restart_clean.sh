#!/bin/bash
# Clean restart script

set -e

cd "$(dirname "$0")"

echo "🛑 Killing all uvicorn processes..."
pkill -9 -f uvicorn || true
lsof -ti:8000 | xargs kill -9 2>/dev/null || true

echo "⏳ Waiting 3 seconds..."
sleep 3

echo "🚀 Starting fresh server..."
source venv/bin/activate
uvicorn glowupai.complete_api:app --host 0.0.0.0 --port 8000 --reload

