# GlowUp AI Backend - Railway Deployment Guide

## One-Click Deploy Checklist

Follow these steps in order for a successful deployment:

- [ ] Railway account created
- [ ] Railway project created
- [ ] PostgreSQL database provisioned
- [ ] All environment variables configured
- [ ] Photo storage solution chosen and configured
- [ ] Domain configured (if custom domain needed)
- [ ] First deployment triggered
- [ ] Health check verified
- [ ] Database migrations confirmed
- [ ] CORS tested from frontend

---

## Step 1: Create Railway Project

### 1.1 Sign up / Log in
Go to [railway.app](https://railway.app) and create an account or log in.

### 1.2 Create New Project
```bash
# Option A: Using Railway CLI (recommended)
npm install -g @railway/cli
railway login
cd /Users/21cabbage/GlowupAI/backend
railway init

# Option B: Using Railway Dashboard
# Click "New Project" -> "Deploy from GitHub repo" -> Select your repository
```

---

## Step 2: Provision PostgreSQL Database

### 2.1 Add PostgreSQL to Project
In your Railway project dashboard:
1. Click "New" -> "Database" -> "Add PostgreSQL"
2. Railway will automatically create `DATABASE_URL` environment variable
3. The database will be accessible within your private network

### 2.2 Verify Database Connection
Railway automatically injects `DATABASE_URL` in this format:
```
postgresql://postgres:PASSWORD@REGION.railway.app:PORT/railway
```

You don't need to manually set this - Railway handles it automatically.

---

## Step 3: Configure Environment Variables

### 3.1 Required Variables (MUST SET)

Navigate to your service settings -> Variables tab and add:

```bash
# Firebase Authentication (REQUIRED)
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7

# Environment Mode (REQUIRED)
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1

# Gemini API for OCR and Q&A (REQUIRED for shelf scan and insights)
GEMINI_API_KEY=your_gemini_api_key_here

# CORS - Your Production Frontend Domain (REQUIRED)
GLOWUPAI_ALLOWED_ORIGINS=https://your-production-domain.com,https://www.your-production-domain.com
```

### 3.2 Authentication & Security

```bash
# Firebase Auth
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_AUTH_REQUIRED=1                    # Set to 1 once frontend sends tokens

# Admin Routes Protection
GLOWUPAI_ADMIN_TOKEN=<generate-long-random-token>
```

Generate a secure admin token:
```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### 3.3 Gemini Configuration

```bash
GEMINI_API_KEY=<your-api-key>
GLOWUPAI_GEMINI_MODEL=gemini-3.5-flash-lite
GLOWUPAI_GEMINI_ENABLED=1
```

Get your Gemini API key from: https://ai.google.dev/

### 3.4 Database Pool Settings (Optional - Defaults are good)

```bash
GLOWUPAI_DB_POOL_MIN_SIZE=1
GLOWUPAI_DB_POOL_MAX_SIZE=10
GLOWUPAI_DB_CONNECT_TIMEOUT=10
```

### 3.5 Photo Storage Configuration

**Option A: Railway Volume (Recommended for MVP)**
```bash
# In Railway Dashboard:
# 1. Go to your service -> Settings -> Volumes
# 2. Click "New Volume" -> Name: "photos" -> Mount path: "/data/photos"
# 3. Add these variables:

GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=<generate-32-byte-base64-key>
GLOWUPAI_RAW_RETENTION_DAYS=730
```

Generate the photo encryption key:
```bash
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

**Option B: In-Memory (Development Only - Photos lost on restart)**
```bash
# Don't set GLOWUPAI_PHOTO_DIR or GLOWUPAI_PHOTO_KEY
# Photos will be stored in memory only
```

**Option C: S3/GCS (Production Recommended)**
For production scale, integrate S3 or Google Cloud Storage:
```bash
# Future enhancement - requires code changes
# See backend/docs/operations.md for storage architecture
```

### 3.6 Model & Policy Versions (Optional - Has good defaults)

```bash
GLOWUPAI_MODEL_VERSION=deterministic-3.0
GLOWUPAI_POLICY_VERSION=2026-01
```

---

## Step 4: Deploy Configuration Files

The repository already includes the necessary files:

### 4.1 Existing Files
- `railway.json` - Railway build and deploy configuration
- `Dockerfile` - Multi-stage Docker build optimized for Railway
- `pyproject.toml` - Python dependencies

### 4.2 Verify railway.json
Your `railway.json` should contain:
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "numReplicas": 1,
    "healthcheckPath": "/api/health",
    "healthcheckTimeout": 30,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 5
  }
}
```

---

## Step 5: Deploy

### 5.1 Deploy from CLI
```bash
cd /Users/21cabbage/GlowupAI/backend
railway up
```

### 5.2 Deploy from GitHub (Recommended)
1. Push your code to GitHub
2. In Railway Dashboard: Settings -> Connect to GitHub repository
3. Railway will auto-deploy on every push to main branch

### 5.3 Monitor Deployment
Watch the build logs in Railway dashboard:
- Build phase: Installing dependencies
- Deploy phase: Starting uvicorn server
- Health checks: Railway pings /api/health every 30s

---

## Step 6: Verify Deployment

### 6.1 Check Health Endpoint
Once deployed, Railway will provide a public URL like:
```
https://your-project.up.railway.app
```

Test the health endpoint:
```bash
curl https://your-project.up.railway.app/api/health
```

Expected response:
```json
{
  "status": "ok",
  "database": "postgres",
  "database_ready": true,
  "version": "3.0.0",
  "scope": "cosmetic_tracking",
  "features": ["experiments", "qna", "discover", "commerce", "reprocessing", "shelf_scan", "product_prediction", "root_cause_search", "budget_optimizer", "derm_export"]
}
```

### 6.2 Verify Database Migrations
Database migrations run automatically on startup. Check logs for:
```
INFO: Running PostgreSQL migrations...
INFO: Applied migration: 0001_initial.sql
INFO: Applied migration: 0002_growth_features.sql
INFO: Applied migration: 0003_checkins_measurement_feedback.sql
INFO: Applied migration: 0004_firebase_identity.sql
```

### 6.3 Test CORS
From your frontend domain, make a test request to ensure CORS is working:
```javascript
fetch('https://your-project.up.railway.app/api/health')
  .then(r => r.json())
  .then(console.log)
