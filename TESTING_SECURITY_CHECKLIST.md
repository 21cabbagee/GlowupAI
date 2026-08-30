# GlowUp AI - Security Test Checklist

Last updated: 2026-08-30

## Overview
This security checklist ensures GlowUp AI protects user data, complies with privacy regulations, and follows security best practices.

**Critical:** This app handles sensitive personal data (facial photos, skincare health info). Security failures can result in data breaches, regulatory fines, and loss of user trust.

---

## Threat Model

### Assets to Protect
1. **User facial photos** (highest sensitivity)
2. User identity (email, Firebase UID)
3. Skincare metrics and health tracking data
4. Routine and product data
5. Q&A conversations
6. Authentication tokens
7. Backend API keys
8. Photo encryption keys

### Threat Actors
- **External attackers:** Seeking to steal user photos or PII
- **Malicious users:** Attempting to access other users' data
- **Compromised devices:** Malware trying to extract data
- **Network eavesdroppers:** MITM attacks on insecure networks

### Attack Vectors
- API authentication bypass
- Unauthorized data access (broken access control)
- Man-in-the-middle attacks
- SQL injection
- XSS (cross-site scripting)
- CSRF (cross-site request forgery)
- Photo storage access
- Local data extraction from device
- Firebase token theft
- Brute force attacks

---

## Authentication & Authorization Tests

### 1. Firebase Authentication (P0)

#### 1.1 Token Validation
- [ ] **Valid token accepted**
  - User signs in, receives Firebase ID token
  - Token sent in Authorization header
  - Backend validates token against Firebase project
  - Request succeeds

- [ ] **Invalid token rejected**
  - Send request with malformed token
  - Backend returns 401 Unauthorized
  - Error message: "Invalid authentication token"

- [ ] **Expired token rejected**
  - Use token that expired (> 1 hour old)
  - Backend returns 401 Unauthorized
  - App automatically refreshes token

- [ ] **Token from wrong Firebase project**
  - Use token from different Firebase project
  - Backend rejects (project ID mismatch)
  - Returns 401 Unauthorized

**Test:**
```bash
# Valid request
curl -H "Authorization: Bearer {valid_token}" \
  https://api.glowup.ai/api/users/{user_id}/dashboard

# Invalid token
curl -H "Authorization: Bearer invalid_token" \
  https://api.glowup.ai/api/users/{user_id}/dashboard
# Expected: 401 Unauthorized

# Expired token
curl -H "Authorization: Bearer {expired_token}" \
  https://api.glowup.ai/api/users/{user_id}/dashboard
# Expected: 401 Unauthorized
```

---

#### 1.2 Auth Requirement Enforcement
- [ ] **SKINPROOF_AUTH_REQUIRED=1 in production**
  - Verify environment variable set
  - All user-scoped endpoints require auth
  - Unauthenticated requests return 401

- [ ] **Auth bypass not possible**
  - Cannot access API without Authorization header
  - Cannot spoof user_id in path without matching token
  - Admin endpoints require SKINPROOF_ADMIN_TOKEN

**Test:**
```bash
# No auth header
curl https://api.glowup.ai/api/users/test_user/dashboard
# Expected: 401 Unauthorized

# Admin endpoint without admin token
curl -X POST https://api.glowup.ai/api/admin/users
# Expected: 403 Forbidden
```

---

### 2. Authorization & Access Control (P0)

#### 2.1 User Data Isolation
- [ ] **Cannot access other user's data**
  - User A signs in
  - User A requests User B's dashboard
  - Backend checks token sub (user_id) matches path user_id
  - Returns 403 Forbidden

**Test:**
```bash
# User A token, User B data
curl -H "Authorization: Bearer {user_a_token}" \
  https://api.glowup.ai/api/users/user_b_id/dashboard
# Expected: 403 Forbidden
```

- [ ] **Test all user-scoped endpoints:**
  - GET /api/users/{user_id}/dashboard
  - GET /api/users/{user_id}/history
  - GET /api/users/{user_id}/products
  - POST /api/users/{user_id}/captures
  - GET /api/users/{user_id}/experiments
  - GET /api/users/{user_id}/qna
  - DELETE /api/users/{user_id}

**Expected:** All return 403 when user_id in path doesn't match token.

---

#### 2.2 Photo Access Control
- [ ] **Photos not accessible via direct URL**
  - Photos stored with encrypted filenames
  - No public URL guessing possible
  - Photo retrieval requires user authentication

