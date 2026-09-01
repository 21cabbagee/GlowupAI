#!/bin/bash
# Staging Deployment Verification Script

set -e

STAGING_URL="${STAGING_URL:-https://glowupai-backend-staging.onrender.com}"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "================================================"
echo "  GlowupAI Staging Deployment Verification"
echo "================================================"
echo ""
echo "Staging URL: $STAGING_URL"
echo ""

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Test 1: Health Check
echo "1. Testing Health Check..."
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 30 "$STAGING_URL/api/health")
if [ "$HEALTH_RESPONSE" = "200" ]; then
    HEALTH_DATA=$(curl -s --max-time 30 "$STAGING_URL/api/health")
    print_status 0 "Health check passed (200 OK)"
    echo "   Response: $HEALTH_DATA"
else
    print_status 1 "Health check failed (HTTP $HEALTH_RESPONSE)"
    exit 1
fi
echo ""

# Test 2: Database Health
echo "2. Testing Database Connection..."
DB_STATUS=$(echo "$HEALTH_DATA" | grep -o '"database":{"status":"[^"]*"' | cut -d'"' -f6)
if [ "$DB_STATUS" = "healthy" ]; then
    print_status 0 "Database connection healthy"
else
    print_status 1 "Database connection failed (status: $DB_STATUS)"
fi
echo ""

# Test 3: Analytics Endpoint
echo "3. Testing Analytics Endpoint..."
ANALYTICS_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 30 "$STAGING_URL/api/analytics/summary")
if [ "$ANALYTICS_RESPONSE" = "200" ] || [ "$ANALYTICS_RESPONSE" = "404" ]; then
    print_status 0 "Analytics endpoint accessible (HTTP $ANALYTICS_RESPONSE)"
else
    print_status 1 "Analytics endpoint failed (HTTP $ANALYTICS_RESPONSE)"
fi
echo ""

# Test 4: Rate Limiting
echo "4. Testing Rate Limiting..."
print_warning "Sending 10 requests to test rate limiting..."
RATE_LIMIT_HIT=0
for i in {1..10}; do
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$STAGING_URL/api/health")
    if [ "$RESPONSE" = "429" ]; then
        RATE_LIMIT_HIT=1
        break
    fi
    sleep 0.1
done

if [ $RATE_LIMIT_HIT -eq 1 ]; then
    print_status 0 "Rate limiting is active (received 429)"
else
    print_warning "Rate limiting not triggered in 10 requests (may need more requests)"
fi
echo ""

# Test 5: Environment Check
echo "5. Checking Environment Configuration..."
VERSION=$(echo "$HEALTH_DATA" | grep -o '"version":"[^"]*"' | cut -d'"' -f4)
if [ ! -z "$VERSION" ]; then
    print_status 0 "Version: $VERSION"
else
    print_warning "Version not found in health check"
fi
echo ""

# Test 6: Response Time
echo "6. Testing Response Time..."
START_TIME=$(date +%s%N)
curl -s --max-time 30 "$STAGING_URL/api/health" > /dev/null
END_TIME=$(date +%s%N)
RESPONSE_TIME=$(( ($END_TIME - $START_TIME) / 1000000 ))
if [ $RESPONSE_TIME -lt 2000 ]; then
    print_status 0 "Response time: ${RESPONSE_TIME}ms (< 2000ms)"
else
    print_warning "Response time: ${RESPONSE_TIME}ms (slower than expected)"
fi
echo ""

# Summary
echo "================================================"
echo "  Verification Summary"
echo "================================================"
echo ""
echo -e "${GREEN}Staging deployment verification complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Monitor deployment logs for any errors"
echo "2. Test new endpoints manually"
echo "3. Run integration tests"
echo "4. If all tests pass, merge to main for production deployment"
echo ""
