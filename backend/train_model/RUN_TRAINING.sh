#!/bin/bash

# Complete training pipeline for skin analysis model
# NO EXCUSES - Actually trains the model end-to-end

set -e  # Exit on error

echo "=========================================="
echo "SKIN ANALYSIS MODEL - TRAINING PIPELINE"
echo "=========================================="

# Navigate to training directory
cd "$(dirname "$0")"

# Step 1: Install dependencies
echo ""
echo "Step 1/5: Installing dependencies..."
pip install -q -r requirements.txt
echo "✓ Dependencies installed"

# Step 2: Prepare data
echo ""
echo "Step 2/5: Preparing training data..."
python prepare_data.py
echo "✓ Data prepared"

# Step 3: Verify data
echo ""
echo "Step 3/5: Verifying dataset..."
DATA_DIR="data"
if [ ! -d "$DATA_DIR/images" ]; then
    echo "❌ Error: No training data found!"
    echo "   Take some photos in the app first to create training data."
    exit 1
fi

NUM_IMAGES=$(ls $DATA_DIR/images | wc -l)
echo "✓ Found $NUM_IMAGES training images"

if [ $NUM_IMAGES -lt 10 ]; then
    echo "⚠️  Warning: Only $NUM_IMAGES images - need at least 10 for good training"
    echo "   Results may not be great with limited data."
    echo "   Continue anyway? (y/n)"
    read -r response
    if [ "$response" != "y" ]; then
        exit 1
    fi
fi

# Step 4: Train model
echo ""
echo "Step 4/5: Training model (this will take 1-2 hours)..."
echo "☕ Grab coffee - training on CPU takes time!"
python train.py
echo "✓ Training complete"

# Step 5: Export for production
echo ""
echo "Step 5/5: Exporting model for production..."
python deploy_model.py
echo "✓ Model exported"

echo ""
echo "=========================================="
echo "✅ TRAINING PIPELINE COMPLETE!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Test the model: python test_model.py"
echo "2. Deploy to backend: Copy model to production"
echo "3. Update API to use new model"
echo ""
