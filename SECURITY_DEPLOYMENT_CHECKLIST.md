# Security Deployment Checklist

This checklist must be completed before deploying to production. Items marked with 🔴 are critical and will cause deployment failures if not configured.

## Pre-Deployment Configuration

### 🔴 Critical (Required for Launch)

- [ ] **Backend HTTPS Endpoint Deployed**
  - Deploy backend to production environment (Railway, Cloud Run, etc.)
  - Verify HTTPS certificate is valid
  - Test health endpoint: `curl https://api.glowup.ai/api/health`

- [ ] **Production API URLs Configured**
  - Set in Android build: `./gradlew assembleRelease -PRELEASE_API_BASE_URL=https://api.glowup.ai/api/`
  - Verify BuildConfig.API_BASE_URL in release APK
  - Test network connectivity from release build

- [ ] **Authentication Enabled in Production**
  ```bash
  # Set in production environment:
  SKINPROOF_AUTH_REQUIRED=1
  SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
  ```

- [ ] **CORS Origins Explicitly Configured**
  ```bash
  # Production environment must set:
  SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.ai,https://www.glowup.ai
  # DO NOT use localhost origins in production
  ```

- [ ] **Photo Encryption Key Generated**
  ```bash
  # Generate 32-byte key and encode as base64:
  python3 -c "import os, base64; print(base64.b64encode(os.urandom(32)).decode())"
  
  # Set in production:
  SKINPROOF_PHOTO_KEY=<generated_key_here>
  SKINPROOF_PHOTO_DIR=/var/data/photos
  ```
  - Store key in secrets manager (Railway Secrets, GCP Secret Manager, AWS Secrets Manager)
  - Never commit key to git
  - Document key backup procedure

- [ ] **Environment Variable Set Correctly**
  ```bash
  SKINPROOF_ENV=production
  ```

### 🟡 High Priority (Should Complete Before Launch)

- [ ] **Rate Limiting Enabled**
  ```python
  # In complete_api.py (already added to codebase):
  from .rate_limit import create_rate_limiter, RateLimitMiddleware, get_remote_address
  
  limiter, rate_limits = create_rate_limiter(enabled=True)
  app.add_middleware(RateLimitMiddleware, limiter=limiter, get_client_id=get_remote_address, rate_limits=rate_limits)
  ```
  - Test rate limiting with load testing tool
  - Monitor 429 responses in production logs

- [ ] **Admin Token Configured Securely**
  ```bash
  # Generate strong token:
  python3 -c "import secrets; print(secrets.token_urlsafe(32))"
  
  # Set in production:
  SKINPROOF_ADMIN_TOKEN=<generated_token_here>
  ```
  - Store in secrets manager
  - Restrict admin endpoints by IP if possible
  - Set up rotation schedule (monthly)

- [ ] **HTML Sanitization Applied**
  ```python
  # In complete_service.py:
  from .html_sanitize import sanitize_derm_export
  
  # When generating dermatologist export:
  printable_html = sanitize_derm_export(generated_html)
  ```

- [ ] **Structured Logging Configured**
  ```bash
  # Production logging settings:
  SKINPROOF_LOG_LEVEL=INFO
  SKINPROOF_JSON_LOGS=1
  ```
  - Configure log aggregation (CloudWatch, Datadog, etc.)
  - Set up alerts for auth failures, errors

- [ ] **Database Backups Enabled**
  - Configure automated backups (daily minimum)
  - Test restore procedure
  - Document backup retention policy

### 🟠 Medium Priority (Complete Within First Week)

- [ ] **Certificate Pinning Configured**
  - Generate certificate pins for production domain
  - Update `/app/src/main/res/xml/network_security_config.xml`
  - Test with release build

- [ ] **Firebase Security Rules Configured**
  - Add `firestore.rules` to repository (if using Firestore)
  - Add `storage.rules` to repository (if using Cloud Storage)
  - Test rules with Firebase Emulator
  - Deploy rules: `firebase deploy --only firestore:rules,storage`

- [ ] **Monitoring and Alerts Set Up**
  - Configure uptime monitoring (UptimeRobot, Pingdom)
  - Set up error tracking (Sentry, Crashlytics already enabled)
  - Configure alerts for:
    - High error rates
    - Authentication failures
    - Rate limit violations
    - Database connection failures

- [ ] **Security Headers Configured**
  ```python
  # Add security headers middleware:
  from starlette.middleware.trustedhost import TrustedHostMiddleware
  
  app.add_middleware(TrustedHostMiddleware, allowed_hosts=["api.glowup.ai"])
  
  # Or use custom middleware for comprehensive headers
  ```

- [ ] **Dependencies Updated and Scanned**
  ```bash
  # Update dependencies:
  pip list --outdated
  pip install --upgrade <packages>
  
  # Scan for vulnerabilities:
  pip install safety
  safety check --json
  ```

### 🔵 Low Priority (Ongoing)

