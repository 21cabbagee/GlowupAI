# Environment Variables Reference

Complete reference for all environment variables used by GlowUp AI Backend.

## Quick Copy-Paste for Railway

```bash
# === CORE REQUIRED (4 vars) ===
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com

# === AUTHENTICATION (2 vars) ===
GEMINI_API_KEY=your_gemini_key_here
GLOWUPAI_ADMIN_TOKEN=your_random_token_here

# === AUTH CONTROL (1 var) ===
GLOWUPAI_AUTH_REQUIRED=0

# === PHOTO STORAGE (3 vars) ===
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=your_base64_key_here
GLOWUPAI_RAW_RETENTION_DAYS=730
```

---

## Variable Reference Table

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| **DATABASE** ||||
| `DATABASE_URL` | Auto | - | PostgreSQL connection URL (Railway auto-injects) |
| `GLOWUPAI_DB_POOL_MIN_SIZE` | No | 1 | Minimum database connections in pool |
| `GLOWUPAI_DB_POOL_MAX_SIZE` | No | 10 | Maximum database connections in pool |
| `GLOWUPAI_DB_CONNECT_TIMEOUT` | No | 10 | Database connection timeout (seconds) |
| **CORE** ||||
| `GLOWUPAI_ENV` | **Yes** | development | Environment: `production` or `development` |
| `GLOWUPAI_FIREBASE_PROJECT_ID` | **Yes** | - | Firebase project ID for auth |
| `GLOWUPAI_ALLOWED_ORIGINS` | **Yes** | localhost | Comma-separated CORS origins |
| `GLOWUPAI_DISABLE_LEGACY_KEY_FILE` | **Yes** | 0 | Disable local key file (set to 1 in prod) |
| **AUTHENTICATION** ||||
| `GLOWUPAI_AUTH_REQUIRED` | No | 0 | Require Firebase auth on endpoints (0 or 1) |
| `GLOWUPAI_ADMIN_TOKEN` | Recommended | - | Secret token for /api/admin/* routes |
| **GEMINI AI** ||||
| `GEMINI_API_KEY` | **Yes** | - | Google Gemini API key |
| `GLOWUPAI_GEMINI_MODEL` | No | gemini-3.5-flash-lite | Gemini model to use |
| `GLOWUPAI_GEMINI_ENABLED` | No | 1 | Enable/disable Gemini features |
| **PHOTO STORAGE** ||||
| `GLOWUPAI_PHOTO_DIR` | No | - | Directory for encrypted photo storage |
| `GLOWUPAI_PHOTO_KEY` | No | - | Base64-encoded 32-byte encryption key |
| `GLOWUPAI_RAW_RETENTION_DAYS` | No | 730 | Days to retain raw photo data |
| **VERSIONING** ||||
| `GLOWUPAI_MODEL_VERSION` | No | deterministic-3.0 | Measurement model version |
| `GLOWUPAI_POLICY_VERSION` | No | 2026-01 | Privacy policy version |
| **IMAGE PREPROCESSING** ||||
| `GLOWUPAI_ENABLE_PREPROCESSING` | No | 1 | Enable image preprocessing pipeline (0 or 1) |
| `GLOWUPAI_SKIP_QUALITY_CHECKS` | No | 0 | Skip quality validation (testing only, 0 or 1) |
| **RUNTIME** ||||
| `PORT` | Auto | 8000 | Server port (Railway auto-injects) |
| **MONITORING** ||||
| `SENTRY_DSN` | Recommended | - | Sentry DSN for error monitoring |
| `SENTRY_TRACES_SAMPLE_RATE` | No | 0.1 | Percentage of traces to capture (0.0-1.0) |
| `SENTRY_PROFILES_SAMPLE_RATE` | No | 0.1 | Percentage of profiles to capture (0.0-1.0) |
| **PERFORMANCE** ||||
| `REDIS_URL` | Recommended | - | Redis URL for caching and rate limiting |
| `GLOWUPAI_CACHE_ENABLED` | No | 1 | Enable response caching (0 or 1) |
| `GLOWUPAI_SLOW_THRESHOLD_MS` | No | 1000 | Threshold for logging slow requests (ms) |
| `GLOWUPAI_MAX_IMAGE_DIMENSION` | No | 1024 | Max image dimension for compression (px) |
| `GLOWUPAI_IMAGE_QUALITY` | No | 85 | JPEG quality for compression (0-100) |

---

## Detailed Variable Descriptions

### DATABASE_URL
**Format:** `postgresql://user:password@host:port/database`

**Set by:** Railway automatically when PostgreSQL is added

**Description:** Connection string for PostgreSQL database. Also accepts:
- `GLOWUPAI_DATABASE_URL` (alternative name)
- `POSTGRES_URL` (alternative name)

If none are set, falls back to SQLite at `GLOWUPAI_DB_PATH`.

**Example:**
```
postgresql://postgres:ABC123@containers-us-west-1.railway.app:5432/railway
```

---

### GLOWUPAI_ENV
**Required:** Yes

**Values:** `production` | `development`

**Description:** Environment mode. MUST be `production` in deployed environments.

**Effects:**
- Disables legacy key file loading in production
- Controls error detail verbosity
- Affects security defaults

**Set to:**
```bash
GLOWUPAI_ENV=production
```

---

### GLOWUPAI_FIREBASE_PROJECT_ID
**Required:** Yes

**Format:** Firebase project ID string

**Description:** Your Firebase project ID for token verification. Used for JWT authentication.

**For GlowUp AI:**
```bash
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
```

**How to find:**
- Firebase Console -> Project Settings -> Project ID

---

### GLOWUPAI_ALLOWED_ORIGINS
**Required:** Yes (in production)

**Format:** Comma-separated URLs (no spaces, no trailing slashes)

**Description:** CORS allowed origins. Determines which domains can make API requests.

**Examples:**
```bash
# Single domain
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com

# Multiple domains
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com

# Development (default if unset)
GLOWUPAI_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8000
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
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

**Alternative name:** `GLOWUPAI_GEMINI_API_KEY`

**If not set:** AI features will be disabled but app will still work for basic tracking.

---

### GLOWUPAI_ADMIN_TOKEN
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
curl -H "Authorization: Bearer your_token_here" https://api.glowup.com/api/admin/audit
```

**If not set:** All admin routes return 403 Forbidden.

---

### GLOWUPAI_AUTH_REQUIRED
**Required:** No

**Values:** `0` (off) | `1` (on)

**Default:** `0`

**Description:** Require Firebase authentication on all user-scoped endpoints.

**Deployment strategy:**
1. Deploy with `GLOWUPAI_AUTH_REQUIRED=0`
2. Test API without auth
3. Integrate Firebase auth in frontend
4. Test with auth headers
5. Flip to `GLOWUPAI_AUTH_REQUIRED=1`

**Set to 1 when:** Frontend sends `Authorization: Bearer <token>` on all requests.

---

### GLOWUPAI_PHOTO_DIR
**Required:** No (but recommended for production)

**Format:** Absolute path to directory

**Description:** Directory for encrypted photo storage on disk.

**For Railway Volume:**
```bash
GLOWUPAI_PHOTO_DIR=/data/photos
```

**Requirements:**
- Must be writable by app user
- Should be on persistent volume (not ephemeral)
- Must set `GLOWUPAI_PHOTO_KEY` when using this

**If not set:** Photos stored in memory only (lost on restart).

---

### GLOWUPAI_PHOTO_KEY
**Required:** If using GLOWUPAI_PHOTO_DIR

**Format:** Base64-encoded 32-byte key

**Description:** AES-GCM encryption key for photo storage.

**Generate:**
```bash
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

**Example:**
```bash
GLOWUPAI_PHOTO_KEY=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6==
```

**Security:**
- Never commit to Git
- Store in Railway variables only
- Rotate periodically (requires re-encryption)

---

### GLOWUPAI_DISABLE_LEGACY_KEY_FILE
**Required:** Yes in production

**Values:** `0` (allow) | `1` (disable)

**Description:** Disables reading API keys from local `first.py` file (development bridge).

**Set to:**
```bash
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
```

**Why:** Security - prevents accidental exposure of local development keys.

---

### GLOWUPAI_DB_POOL_MIN_SIZE / MAX_SIZE
**Required:** No

**Defaults:** Min: 1, Max: 10

**Description:** PostgreSQL connection pool sizing.

**Tuning guidance:**
- **Low traffic:** Min: 1, Max: 10 (default)
- **Medium traffic:** Min: 2, Max: 20
- **High traffic:** Min: 5, Max: 50

**Example:**
```bash
GLOWUPAI_DB_POOL_MIN_SIZE=2
GLOWUPAI_DB_POOL_MAX_SIZE=20
```

**Monitor:** If seeing "connection pool exhausted" errors, increase MAX_SIZE.

---

### GLOWUPAI_RAW_RETENTION_DAYS
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
GLOWUPAI_RAW_RETENTION_DAYS=365
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
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com
GEMINI_API_KEY=your_key
GLOWUPAI_AUTH_REQUIRED=0
GLOWUPAI_ADMIN_TOKEN=your_token
```

### With Persistent Photo Storage
Add these 3 for photo persistence:
```bash
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=your_base64_key
GLOWUPAI_RAW_RETENTION_DAYS=730
```

### Production Hardened
Full production setup (14 variables):
```bash
# Core
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com

# Auth
GEMINI_API_KEY=your_key
GLOWUPAI_ADMIN_TOKEN=your_token
GLOWUPAI_AUTH_REQUIRED=1

# Photos
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=your_base64_key
GLOWUPAI_RAW_RETENTION_DAYS=730

# Image Processing
GLOWUPAI_ENABLE_PREPROCESSING=1

# Tuning
GLOWUPAI_DB_POOL_MIN_SIZE=2
GLOWUPAI_DB_POOL_MAX_SIZE=20
GLOWUPAI_GEMINI_MODEL=gemini-3.5-flash-lite
```

---

## Validation Checklist

Use this to verify your configuration:

### Required Variables Check
- [ ] `GLOWUPAI_ENV=production` (exactly)
- [ ] `GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7` (exactly)
- [ ] `GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1` (exactly)
- [ ] `GLOWUPAI_ALLOWED_ORIGINS` contains your actual domain
- [ ] `GEMINI_API_KEY` is a valid Gemini key
- [ ] `GLOWUPAI_ADMIN_TOKEN` is 32+ character random string

### Optional but Recommended
- [ ] `GLOWUPAI_PHOTO_DIR=/data/photos` (if using volume)
- [ ] `GLOWUPAI_PHOTO_KEY` is base64-encoded 32 bytes
- [ ] `GLOWUPAI_AUTH_REQUIRED=0` initially, then flip to 1

### Security Validation
- [ ] No secrets in Git
- [ ] `GLOWUPAI_ENV` is NOT "development"
- [ ] `GLOWUPAI_ALLOWED_ORIGINS` does NOT include localhost
- [ ] `GLOWUPAI_ADMIN_TOKEN` is NOT "admin" or "test"
- [ ] Photo encryption key is properly random

---

## Common Mistakes

### ❌ Wrong: Trailing slashes in CORS
```bash
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com/
```

### ✅ Correct: No trailing slashes
```bash
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com
```

---

### ❌ Wrong: Spaces after commas
```bash
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com, https://www.glowup.com
```

### ✅ Correct: No spaces
```bash
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com
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
GLOWUPAI_ADMIN_TOKEN=admin123
```

### ✅ Correct: Strong random token
```bash
GLOWUPAI_ADMIN_TOKEN=X7f9JmK3pL2nQ8rT6vY1wZ4bN5hM9sD0cA2eG7iU3oP5
```

---

### ❌ Wrong: Development mode in production
```bash
GLOWUPAI_ENV=development
```

### ✅ Correct: Production mode
```bash
GLOWUPAI_ENV=production
```

---

### GLOWUPAI_ENABLE_PREPROCESSING
**Required:** No

**Default:** `1` (enabled)

**Values:** `0` | `1`

**Description:** Controls the image preprocessing pipeline that normalizes lighting and quality before analysis.

**What it does:**
- Applies white balance correction to normalize color temperature
- Uses CLAHE (Contrast Limited Adaptive Histogram Equalization) for local contrast
- Reduces noise while preserving edges
- Standardizes image dimensions for consistent analysis

**When to disable:**
- Testing with raw images
- Comparing preprocessed vs non-preprocessed results
- Debugging analysis issues

**Example:**
```bash
# Enable preprocessing (default, recommended)
GLOWUPAI_ENABLE_PREPROCESSING=1

# Disable preprocessing (testing only)
GLOWUPAI_ENABLE_PREPROCESSING=0
```

**Note:** Preprocessing improves consistency across different lighting conditions and reduces false variations in metrics. Keep enabled in production for best results.

---

### SENTRY_DSN
**Required:** Recommended for production

**Format:** Sentry DSN URL

**Description:** Sentry Data Source Name for error monitoring and performance tracking.

**Get your DSN:**
1. Sign up at https://sentry.io
2. Create a new project (FastAPI/Python)
3. Copy the DSN from project settings

**Example:**
```bash
SENTRY_DSN=https://examplePublicKey@o0.ingest.sentry.io/0
```

**Benefits:**
- Automatic error capture and stack traces
- Performance monitoring for slow endpoints
- User context and breadcrumbs
- Real-time alerts for errors

**If not set:** Error monitoring is disabled, errors only logged locally.

---

### REDIS_URL
**Required:** Recommended for production

**Format:** Redis connection URL

**Description:** Redis URL for rate limiting and response caching. Improves performance and prevents API abuse.

**For Railway:**
1. Add Redis service to your project
2. Railway auto-injects `REDIS_URL` or `REDIS_PRIVATE_URL`
3. No manual configuration needed

**For other providers:**
```bash
REDIS_URL=redis://localhost:6379/0
# OR
REDIS_URL=rediss://user:password@host:6379/0  # TLS
```

**Features enabled:**
- Redis-backed rate limiting (more accurate than in-memory)
- Response caching for dashboard endpoints (5-minute TTL)
- Distributed rate limiting across multiple instances

**Fallback:** If Redis is unavailable, falls back to in-memory rate limiting and caching.

---

### GLOWUPAI_MAX_IMAGE_DIMENSION
**Required:** No

**Default:** 1024

**Description:** Maximum image dimension (width or height) in pixels before compression. Images larger than this are resized while maintaining aspect ratio.

**Examples:**
```bash
# Default - good balance of quality and storage
GLOWUPAI_MAX_IMAGE_DIMENSION=1024

# Higher quality, more storage
GLOWUPAI_MAX_IMAGE_DIMENSION=2048

# Lower quality, less storage
GLOWUPAI_MAX_IMAGE_DIMENSION=768
```

**Impact:**
- 1024px: ~150-300KB per photo
- 2048px: ~500KB-1MB per photo
- 768px: ~100-200KB per photo

---

### GLOWUPAI_IMAGE_QUALITY
**Required:** No

**Default:** 85

**Values:** 0-100

**Description:** JPEG compression quality. Higher = better quality but larger files.

**Recommendations:**
- 95-100: Minimal compression, very large files
- 85-95: High quality, good balance (recommended)
- 70-85: Good quality, smaller files
- Below 70: Noticeable quality loss

**Example:**
```bash
GLOWUPAI_IMAGE_QUALITY=85
```

---

### GLOWUPAI_SKIP_QUALITY_CHECKS
**Required:** No

**Default:** `0` (checks enabled)

**Values:** `0` | `1`

**Description:** Skips capture quality validation checks. FOR TESTING ONLY.

**When set to 1:**
- All photos are accepted regardless of quality
- No validation for brightness, sharpness, pose, distance
- Useful for testing with synthetic or non-standard images

**Warning:** Never enable in production - this bypasses important quality gates that ensure consistent measurements.

**Example:**
```bash
# Normal mode - quality checks enabled
GLOWUPAI_SKIP_QUALITY_CHECKS=0

# Testing mode - accept all photos
GLOWUPAI_SKIP_QUALITY_CHECKS=1
```

---

## Environment-Specific Settings

### Development (Local)
```bash
GLOWUPAI_ENV=development
DATABASE_URL=postgresql://glowupai:glowupai@localhost:5432/glowupai
GLOWUPAI_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8000
GEMINI_API_KEY=your_dev_key
GLOWUPAI_AUTH_REQUIRED=0
```

### Staging
```bash
GLOWUPAI_ENV=production
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-staging
GLOWUPAI_ALLOWED_ORIGINS=https://staging.glowup.com
GEMINI_API_KEY=your_staging_key
GLOWUPAI_AUTH_REQUIRED=1
GLOWUPAI_ADMIN_TOKEN=staging_token_here
```

### Production
```bash
GLOWUPAI_ENV=production
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com
GEMINI_API_KEY=your_production_key
GLOWUPAI_AUTH_REQUIRED=1
GLOWUPAI_ADMIN_TOKEN=production_token_here
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=production_photo_key_here
```

---

## Troubleshooting by Variable

### Health Check Fails
**Check:**
- `DATABASE_URL` exists (Railway auto-injects)
- PostgreSQL service is running

### CORS Errors
**Check:**
- `GLOWUPAI_ALLOWED_ORIGINS` includes your frontend domain
- No trailing slashes
- No spaces after commas
- HTTPS matches (http vs https)

### 401 Unauthorized
**Check:**
- `GLOWUPAI_AUTH_REQUIRED` is 0 for initial testing
- `GLOWUPAI_FIREBASE_PROJECT_ID` matches your Firebase project
- Frontend sends `Authorization: Bearer <token>` header

### Photos Disappear
**Check:**
- `GLOWUPAI_PHOTO_DIR=/data/photos` is set
- Railway volume is mounted at `/data/photos`
- `GLOWUPAI_PHOTO_KEY` is set and valid base64

### Gemini Features Fail
**Check:**
- `GEMINI_API_KEY` is set
- Key is valid (test at https://ai.google.dev/)
- `GLOWUPAI_GEMINI_ENABLED=1`
- API quota not exceeded

### Admin Routes Return 403
**Check:**
- `GLOWUPAI_ADMIN_TOKEN` is set
- Request includes `X-Admin-Token` header
- Token matches exactly (no spaces)

---

## Quick Reference Card

**Paste this into Railway Variables tab:**

```bash
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com
GEMINI_API_KEY=your_gemini_key_here
GLOWUPAI_ADMIN_TOKEN=your_random_token_here
GLOWUPAI_AUTH_REQUIRED=0
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=your_base64_key_here
GLOWUPAI_RAW_RETENTION_DAYS=730
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
