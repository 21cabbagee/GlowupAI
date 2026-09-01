#!/bin/bash
# Load testing script for GlowupAI backend
# Runs three test scenarios and collects metrics

set -e

RESULTS_DIR="load_test_results"
LOCUST_FILE="backend/tests/load/simple_locustfile.py"
HOST="http://localhost:8000"

echo "=================================="
echo "GlowupAI Backend Load Testing"
echo "=================================="
echo ""
echo "Target: $HOST"
echo "Results directory: $RESULTS_DIR"
echo ""

# Activate the load test environment
source load_test_env/bin/activate

# Test 1: Baseline - 10 users, 1 minute
echo "Test 1/3: BASELINE (10 users, 1 min)"
echo "--------------------------------------"
locust -f "$LOCUST_FILE" \
    --host="$HOST" \
    --users 10 \
    --spawn-rate 2 \
    --run-time 1m \
    --headless \
    --html "$RESULTS_DIR/baseline_report.html" \
    --csv "$RESULTS_DIR/baseline" \
    --loglevel WARNING

echo ""
echo "✅ Baseline test complete"
echo ""
sleep 5

# Test 2: Medium - 50 users, 2 minutes
echo "Test 2/3: MEDIUM LOAD (50 users, 2 min)"
echo "--------------------------------------"
locust -f "$LOCUST_FILE" \
    --host="$HOST" \
    --users 50 \
    --spawn-rate 5 \
    --run-time 2m \
    --headless \
    --html "$RESULTS_DIR/medium_report.html" \
    --csv "$RESULTS_DIR/medium" \
    --loglevel WARNING

echo ""
echo "✅ Medium load test complete"
echo ""
sleep 5

# Test 3: High - 100 users, 3 minutes
echo "Test 3/3: HIGH LOAD (100 users, 3 min)"
echo "--------------------------------------"
locust -f "$LOCUST_FILE" \
    --host="$HOST" \
    --users 100 \
    --spawn-rate 10 \
    --run-time 3m \
    --headless \
    --html "$RESULTS_DIR/high_report.html" \
    --csv "$RESULTS_DIR/high" \
    --loglevel WARNING

echo ""
echo "✅ High load test complete"
echo ""

echo "=================================="
echo "All tests completed!"
echo "=================================="
echo ""
echo "Results saved to: $RESULTS_DIR/"
echo "- baseline_report.html"
echo "- medium_report.html"
echo "- high_report.html"
echo "- CSV files for detailed analysis"