```

### 6.4 Test User Creation
```bash
curl -X POST https://your-project.up.railway.app/api/users \
  -H "Content-Type: application/json" \
  -d '{"skin_type": "combination"}'
```

---

## Step 7: Custom Domain (Optional)

### 7.1 Configure Custom Domain
1. Railway Dashboard -> Settings -> Domains
2. Click "Custom Domain"
3. Enter your domain (e.g., api.glowup.com)
4. Add the CNAME record to your DNS provider:
   ```
   CNAME: api -> your-project.up.railway.app
   ```
5. Railway auto-provisions SSL certificate

### 7.2 Update CORS Settings
After setting custom domain, update:
```bash
GLOWUPAI_ALLOWED_ORIGINS=https://app.glowup.com,https://www.glowup.com
```

---

## Step 8: Monitoring & Maintenance

### 8.1 View Logs
```bash
# CLI
railway logs

# Or in Dashboard -> Deployments -> View Logs
```

### 8.2 Database Backups
Railway PostgreSQL includes automatic backups:
- Dashboard -> Database -> Backups
- Configure backup retention and frequency

### 8.3 Scaling
To scale horizontally:
1. Update `railway.json`:
   ```json
   "deploy": {
     "numReplicas": 3
   }
   ```
2. Redeploy

For vertical scaling:
- Settings -> Resources -> Adjust RAM/CPU

### 8.4 Environment Variable Updates
After changing environment variables:
- Railway automatically restarts the service
- No manual redeploy needed

---

## Troubleshooting

### Issue: Health Check Failing
**Symptoms:** Deployment shows unhealthy, service keeps restarting

**Solutions:**
1. Check `DATABASE_URL` is set correctly
2. Verify PostgreSQL service is running
3. Check logs for migration errors:
   ```bash
   railway logs --service backend
   ```
4. Ensure port is not hardcoded (Railway sets PORT automatically)

### Issue: Database Connection Timeout
**Symptoms:** `503 database unavailable` errors

**Solutions:**
1. Check database service is running in same project
2. Verify `DATABASE_URL` format
3. Check database connection pool settings:
   ```bash
   GLOWUPAI_DB_CONNECT_TIMEOUT=30  # Increase timeout
   ```

### Issue: CORS Errors from Frontend
**Symptoms:** Browser console shows CORS policy errors

**Solutions:**
1. Verify `GLOWUPAI_ALLOWED_ORIGINS` includes your frontend domain
2. Check for trailing slashes in domain URLs (don't include them)
3. Ensure both www and non-www versions are listed if needed
4. Check that frontend is using HTTPS if backend is

### Issue: Photos Not Persisting
**Symptoms:** Uploaded photos disappear after restart

**Solutions:**
1. Check Railway Volume is mounted at `/data/photos`
2. Verify `GLOWUPAI_PHOTO_DIR=/data/photos` is set
3. Confirm `GLOWUPAI_PHOTO_KEY` is set with valid base64 key
4. Check volume mount in Settings -> Volumes

### Issue: Gemini Features Not Working
**Symptoms:** Shelf scan fails, Q&A returns errors

**Solutions:**
1. Verify `GEMINI_API_KEY` is set correctly
2. Check Gemini API quota at https://ai.google.dev/
3. Confirm `GLOWUPAI_GEMINI_ENABLED=1`
4. Test API key with curl:
   ```bash
   curl "https://generativelanguage.googleapis.com/v1beta/models?key=YOUR_KEY"
   ```

### Issue: Firebase Auth Failing
**Symptoms:** 401 Unauthorized errors

**Solutions:**
1. Verify `GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7`
2. Ensure frontend is sending Authorization header:
   ```
   Authorization: Bearer <firebase-id-token>
   ```
3. Check if `GLOWUPAI_AUTH_REQUIRED=1` is set (set to 0 for testing)

---

## Security Checklist

- [ ] `GLOWUPAI_ENV=production` is set
- [ ] `GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1` is set
- [ ] `GLOWUPAI_ADMIN_TOKEN` is a long random string (32+ chars)
- [ ] `GLOWUPAI_PHOTO_KEY` is a proper 32-byte base64 key
- [ ] `GEMINI_API_KEY` is not exposed in logs or frontend
- [ ] `GLOWUPAI_ALLOWED_ORIGINS` only includes your production domains
- [ ] `GLOWUPAI_AUTH_REQUIRED=1` once frontend integration is complete
- [ ] Railway environment variables are not exposed in GitHub
- [ ] Database backups are configured
- [ ] SSL/HTTPS is enabled (automatic with Railway)

---

## Environment Variables Quick Reference

### Copy-Paste Template for Railway
```bash
# === REQUIRED ===
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GEMINI_API_KEY=your_gemini_api_key_here
GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com

