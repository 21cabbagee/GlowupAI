# Security Hardening Guide

This guide covers security best practices and hardening measures for the GlowUp AI backend in production.

## Security Layers

The backend implements defense-in-depth with multiple security layers:

1. **Transport Security** (TLS/HTTPS)
2. **Authentication & Authorization** (Firebase Auth + Bearer tokens)
3. **Rate Limiting** (DDoS protection)
4. **Input Validation** (Pydantic models)
5. **Database Security** (Connection encryption, parameterized queries)
6. **Container Security** (Non-root user, minimal image)
7. **Secrets Management** (Environment variables, no hardcoded secrets)

## Quick Security Checklist

Before going to production:

- [ ] HTTPS/TLS enabled (API behind HTTPS load balancer)
- [ ] CORS configured with explicit origins (no `*`)
- [ ] Authentication enabled (`SKINPROOF_AUTH_REQUIRED=1`)
- [ ] Admin token set to strong random value
- [ ] Database uses SSL/TLS connections
- [ ] All secrets in environment variables (not in code)
- [ ] Rate limiting enabled
- [ ] Docker image scanned for vulnerabilities
- [ ] Firewall rules configured (only necessary ports open)
- [ ] Logging and monitoring configured

## 1. Transport Security

### HTTPS/TLS

**Never run the API over plain HTTP in production.**

**Setup**:
- Use platform load balancer (Railway, Render, AWS ALB)
- Or use reverse proxy (nginx, Caddy)
- Enforce HTTPS redirects

**Verify**:
```bash
curl -I https://api.glowup.ai/api/health
# Should see: Strict-Transport-Security header
```

**Certificate Management**:
- Use Let's Encrypt for free TLS certificates
- Set up automatic renewal
- Monitor certificate expiration

## 2. Authentication & Authorization

### Firebase Authentication

**Setup**:
```bash
SKINPROOF_FIREBASE_PROJECT_ID=your-project-id
SKINPROOF_AUTH_REQUIRED=1
```

**How it works**:
1. Client authenticates with Firebase
2. Client gets ID token from Firebase
3. Client sends token in `Authorization: Bearer <token>` header
4. Backend verifies token using Firebase JWKS
5. Backend checks user owns requested resources

**Token Verification**:
- Tokens verified using public keys from Firebase
- No service account credentials needed on backend
- Tokens expire automatically (Firebase manages this)

### User Authorization

Every user-scoped endpoint checks:
1. Token is valid (not expired, signature valid)
2. User ID in token matches requested resource owner

**Example**:
```python
# GET /api/users/user_123/profile
# Token must belong to user_123, or request is rejected with 403
```

### Admin Endpoints

Protected by separate admin token:

**Generate strong admin token**:
```bash
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

**Configure**:
```bash
SKINPROOF_ADMIN_TOKEN=<generated-token>
```

**Protected endpoints**:
- `/api/metrics` - Application metrics
- `/api/admin/*` - Admin operations

**Usage**:
```bash
curl -H "Authorization: Bearer <admin-token>" \
  https://api.glowup.ai/api/metrics
```

**Security**:
- Never commit admin token to version control
- Rotate admin token regularly (quarterly)
- Use different token per environment (dev/staging/prod)
- Monitor admin endpoint access logs

## 3. CORS Configuration

### Why CORS Matters

CORS prevents malicious websites from making requests to your API on behalf of users.

**Bad** (allows any website to call your API):
```bash
SKINPROOF_ALLOWED_ORIGINS=*
```

**Good** (only your frontend can call API):
```bash
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.ai,https://www.glowup.ai
```

### Configuration

**Development** (permissive):
```bash
# Defaults to localhost addresses when SKINPROOF_ENV=development
```

**Production** (strict):
```bash
SKINPROOF_ENV=production
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.ai
```

**Multiple domains**:
```bash
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.ai,https://www.glowup.ai,https://staging.glowup.ai
```

### Security Settings

Current CORS configuration:
- `allow_credentials=False` - Uses Bearer tokens (not cookies)
- `allow_methods` - Explicit list (GET, POST, PUT, DELETE, PATCH)
- `allow_headers` - Explicit list (Authorization, Content-Type)
- No wildcard headers

This prevents:
- CSRF attacks (no cookies)
- Unauthorized cross-origin requests
- Header injection attacks

## 4. Rate Limiting

### Purpose

Rate limiting prevents:
- DDoS attacks
- Brute force authentication attempts
- API abuse
- Resource exhaustion

### Configuration

Enable rate limiting:
```bash
SKINPROOF_RATE_LIMIT_ENABLED=1
```

### Current Limits

- **Auth endpoints**: 10 requests/minute, burst 20
- **Upload endpoints**: 5 requests/minute, burst 10
- **Standard API**: 60 requests/minute, burst 100
- **Admin endpoints**: 100 requests/minute, burst 200

### Customizing Limits

Edit `skinproof/middleware.py`:
```python
self.limits = {
    "auth": (10, 20),  # (requests_per_minute, burst_size)
    "upload": (5, 10),
    "api": (60, 100),
    "admin": (100, 200),
}
```

### Monitoring

Watch for:
- Spike in 429 (rate limit) responses
- Same IP hitting limits repeatedly (potential attack)
- Legitimate users affected (may need to increase limits)

**Check rate limit logs**:
```bash
cat logs.json | jq 'select(.status_code==429)'
```

## 5. Input Validation

### Pydantic Models

All API inputs validated using Pydantic:

**Example**:
```python
class CaptureCreate(BaseModel):
    user_id: str
    image_base64: str
    quality: dict | None = None
    # ... validated types
```

**Protects against**:
- Type confusion attacks
- SQL injection (combined with parameterized queries)
- XSS (no user input rendered in HTML)
- Buffer overflows (Python prevents this)

### Size Limits

**Request body size**: Limited by platform (usually 10-100MB)
**String lengths**: Enforced by Pydantic `Field` validators

**Examples**:
```python
name: str = Field(min_length=1, max_length=160)
note: str | None = Field(default=None, max_length=400)
```

### Image Validation

Images validated on upload:
1. Base64 decoding (rejects invalid format)
2. Pillow opens image (rejects non-image data)
3. Size limits enforced

## 6. Database Security

### Connection Security

**Use SSL/TLS for database connections**:
```bash
DATABASE_URL=postgresql://user:pass@host:5432/db?sslmode=require
```

**SSL modes**:
- `disable` - No SSL (never use in production)
- `require` - SSL required
- `verify-ca` - Verify server certificate
- `verify-full` - Verify server certificate and hostname

**Recommended**: `sslmode=require` minimum, `verify-full` for high security

### SQL Injection Prevention

**All queries use parameterized queries**:

**Good** (parameterized):
```python
db.fetchone("SELECT * FROM users WHERE id = ?", (user_id,))
```

**Bad** (SQL injection vulnerable):
```python
db.fetchone(f"SELECT * FROM users WHERE id = '{user_id}'")  # NEVER DO THIS
```

The codebase uses parameterized queries exclusively.

### Least Privilege

**Create dedicated database user**:
```sql
CREATE USER skinproof_app WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE skinproof_prod TO skinproof_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO skinproof_app;
```

**Do NOT use**:
- Database superuser
- User with DROP or TRUNCATE privileges
- User with access to other databases

### Secrets Management

**Never commit database credentials**:
```bash
# Good - use environment variables
DATABASE_URL=postgresql://...

# Bad - hardcoded in code
DATABASE_URL = "postgresql://user:password@host/db"  # NEVER DO THIS
```

**Options for secret storage**:
- Platform environment variables (Railway, Render)
- AWS Secrets Manager
- HashiCorp Vault
- Kubernetes Secrets

## 7. Container Security

### Non-Root User

Docker container runs as non-root user (UID 10001):

```dockerfile
RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin skinproof
USER skinproof
```

**Benefits**:
- Limited privileges if container compromised
- Cannot install packages or modify system
- Follows least privilege principle

### Minimal Base Image

Uses `python:3.12-slim`:
- Small attack surface
- Fewer vulnerabilities
- Faster builds

### Security Scanning

**Scan Docker image before deployment**:

```bash
# Using Docker built-in scanner
docker scan skinproof:latest

# Using Trivy
trivy image skinproof:latest

# Using Snyk
snyk container test skinproof:latest
```

**Set up automated scanning**:
- GitHub Actions: Scan on every PR
- CI/CD pipeline: Scan before deploy
- Registry scanning: Enable on Docker Hub/ECR

### Image Signing

**Sign images for supply chain security**:
```bash
# Using Docker Content Trust
export DOCKER_CONTENT_TRUST=1
docker push skinproof:latest

# Using Cosign
cosign sign skinproof:latest
```

## 8. Secrets Management

### Environment Variables

**All secrets must be in environment variables**:

✅ **Correct**:
```bash
export GEMINI_API_KEY=<secret>
export DATABASE_URL=postgresql://...
export SKINPROOF_ADMIN_TOKEN=<secret>
```

❌ **Wrong**:
```python
GEMINI_API_KEY = "hardcoded-key"  # NEVER DO THIS
```

### .env Files

**Rules**:
- `.env` files are git-ignored
- Only `.env.example` is committed (no secrets)
- Never commit `.env`, `.env.production`, etc.

**Verify**:
```bash
git ls-files | grep "\.env"
# Should only show .env.example
```

### Rotating Secrets

**When to rotate**:
- Quarterly (regular rotation)
- After team member leaves
- After suspected compromise
- After security incident

**What to rotate**:
1. Admin token
2. Database password
3. API keys (Gemini)
4. Firebase project keys (if compromised)

**How to rotate without downtime**:
1. Add new secret alongside old
2. Deploy code that accepts both
3. Update clients to use new secret
4. Remove old secret after migration complete

## 9. API Security Best Practices

### Request Timeouts

Prevent resource exhaustion:
```bash
SKINPROOF_REQUEST_TIMEOUT=30
```

**Benefits**:
- Prevents slow loris attacks
- Frees up resources from hung requests
- Forces clients to implement retries

### Error Messages

**Never expose**:
- Stack traces to clients
- Internal system details
- Database schema
- File paths

**Current behavior**:
- Generic error messages to clients
- Detailed errors in logs
- Request ID for correlation

**Example**:
```json
// Client sees:
{"detail": "An internal server error occurred", "error_code": "INTERNAL_SERVER_ERROR"}

// Logs contain:
{"level": "ERROR", "message": "DatabaseError: connection lost", "request_id": "abc123", ...}
```

### Security Headers

**Add these via reverse proxy or platform**:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
```

**If using nginx**:
```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
```

## 10. Monitoring & Incident Response

### Security Monitoring

**Monitor for**:
- Failed authentication attempts (401 responses)
- Rate limit hits (429 responses)
- Admin endpoint access
- Unusual traffic patterns
- Error spikes

**Alerts to set up**:
- > 100 failed auth attempts from single IP in 5 minutes
- > 1000 rate limit hits in 5 minutes
- Any admin endpoint access (log review)
- Unusual geographic access patterns

### Logging

**Security-relevant logs**:
```bash
# Failed auth
cat logs.json | jq 'select(.status_code==401)'

# Rate limited
cat logs.json | jq 'select(.status_code==429)'

# Admin access
cat logs.json | jq 'select(.endpoint | startswith("/api/admin"))'

# Errors
cat logs.json | jq 'select(.level=="ERROR")'
```

### Incident Response

**If security incident detected**:

1. **Assess Impact**
   - What data was accessed?
   - How many users affected?
   - Is attacker still active?

2. **Contain**
   - Block attacker IP
   - Rotate compromised credentials
   - Disable compromised accounts

3. **Investigate**
   - Review logs with request IDs
   - Identify attack vector
   - Check for data exfiltration

4. **Remediate**
   - Patch vulnerability
   - Deploy fix
   - Verify fix works

5. **Post-Mortem**
   - Document what happened
   - How it happened
   - How to prevent in future
   - Update security procedures

## 11. Compliance

### GDPR Compliance

**User Rights**:
- ✅ Right to access: `GET /api/users/{user_id}/export`
- ✅ Right to deletion: `DELETE /api/users/{user_id}`
- ✅ Data retention: Configurable via `SKINPROOF_RAW_RETENTION_DAYS`
- ✅ Consent tracking: `consent_events` table

**Data Protection**:
- ✅ Encryption in transit (HTTPS/TLS)
- ✅ Encryption at rest (database-level)
- ✅ Access controls (authentication)
- ✅ Audit logs (admin audit endpoint)

### Data Retention

**Configure retention**:
```bash
SKINPROOF_RAW_RETENTION_DAYS=730  # 2 years
```

**Implement cleanup**:
- Schedule job to delete old photos
- Archive old data to cold storage
- Document retention policy

## 12. Third-Party Security

### Gemini API

**Protect API key**:
```bash
SKINPROOF_GEMINI_API_KEY=<secret>
```

**Best practices**:
- Use API key restrictions (Google Cloud Console)
- Limit to specific IPs or domains
- Set usage quotas
- Monitor usage for anomalies

### Firebase

**Security**:
- Use Firebase Auth ID tokens (not service accounts)
- Tokens verified using public keys
- No sensitive credentials on backend
- Token expiration managed by Firebase

## Security Testing

### Manual Testing

**Test authentication**:
```bash
# Should fail without token
curl https://api.glowup.ai/api/users/user_123/profile

# Should work with valid token
curl -H "Authorization: Bearer <valid-token>" \
  https://api.glowup.ai/api/users/user_123/profile
```

**Test rate limiting**:
```bash
# Send many requests
for i in {1..100}; do
  curl https://api.glowup.ai/api/health
done
# Should see 429 after limits exceeded
```

**Test input validation**:
```bash
# Should fail validation
curl -X POST https://api.glowup.ai/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": ""}'  # Empty name
```

### Automated Testing

**Tools**:
- OWASP ZAP: Web application scanner
- Burp Suite: Security testing platform
- SQLMap: SQL injection testing
- Nmap: Port scanning

**Example with OWASP ZAP**:
```bash
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t https://api.glowup.ai
```

### Penetration Testing

**Schedule**:
- Before initial launch
- After major changes
- Annually

**What to test**:
- Authentication bypass
- Authorization issues
- SQL injection
- XSS attacks
- API abuse
- Rate limit bypass
- Session hijacking

## Resources

### Security Tools

- **Secrets scanning**: git-secrets, truffleHog
- **Dependency scanning**: safety, snyk
- **Container scanning**: trivy, anchore
- **SAST**: bandit, semgrep
- **Penetration testing**: OWASP ZAP, Burp Suite

### Security Standards

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- OWASP API Security: https://owasp.org/www-project-api-security/
- CIS Benchmarks: https://www.cisecurity.org/cis-benchmarks/

### Compliance

- GDPR: https://gdpr.eu/
- HIPAA: https://www.hhs.gov/hipaa/ (if handling health data)
- SOC 2: https://www.aicpa.org/soc4so

## Summary

Security checklist for production:

✅ HTTPS/TLS enforced  
✅ Authentication enabled  
✅ CORS configured (no wildcards)  
✅ Rate limiting enabled  
✅ Input validation via Pydantic  
✅ Parameterized SQL queries  
✅ Non-root container user  
✅ Secrets in environment variables  
✅ Request timeouts configured  
✅ Security logging enabled  
✅ Admin token rotated quarterly  
✅ Database uses SSL  
✅ Docker image scanned  
✅ Monitoring and alerts configured  
✅ Incident response plan documented  

The backend implements defense-in-depth security with multiple layers of protection.
