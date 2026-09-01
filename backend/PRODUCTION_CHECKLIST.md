# Production Deployment Checklist

This checklist covers everything needed to deploy the GlowUp AI backend to production safely and reliably.

## Pre-Deployment Configuration

### Environment Variables (Required)

```bash
# Database Configuration
DATABASE_URL=postgresql://user:password@host:5432/dbname
GLOWUPAI_DB_POOL_MIN_SIZE=2
GLOWUPAI_DB_POOL_MAX_SIZE=20
GLOWUPAI_DB_CONNECT_TIMEOUT=10
GLOWUPAI_DB_STATEMENT_TIMEOUT=30000  # 30 seconds
GLOWUPAI_DB_POOL_TIMEOUT=30

# Environment
GLOWUPAI_ENV=production

# CORS Configuration (REQUIRED IN PRODUCTION)
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.ai,https://www.glowup.ai

# Authentication
GLOWUPAI_AUTH_REQUIRED=1
GLOWUPAI_FIREBASE_PROJECT_ID=your-firebase-project-id

# Admin Access
GLOWUPAI_ADMIN_TOKEN=<generate-secure-random-token>

# AI/Gemini API
GLOWUPAI_GEMINI_API_KEY=<your-api-key>
GLOWUPAI_GEMINI_MODEL=gemini-3.5-flash-lite
GLOWUPAI_GEMINI_ENABLED=1

# Logging
GLOWUPAI_LOG_LEVEL=INFO
GLOWUPAI_JSON_LOGS=1

# Rate Limiting & Timeouts
GLOWUPAI_RATE_LIMIT_ENABLED=1
GLOWUPAI_REQUEST_TIMEOUT=30

# Photo Storage (if using local storage)
GLOWUPAI_PHOTO_DIR=/app/photos

# Optional: OpenTelemetry
OTEL_ENABLED=0  # Set to 1 if you have observability backend
OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317
OTEL_SERVICE_NAME=glowupai

# Uvicorn Workers (for horizontal scaling)
UVICORN_WORKERS=2  # Adjust based on CPU cores
```

### Security Checklist

- [ ] **Generate Secure Admin Token**: Use a cryptographically secure random token
  ```bash
  python -c "import secrets; print(secrets.token_urlsafe(32))"
  ```

- [ ] **CORS Origins**: Set `GLOWUPAI_ALLOWED_ORIGINS` to your actual frontend domains
  - Never use `*` in production
  - Include all subdomains if needed

- [ ] **Authentication**: Enable `GLOWUPAI_AUTH_REQUIRED=1` when Firebase is configured

- [ ] **Database Credentials**: Use strong passwords and rotate regularly

- [ ] **SSL/TLS**: Ensure all external connections use HTTPS
  - Database connections should use SSL
  - API must be behind HTTPS load balancer

- [ ] **API Keys**: Protect Gemini API key
  - Use secret management (AWS Secrets Manager, Kubernetes Secrets, etc.)
  - Never commit to version control

- [ ] **Docker Security**:
  - [ ] Image runs as non-root user (already configured)
  - [ ] Scan image for vulnerabilities: `docker scan glowupai:latest`
  - [ ] Use image signing/verification if available

### Database Setup

- [ ] **Create Production Database**
  ```sql
  CREATE DATABASE glowupai_prod;
  CREATE USER glowupai_user WITH PASSWORD 'secure-password';
  GRANT ALL PRIVILEGES ON DATABASE glowupai_prod TO glowupai_user;
  ```

- [ ] **Run Migrations**
  - Schema is auto-created on first startup
  - Verify all tables created successfully

- [ ] **Indexes**: Verify critical indexes exist
  - `users.firebase_uid` (unique index)
  - `routine_events.user_id`
  - `captures.user_id`
  - `experiments.user_id`

- [ ] **Connection Pooling**
  - Set appropriate pool size based on expected load
  - Monitor connection pool usage

- [ ] **Backups**
  - [ ] Automated daily backups configured
  - [ ] Test restore procedure
  - [ ] Set retention policy (recommend 30 days)

### Resource Planning

#### Compute Resources (Minimum Recommendations)

- **Small Deployment (< 1000 users)**
  - CPU: 2 cores
  - RAM: 2GB
  - Workers: 2

- **Medium Deployment (1000-10000 users)**
  - CPU: 4 cores
  - RAM: 4GB
  - Workers: 4

- **Large Deployment (> 10000 users)**
  - CPU: 8+ cores
  - RAM: 8GB+
  - Workers: 8+

#### Storage

- **Database**: Plan for ~1MB per active user per month
- **Photos**: If storing locally, plan for ~5MB per user per month
  - Consider object storage (S3, GCS) for production

### Monitoring Setup

- [ ] **Health Check Endpoint**: `/api/health`
  - Configure platform health checks (Railway, Render, K8s)
  - Set appropriate intervals (30s recommended)