- [ ] **Cannot request other user's photos**
  - User A captures photo, gets capture_id
  - User B tries to fetch User A's photo
  - Backend denies access (photo belongs to User A)

**Test:**
```bash
# Try to access photo without auth
curl https://api.glowup.ai/api/photos/{photo_id}
# Expected: 401 or 404

# Try to access other user's photo
curl -H "Authorization: Bearer {user_b_token}" \
  https://api.glowup.ai/api/users/user_a_id/captures/{capture_id}/photo
# Expected: 403 Forbidden
```

---

#### 2.3 Premium Feature Gating
- [ ] **Free user cannot access premium endpoints**
  - Free user requests premium-only feature
  - Backend checks entitlement.plan and status
  - Returns 403 with upgrade prompt

**Test:**
```bash
# Free user tries shelf scan
curl -H "Authorization: Bearer {free_user_token}" \
  -X POST https://api.glowup.ai/api/users/{free_user_id}/shelf-scan \
  -d '{"image_base64":"..."}'
# Expected: 403 Forbidden, message: "Premium feature"
```

- [ ] **Premium features:**
  - Shelf scan OCR
  - Freeze days
  - Unlimited product verdicts
  - Advanced insights

---

### 3. Session Management (P1)

#### 3.1 Token Refresh
- [ ] **Expired tokens refreshed automatically**
  - Firebase ID token expires after 1 hour
  - App detects 401 on request
  - Calls Firebase SDK refreshToken()
  - Retries request with new token

- [ ] **Refresh token rotation**
  - Refresh tokens rotated on use (Firebase handles)
  - Old refresh tokens invalidated

---

#### 3.2 Sign Out
- [ ] **Sign out clears local session**
  - User signs out
  - Firebase token cleared from memory
  - user_id removed from SharedPreferences/DataStore
  - Redirect to login screen

- [ ] **Signed out user cannot access app**
  - After sign out, no API calls succeed
  - Local data remains but UI inaccessible

---

## Data Protection Tests

### 4. Data in Transit (P0)

#### 4.1 HTTPS Enforcement
- [ ] **All API calls use HTTPS**
  - Android app configured with HTTPS base URL
  - Certificate pinning enabled (optional but recommended)
  - HTTP requests blocked by Network Security Config

**Network Security Config:**
```xml
<!-- app/src/main/res/xml/network_security_config.xml -->
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

- [ ] **Test HTTP fallback blocked:**
```bash
# Try HTTP (should fail)
curl http://api.glowup.ai/api/health
# Expected: Connection refused or redirect to HTTPS
```

- [ ] **TLS version check:**
  - Backend enforces TLS 1.2 or higher
  - Weak ciphers disabled

**Test:**
```bash
nmap --script ssl-enum-ciphers -p 443 api.glowup.ai
# Verify TLS 1.2/1.3 only, strong ciphers
```

---

#### 4.2 Certificate Validation
- [ ] **Invalid certificates rejected**
  - Self-signed cert: connection refused
  - Expired cert: connection refused
  - Wrong domain cert: connection refused

- [ ] **Certificate pinning (if implemented)**
  - App pins specific cert or public key
  - MITM proxy with valid cert but wrong pin rejected

---

### 5. Data at Rest (P0)

#### 5.1 Photo Encryption
- [ ] **Photos encrypted before storage**
  - Backend uses SKINPROOF_PHOTO_KEY to encrypt
  - Photos stored as encrypted blobs
  - Decryption requires key (never sent to client)

**Verify:**
```bash
# Check photo file is not readable plaintext
cat {photo_storage_dir}/user_123_capture_456.enc
# Should be binary gibberish, not JPEG/PNG

# Verify encryption key not in code
grep -r "SKINPROOF_PHOTO_KEY" app/src/
# Should return no results
```

- [ ] **Encryption key management**
  - Key stored in environment variable or secret manager
  - Key rotation plan documented
  - Key never logged or exposed in error messages

---

#### 5.2 Local Data Security (Android)

- [ ] **Sensitive data encrypted on device**
  - Room database uses SQLCipher (if implemented)
  - SharedPreferences/DataStore encrypted
  - Files stored in app-private directory

**Android Data Storage:**
```kotlin
// DataStore with encryption
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigration = { /* ... */ }
)
```

- [ ] **Test local data access:**
  - Rooted device: Can attacker access /data/data/com.glowup.ai/?
  - Non-rooted device: App data inaccessible
  - Exported backup disallowed in AndroidManifest

**AndroidManifest.xml:**
```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="false"
    android:dataExtractionRules="@xml/data_extraction_rules">
```

---

#### 5.3 Secure Data Deletion
- [ ] **Account deletion removes all data**
  - User deletes account
  - Backend deletes:
    - User profile
    - All captures and metrics
    - Photos (encrypted files)
    - Products, experiments, Q&A threads
    - Subscription records
  - Firebase Auth user deleted
  - Local app data cleared

**Verify:**
```sql
-- Check no data remains
SELECT * FROM users WHERE user_id = '{deleted_user_id}';
-- Expected: 0 rows

SELECT * FROM captures WHERE user_id = '{deleted_user_id}';
-- Expected: 0 rows
```

```bash
# Check photo files deleted
ls {photo_storage_dir} | grep {deleted_user_id}
# Expected: no results
```

---

### 6. Input Validation (P0)

#### 6.1 SQL Injection Prevention
- [ ] **Parameterized queries used**
  - All SQL queries use parameter binding
  - No string concatenation in queries

**Check codebase:**
```bash
# Search for SQL injection risks
grep -r "SELECT.*+.*user_id" backend/skinproof/
# Should return no results

grep -r "f\"SELECT" backend/skinproof/
# Should return no results (f-strings in queries)
```

- [ ] **Test SQL injection attempts:**
```bash
# Try SQL injection in user_id
curl -H "Authorization: Bearer {valid_token}" \
  "https://api.glowup.ai/api/users/123' OR '1'='1/dashboard"
# Expected: 400 Bad Request or 404 Not Found (not SQL error)

# Try SQL injection in product name
curl -H "Authorization: Bearer {valid_token}" \
  -X POST https://api.glowup.ai/api/users/{user_id}/products \
  -d '{"name":"'; DROP TABLE products;--"}'
# Expected: 400 or validation error (not SQL execution)
```

---

#### 6.2 XSS Prevention
- [ ] **User input sanitized**
  - Product names, experiment hypotheses, Q&A questions
  - HTML tags stripped or escaped
  - No `<script>` injection possible

**Test:**
```bash
# Try XSS in product name
curl -H "Authorization: Bearer {valid_token}" \
  -X POST https://api.glowup.ai/api/users/{user_id}/products \
  -d '{"name":"<script>alert(1)</script>"}'
# Expected: Input sanitized, script tags removed

# Verify response
curl -H "Authorization: Bearer {valid_token}" \
  https://api.glowup.ai/api/users/{user_id}/products
# Response should not contain <script> tag
```

---

#### 6.3 File Upload Validation
- [ ] **Image upload validation**
  - Only accept base64-encoded images
  - Validate image format (JPEG, PNG only)
  - Reject non-image files (PDF, EXE, etc.)
  - Max file size enforced (e.g., 10MB)

**Test:**
```bash
# Try uploading PDF disguised as image
curl -H "Authorization: Bearer {valid_token}" \
  -X POST https://api.glowup.ai/api/users/{user_id}/captures \
  -d '{"user_id":"123","image_base64":"{base64_pdf}"}'
# Expected: 400 Bad Request, "Invalid image format"

# Try oversized image (20MB)
curl -H "Authorization: Bearer {valid_token}" \
  -X POST https://api.glowup.ai/api/users/{user_id}/captures \
  -d '{"user_id":"123","image_base64":"{huge_base64}"}'
# Expected: 413 Payload Too Large or 400 validation error
```

- [ ] **Malicious image handling**
  - Image processing library (PIL/Pillow) doesn't crash on malformed images
  - No RCE (remote code execution) via crafted image

---

### 7. API Security (P0)

#### 7.1 Rate Limiting
- [ ] **Rate limits enforced**
  - POST /captures: Max 10 per hour per user
  - POST /qna: Max 50 per day per user
  - POST /users: Max 5 per IP per hour (sign up)

**Test:**
```bash
# Rapid capture uploads
for i in {1..15}; do
  curl -H "Authorization: Bearer {valid_token}" \
    -X POST https://api.glowup.ai/api/users/{user_id}/captures \
    -d '{"user_id":"123","image_base64":"..."}'
