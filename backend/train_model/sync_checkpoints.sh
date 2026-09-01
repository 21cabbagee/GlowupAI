#!/bin/bash
# Sync ML checkpoints to GitHub and external storage

set -e

echo "============================================================"
echo "SYNCING ML CHECKPOINTS"
echo "============================================================"

cd "$(dirname "$0")"

# 1. Push best model to GitHub
echo ""
echo "📦 Step 1: Syncing best_model.pth to GitHub..."
if git diff --quiet checkpoints/best_model.pth 2>/dev/null; then
    echo "   ✅ best_model.pth already up to date on GitHub"
else
    git add checkpoints/best_model.pth
    git commit -m "Update best ML model checkpoint" || true
    git push origin staging
    echo "   ✅ best_model.pth pushed to GitHub"
fi

# 2. Upload all checkpoints to Hugging Face
echo ""
echo "📦 Step 2: Uploading all checkpoints to Hugging Face..."
if command -v python3 &> /dev/null; then
    if python3 -c "import huggingface_hub" 2>/dev/null; then
        python3 upload_to_huggingface.py
    else
        echo "   ⚠️  huggingface_hub not installed"
        echo "   Run: pip install huggingface_hub"
        echo "   Then: huggingface-cli login"
    fi
else
    echo "   ⚠️  Python 3 not found"
fi

# 3. Generate backup report
echo ""
echo "📊 Step 3: Generating backup report..."
echo ""
echo "Current Checkpoints:"
ls -lh checkpoints/*.pth 2>/dev/null | awk '{print "   " $9 " (" $5 ")"}'

echo ""
echo "============================================================"
echo "✅ SYNC COMPLETE"
echo "============================================================"
echo ""
echo "📍 Locations:"
echo "   - GitHub: checkpoints/best_model.pth"
echo "   - Hugging Face: All checkpoints"
echo "   - Local: $(pwd)/checkpoints/"
echo ""
