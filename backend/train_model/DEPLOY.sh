#!/bin/bash
#
# Deploy trained model to backend
#
# This script:
# 1. Verifies the trained model exists
# 2. Creates models directory in backend
# 3. Copies model to production location
# 4. Tests model loading
# 5. Runs consistency test
#

set -e  # Exit on any error

echo "======================================"
echo "MODEL DEPLOYMENT SCRIPT"
echo "======================================"

# Paths
TRAIN_DIR="/Users/21cabbage/GlowupAI/backend/train_model"
BACKEND_DIR="/Users/21cabbage/GlowupAI/backend/glowupai"
MODEL_SOURCE="$TRAIN_DIR/checkpoints/best_model.pth"
MODEL_DEST="$BACKEND_DIR/models/skin_analysis_v2.pth"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Verify source model exists
echo ""
echo "Step 1: Verifying trained model..."
if [ ! -f "$MODEL_SOURCE" ]; then
    echo -e "${RED}ERROR: Trained model not found at: $MODEL_SOURCE${NC}"
    echo "Please ensure training has completed successfully."
    exit 1
fi

MODEL_SIZE=$(du -h "$MODEL_SOURCE" | cut -f1)
echo -e "${GREEN}✓ Found trained model: $MODEL_SOURCE ($MODEL_SIZE)${NC}"

# Step 2: Create models directory
echo ""
echo "Step 2: Creating models directory..."
mkdir -p "$BACKEND_DIR/models"
echo -e "${GREEN}✓ Models directory ready: $BACKEND_DIR/models${NC}"

# Step 3: Copy model to production
echo ""
echo "Step 3: Copying model to production..."
cp "$MODEL_SOURCE" "$MODEL_DEST"
DEST_SIZE=$(du -h "$MODEL_DEST" | cut -f1)
echo -e "${GREEN}✓ Model copied: $MODEL_DEST ($DEST_SIZE)${NC}"

# Step 4: Verify model loads correctly
echo ""
echo "Step 4: Testing model loading..."
cd "$TRAIN_DIR"

python3 << 'EOF'
import sys
import torch
sys.path.insert(0, '/Users/21cabbage/GlowupAI/backend')

try:
    from glowupai.ml_model import MLModelInference

    # Try to load the model
    model = MLModelInference()
    print("✓ Model loaded successfully")
    print(f"  Device: {model.device}")
    print(f"  Model path: {model.model_path}")

except Exception as e:
    print(f"✗ Model loading failed: {e}")
    sys.exit(1)
EOF

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Model loading test failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Model loads correctly${NC}"

# Step 5: Run consistency test
echo ""
echo "Step 5: Running consistency test..."
if [ -f "$TRAIN_DIR/test_consistency.py" ]; then
    cd "$TRAIN_DIR"
    python3 test_consistency.py

    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}WARNING: Consistency test failed${NC}"
        echo "Model has been deployed but may not be consistent."
        echo "Review test results before enabling in production."
    else
        echo -e "${GREEN}✓ Consistency test passed${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Consistency test script not found, skipping...${NC}"
fi

# Summary
echo ""
echo "======================================"
echo "DEPLOYMENT COMPLETE"
echo "======================================"
echo ""
echo "Model deployed to: $MODEL_DEST"
echo ""
echo "To enable the ML model in production:"
echo "  export USE_NEW_MODEL=1"
echo ""
echo "To test with a single capture:"
echo "  USE_NEW_MODEL=1 python3 -m glowupai.cli analyze <image_path>"
echo ""
echo "To rollback to deterministic model:"
echo "  unset USE_NEW_MODEL"
echo "  # or"
echo "  export USE_NEW_MODEL=0"
echo ""
echo -e "${GREEN}Deployment successful!${NC}"
