#!/bin/bash
# Training progress monitor

echo "======================================================"
echo "ML MODEL TRAINING PROGRESS MONITOR"
echo "======================================================"
echo ""

# Check if process is running
PID=$(ps aux | grep "train_resume.py" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "❌ Training process is NOT running!"
    echo ""
    echo "Checking final results..."
    echo ""
else
    echo "✅ Training process IS RUNNING (PID: $PID)"
    echo ""
fi

# Extract progress from log
echo "📊 TRAINING PROGRESS:"
echo "------------------------------------------------------"
grep -E "^(📈 Epoch [0-9]+/50|   📊 Results:|      Train Loss:|      Val Loss:|      ✓ NEW BEST|      No improvement|⚠️|✅ TRAINING COMPLETE)" training_resume_output.log | tail -50

echo ""
echo "======================================================"
echo "SUMMARY:"
echo "------------------------------------------------------"

# Count completed epochs
COMPLETED_EPOCHS=$(grep -c "^   📊 Results:" training_resume_output.log)
echo "✓ Completed epochs: $COMPLETED_EPOCHS"

# Get best val loss
BEST_LOSS=$(grep "✓ NEW BEST MODEL" training_resume_output.log | tail -1 | grep -oE "[0-9]+\.[0-9]+" | head -1)
if [ ! -z "$BEST_LOSS" ]; then
    echo "✓ Best validation loss: $BEST_LOSS"
fi

# Get current epoch
CURRENT_EPOCH=$(grep "^📈 Epoch" training_resume_output.log | tail -1 | grep -oE "[0-9]+/50" | head -1)
if [ ! -z "$CURRENT_EPOCH" ]; then
    echo "✓ Current epoch: $CURRENT_EPOCH"
fi

echo "======================================================"