# === AUTHENTICATION ===
GLOWUPAI_AUTH_REQUIRED=1
GLOWUPAI_ADMIN_TOKEN=your_secure_random_token_here

# === PHOTO STORAGE (choose one) ===
# Option A: Railway Volume
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=your_base64_32byte_key_here
GLOWUPAI_RAW_RETENTION_DAYS=730

# Option B: In-Memory (dev only - leave commented)
# No variables needed

# === OPTIONAL - Good Defaults ===
GLOWUPAI_GEMINI_MODEL=gemini-3.5-flash-lite
GLOWUPAI_GEMINI_ENABLED=1
GLOWUPAI_MODEL_VERSION=deterministic-3.0
GLOWUPAI_POLICY_VERSION=2026-01
GLOWUPAI_DB_POOL_MIN_SIZE=1
GLOWUPAI_DB_POOL_MAX_SIZE=10
GLOWUPAI_DB_CONNECT_TIMEOUT=10

# === AUTOMATICALLY SET BY RAILWAY ===
# DATABASE_URL (from PostgreSQL service)
# PORT (Railway sets this automatically)
```

---

## Cost Estimation

Railway pricing (as of 2024):
- **Hobby Plan:** $5/month + resource usage
  - Suitable for MVP/testing
  - Includes $5 of resource credits
  
- **Resource Costs:**
  - Web service (512MB RAM): ~$5-10/month
  - PostgreSQL (1GB): ~$5/month
  - Volume storage (1GB): ~$0.25/month
  
**Estimated Monthly Cost:** $10-20 for light usage

For production with moderate traffic:
- **Pro Plan:** $20/month + resource usage
- Scale services as needed

---

## Next Steps After Deployment

1. **Frontend Integration**
   - Update frontend API base URL to Railway URL
   - Test all API endpoints from frontend
   - Enable authentication once confirmed working

2. **Monitoring Setup**
   - Set up Railway metrics alerts
   - Configure log retention
   - Set up uptime monitoring (UptimeRobot, Pingdom)

3. **Performance Optimization**
   - Monitor database query performance
   - Adjust connection pool settings based on load
   - Consider adding Redis for caching (future)

4. **Backup Strategy**
   - Verify Railway database backups
   - Export critical data periodically
   - Document recovery procedures

5. **Production Hardening**
   - Enable `GLOWUPAI_AUTH_REQUIRED=1`
   - Set up rate limiting (future enhancement)
   - Configure monitoring alerts
   - Review and rotate secrets regularly

---

## Support Resources

- **Railway Docs:** https://docs.railway.app
- **Railway Discord:** https://discord.gg/railway
- **GlowupAI Backend Docs:** `/Users/21cabbage/GlowupAI/backend/docs/`
- **API Documentation:** https://your-project.up.railway.app/docs (FastAPI auto-generated)

---

## Rollback Procedure

If deployment fails or issues arise:

### Quick Rollback (Railway Dashboard)
1. Go to Deployments
2. Find last working deployment
3. Click "..." menu -> "Redeploy"

### CLI Rollback
```bash
railway status
railway rollback <deployment-id>
```

### Emergency Shutdown
```bash
railway down
```

This will stop the service while you investigate issues.

---

## Success Criteria

Your deployment is successful when:

- [ ] Health endpoint returns 200 OK with `"database": "postgres"`
- [ ] Can create a user via POST /api/users
- [ ] Frontend can make CORS requests successfully
- [ ] Database migrations are applied (check logs)
- [ ] Photos persist after service restart (if using volume)
- [ ] Gemini features work (shelf scan, Q&A)
- [ ] Firebase authentication validates tokens
- [ ] Service restarts automatically on failure
- [ ] Logs show no critical errors

**Congratulations! Your GlowUp AI backend is live.**
