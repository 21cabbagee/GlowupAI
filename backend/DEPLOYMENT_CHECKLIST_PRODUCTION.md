# Production Deployment Checklist

Use this checklist when deploying the GlowUp AI Backend with all production features.

## Pre-Deployment

### Code Verification

- [x] All new files have valid Python syntax
- [ ] Run tests: `pytest tests/`
- [ ] Review `git diff` for unintended changes
- [ ] Update version number if needed

### Dependencies

- [ ] Install new dependencies:
  ```bash
  pip install redis>=5.0.0 sentry-sdk[fastapi]>=2.0.0
  ```
- [ ] Verify installation:
  ```bash
  pip list | grep -E "(redis|sentry-sdk)"
  ```

### Database

- [ ] Review migration: `glowupai/migrations/0005_analytics_and_indexes.sql`
- [ ] Test migration on development database first
- [ ] Backup production database before deployment

## Environment Configuration

### Required Variables

- [ ] `GLOWUPAI_ENV=production`
- [ ] `GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7`
- [ ] `GLOWUPAI_ALLOWED_ORIGINS=https://your-app.com`
- [ ] `GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1`
- [ ] `GEMINI_API_KEY=your_gemini_key`
- [ ] `GLOWUPAI_ADMIN_TOKEN=your_secure_token` (32+ chars)

### Monitoring (Recommended)

- [ ] `SENTRY_DSN=https://...@sentry.io/...`
  - [ ] Sentry account created
  - [ ] FastAPI project created in Sentry
  - [ ] DSN copied from project settings

### Performance (Recommended)

- [ ] `REDIS_URL=redis://...` (or Railway auto-injects)
  - [ ] Redis service added to Railway/platform
  - [ ] Redis connection tested

### Optional Tuning

- [ ] `GLOWUPAI_MAX_IMAGE_DIMENSION=1024` (default)
- [ ] `GLOWUPAI_IMAGE_QUALITY=85` (default)
- [ ] `GLOWUPAI_CACHE_ENABLED=1` (default)
- [ ] `GLOWUPAI_RATE_LIMIT_ENABLED=1` (default)
- [ ] `GLOWUPAI_SLOW_THRESHOLD_MS=1000` (default)

## Deployment Steps

### Railway Deployment

1. **Install Dependencies**
   ```bash
   cd /Users/21cabbage/GlowupAI/backend
   pip install -e .
   ```

2. **Add Services**
   ```bash
   railway add redis  # If not already added
   ```

3. **Set Environment Variables**
   ```bash
   railway variables set GLOWUPAI_ENV=production
   railway variables set SENTRY_DSN=your-sentry-dsn
   # ... (see Required Variables above)
   ```

4. **Deploy**
   ```bash
   git add .
   git commit -m "Add production features: rate limiting, monitoring, analytics, performance"
   git push
   railway up
   ```

5. **Wait for deployment** (~2-5 minutes)

### Alternative Platforms

- [ ] **Render**: Follow steps in `PRODUCTION_DEPLOYMENT.md` → Render section
- [ ] **Docker**: Build and push: `docker build -t glowupai/backend .`
- [ ] **Other**: Follow platform-specific instructions

## Post-Deployment Verification

### Health Check

```bash
# Should return 200 with status: healthy
curl https://your-api-url.com/api/health
```

- [ ] Status is "healthy"
- [ ] Database check passes
- [ ] Disk check passes

### Rate Limiting

```bash
# Test rate limit on captures endpoint
for i in {1..12}; do
  curl -X POST https://your-api-url.com/api/captures \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"user_id":"test","image_base64":"..."}'
done
```

- [ ] First 10 requests succeed (200)
- [ ] 11th+ requests fail with 429
- [ ] Response includes `Retry-After` header

### Sentry Integration

```bash
# Trigger test error (create this endpoint or manually trigger error)
curl https://your-api-url.com/api/test-error
```

- [ ] Error appears in Sentry dashboard within 1 minute
- [ ] Stack trace is visible
- [ ] Environment is set correctly (production)

### Analytics

```bash
# Create a capture to trigger analytics
curl -X POST https://your-api-url.com/api/captures \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"user_id":"test","image_base64":"..."}'

# Check analytics
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://your-api-url.com/api/admin/analytics
```

- [ ] `capture_created` event recorded
- [ ] Analytics endpoint returns data
- [ ] Event counts are accurate

### Caching

```bash
# First request (cache miss)
time curl https://your-api-url.com/api/users/test/dashboard \
  -H "Authorization: Bearer $TOKEN"

# Second request (cache hit)
time curl https://your-api-url.com/api/users/test/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

- [ ] Second request is faster than first
- [ ] Response headers include `X-Response-Time`

### Image Compression

```bash
# Upload a large image
curl -X POST https://your-api-url.com/api/captures \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @large_image.json

# Check logs
railway logs | grep "Image compressed"
```

- [ ] Log shows compression (e.g., "3456KB → 234KB")
- [ ] Capture is created successfully
- [ ] Image quality is acceptable

### Metrics

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://your-api-url.com/api/metrics
```

