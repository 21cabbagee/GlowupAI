# Production Backend - Quick Reference

**Version**: 3.0.0 | **Status**: PRODUCTION READY ✅

## Essential Environment Variables

```bash
# Database (REQUIRED)
DATABASE_URL=postgresql://user:pass@host:5432/dbname

# CORS (REQUIRED in production)
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.ai

# Environment
GLOWUPAI_ENV=production

# Authentication
GLOWUPAI_AUTH_REQUIRED=1
GLOWUPAI_FIREBASE_PROJECT_ID=your-project-id

# Admin Access
GLOWUPAI_ADMIN_TOKEN=<generate-random-token>

# AI
GLOWUPAI_GEMINI_API_KEY=<your-key>
```

## Key Endpoints

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/api/health` | GET | None | Health check |
| `/api/metrics` | GET | Admin | Metrics |
| `/api/users` | POST | None | Create user |
| `/api/captures` | POST | User | Upload photo |

## Health Check

```bash
curl https://api.glowup.ai/api/health
```

**Healthy Response** (200):
```json
{"status": "healthy", "checks": {...}}
```

**Unhealthy Response** (503):
```json
{"status": "unhealthy", "checks": {...}}
```

## Metrics

```bash
curl -H "Authorization: Bearer <admin-token>" \
  https://api.glowup.ai/api/metrics
```

**Response**:
```json
{
  "total_requests": 15234,
  "error_rate": 0.29,
  "avg_duration_ms": 145.67,
  "top_endpoints": [...]
}
```

## Rate Limits

| Type | Limit | Burst |
|------|-------|-------|
| Auth | 10/min | 20 |
| Upload | 5/min | 10 |
| API | 60/min | 100 |
| Admin | 100/min | 200 |

**Response when exceeded**: 429 with `Retry-After` header

## Log Format

```json
{
  "timestamp": "2026-08-31T10:15:30Z",
  "level": "INFO",
  "message": "POST /api/captures",
  "request_id": "abc123",
  "status_code": 200,
  "duration_ms": 234.5
}
```

## Common Issues

### Service Won't Start
**Error**: `GLOWUPAI_ALLOWED_ORIGINS must be configured`  
**Fix**: Set `GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com`

### Database Connection Failed
**Error**: `database unavailable`  
**Fix**: Check `DATABASE_URL`, network, credentials

### High Error Rate
**Symptom**: Error rate > 5%  
**Fix**: Check logs, recent deploys, dependencies

### Rate Limit Too Strict
**Symptom**: Many 429 responses  
**Fix**: Adjust limits in `middleware.py`

## Quick Deployment

```bash
# 1. Set environment variables
export DATABASE_URL=postgresql://...
export GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.ai
# ... (see above for all required vars)

# 2. Build
docker build -t glowupai:production .

# 3. Test locally
docker run --env-file .env.production -p 8000:8000 glowupai:production

# 4. Deploy
railway up  # or your platform

# 5. Verify
curl https://api.glowup.ai/api/health
```

## Monitoring Checklist

- [ ] Health check endpoint configured
- [ ] Log forwarding enabled
- [ ] Alerts set up (error rate, service down)
- [ ] Metrics dashboard created
- [ ] Admin token secured

## Security Checklist

- [ ] HTTPS enabled
- [ ] CORS configured (no `*`)
- [ ] Auth enabled (`GLOWUPAI_AUTH_REQUIRED=1`)
- [ ] Admin token strong and secret
- [ ] Database uses SSL
- [ ] Rate limiting enabled
- [ ] Secrets in environment (not code)

## Performance Targets

| Metric | Target |
|--------|--------|
| p50 latency | < 200ms |
| p95 latency | < 1000ms |
| p99 latency | < 2000ms |
| Error rate | < 1% |
| Uptime | > 99.9% |

## Documentation

- **Overview**: `PRODUCTION_READY.md`
- **Deployment**: `PRODUCTION_CHECKLIST.md`
- **Monitoring**: `MONITORING_GUIDE.md`
- **Security**: `SECURITY_HARDENING.md`
- **Database**: `DATABASE_GUIDE.md`

## Support

**Health Check**: `/api/health`  
**Metrics**: `/api/metrics` (admin only)  
**Logs**: Check platform or aggregation service  
**Docs**: See guides above

---

**Need help?** See `MONITORING_GUIDE.md` for troubleshooting.
