# 🚂 GlowUp AI - Railway Deployment Guide
## One-Command Backend Deployment

> **Goal**: Deploy FastAPI backend to Railway in under 10 minutes  
> **Cost**: ~$5-10/month (PostgreSQL + hosting)

---

## 🚀 QUICK START (3 Steps):

### Step 1: Install Railway CLI
```bash
# Install
npm install -g @railway/cli

# Or with Homebrew
brew install railway

# Login
railway login
```

### Step 2: Deploy from Backend Directory
```bash
cd /Users/21cabbage/Skinproof/backend

# Initialize Railway project
railway init

# Link to new project or existing
railway link

# Deploy!
railway up
```

### Step 3: Set Environment Variables
```bash
# Add PostgreSQL addon
railway add --plugin postgresql

# Set variables (Railway CLI)
railway variables set SKINPROOF_ENV=production
railway variables set SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
railway variables set SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
railway variables set SKINPROOF_MODEL_VERSION=deterministic-3.0
railway variables set SKINPROOF_POLICY_VERSION=2026-01
railway variables set SKINPROOF_AUTH_REQUIRED=0  # Start with 0, enable after Android wired up
railway variables set SKINPROOF_ALLOWED_ORIGINS="https://yourdomain.com,https://www.yourdomain.com"

# Generate and set admin token
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
# Copy output and:
railway variables set SKINPROOF_ADMIN_TOKEN="<paste-here>"

# Optional: Gemini API key (for shelf-scan + Q&A)
railway variables set GEMINI_API_KEY="<your-key>"
railway variables set SKINPROOF_GEMINI_ENABLED=1
```

**Done!** Your backend is live at `https://your-app.railway.app`

---

## 📋 DETAILED SETUP

### Environment Variables Checklist:

#### Required (P0 - Must Have):
- [ ] `DATABASE_URL` - ✅ Auto-set by Railway PostgreSQL addon
- [ ] `SKINPROOF_ENV=production`
- [ ] `SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7`
- [ ] `SKINPROOF_DISABLE_LEGACY_KEY_FILE=1`
- [ ] `SKINPROOF_ALLOWED_ORIGINS` - Comma-separated list
- [ ] `SKINPROOF_ADMIN_TOKEN` - Long random secret

#### Important (P1 - Should Have):
- [ ] `SKINPROOF_AUTH_REQUIRED=1` - Enable after Android sends tokens
- [ ] `GEMINI_API_KEY` - For shelf-scan OCR and Q&A
- [ ] `SKINPROOF_GEMINI_ENABLED=1`
- [ ] `SKINPROOF_PHOTO_DIR=/data/photos` - If using volume storage
- [ ] `SKINPROOF_PHOTO_KEY` - Base64-encoded 32-byte key

#### Optional (P2 - Nice to Have):
- [ ] `SKINPROOF_MODEL_VERSION=deterministic-3.0`
- [ ] `SKINPROOF_POLICY_VERSION=2026-01`
- [ ] `SKINPROOF_RAW_RETENTION_DAYS=730`
- [ ] `SKINPROOF_DB_POOL_MAX_SIZE=10`

---

## 🗄️ PostgreSQL Setup:

### Option 1: Railway Addon (Recommended)
```bash
railway add --plugin postgresql
```

This automatically:
- ✅ Creates PostgreSQL 15 instance
- ✅ Sets `DATABASE_URL` environment variable
- ✅ Configures connection pooling
- ✅ Enables automated backups

### Option 2: External PostgreSQL
Use Neon, Supabase, or RDS:
```bash
railway variables set DATABASE_URL="postgresql://user:pass@host:5432/dbname"
```

### Verify Database:
```bash
# Check connection
railway run python -c "from skinproof.complete_db import build_full_database; from skinproof.config import Settings; db = build_full_database(Settings.from_env()); print('DB connected!')"
```

---

## 📦 Photo Storage Options:

### Option 1: In-Memory (Development Only)
**Don't use in production!** Photos lost on restart.

No config needed - this is the default.

### Option 2: Railway Volume (Simple)
```bash
# Create volume
railway volume create --name photos --mount-path /data/photos

# Set environment
railway variables set SKINPROOF_PHOTO_DIR=/data/photos
railway variables set SKINPROOF_PHOTO_KEY=$(python3 -c 'import base64,secrets; print(base64.b64encode(secrets.token_bytes(32)).decode())')
```

**Pros**: Simple, included in Railway  
**Cons**: Not scalable (single instance only)

### Option 3: S3/GCS (Production Recommended)
**Coming soon** - Need to implement S3 PhotoStore backend

For now, use Railway Volume and plan to migrate to S3 later.

---

## 🔒 Security Checklist:

- [ ] `SKINPROOF_ENV=production` (disables debug features)
- [ ] `SKINPROOF_DISABLE_LEGACY_KEY_FILE=1` (no local key file)
- [ ] Strong `SKINPROOF_ADMIN_TOKEN` (32+ chars)
- [ ] Specific `SKINPROOF_ALLOWED_ORIGINS` (not wildcard)
- [ ] `SKINPROOF_AUTH_REQUIRED=1` (after Android ready)
- [ ] Photo encryption key set (`SKINPROOF_PHOTO_KEY`)
- [ ] PostgreSQL SSL enabled (Railway default)
- [ ] Secrets not in git (use Railway dashboard)

---

## 🏥 Health Checks:

Railway will auto-configure health checks. Verify:

```bash
# Get your deployment URL
railway status

# Test health endpoint
curl https://your-app.railway.app/api/health
```

Expected response:
```json
{
  "status": "ok",
  "database": "postgresql",
  "database_ready": true,
  "version": "3.0.0",
  "scope": "cosmetic_tracking"
}
```

---

## 📈 Monitoring Setup:

### Railway Dashboard:
- CPU usage
- Memory usage
- Request count
- Error rate

### Add Sentry (Optional):
```bash
pip install sentry-sdk

# In complete_api.py
import sentry_sdk
sentry_sdk.init(dsn="your-dsn")
```

### Add UptimeRobot (Free):
1. Go to uptimerobot.com
2. Add monitor: `https://your-app.railway.app/api/health`
3. Check interval: 5 minutes
4. Email alert on down

---

## 🚀 Deployment Commands:

### Initial Deploy:
```bash
cd backend
railway up
```

### Update Deployment:
```bash
git push  # If using GitHub integration
# OR
railway up  # Direct deploy
```

### View Logs:
```bash
railway logs
```

### Check Status:
```bash
railway status
```

### Open in Browser:
```bash
railway open
```

---

## 🐛 Troubleshooting:

### Build Fails:
```bash
# Check logs
railway logs --build

# Common issue: Missing dependencies
# Fix: Ensure requirements in pyproject.toml
```

### Database Connection Error:
```bash
# Verify DATABASE_URL is set
railway variables

# Test connection locally
export DATABASE_URL="<from-railway>"
python -c "from sqlalchemy import create_engine; create_engine(DATABASE_URL).connect()"
```

### Health Check Fails:
```bash
# Check if app is running
railway logs

# Test locally with same env
railway run uvicorn skinproof.complete_api:app --host 0.0.0.0
```

### Photos Not Persisting:
- Make sure SKINPROOF_PHOTO_DIR is set
- Verify volume is mounted
- Check SKINPROOF_PHOTO_KEY is set

---

## 💰 Cost Estimate:

### Railway Pricing (as of 2026):
- **Hobby Plan**: $5/month
  - 500 hours
  - 8GB RAM
  - 100GB bandwidth

- **Pro Plan**: $20/month
  - Unlimited hours
  - More resources
  - Better support

### PostgreSQL:
- **Railway PostgreSQL**: ~$5-10/month
  - 1GB storage
  - Automated backups

### Total: ~$10-15/month to start

---

## 🔄 CI/CD Setup (Optional):

### GitHub Actions:
```yaml
# .github/workflows/deploy.yml
name: Deploy to Railway
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: npm i -g @railway/cli
      - run: railway up --service backend
        env:
          RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}
```

Get token: `railway login --token`

---

## ✅ Post-Deployment Checklist:

- [ ] Health check returns 200 OK
- [ ] PostgreSQL connected (check logs)
- [ ] Can create user via API
- [ ] Can upload capture via API
- [ ] Photos are encrypted and persist
- [ ] Firebase token verification works
- [ ] CORS configured (Android can connect)
- [ ] Admin routes protected
- [ ] Monitoring set up
- [ ] Backup verified (PostgreSQL)

---

## 📱 Connect Android App:

Update Android app's release build config:
```kotlin
// app/build.gradle.kts
release {
    buildConfigField("String", "API_BASE_URL", "\"https://your-app.railway.app/api/\"")
}
```

Build release APK:
```bash
./gradlew :app:assembleRelease -PRELEASE_API_BASE_URL=https://your-app.railway.app/api/
```

---

## 🎉 You're Live!

Your GlowUp AI backend is now:
- ✅ Deployed to Railway
- ✅ Connected to PostgreSQL
- ✅ Encrypted photo storage
- ✅ Firebase auth ready
- ✅ Monitored and backed up

**Next**: Build release APK and test end-to-end!

---

## 🆘 Need Help?

- Railway Docs: https://docs.railway.app
- GlowUp AI Issues: Create issue on GitHub
- Backend logs: `railway logs`
- Support: support@glowup.ai (once set up)
