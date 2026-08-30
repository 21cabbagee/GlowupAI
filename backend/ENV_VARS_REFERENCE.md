# Environment Variables Reference

Complete reference for all environment variables used by GlowUp AI Backend.

## Quick Copy-Paste for Railway

```bash
# === CORE REQUIRED (4 vars) ===
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
SKINPROOF_ENV=production
SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
SKINPROOF_ALLOWED_ORIGINS=https://your-domain.com

# === AUTHENTICATION (2 vars) ===
GEMINI_API_KEY=your_gemini_key_here
SKINPROOF_ADMIN_TOKEN=your_random_token_here

# === AUTH CONTROL (1 var) ===
SKINPROOF_AUTH_REQUIRED=0

# === PHOTO STORAGE (3 vars) ===
SKINPROOF_PHOTO_DIR=/data/photos
SKINPROOF_PHOTO_KEY=your_base64_key_here
SKINPROOF_RAW_RETENTION_DAYS=730
```

---

## Variable Reference Table

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| **DATABASE** ||||
| `DATABASE_URL` | Auto | - | PostgreSQL connection URL (Railway auto-injects) |
| `SKINPROOF_DB_POOL_MIN_SIZE` | No | 1 | Minimum database connections in pool |
| `SKINPROOF_DB_POOL_MAX_SIZE` | No | 10 | Maximum database connections in pool |
| `SKINPROOF_DB_CONNECT_TIMEOUT` | No | 10 | Database connection timeout (seconds) |
| **CORE** ||||
| `SKINPROOF_ENV` | **Yes** | development | Environment: `production` or `development` |
| `SKINPROOF_FIREBASE_PROJECT_ID` | **Yes** | - | Firebase project ID for auth |
| `SKINPROOF_ALLOWED_ORIGINS` | **Yes** | localhost | Comma-separated CORS origins |
| `SKINPROOF_DISABLE_LEGACY_KEY_FILE` | **Yes** | 0 | Disable local key file (set to 1 in prod) |
| **AUTHENTICATION** ||||
| `SKINPROOF_AUTH_REQUIRED` | No | 0 | Require Firebase auth on endpoints (0 or 1) |
| `SKINPROOF_ADMIN_TOKEN` | Recommended | - | Secret token for /api/admin/* routes |
| **GEMINI AI** ||||
| `GEMINI_API_KEY` | **Yes** | - | Google Gemini API key |
| `SKINPROOF_GEMINI_MODEL` | No | gemini-3.5-flash-lite | Gemini model to use |
| `SKINPROOF_GEMINI_ENABLED` | No | 1 | Enable/disable Gemini features |
| **PHOTO STORAGE** ||||
| `SKINPROOF_PHOTO_DIR` | No | - | Directory for encrypted photo storage |
| `SKINPROOF_PHOTO_KEY` | No | - | Base64-encoded 32-byte encryption key |
| `SKINPROOF_RAW_RETENTION_DAYS` | No | 730 | Days to retain raw photo data |
| **VERSIONING** ||||
| `SKINPROOF_MODEL_VERSION` | No | deterministic-3.0 | Measurement model version |
| `SKINPROOF_POLICY_VERSION` | No | 2026-01 | Privacy policy version |
| **RUNTIME** ||||
| `PORT` | Auto | 8000 | Server port (Railway auto-injects) |

---

## Detailed Variable Descriptions

### DATABASE_URL
**Format:** `postgresql://user:password@host:port/database`

**Set by:** Railway automatically when PostgreSQL is added

**Description:** Connection string for PostgreSQL database. Also accepts:
- `SKINPROOF_DATABASE_URL` (alternative name)
- `POSTGRES_URL` (alternative name)

If none are set, falls back to SQLite at `SKINPROOF_DB_PATH`.

**Example:**
```
postgresql://postgres:ABC123@containers-us-west-1.railway.app:5432/railway
```

---

### SKINPROOF_ENV
**Required:** Yes

**Values:** `production` | `development`

**Description:** Environment mode. MUST be `production` in deployed environments.

**Effects:**
- Disables legacy key file loading in production
- Controls error detail verbosity
- Affects security defaults

**Set to:**
```bash
SKINPROOF_ENV=production
```

---

### SKINPROOF_FIREBASE_PROJECT_ID
**Required:** Yes

**Format:** Firebase project ID string

**Description:** Your Firebase project ID for token verification. Used for JWT authentication.

**For GlowUp AI:**
```bash
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
```

**How to find:**
- Firebase Console -> Project Settings -> Project ID

---

### SKINPROOF_ALLOWED_ORIGINS
**Required:** Yes (in production)

**Format:** Comma-separated URLs (no spaces, no trailing slashes)

**Description:** CORS allowed origins. Determines which domains can make API requests.

**Examples:**
```bash
# Single domain
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com

# Multiple domains
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com

# Development (default if unset)
SKINPROOF_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8000
```

**Important:**
- No spaces after commas
- No trailing slashes
- Include both www and non-www if needed
- Use HTTPS in production

---

### GEMINI_API_KEY
**Required:** Yes (for AI features)

**Format:** Google API key string

**Description:** Google Gemini API key for:
- Shelf scan OCR (product detection from photos)
- Q&A with citations
- Ingredient analysis

**Get your key:** https://ai.google.dev/

**Example:**
```bash
GEMINI_API_KEY=AIzaSyD1234567890abcdefghijklmnopqrstuv
```

**Alternative name:** `SKINPROOF_GEMINI_API_KEY`

**If not set:** AI features will be disabled but app will still work for basic tracking.

---

### SKINPROOF_ADMIN_TOKEN
**Required:** Recommended for production

**Format:** Long random string (32+ characters)

**Description:** Secret token to access `/api/admin/*` routes including:
- Audit logs
- System metrics
- User management

**Generate:**
```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

**Usage:**
```bash
curl -H "X-Admin-Token: your_token_here" https://api.glowup.com/api/admin/audit
```

**If not set:** All admin routes return 403 Forbidden.

---

### SKINPROOF_AUTH_REQUIRED
**Required:** No

**Values:** `0` (off) | `1` (on)

**Default:** `0`

**Description:** Require Firebase authentication on all user-scoped endpoints.

**Deployment strategy:**
1. Deploy with `SKINPROOF_AUTH_REQUIRED=0`
2. Test API without auth
3. Integrate Firebase auth in frontend
4. Test with auth headers
5. Flip to `SKINPROOF_AUTH_REQUIRED=1`

**Set to 1 when:** Frontend sends `Authorization: Bearer <token>` on all requests.

---

### SKINPROOF_PHOTO_DIR
**Required:** No (but recommended for production)

**Format:** Absolute path to directory

**Description:** Directory for encrypted photo storage on disk.

**For Railway Volume:**
```bash
SKINPROOF_PHOTO_DIR=/data/photos
```

**Requirements:**
- Must be writable by app user
- Should be on persistent volume (not ephemeral)
- Must set `SKINPROOF_PHOTO_KEY` when using this

**If not set:** Photos stored in memory only (lost on restart).

---

### SKINPROOF_PHOTO_KEY
**Required:** If using SKINPROOF_PHOTO_DIR

**Format:** Base64-encoded 32-byte key

**Description:** AES-GCM encryption key for photo storage.

**Generate:**
```bash
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

**Example:**
```bash
SKINPROOF_PHOTO_KEY=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6==
```

**Security:**
- Never commit to Git
- Store in Railway variables only
- Rotate periodically (requires re-encryption)

---

### SKINPROOF_DISABLE_LEGACY_KEY_FILE
**Required:** Yes in production

**Values:** `0` (allow) | `1` (disable)

**Description:** Disables reading API keys from local `first.py` file (development bridge).

**Set to:**
```bash
SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
```

**Why:** Security - prevents accidental exposure of local development keys.

---

### SKINPROOF_DB_POOL_MIN_SIZE / MAX_SIZE
**Required:** No

**Defaults:** Min: 1, Max: 10

**Description:** PostgreSQL connection pool sizing.

**Tuning guidance:**
- **Low traffic:** Min: 1, Max: 10 (default)
- **Medium traffic:** Min: 2, Max: 20
- **High traffic:** Min: 5, Max: 50

**Example:**
```bash
SKINPROOF_DB_POOL_MIN_SIZE=2
SKINPROOF_DB_POOL_MAX_SIZE=20
```

**Monitor:** If seeing "connection pool exhausted" errors, increase MAX_SIZE.

---

### SKINPROOF_RAW_RETENTION_DAYS
**Required:** No

**Default:** 730 (2 years)

**Description:** How many days to retain raw photo data before purging.

**Values:**
- `365` = 1 year
- `730` = 2 years (default)
- `1095` = 3 years
- `-1` = never delete

**Example:**
```bash
SKINPROOF_RAW_RETENTION_DAYS=365
```

---

### PORT
**Set by:** Railway automatically

**Default:** 8000 (local dev only)

**Description:** HTTP server port. Railway injects this automatically.

**DO NOT SET MANUALLY** - Railway manages this.

---

## Variable Groups by Use Case

### Minimal Working Deployment
These 7 variables get you a working deployment:
```bash
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
SKINPROOF_ENV=production
SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
SKINPROOF_ALLOWED_ORIGINS=https://your-domain.com
GEMINI_API_KEY=your_key
SKINPROOF_AUTH_REQUIRED=0
SKINPROOF_ADMIN_TOKEN=your_token
```

### With Persistent Photo Storage
Add these 3 for photo persistence:
```bash
SKINPROOF_PHOTO_DIR=/data/photos
SKINPROOF_PHOTO_KEY=your_base64_key
SKINPROOF_RAW_RETENTION_DAYS=730
```

### Production Hardened
Full production setup (13 variables):
```bash
# Core
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
SKINPROOF_ENV=production
SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com

# Auth
GEMINI_API_KEY=your_key
SKINPROOF_ADMIN_TOKEN=your_token
SKINPROOF_AUTH_REQUIRED=1

# Photos
SKINPROOF_PHOTO_DIR=/data/photos
SKINPROOF_PHOTO_KEY=your_base64_key
SKINPROOF_RAW_RETENTION_DAYS=730

# Tuning
SKINPROOF_DB_POOL_MIN_SIZE=2
SKINPROOF_DB_POOL_MAX_SIZE=20
SKINPROOF_GEMINI_MODEL=gemini-3.5-flash-lite
```

---

## Validation Checklist

Use this to verify your configuration:

### Required Variables Check
- [ ] `SKINPROOF_ENV=production` (exactly)
- [ ] `SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7` (exactly)
- [ ] `SKINPROOF_DISABLE_LEGACY_KEY_FILE=1` (exactly)
- [ ] `SKINPROOF_ALLOWED_ORIGINS` contains your actual domain
- [ ] `GEMINI_API_KEY` is a valid Gemini key
- [ ] `SKINPROOF_ADMIN_TOKEN` is 32+ character random string

### Optional but Recommended
- [ ] `SKINPROOF_PHOTO_DIR=/data/photos` (if using volume)
- [ ] `SKINPROOF_PHOTO_KEY` is base64-encoded 32 bytes
- [ ] `SKINPROOF_AUTH_REQUIRED=0` initially, then flip to 1

### Security Validation
- [ ] No secrets in Git
- [ ] `SKINPROOF_ENV` is NOT "development"
- [ ] `SKINPROOF_ALLOWED_ORIGINS` does NOT include localhost
- [ ] `SKINPROOF_ADMIN_TOKEN` is NOT "admin" or "test"
- [ ] Photo encryption key is properly random

---

## Common Mistakes

### ❌ Wrong: Trailing slashes in CORS
```bash
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com/
```

### ✅ Correct: No trailing slashes
```bash
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com
```

---

### ❌ Wrong: Spaces after commas
```bash
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com, https://www.glowup.com
```

### ✅ Correct: No spaces
```bash
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com
```

---

### ❌ Wrong: Setting DATABASE_URL manually
```bash
DATABASE_URL=postgresql://...
```

### ✅ Correct: Let Railway set it
```bash
# Don't set DATABASE_URL - Railway injects it automatically
```

---

### ❌ Wrong: Weak admin token
```bash
SKINPROOF_ADMIN_TOKEN=admin123
```

### ✅ Correct: Strong random token
```bash
SKINPROOF_ADMIN_TOKEN=X7f9JmK3pL2nQ8rT6vY1wZ4bN5hM9sD0cA2eG7iU3oP5
```

---

### ❌ Wrong: Development mode in production
```bash
SKINPROOF_ENV=development
```

### ✅ Correct: Production mode
```bash
SKINPROOF_ENV=production
```

---

## Environment-Specific Settings

### Development (Local)
```bash
SKINPROOF_ENV=development
DATABASE_URL=postgresql://skinproof:skinproof@localhost:5432/skinproof
SKINPROOF_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8000
GEMINI_API_KEY=your_dev_key
SKINPROOF_AUTH_REQUIRED=0
```

### Staging
```bash
SKINPROOF_ENV=production
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-staging
SKINPROOF_ALLOWED_ORIGINS=https://staging.glowup.com
GEMINI_API_KEY=your_staging_key
SKINPROOF_AUTH_REQUIRED=1
SKINPROOF_ADMIN_TOKEN=staging_token_here
```

### Production
```bash
SKINPROOF_ENV=production
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
SKINPROOF_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com
GEMINI_API_KEY=your_production_key
SKINPROOF_AUTH_REQUIRED=1
SKINPROOF_ADMIN_TOKEN=production_token_here
SKINPROOF_PHOTO_DIR=/data/photos
SKINPROOF_PHOTO_KEY=production_photo_key_here
```

---

## Troubleshooting by Variable

### Health Check Fails
**Check:**
- `DATABASE_URL` exists (Railway auto-injects)
- PostgreSQL service is running

### CORS Errors
**Check:**
- `SKINPROOF_ALLOWED_ORIGINS` includes your frontend domain
- No trailing slashes
- No spaces after commas
- HTTPS matches (http vs https)

### 401 Unauthorized
**Check:**
- `SKINPROOF_AUTH_REQUIRED` is 0 for initial testing
- `SKINPROOF_FIREBASE_PROJECT_ID` matches your Firebase project
- Frontend sends `Authorization: Bearer <token>` header

### Photos Disappear
**Check:**
- `SKINPROOF_PHOTO_DIR=/data/photos` is set
- Railway volume is mounted at `/data/photos`
- `SKINPROOF_PHOTO_KEY` is set and valid base64

### Gemini Features Fail
**Check:**
- `GEMINI_API_KEY` is set
- Key is valid (test at https://ai.google.dev/)
- `SKINPROOF_GEMINI_ENABLED=1`
- API quota not exceeded

### Admin Routes Return 403
**Check:**
- `SKINPROOF_ADMIN_TOKEN` is set
- Request includes `X-Admin-Token` header
- Token matches exactly (no spaces)

---

## Quick Reference Card

**Paste this into Railway Variables tab:**

```bash
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
SKINPROOF_ENV=production
SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
SKINPROOF_ALLOWED_ORIGINS=https://your-domain.com
GEMINI_API_KEY=your_gemini_key_here
SKINPROOF_ADMIN_TOKEN=your_random_token_here
SKINPROOF_AUTH_REQUIRED=0
SKINPROOF_PHOTO_DIR=/data/photos
SKINPROOF_PHOTO_KEY=your_base64_key_here
SKINPROOF_RAW_RETENTION_DAYS=730
```

**Remember to:**
1. Replace `your-domain.com` with actual domain
2. Get Gemini key from https://ai.google.dev/
3. Generate tokens with commands in RAILWAY_DEPLOY.md
4. Create Railway volume mounted at `/data/photos`

---

## Need Help?

- **Full Guide:** See `RAILWAY_DEPLOY.md`
- **Quick Start:** See `RAILWAY_QUICKSTART.md`
- **Deployment Checklist:** See `DEPLOYMENT_CHECKLIST.md`
- **Helper Commands:** Source `railway-commands.sh`