- [ ] **Metrics Endpoint**: `/api/metrics` (admin only)
  - Monitor request rates, error rates, latencies
  - Set up alerting for high error rates

- [ ] **Log Aggregation**
  - Configure log forwarding (CloudWatch, DataDog, etc.)
  - Set up log retention policy
  - JSON logs enabled for easy parsing

- [ ] **Alerting Rules** (examples):
  - Error rate > 5% for 5 minutes
  - Response time p95 > 2 seconds
  - Database connection pool exhausted
  - Disk usage > 80%

### Performance Tuning

- [ ] **Database Connection Pool**
  - Start with `max_size = 2 * num_workers`
  - Monitor and adjust based on usage

- [ ] **Request Timeouts**
  - Default 30s is reasonable for most operations
  - Increase for long-running operations if needed

- [ ] **Rate Limiting**
  - Adjust limits in `middleware.py` based on usage patterns
  - Monitor 429 (rate limit) responses

- [ ] **Worker Count**
  - Formula: `(2 * num_cores) + 1`
  - Start conservative, scale up based on load

## Deployment Steps

### 1. Build Docker Image

```bash
cd backend
docker build -t glowupai:latest .

# Optional: Scan for vulnerabilities
docker scan glowupai:latest
```

### 2. Test Locally with Production Config

```bash
# Create .env.production file with production settings
docker run --env-file .env.production -p 8000:8000 glowupai:latest

# Test health check
curl http://localhost:8000/api/health

# Test with sample request
curl -X POST http://localhost:8000/api/users \
  -H "Content-Type: application/json" \
  -d '{"skin_type": "combination"}'
```

### 3. Deploy to Platform

#### Railway
```bash
railway up
railway variables set GLOWUPAI_ENV=production
# Set other environment variables via Railway dashboard
```

#### Render
```bash
# Configure via render.yaml or dashboard
# Set environment variables in Render dashboard
```

#### Kubernetes
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

### 4. Post-Deployment Verification

- [ ] Health check returns 200 OK
- [ ] Database connectivity confirmed
- [ ] Logs showing up in aggregation system
- [ ] Test user creation
- [ ] Test authentication flow
- [ ] Test image upload
- [ ] Verify rate limiting works
- [ ] Check metrics endpoint

## Post-Deployment Monitoring

### First 24 Hours

- [ ] Monitor error rates closely
- [ ] Watch for memory leaks
- [ ] Check database connection pool usage
- [ ] Verify log levels appropriate
- [ ] Monitor API response times

### First Week

- [ ] Review error logs for patterns
- [ ] Adjust rate limits if needed
- [ ] Tune database pool settings
- [ ] Verify backup restoration works
- [ ] Review security logs

### Ongoing

- [ ] Weekly: Review metrics and logs
- [ ] Monthly: Security updates and patches
- [ ] Monthly: Database performance review
- [ ] Quarterly: Load testing
- [ ] Quarterly: Disaster recovery drill

## Rollback Plan

If issues occur after deployment:

1. **Immediate Rollback**
   ```bash
   # Railway
   railway rollback
   
   # Kubernetes
   kubectl rollout undo deployment/glowupai
   ```

2. **Database Rollback** (if schema changed)
   - Have migration rollback scripts ready
   - Test rollback in staging first

3. **Communication**
   - Notify users via status page
   - Document incident in post-mortem

## Scaling Checklist

When you need to scale:

- [ ] **Horizontal Scaling**
  - Increase number of replicas/instances
  - Ensure database can handle increased connections
  - Verify session/state is stateless

- [ ] **Database Scaling**
  - Consider read replicas for read-heavy workloads
  - Scale up database instance size
  - Optimize slow queries

- [ ] **Caching** (future enhancement)
  - Add Redis for session caching
  - Cache frequently accessed data

## Security Scanning

Run these before each production deployment:

```bash
# Scan Docker image
docker scan glowupai:latest

# Scan Python dependencies
pip install safety
safety check --file pyproject.toml

# Check for secrets in code
git secrets --scan

# SAST scanning (example with Bandit)
pip install bandit
bandit -r glowupai/
```

## Compliance & Privacy

- [ ] GDPR compliance verified
- [ ] Privacy policy updated
- [ ] Data retention policies implemented
- [ ] User deletion working correctly (`DELETE /api/users/{user_id}`)
- [ ] Data export working correctly (`GET /api/users/{user_id}/export`)

## Emergency Contacts

Document key contacts:

- Database Admin: _______________
- Platform Support: _______________
- On-Call Engineer: _______________
- Security Team: _______________

## Additional Resources

- See `MONITORING_GUIDE.md` for detailed monitoring setup
- See `ENV_VARS_REFERENCE.md` for complete environment variable list
- See `RAILWAY_DEPLOY_GUIDE.md` for Railway-specific instructions
