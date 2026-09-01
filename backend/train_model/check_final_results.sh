#!/bin/bash
# Check final training results

echo "======================================================"
echo "FINAL TRAINING RESULTS"
echo "======================================================"
echo ""

# Check if process is still running
PID=$(ps aux | grep "train_resume.py" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "✅ Training process COMPLETED"
else
    echo "⏳ Training process STILL RUNNING (PID: $PID)"
    echo "   This is expected - training 50 epochs takes 2-4 hours"
fi

echo ""
echo "------------------------------------------------------"
echo "📊 TRAINING STATISTICS:"
echo "------------------------------------------------------"

# Count completed epochs
COMPLETED_EPOCHS=$(grep -c "^   📊 Results:" training_resume_output.log)
COMPLETION_PCT=$(echo "scale=1; ($COMPLETED_EPOCHS / 43) * 100" | bc)
echo "Epochs completed: $COMPLETED_EPOCHS/43 (${COMPLETION_PCT}%)"

# Get best validation loss
echo ""
echo "Best models saved:"
grep "✓ NEW BEST MODEL" training_resume_output.log | tail -5

# Get latest results
echo ""
echo "------------------------------------------------------"
echo "📈 RECENT PROGRESS (Last 10 epochs):"
echo "------------------------------------------------------"
grep -E "^(📈 Epoch|      Val Loss:|      ✓ NEW BEST)" training_resume_output.log | tail -30

# Check for completion
echo ""
echo "------------------------------------------------------"
echo "🎯 COMPLETION STATUS:"
echo "------------------------------------------------------"

if grep -q "✅ TRAINING COMPLETE" training_resume_output.log; then
    echo "✅ Training FINISHED successfully!"
    echo ""
    echo "Final results:"
    grep -A 5 "✅ TRAINING COMPLETE" training_resume_output.log
elif grep -q "⚠️  Early stopping" training_resume_output.log; then
    echo "⚠️  Early stopping triggered"
    echo ""
    grep -A 3 "⚠️  Early stopping" training_resume_output.log
else
    echo "⏳ Training in progress..."

    # Estimate time remaining
    EPOCHS_LEFT=$((43 - COMPLETED_EPOCHS))
    TIME_PER_EPOCH=35  # seconds
    MINUTES_LEFT=$((EPOCHS_LEFT * TIME_PER_EPOCH / 60))

    echo ""
    echo "Estimated time remaining: ~${MINUTES_LEFT} minutes"
    echo "Expected completion: $(date -v+${MINUTES_LEFT}M '+%I:%M %p')"
fi

echo ""
echo "------------------------------------------------------"
echo "📁 OUTPUT FILES:"
echo "------------------------------------------------------"
ls -lh checkpoints/*.pth 2>/dev/null | awk '{print $9, "("$5")"}'

if [ -f "checkpoints/training_history_resumed.png" ]; then
    echo ""
    echo "📊 Training plot: checkpoints/training_history_resumed.png"
fi

echo ""
echo "======================================================"
echo "To monitor live progress, run:"
echo "  tail -f training_resume_output.log"
echo "======================================================"
