#!/bin/bash
# Quick start script for local development

set -e

cd "$(dirname "$0")"

echo "🚀 Starting GlowupAI Backend..."

# Activate venv
if [ ! -d "venv" ]; then
    echo "❌ venv not found! Run: python3 -m venv venv && source venv/bin/activate && pip install -e ."
    exit 1
fi

source venv/bin/activate

# Start server
echo "✅ Starting on http://localhost:8000"
echo "📚 Docs: http://localhost:8000/docs"
echo "🏥 Health: http://localhost:8000/api/health"
echo ""

uvicorn glowupai.complete_api:app --host 0.0.0.0 --port 8000 --reload