- [ ] Metrics endpoint returns data
- [ ] Request counts are accurate
- [ ] Error rate is calculated
- [ ] Top endpoints are listed

## Monitoring Setup

### Uptime Monitoring

- [ ] Add health check to monitoring service:
  - UptimeRobot (free)
  - Pingdom
  - Better Stack
  - Railway built-in monitoring
- [ ] Configure alerts for downtime
- [ ] Test alert by stopping service temporarily

### Sentry Alerts

- [ ] Configure error rate alert (> 1% for 5 minutes)
- [ ] Configure performance alert (p95 > 2 seconds)
- [ ] Configure new issue alert (notify immediately)
- [ ] Add notification channels (email, Slack, PagerDuty)
- [ ] Test alerts by triggering errors

### Log Monitoring

- [ ] Review logs for errors: `railway logs | grep ERROR`
- [ ] Review slow requests: `railway logs | grep "Slow request"`
- [ ] Set up log aggregation (optional):
  - Datadog
  - Logtail
  - Papertrail

## Security Verification

### Authentication

- [ ] Admin endpoints require token
  ```bash
  # Should return 403
  curl https://your-api-url.com/api/admin/analytics
  ```
- [ ] Invalid admin token returns 403
- [ ] User endpoints require valid Firebase token (if auth enabled)

### CORS

- [ ] Production origin can make requests
- [ ] Localhost/development origins are blocked
- [ ] Credentials are not allowed (Bearer token auth)

### Rate Limiting

- [ ] Rate limits are enforced per IP
- [ ] Rate limit headers are present
- [ ] 429 responses include retry-after

### Environment

- [ ] `GLOWUPAI_ENV=production` (not development)
- [ ] Legacy key file is disabled
- [ ] Secrets are not in logs
- [ ] Secrets are not in version control

## Performance Verification

### Response Times

- [ ] `/api/health`: < 100ms
- [ ] `/api/dashboard`: < 500ms (first request), < 150ms (cached)
- [ ] `/api/captures`: < 2 seconds (with compression)
- [ ] `/api/history`: < 500ms

### Database

- [ ] Connection pool size is appropriate
- [ ] No "connection pool exhausted" errors
- [ ] Indexes are created (check with `\di` in psql)

### Storage

- [ ] Photo storage directory exists and is writable
- [ ] Disk space is sufficient (> 5GB free)
- [ ] Photos are encrypted (if enabled)

### Redis

- [ ] Redis connection is successful
- [ ] Rate limiting uses Redis (check logs)
- [ ] Caching uses Redis (check logs)
- [ ] Fallback to memory works if Redis fails

## Rollback Plan

If issues occur after deployment:

### Quick Rollback

```bash
# Railway
railway rollback

# Git
git revert HEAD
git push
```

### Disable Features

If specific features cause issues, disable them:

```bash
# Disable rate limiting
railway variables set GLOWUPAI_RATE_LIMIT_ENABLED=0

# Disable caching
railway variables set GLOWUPAI_CACHE_ENABLED=0

# Remove Sentry
railway variables delete SENTRY_DSN

# Restart
railway restart
```

### Database Rollback

If migration causes issues:

```bash
# Drop analytics table (data loss!)
railway run psql $DATABASE_URL -c "DROP TABLE IF EXISTS analytics_events;"

# Drop indexes
railway run psql $DATABASE_URL -c "
  DROP INDEX IF EXISTS idx_captures_user_created;
  DROP INDEX IF EXISTS idx_routine_events_user_time;
  DROP INDEX IF EXISTS idx_analytics_user_type;
  DROP INDEX IF EXISTS idx_users_firebase_uid;
"
```

## Success Criteria

All checks passed:

- [ ] Health check returns 200
- [ ] Rate limiting works (429 responses)
- [ ] Sentry receives errors
- [ ] Analytics tracks events
- [ ] Caching improves performance
- [ ] Image compression reduces size
- [ ] Admin endpoints require token
- [ ] CORS is configured correctly
- [ ] Response times are acceptable
- [ ] No errors in logs

## Post-Deployment Tasks

### Documentation

- [ ] Update internal documentation with new features
- [ ] Share admin endpoints with team
- [ ] Document admin token location

### Monitoring

- [ ] Set up weekly review of Sentry errors
- [ ] Set up weekly review of analytics
- [ ] Monitor storage growth
- [ ] Monitor Redis memory usage

### Optimization

- [ ] Review slow request logs weekly
- [ ] Optimize slow endpoints
- [ ] Adjust rate limits based on usage
- [ ] Tune cache TTL based on data freshness

## Troubleshooting

If issues occur, see:
- `PRODUCTION_DEPLOYMENT.md` → Troubleshooting section
- `PRODUCTION_FEATURES_SUMMARY.md` → Testing section
- Railway logs: `railway logs`
- Sentry dashboard: https://sentry.io/issues/

## Contacts

- **DevOps**: devops@glowupai.com
- **Backend Lead**: backend@glowupai.com
- **On-Call**: oncall@glowupai.com

---

**Deployment Date**: _____________

**Deployed By**: _____________

**Version**: 3.0.0

**Git Commit**: _____________

**Notes**:
