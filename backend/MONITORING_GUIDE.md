# Production Monitoring Guide

This guide covers monitoring, observability, and alerting for the GlowUp AI backend in production.

## Overview

The backend includes built-in monitoring capabilities:
- Structured JSON logging with request IDs
- Metrics collection and exposure
- Health check endpoint with dependency checks
- OpenTelemetry support for distributed tracing

## Quick Start

### 1. Health Check

The `/api/health` endpoint provides comprehensive health status:

```bash
curl https://api.glowup.ai/api/health
```

**Response (Healthy)**:
```json
{
  "status": "healthy",
  "checks": {
    "database": {
      "status": "healthy",
      "backend": "postgres"
    },
    "disk": {
      "status": "healthy",
      "free_gb": 45.2
    },
    "gemini_api": {
      "status": "configured"
    }
  },
  "version": "3.0.0",
  "scope": "cosmetic_tracking"
}
```

**Response (Unhealthy)** (503 status code):
```json
{
  "status": "unhealthy",
  "checks": {
    "database": {
      "status": "unhealthy",
      "error": "connection timeout"
    }
  }
}
```

### 2. Metrics Endpoint

The `/api/metrics` endpoint exposes application metrics (admin only):

```bash
curl -H "Authorization: Bearer <admin-token>" \
  https://api.glowup.ai/api/metrics
```

**Response**:
```json
{
  "total_requests": 15234,
  "total_errors": 45,
  "error_rate": 0.29,
  "avg_duration_ms": 145.67,
  "top_endpoints": [
    ["POST /api/captures", 3421],
    ["GET /api/users/{user_id}/dashboard", 2134],
    ["POST /api/routine-events", 1876]
  ],
  "status_codes": {
    "200": 14823,
    "400": 234,
    "401": 89,
    "403": 45,
    "500": 43
  }
}
```

## Structured Logging

### Log Format

All logs are output as JSON for easy parsing and aggregation:

```json
{
  "timestamp": "2026-08-31T10:15:30Z",
  "level": "INFO",
  "logger": "skinproof.access",
  "message": "POST /api/captures",
  "request_id": "abc123-def456-789",
  "endpoint": "POST /api/captures",
  "client_ip": "192.168.1.1",
  "user_id": "user_123",
  "status_code": 200,
  "duration_ms": 234.5
}
```

### Log Levels

Configure via `SKINPROOF_LOG_LEVEL` environment variable:
- `DEBUG`: Detailed diagnostic information
- `INFO`: General informational messages (default)
- `WARNING`: Warning messages for potentially harmful situations
- `ERROR`: Error messages for serious problems
- `CRITICAL`: Critical errors that may prevent the app from continuing

### Request Tracing

Every request gets a unique `request_id` that appears in all logs for that request:
- Automatically generated if not provided
- Can be passed via `X-Request-ID` header
- Returned in response headers for correlation

### Key Log Events

Watch for these important log events:

**Application Startup**:
```json
{
  "level": "INFO",
  "message": "Initializing SkinProof application",
  "version": "3.0.0"
}
```

**Request Errors**:
```json
{
  "level": "ERROR",
  "message": "Unhandled exception in POST /api/captures",
  "request_id": "abc123",
  "exception_type": "DatabaseError",
  "exception": "connection lost..."
}
```

**Rate Limit Exceeded**:
```json
{
  "level": "WARNING",
  "message": "Rate limit exceeded for 192.168.1.1 on POST /api/captures",
  "client_id": "192.168.1.1",
  "endpoint": "POST /api/captures"
}
```

## Key Metrics to Monitor

### 1. Request Rate

**Metric**: `total_requests`  
**Description**: Total number of HTTP requests processed  
**Alert**: Sudden drop indicates downtime or client issues

**Query Example (if using Prometheus)**:
```promql
rate(http_requests_total[5m])
```

### 2. Error Rate

**Metric**: `error_rate` (percentage)  
**Description**: Percentage of requests resulting in 4xx/5xx errors  
**Alert**: > 5% for 5 minutes

**Recommended Alert**:
```yaml
alert: HighErrorRate
expr: error_rate > 5
for: 5m
severity: warning
```

### 3. Response Time

**Metric**: `avg_duration_ms`  
**Description**: Average request duration in milliseconds  
**Alert**: p95 > 2000ms or p99 > 5000ms

**Good Values**:
- p50: < 200ms
- p95: < 1000ms
- p99: < 2000ms

### 4. Database Health

**Metric**: Via `/api/health` endpoint  
**Description**: Database connection and query responsiveness  
**Alert**: Status "unhealthy" or repeated connection failures

### 5. Status Code Distribution

**Metrics**: `status_codes`  
**Key codes to watch**:
- `200`: Successful requests (should be majority)
- `400`: Bad requests (client errors)
- `401`: Unauthorized (auth failures)
- `429`: Rate limited (may need to adjust limits)
- `500`: Internal server errors (investigate immediately)
- `503`: Service unavailable (dependency failures)

### 6. Endpoint Performance

**Metric**: `top_endpoints`  
**Description**: Most frequently called endpoints  
**Use**: Identify hot paths for optimization

### 7. Database Connection Pool

**Not directly exposed yet** - monitor via logs and database metrics:
- Active connections
- Waiting connections
- Connection errors

Look for log messages like:
```
WARNING: Database connection pool exhausted
```

## Setting Up External Monitoring

### Option 1: Platform Built-in Monitoring

Most platforms have built-in monitoring:

**Railway**:
- Metrics available in dashboard
- Set up health check: `/api/health`
- Configure alerts in Railway UI

**Render**:
- Health check path: `/api/health`
- View metrics in Render dashboard
- Set up Slack/email notifications

**Kubernetes**:
```yaml
livenessProbe:
  httpGet:
    path: /api/health
    port: 8000
  initialDelaySeconds: 15
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /api/health
    port: 8000
  initialDelaySeconds: 5
  periodSeconds: 10
```

### Option 2: OpenTelemetry + Backend

For comprehensive observability, use OpenTelemetry:

**1. Enable OpenTelemetry**:
```bash
# Install dependencies
pip install opentelemetry-api opentelemetry-sdk \
    opentelemetry-instrumentation-fastapi \
    opentelemetry-exporter-otlp

# Set environment variables
OTEL_ENABLED=1
OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317
OTEL_SERVICE_NAME=skinproof
```

**2. Supported Backends**:
- **Jaeger**: Distributed tracing
- **Prometheus**: Metrics collection
- **Grafana**: Visualization
- **DataDog**: Full observability platform
- **New Relic**: Application performance monitoring
- **Honeycomb**: Observability for production systems

**3. Example: Jaeger Setup**:
```bash
# Run Jaeger locally
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 4317:4317 \
  jaegertracing/all-in-one:latest

# Configure backend
OTEL_ENABLED=1
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```

### Option 3: Log Aggregation

Forward logs to a centralized system:

**CloudWatch (AWS)**:
```bash
# Install CloudWatch agent
# Configure to tail stdout (where app logs go)
# Logs appear in CloudWatch Logs console
```

**DataDog**:
```bash
# Add DataDog agent as sidecar
# It automatically collects stdout logs
# Configure log parsing for JSON format
```

**Grafana Loki**:
```yaml
# docker-compose.yml addition
loki:
  image: grafana/loki:latest
  ports:
    - "3100:3100"

promtail:
  image: grafana/promtail:latest
  volumes:
    - /var/log:/var/log
```

## Recommended Alerts

### Critical Alerts (Page immediately)

1. **Service Down**
   - Health check returning 503
   - No requests in last 5 minutes
   - Action: Check application logs, database connectivity

2. **High Error Rate**
   - Error rate > 10% for 5 minutes
   - Action: Check recent deploys, review error logs

3. **Database Connection Failures**
   - Database check failing in `/api/health`
   - Action: Check database status, connection pool settings

### Warning Alerts (Investigate within 1 hour)

1. **Elevated Error Rate**
   - Error rate > 5% for 10 minutes
   - Action: Review error logs for patterns

2. **Slow Response Times**
   - p95 response time > 2 seconds
   - Action: Check database query performance, identify slow endpoints

3. **Rate Limiting Spike**
   - Unusual number of 429 responses
   - Action: Check for potential attack or legitimate traffic spike

4. **Disk Space Low**
   - Free disk < 10%
   - Action: Clean up old data, increase storage

### Info Alerts (Monitor trends)

1. **Traffic Patterns**
   - Unusual traffic spikes
   - Action: Ensure scaling is adequate

2. **Authentication Failures**
   - Spike in 401 responses
   - Action: Check for potential brute force attempts

## Dashboard Template

Create a dashboard with these panels:

### Top Row: Health
- Service status (green/red indicator)
- Error rate (percentage)
- Request rate (req/sec)
- Response time p95 (milliseconds)

### Middle Row: Traffic
- Requests over time (line graph)
- Status code distribution (pie chart)
- Top endpoints (bar chart)
- Geographic distribution (if available)

### Bottom Row: Resources
- Database connections (gauge)
- Memory usage (line graph)
- CPU usage (line graph)
- Disk usage (gauge)

## Troubleshooting Common Issues

### High Error Rate

**Symptoms**: Error rate > 5%, lots of 500 responses

