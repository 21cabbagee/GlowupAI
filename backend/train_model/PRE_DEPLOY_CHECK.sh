#!/bin/bash
#
# Pre-deployment validation checklist
#
# Run this BEFORE deploying to verify everything is ready
#

set -e

echo "======================================"
echo "PRE-DEPLOYMENT VALIDATION"
echo "======================================"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

TRAIN_DIR="/Users/21cabbage/GlowupAI/backend/train_model"
BACKEND_DIR="/Users/21cabbage/GlowupAI/backend/glowupai"

all_passed=true

# Check 1: Trained model exists
echo ""
echo "1. Checking trained model..."
if [ -f "$TRAIN_DIR/checkpoints/best_model.pth" ]; then
    MODEL_SIZE=$(du -h "$TRAIN_DIR/checkpoints/best_model.pth" | cut -f1)
    echo -e "${GREEN}✓ PASS${NC} - Model checkpoint exists ($MODEL_SIZE)"
else
    echo -e "${RED}✗ FAIL${NC} - Model checkpoint not found"
    echo "  Expected: $TRAIN_DIR/checkpoints/best_model.pth"
    echo "  Training may not be complete yet"
    all_passed=false
fi

# Check 2: Training completed successfully
echo ""
echo "2. Checking training logs..."
if [ -f "$TRAIN_DIR/training.log" ]; then
    if grep -q "Training completed" "$TRAIN_DIR/training.log"; then
        echo -e "${GREEN}✓ PASS${NC} - Training completed successfully"
    else
        echo -e "${YELLOW}⚠ WARNING${NC} - Training log exists but no completion marker found"
    fi
else
    echo -e "${YELLOW}⚠ WARNING${NC} - No training log found"
fi

# Check 3: Backend integration files exist
echo ""
echo "3. Checking integration files..."
if [ -f "$BACKEND_DIR/ml_model.py" ]; then
    echo -e "${GREEN}✓ PASS${NC} - ml_model.py exists"
else
    echo -e "${RED}✗ FAIL${NC} - ml_model.py not found"
    all_passed=false
fi

if grep -q "use_ml_model = os.getenv" "$BACKEND_DIR/metrics.py"; then
    echo -e "${GREEN}✓ PASS${NC} - metrics.py updated with model switching"
else
    echo -e "${RED}✗ FAIL${NC} - metrics.py not updated"
    all_passed=false
fi

# Check 4: Deployment scripts exist
echo ""
echo "4. Checking deployment scripts..."
if [ -x "$TRAIN_DIR/DEPLOY.sh" ]; then
    echo -e "${GREEN}✓ PASS${NC} - DEPLOY.sh exists and is executable"
else
    echo -e "${RED}✗ FAIL${NC} - DEPLOY.sh not found or not executable"
    all_passed=false
fi

if [ -f "$TRAIN_DIR/test_consistency.py" ]; then
    echo -e "${GREEN}✓ PASS${NC} - test_consistency.py exists"
else
    echo -e "${RED}✗ FAIL${NC} - test_consistency.py not found"
    all_passed=false
fi

if [ -f "$TRAIN_DIR/DEPLOYMENT_GUIDE.md" ]; then
    echo -e "${GREEN}✓ PASS${NC} - DEPLOYMENT_GUIDE.md exists"
else
    echo -e "${RED}✗ FAIL${NC} - DEPLOYMENT_GUIDE.md not found"
    all_passed=false
fi

# Check 5: Test data available
echo ""
echo "5. Checking test data..."
if [ -d "$TRAIN_DIR/data" ]; then
    NUM_IMAGES=$(find "$TRAIN_DIR/data" -name "*.jpg" -o -name "*.png" | wc -l | xargs)
    if [ "$NUM_IMAGES" -gt 0 ]; then
        echo -e "${GREEN}✓ PASS${NC} - Test images available ($NUM_IMAGES found)"
    else
        echo -e "${YELLOW}⚠ WARNING${NC} - No test images in data directory"
        echo "  Consistency test will not be able to run"
    fi
else
    echo -e "${YELLOW}⚠ WARNING${NC} - data directory not found"
fi

# Check 6: Python dependencies
echo ""
echo "6. Checking Python dependencies..."
python3 << 'EOF'
import sys

required = ['torch', 'torchvision', 'numpy', 'cv2', 'PIL']
missing = []

for module in required:
    try:
        __import__(module)
    except ImportError:
        missing.append(module)

if missing:
    print(f"✗ FAIL - Missing dependencies: {', '.join(missing)}")
    sys.exit(1)
else:
    print("✓ PASS - All Python dependencies available")
    sys.exit(0)
EOF

if [ $? -ne 0 ]; then
    all_passed=false
fi

# Summary
echo ""
echo "======================================"
if [ "$all_passed" = true ]; then
    echo -e "${GREEN}✓ ALL CHECKS PASSED${NC}"
    echo ""
    echo "System is ready for deployment!"
    echo ""
    echo "Next steps:"
    echo "  1. Review DEPLOYMENT_GUIDE.md"
    echo "  2. Run: ./DEPLOY.sh"
    echo "  3. Test with: USE_NEW_MODEL=1 python3 test_model.py"
    echo "  4. Enable in production: export USE_NEW_MODEL=1"
else
    echo -e "${RED}✗ SOME CHECKS FAILED${NC}"
    echo ""
    echo "Please fix the issues above before deploying."
    echo "Review DEPLOYMENT_GUIDE.md for troubleshooting."
fi
echo "======================================"

exit $([ "$all_passed" = true ] && echo 0 || echo 1)