done
# Expected: After 10th request, 429 Too Many Requests
```

- [ ] **Rate limit bypass prevention**
  - Cannot bypass by changing IP (user-level rate limit)
  - Cannot bypass by creating multiple accounts (IP-level sign up limit)

---

#### 7.2 CORS (Cross-Origin Resource Sharing)
- [ ] **CORS configured correctly**
  - Backend allows requests from app origin only
  - No wildcard `Access-Control-Allow-Origin: *`

**Backend CORS config:**
```python
# skinproof/complete_api.py
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://app.glowup.ai"],  # Not "*"
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["*"],
)
```

- [ ] **Test CORS:**
```bash
# Request from unauthorized origin
curl -H "Origin: https://evil.com" \
  https://api.glowup.ai/api/health
# Response should not include Access-Control-Allow-Origin: https://evil.com
```

---

#### 7.3 API Error Handling
- [ ] **Error messages don't leak sensitive info**
  - Database errors: generic "Internal server error"
  - Stack traces not exposed in production
  - User IDs not leaked in error messages

**Test:**
```bash
# Trigger server error
curl -H "Authorization: Bearer {valid_token}" \
  https://api.glowup.ai/api/users/invalid_user_id/dashboard
# Expected: {"detail":"User not found"} (not SQL error or stack trace)
```

- [ ] **Status codes used correctly**
  - 401 for authentication failure
  - 403 for authorization failure
  - 404 for not found (when appropriate)
  - 500 for server errors (generic)

---

### 8. Third-Party Integrations (P1)

#### 8.1 Firebase Security
- [ ] **Firebase rules configured**
  - Firestore/Realtime Database rules deny unauthenticated access
  - Users can only read/write their own data

**Example rules:**
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    }
  }
}
```

- [ ] **Firebase API keys restricted**
  - Android API key restricted to app package name
  - Web API key restricted to domain
  - API keys not checked into Git

---

#### 8.2 Gemini API Security
- [ ] **API key not exposed**
  - GEMINI_API_KEY stored as environment variable
  - Not in codebase or logs
  - Backend makes Gemini calls, not client

- [ ] **User input sanitized before Gemini**
  - Q&A questions sanitized to prevent prompt injection
  - Max question length enforced (2000 chars)

---

#### 8.3 Analytics & Crashlytics
- [ ] **PII not logged**
  - User emails, user_ids not sent to Firebase Analytics
  - Photos never uploaded to Crashlytics
  - Custom events anonymized

**Check:**
```kotlin
// Don't log user_id
FirebaseAnalytics.getInstance(context).logEvent("capture_uploaded") {
    // param("user_id", userId) // WRONG
    param("capture_count", count) // OK
}
```

---

## Privacy & Compliance Tests

### 9. GDPR/CCPA Compliance (P0)

#### 9.1 Consent Management
- [ ] **Explicit consent required**
  - User must accept consent before using app
  - Consent version tracked (policy_version)
  - Consent stored in backend

- [ ] **Consent withdrawal**
  - User can revoke consent
  - Revocation triggers account deletion flow
  - User cannot use app without consent

---

#### 9.2 Data Export (Right to Access)
- [ ] **User can export their data**
  - GET /api/users/{user_id}/export endpoint
  - Returns JSON with all user data:
    - Profile
    - Captures and metrics
    - Products and routine events
    - Experiments
    - Q&A threads
    - Subscription history

**Test:**
```bash
curl -H "Authorization: Bearer {valid_token}" \
  https://api.glowup.ai/api/users/{user_id}/export \
  -o user_data_export.json
# Verify all data present
```

---

#### 9.3 Data Deletion (Right to be Forgotten)
- [ ] **Account deletion is permanent**
  - DELETE /api/users/{user_id} removes all data
  - No soft-delete (data actually removed)
  - Deletion confirmed within 30 days
  - User notified of completion

---

#### 9.4 Data Minimization
- [ ] **Only necessary data collected**
  - Don't collect location unless needed
  - Don't collect device info beyond OS/model
  - Photos deleted after retention period (if policy specifies)

---

### 10. Secure Development Practices (P1)

#### 10.1 Secrets Management
- [ ] **No secrets in code**
```bash
# Check for hardcoded secrets
grep -ri "api_key.*=" app/src/
grep -ri "password.*=" app/src/
grep -ri "sk_" app/src/  # Stripe secret keys
grep -ri "AKIA" app/src/  # AWS keys
# Should return no secrets
```

- [ ] **Environment variables used**
  - Backend: .env file (not committed)
  - Android: local.properties or BuildConfig

---

#### 10.2 Dependency Vulnerabilities
- [ ] **Dependencies up to date**
```bash
# Python dependencies
cd backend
pip list --outdated

# Android dependencies
./gradlew dependencyUpdates
```

- [ ] **Known vulnerabilities patched**
```bash
# Python
safety check

# Android (if using)
./gradlew dependencyCheckAnalyze
```

---

#### 10.3 Code Obfuscation (Android)
- [ ] **Release builds obfuscated**
  - ProGuard/R8 enabled
  - Code shrinking enabled
  - Resource shrinking enabled

**build.gradle.kts:**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

- [ ] **ProGuard rules tested**
  - Release APK runs without crashes
  - Reflection-based libraries (Retrofit, Room) not broken

---

#### 10.4 Logging Security
- [ ] **No sensitive data logged**
```bash
# Check for sensitive logging
grep -r "Log.d.*user_id" app/src/
grep -r "Log.d.*token" app/src/
grep -r "print(user_id)" backend/skinproof/
grep -r "logger.info.*token" backend/skinproof/
# Should return minimal results, never in production code
```

- [ ] **Production logging sanitized**
  - User IDs redacted or hashed
  - Tokens never logged
  - Photos never logged (even as base64)

---

## Penetration Testing

### 11. Manual Penetration Tests (P1)

#### 11.1 OWASP Top 10 Coverage
- [ ] A01: Broken Access Control → Tested in Section 2
- [ ] A02: Cryptographic Failures → Tested in Section 5
- [ ] A03: Injection → Tested in Section 6
- [ ] A04: Insecure Design → Architecture review
- [ ] A05: Security Misconfiguration → Check Sections 7, 8
- [ ] A06: Vulnerable Components → Tested in Section 10.2
- [ ] A07: Authentication Failures → Tested in Section 1
- [ ] A08: Software/Data Integrity → Code signing, dependencies
- [ ] A09: Logging Failures → Tested in Section 10.4
- [ ] A10: Server-Side Request Forgery (SSRF) → N/A (no user-provided URLs)

---

#### 11.2 Automated Scanning
- [ ] **OWASP ZAP scan**
```bash
zap-cli quick-scan https://api.glowup.ai
```

- [ ] **SQLMap for SQL injection**
```bash
sqlmap -u "https://api.glowup.ai/api/users/123/dashboard" --headers="Authorization: Bearer {token}"
```

- [ ] **Nikto web scanner**
```bash
nikto -h https://api.glowup.ai
```

---

### 12. Android Security Tests (P1)

#### 12.1 Static Analysis
- [ ] **Android Lint checks**
```bash
./gradlew lint
# Check report for security warnings
```

- [ ] **MobSF (Mobile Security Framework)**
  - Upload APK to MobSF
  - Review security score
  - Address critical findings

---

#### 12.2 Dynamic Analysis
- [ ] **Runtime inspection (rooted device)**
  - Install app on rooted device
  - Use Frida/Objection to inspect runtime
  - Check for exposed secrets, weak encryption

- [ ] **Network traffic analysis**
  - Install MITM proxy (Charles, Burp Suite)
  - Capture app traffic
  - Verify all traffic is HTTPS
  - Check for sensitive data in requests

---

## Security Test Summary

| Category | Critical | High | Medium | Low | Status |
|----------|----------|------|--------|-----|--------|
| Authentication | | | | | |
| Authorization | | | | | |
| Data in Transit | | | | | |
| Data at Rest | | | | | |
| Input Validation | | | | | |
| API Security | | | | | |
| Third-Party | | | | | |
| Privacy/Compliance | | | | | |
| Secure Dev | | | | | |
| Penetration Tests | | | | | |

---

## Incident Response Plan

In case of security breach:

1. **Immediate Response**
   - Isolate affected systems
   - Revoke compromised tokens/keys
   - Notify security team

2. **Investigation**
   - Identify scope of breach
   - Determine data accessed
   - Preserve logs for forensics

3. **Remediation**
   - Patch vulnerability
   - Force password/token reset for affected users
   - Deploy fix to production

4. **Notification**
   - Notify affected users (if PII exposed)
   - Report to authorities (if required by GDPR/CCPA)
   - Post-mortem and lessons learned

---

## Security Checklist Sign-off

- [ ] All P0 tests completed and passing
- [ ] Known vulnerabilities documented and triaged
- [ ] Secrets management verified
- [ ] Privacy compliance confirmed
- [ ] Penetration tests completed
- [ ] Incident response plan in place

**Security Lead:** ___________
**Date:** ___________
**Risk Assessment:** Low / Medium / High
**Production Approval:** Yes / No