**Investigation**:
1. Check error logs:
   ```bash
   # Filter for ERROR level
   cat logs.json | jq 'select(.level=="ERROR")'
   ```

2. Identify common exception types:
   ```bash
   cat logs.json | jq 'select(.level=="ERROR") | .exception_type' | sort | uniq -c
   ```

3. Common causes:
   - Database connection issues
   - External API failures (Gemini)
   - Invalid client data
   - Recent code deploy

### Slow Response Times

**Symptoms**: p95 > 2s, timeout errors

**Investigation**:
1. Find slow endpoints:
   ```bash
   cat logs.json | jq 'select(.duration_ms > 2000) | .endpoint' | sort | uniq -c
   ```

2. Common causes:
   - Slow database queries
   - Large image processing
   - Gemini API latency
   - Connection pool exhaustion

3. Solutions:
   - Add database indexes
   - Optimize image processing
   - Increase timeout for long operations
   - Scale database connection pool

### Memory Leaks

**Symptoms**: Memory usage gradually increasing, eventual OOM

**Investigation**:
1. Monitor memory over time
2. Check for:
   - Database connections not being closed
   - Large objects held in memory
   - File handles not being released

3. Solution:
   - Restart as immediate fix
   - Add memory profiling
   - Review recent code changes

### Rate Limit Issues

**Symptoms**: Many 429 responses, legitimate users affected

**Investigation**:
1. Check rate limit logs
2. Identify if attack or legitimate traffic

**Solution**:
- If attack: Keep rate limits, consider IP blocking
- If legitimate: Increase rate limits in `middleware.py`

## Performance Baselines

Record these baselines for your deployment:

- **Typical request rate**: _____ req/sec
- **Typical error rate**: _____ %
- **Typical p95 latency**: _____ ms
- **Peak traffic hours**: _____
- **Average DB connections**: _____
- **Memory usage**: _____ MB

Deviations from baseline warrant investigation.

## Security Monitoring

### Failed Authentication Attempts

Monitor 401 responses:
```bash
cat logs.json | jq 'select(.status_code==401) | .client_ip' | sort | uniq -c
```

Alert if:
- Same IP has > 100 failures in 5 minutes (brute force)
- Unusual spike in auth failures across IPs

### Rate Limiting

Monitor 429 responses:
```bash
cat logs.json | jq 'select(.status_code==429)'
```

Normal: Occasional 429s from individual clients  
Suspicious: Coordinated 429s from many IPs

### Admin Endpoint Access

Monitor `/api/admin/*` and `/api/metrics` access:
```bash
cat logs.json | jq 'select(.endpoint | startswith("/api/admin"))'
```

Alert on:
- Failed admin auth attempts
- Unexpected admin access times
- Admin access from unusual IPs

## Maintenance Windows

For planned maintenance:

1. **Pre-maintenance**:
   - Notify users via status page
   - Set health check to return 503 (if needed)
   - Backup database

2. **During maintenance**:
   - Monitor error logs
   - Keep metrics running
   - Document any issues

3. **Post-maintenance**:
   - Verify health check green
   - Monitor error rate for 30 minutes
   - Confirm metrics normal
   - Send all-clear notification

## Emergency Response Runbook

### Service Down (Health Check Failing)

1. **Check application status**:
   ```bash
   # Railway
   railway logs --tail 100
   
   # Kubernetes
   kubectl logs -l app=skinproof --tail=100
   ```

2. **Check database**:
   - Is database accessible?
   - Are credentials correct?
   - Is connection pool exhausted?

3. **Quick fixes**:
   ```bash
   # Restart application
   railway restart  # or kubectl rollout restart
   ```

4. **If still down**: Rollback to previous version

### Database Issues

1. **Check database status**:
   ```bash
   psql $DATABASE_URL -c "SELECT 1;"
   ```

2. **Check connections**:
   ```sql
   SELECT count(*) FROM pg_stat_activity 
   WHERE datname = 'skinproof_prod';
   ```

3. **If connection pool exhausted**:
   - Increase `SKINPROOF_DB_POOL_MAX_SIZE`
   - Restart application

### High Memory Usage

1. **Check current usage**:
   ```bash
   # In container
   free -h
   ```

2. **Restart to free memory** (immediate):
   ```bash
   railway restart
   ```

3. **Long-term**: Scale up instance size

## Resources

- **Metrics Endpoint**: `/api/metrics` (admin only)
- **Health Endpoint**: `/api/health`
- **Logs**: Structured JSON to stdout
- **OpenTelemetry**: See `observability.py` for instrumentation

## Support Contacts

For monitoring-related issues:

- Application logs: Check platform dashboard
- Database issues: DBA contact: _____
- Platform issues: Platform support: _____
- On-call engineer: _____