- [ ] **Security Testing Completed**
  - Authentication bypass attempts
  - Authorization bypass attempts
  - XSS payload testing
  - SQL injection testing (should all pass)
  - Rate limiting verification

- [ ] **Documentation Updated**
  - [ ] API documentation includes security notes
  - [ ] Deployment guide includes security steps
  - [ ] Incident response plan documented
  - [ ] Security contact email configured

- [ ] **Compliance Checklist**
  - [ ] GDPR: Data export endpoint implemented
  - [ ] GDPR: Data deletion tested
  - [ ] Privacy policy accessible in app
  - [ ] Terms of service accessible in app
  - [ ] Medical disclaimers visible

## Post-Deployment Verification

### Immediately After Deployment

- [ ] **Health Check Passes**
  ```bash
  curl https://api.glowup.ai/api/health
  # Expected: {"status": "ok", "database_ready": true, ...}
  ```

- [ ] **Authentication Works**
  ```bash
  # Test with invalid token (should fail):
  curl -H "Authorization: Bearer invalid_token" https://api.glowup.ai/api/users/test/profile
  # Expected: 401 Unauthorized
  
  # Test with valid Firebase token (should work):
  # (Get token from Android app after sign-in)
  curl -H "Authorization: Bearer $FIREBASE_TOKEN" https://api.glowup.ai/api/users/$USER_ID/profile
  # Expected: 200 OK
  ```

- [ ] **HTTPS Enforced**
  ```bash
  # HTTP should fail or redirect:
  curl -I http://api.glowup.ai/api/health
  # Expected: 301 redirect or connection refused
  ```

- [ ] **CORS Configured Correctly**
  ```bash
  curl -H "Origin: https://malicious-site.com" -H "Access-Control-Request-Method: POST" \
    -H "Access-Control-Request-Headers: Authorization" \
    -X OPTIONS https://api.glowup.ai/api/users
  # Expected: No Access-Control-Allow-Origin header (blocked)
  
  curl -H "Origin: https://app.glowup.ai" -H "Access-Control-Request-Method: POST" \
    -H "Access-Control-Request-Headers: Authorization" \
    -X OPTIONS https://api.glowup.ai/api/users
  # Expected: Access-Control-Allow-Origin: https://app.glowup.ai
  ```

- [ ] **Rate Limiting Works**
  ```bash
  # Make 11 rapid requests to auth endpoint:
  for i in {1..11}; do
    curl https://api.glowup.ai/api/auth/session
  done
  # Expected: 11th request returns 429 Too Many Requests
  ```

- [ ] **Android App Connects Successfully**
  - Install release APK on test device
  - Sign in with Firebase Auth
  - Verify API calls work
  - Check no certificate errors

### Within First 24 Hours

- [ ] **Monitor Error Rates**
  - Check logs for unexpected errors
  - Verify no authentication issues
  - Check rate limiting is not too restrictive

- [ ] **Verify Data Encryption**
  - Upload test photo
  - Verify encrypted file exists on disk
  - Verify file cannot be opened without decryption

- [ ] **Test Key Workflows**
  - User registration
  - Photo capture and analysis
  - Product tracking
  - Premium upgrade
  - Data export

### Within First Week

- [ ] **Security Scan with External Tool**
  ```bash
  # Use OWASP ZAP or similar:
  docker run -t owasp/zap2docker-stable zap-baseline.py -t https://api.glowup.ai
  ```

- [ ] **Penetration Testing** (Optional but Recommended)
  - Hire security consultant
  - Or use HackerOne/Bugcrowd
  - Document findings and fixes

- [ ] **Review Logs for Anomalies**
  - Failed authentication attempts
  - Unusual traffic patterns
  - Error spikes

## Emergency Procedures

### If API Key Leak Detected

1. **Immediately rotate affected keys:**
   ```bash
   # Generate new keys:
   python3 -c "import secrets; print(secrets.token_urlsafe(32))"
   
   # Update in secrets manager
   # Restart backend services
   ```

2. **Revoke old keys in Firebase Console**

3. **Check audit logs for unauthorized access**

4. **Force all users to re-authenticate (if needed)**

### If Data Breach Suspected

1. **Isolate affected systems immediately**

2. **Collect evidence:**
   - Audit logs
   - Access logs
   - Database queries

3. **Notify users within 72 hours (GDPR requirement)**

4. **Document incident for compliance**

5. **Engage security consultant**

## Security Contact

- **Email:** security@glowup.ai (set up after launch)
- **Response Time:** 48 hours for critical, 1 week for others
- **Responsible Disclosure Policy:** Document on website

## Audit Schedule

- **Security Audit:** Every 6 months
- **Dependency Updates:** Monthly
- **Penetration Testing:** Annually
- **Admin Token Rotation:** Monthly
- **Photo Encryption Key Review:** Quarterly

---

**Last Updated:** August 31, 2026  
**Next Review:** September 7, 2026 (post-launch)
