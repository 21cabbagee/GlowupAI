# Production Deployment Guide

Complete guide for deploying GlowUp AI Backend to production with security, monitoring, and performance optimizations.

## Table of Contents

- [Quick Start](#quick-start)
- [Production Features](#production-features)
- [Deployment Platforms](#deployment-platforms)
- [Configuration](#configuration)
- [Monitoring Setup](#monitoring-setup)
- [Performance Tuning](#performance-tuning)
- [Security Checklist](#security-checklist)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites

1. **Database**: PostgreSQL 14+ (Railway auto-provisions)
2. **Redis**: Redis 6+ for caching and rate limiting (Railway auto-provisions)
3. **Sentry Account**: For error monitoring (free tier available)
4. **Firebase Project**: For authentication
5. **Gemini API Key**: For AI features

### Minimal Production Setup (5 minutes)

```bash
# 1. Core configuration
GLOWUPAI_ENV=production
GLOWUPAI_FIREBASE_PROJECT_ID=your-firebase-project
GLOWUPAI_ALLOWED_ORIGINS=https://your-app.com
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1

# 2. API keys
GEMINI_API_KEY=your_gemini_key
GLOWUPAI_ADMIN_TOKEN=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))")

# 3. Monitoring (recommended)
SENTRY_DSN=https://...@sentry.io/...

# 4. Database & Redis (auto-injected by Railway)
# DATABASE_URL - auto-injected
# REDIS_URL - auto-injected
```

---

## Production Features

### 1. Rate Limiting 🛡️

**Purpose**: Prevent API abuse and ensure fair usage

**Implementation**:
- Redis-backed sliding window algorithm
- Fallback to in-memory if Redis unavailable
- Per-endpoint limits:
  - `POST /api/captures`: 10 requests/minute
  - `POST /api/auth/*`: 5 requests/minute
  - `GET /api/dashboard`: 30 requests/minute
  - Other endpoints: 60 requests/minute

**Response Headers**:
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
Retry-After: 42
```

**Configuration**:
```bash
GLOWUPAI_RATE_LIMIT_ENABLED=1  # Default: enabled
REDIS_URL=redis://...           # Required for distributed limiting
```

**Testing**:
```bash
# Should return 429 after 10 requests
for i in {1..15}; do
  curl -X POST https://api.glowup.com/api/captures \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"user_id":"test","image_base64":"..."}'
done
```

---

### 2. Error Monitoring (Sentry) 📊

**Purpose**: Real-time error tracking and performance monitoring

**Setup**:

1. **Create Sentry Project**:
   - Go to https://sentry.io
   - Create new project → FastAPI/Python
   - Copy DSN

2. **Configure**:
   ```bash
   SENTRY_DSN=https://abc123@o0.ingest.sentry.io/123456
   SENTRY_TRACES_SAMPLE_RATE=0.1   # 10% of requests traced
   SENTRY_PROFILES_SAMPLE_RATE=0.1 # 10% profiled
   ```

3. **Verify**:
   ```bash
   # Trigger test error
   curl https://api.glowup.com/api/test-error
   # Check Sentry dashboard for error
   ```

**Features**:
- Automatic exception capture
- Performance monitoring
- Breadcrumbs (user actions leading to error)
- Release tracking (Git commit)
- User context (user_id, endpoint)

**Sentry Dashboard**:
- View errors: https://sentry.io/issues/
- Performance: https://sentry.io/performance/
- Releases: https://sentry.io/releases/

---

### 3. Analytics 📈

**Purpose**: Track user behavior and key metrics

**Events Tracked**:
- `user_signup`: New user registration
- `capture_created`: Photo capture
- `comparison_viewed`: History page view
- `baseline_set`: Baseline photo set
- `streak_milestone`: 3, 7, 14, 30 day streaks

**Admin Endpoints**:

```bash
# Get analytics summary (last 7 days)
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.glowup.com/api/admin/analytics

# Get daily stats (last 30 days)
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.glowup.com/api/admin/analytics/daily

# Get event counts by type
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "https://api.glowup.com/api/admin/analytics/events?event_type=capture_created&days=7"
```

**Response Example**:
```json
{
  "period_days": 7,
  "total_events": 1234,
  "unique_users": 89,
  "event_counts": {
    "capture_created": 456,
    "comparison_viewed": 234,
    "user_signup": 12
  },
  "top_users": [
    {"user_id": "user_abc123", "event_count": 45}
  ]
}
```

---

### 4. Performance Optimizations ⚡

#### Image Compression

**Purpose**: Reduce storage and bandwidth usage

**Configuration**:
```bash
GLOWUPAI_MAX_IMAGE_DIMENSION=1024  # Max width/height
GLOWUPAI_IMAGE_QUALITY=85          # JPEG quality (0-100)
```

**Results**:
- Original: 3-5MB per photo
- Compressed: 150-300KB per photo (90-95% reduction)
- Quality: Visually indistinguishable
- Processing: ~50-100ms per image

#### Response Caching

**Purpose**: Speed up dashboard and history endpoints

**Configuration**:
```bash
GLOWUPAI_CACHE_ENABLED=1  # Default: enabled
REDIS_URL=redis://...      # Required
```

**Cached Endpoints**:
- `GET /api/users/{id}/dashboard` (5-minute TTL)
- `GET /api/users/{id}/history` (5-minute TTL)

**Cache Headers**:
```
X-Cache: HIT  # or MISS
X-Response-Time: 23.45ms
```

#### Request Timing

**Purpose**: Identify slow endpoints

**Configuration**:
```bash
GLOWUPAI_SLOW_THRESHOLD_MS=1000  # Log requests > 1 second
```

**Logs**:
```
WARNING Slow request: POST /api/captures took 1245.67ms
```

#### Database Indexes

Automatically created by migration `0005_analytics_and_indexes.sql`:
- `idx_captures_user_created` - Speed up history queries
- `idx_routine_events_user_time` - Speed up timeline
- `idx_analytics_user_type` - Speed up analytics queries
- `idx_users_firebase_uid` - Speed up auth lookups

---

## Deployment Platforms

### Railway (Recommended)

**Why Railway?**
- Auto-provisions PostgreSQL and Redis
- Zero-config deployments
- Git-based CI/CD
- Environment variable management
- Built-in metrics

**Steps**:

1. **Create Railway Project**:
   ```bash
   # Install Railway CLI
   npm i -g @railway/cli
   
   # Login and init
   railway login
   railway init
   ```

2. **Add Services**:
   ```bash
   # Add PostgreSQL
   railway add postgresql
   
   # Add Redis
   railway add redis
   ```

3. **Set Environment Variables**:
   ```bash
   # See ENV_VARS_REFERENCE.md for full list
   railway variables set GLOWUPAI_ENV=production
   railway variables set GLOWUPAI_FIREBASE_PROJECT_ID=your-project
   railway variables set GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com
   railway variables set GEMINI_API_KEY=your-key
   railway variables set SENTRY_DSN=your-sentry-dsn
   railway variables set GLOWUPAI_ADMIN_TOKEN=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))")
   ```

4. **Deploy**:
   ```bash
   railway up
   ```

5. **Verify**:
   ```bash
   railway open
   # Navigate to /api/health
   ```

### Render

**Steps**:

1. Connect GitHub repository
2. Create Web Service
3. Set build command: `pip install .`
4. Set start command: `uvicorn glowupai.complete_api:app --host 0.0.0.0 --port $PORT`
5. Add PostgreSQL and Redis add-ons
6. Set environment variables (see ENV_VARS_REFERENCE.md)

### Docker

**Dockerfile** (already included):
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY . .
RUN pip install .
CMD ["uvicorn", "glowupai.complete_api:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Docker Compose** (for local testing):
```yaml
version: '3.8'
services:
  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql://user:pass@db:5432/glowupai
      - REDIS_URL=redis://redis:6379/0
      - SENTRY_DSN=your-sentry-dsn
    depends_on:
      - db
      - redis
  
  db:
    image: postgres:14
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
      POSTGRES_DB: glowupai
  
  redis:
    image: redis:7-alpine
```

---

## Configuration

### Environment Variables Priority

See `ENV_VARS_REFERENCE.md` for complete reference.

**Critical (Must Set)**:
```bash
GLOWUPAI_ENV=production
GLOWUPAI_FIREBASE_PROJECT_ID=your-project
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GEMINI_API_KEY=your-key
GLOWUPAI_ADMIN_TOKEN=your-token
```

**Recommended**:
```bash
SENTRY_DSN=your-sentry-dsn
REDIS_URL=redis://...  # Auto-injected by Railway
```

**Optional (Tuning)**:
```bash
GLOWUPAI_DB_POOL_MIN_SIZE=2
GLOWUPAI_DB_POOL_MAX_SIZE=20
GLOWUPAI_MAX_IMAGE_DIMENSION=1024
GLOWUPAI_IMAGE_QUALITY=85
GLOWUPAI_CACHE_ENABLED=1
GLOWUPAI_RATE_LIMIT_ENABLED=1
```

---

## Monitoring Setup

### Health Checks

**Endpoint**: `GET /api/health`

**Response**:
```json
{
  "status": "healthy",
  "version": "3.0.0",
  "checks": {
    "database": {"status": "healthy", "backend": "postgres"},
    "disk": {"status": "healthy", "free_gb": 45.2},
    "gemini_api": {"status": "configured"}
  },
  "features": ["experiments", "qna", "analytics", ...]
}
```

**Monitoring**:
- Railway: Built-in uptime monitoring
- External: UptimeRobot, Pingdom, Better Stack
- Configure to check `/api/health` every 5 minutes

### Metrics Endpoint

**Endpoint**: `GET /api/metrics` (admin only)

**Response**:
```json
{
  "total_requests": 12345,
  "total_errors": 23,
  "error_rate": 0.19,
  "avg_duration_ms": 145.67,
  "top_endpoints": [
    ["POST /api/captures", 3456],
    ["GET /api/dashboard", 2345]
  ],
  "status_codes": {
    "200": 12000,
    "400": 200,
    "401": 100,
    "429": 22,
    "500": 23
  }
}
```

### Sentry Alerts

**Configure in Sentry**:
1. Go to Alerts → Create Alert Rule
2. Set conditions:
   - Error rate > 1% for 5 minutes
   - Response time p95 > 2 seconds
   - New issue occurs
3. Set notification channels (email, Slack, PagerDuty)

---

## Performance Tuning

### Database Connection Pool

**Low Traffic** (< 100 concurrent users):
```bash
GLOWUPAI_DB_POOL_MIN_SIZE=1
GLOWUPAI_DB_POOL_MAX_SIZE=10
```

**Medium Traffic** (100-1000 concurrent users):
```bash
GLOWUPAI_DB_POOL_MIN_SIZE=2
GLOWUPAI_DB_POOL_MAX_SIZE=20
```

**High Traffic** (> 1000 concurrent users):
```bash
GLOWUPAI_DB_POOL_MIN_SIZE=5
GLOWUPAI_DB_POOL_MAX_SIZE=50
```

### Image Compression

**Storage Priority** (minimize storage):
```bash
GLOWUPAI_MAX_IMAGE_DIMENSION=768
GLOWUPAI_IMAGE_QUALITY=75
```

**Quality Priority** (maximize quality):
```bash
GLOWUPAI_MAX_IMAGE_DIMENSION=2048
GLOWUPAI_IMAGE_QUALITY=95
```

**Balanced** (recommended):
```bash
GLOWUPAI_MAX_IMAGE_DIMENSION=1024
GLOWUPAI_IMAGE_QUALITY=85
```

### Caching Strategy

**Aggressive Caching** (slower-changing data):
- Modify `RedisCache` default TTL to 600 (10 minutes)

**Minimal Caching** (fast-changing data):
```bash
GLOWUPAI_CACHE_ENABLED=0  # Disable entirely
```

---

## Security Checklist

### Pre-Deployment

- [ ] `GLOWUPAI_ENV=production` set
- [ ] `GLOWUPAI_ALLOWED_ORIGINS` contains only production domains
- [ ] `GLOWUPAI_ADMIN_TOKEN` is strong (32+ characters)
- [ ] `GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1` set
- [ ] `GEMINI_API_KEY` not exposed in logs/Git
- [ ] `SENTRY_DSN` configured (for error tracking)
- [ ] Database uses SSL/TLS connection
- [ ] Redis uses password authentication
- [ ] Health check endpoint accessible
- [ ] Rate limiting enabled

### Post-Deployment

- [ ] Health check returns 200
- [ ] Sentry receiving events
- [ ] Rate limiting working (test with 429 response)
- [ ] Admin endpoints require token
- [ ] CORS only allows production origins
- [ ] SSL/HTTPS certificate valid
- [ ] Database backups configured
- [ ] Monitoring alerts configured

### Regular Maintenance

- [ ] Review Sentry errors weekly
- [ ] Check analytics for anomalies
- [ ] Monitor database size
- [ ] Review slow request logs
- [ ] Update dependencies monthly
- [ ] Rotate admin token quarterly
- [ ] Review CORS origins

---

## Troubleshooting

### Health Check Fails

**Symptom**: `/api/health` returns 503

**Possible Causes**:
1. Database connection failed
   - Check `DATABASE_URL` is set
   - Verify PostgreSQL service is running
   - Check network connectivity

2. Disk space low
   - Check available disk space
   - Clean up old photos if needed

**Fix**:
```bash
# Check logs
railway logs

# Verify database
railway run psql $DATABASE_URL -c "SELECT 1"

# Check disk
railway run df -h
```

### Rate Limiting Not Working

**Symptom**: No 429 responses, no rate limit headers

**Possible Causes**:
1. Rate limiting disabled
2. Redis not connected
3. Middleware not configured

**Fix**:
```bash
# Enable rate limiting
railway variables set GLOWUPAI_RATE_LIMIT_ENABLED=1

# Verify Redis
railway run redis-cli -u $REDIS_URL ping

# Check logs for Redis connection errors
railway logs | grep -i redis
```

### Sentry Not Receiving Errors

**Symptom**: No events in Sentry dashboard

**Possible Causes**:
1. `SENTRY_DSN` not set
2. Invalid DSN
3. Sentry SDK not installed

**Fix**:
```bash
# Verify DSN is set
railway variables get SENTRY_DSN

# Test Sentry manually
railway run python3 -c "
import sentry_sdk
sentry_sdk.init(dsn='$SENTRY_DSN')
sentry_sdk.capture_message('Test from production')
"

# Check Sentry dashboard within 1 minute
```

### Slow Dashboard Performance

**Symptom**: `/api/dashboard` takes > 2 seconds

**Possible Causes**:
1. Cache not enabled
2. Redis not connected
3. Database slow queries
4. Large dataset

**Fix**:
```bash
# Enable caching
railway variables set GLOWUPAI_CACHE_ENABLED=1
railway variables set REDIS_URL=redis://...

# Check slow queries in logs
railway logs | grep "Slow request"

# Consider database indexes
railway run psql $DATABASE_URL -c "
SELECT schemaname, tablename, indexname 
FROM pg_indexes 
WHERE schemaname = 'public';"
```

### Image Upload Fails

**Symptom**: 413 or 500 on `POST /api/captures`

**Possible Causes**:
1. Image too large (> 10MB)
2. Compression failing
3. Disk space full

**Fix**:
```bash
# Check image size in request
# Should be < 10MB base64

# Verify disk space
railway run df -h

# Check compression logs
railway logs | grep "Image compressed"

# Reduce max dimension if needed
railway variables set GLOWUPAI_MAX_IMAGE_DIMENSION=768
```

---

## Next Steps

1. **Deploy**: Follow platform-specific steps above
2. **Monitor**: Set up Sentry alerts and health check monitoring
3. **Test**: Run security and performance tests
4. **Scale**: Adjust pool sizes and caching based on traffic
5. **Maintain**: Regular reviews of errors and performance

## Resources

- **Full Configuration**: `ENV_VARS_REFERENCE.md`
- **Railway Setup**: `RAILWAY_DEPLOY.md`
- **Security Hardening**: `SECURITY_HARDENING.md`
- **Database Guide**: `DATABASE_GUIDE.md`
- **Monitoring Guide**: `MONITORING_GUIDE.md`

## Support

- **Issues**: GitHub Issues
- **Email**: support@glowupai.com
- **Docs**: docs.glowupai.com
