# Staging Deployment Guide

## Current Status
- **Repository**: https://github.com/piyushxpc7/GlowupAI
- **Staging Branch**: `staging` (pushed)
- **Production URL**: https://glowupai-20ca.onrender.com
- **Production Health**: ✅ Healthy (200 OK)

## Staging Configuration
- **Config File**: `render-staging.yaml`
- **Service Name**: `glowupai-backend-staging`
- **Branch**: `staging`
- **Environment**: Docker
- **Health Check**: `/api/health`

## Key Differences from Production
- Rate limiting: **ENABLED** (production has it disabled)
- Quality checks: **ENABLED** (production skips them)
- Separate database: `glowupai-db-staging`
- Environment variable: `ENVIRONMENT=staging`

## Deployment Options

### Option 1: Render Dashboard (Recommended)
1. Go to https://dashboard.render.com
2. Click "New +" → "Web Service"
3. Connect to repository: `piyushxpc7/GlowupAI`
4. Configure:
   - **Name**: `glowupai-backend-staging`
   - **Branch**: `staging`
   - **Root Directory**: `backend`
   - **Environment**: Docker
   - **Dockerfile Path**: `./Dockerfile`
   - **Plan**: Free
5. Add Environment Variables (from render-staging.yaml):
   - `GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7`
   - `GLOWUPAI_PHOTOS_DIR=/app/photos`
   - `GLOWUPAI_RATE_LIMIT_ENABLED=1`
   - `GLOWUPAI_SKIP_QUALITY_CHECKS=0`
   - `ENVIRONMENT=staging`
6. Create Database:
   - Click "New +" → "PostgreSQL"
   - Name: `glowupai-db-staging`
   - Plan: Free
   - Link to web service
7. Click "Create Web Service"

### Option 2: Render Blueprint
1. Go to https://dashboard.render.com
2. Click "New +" → "Blueprint"
3. Select repository: `piyushxpc7/GlowupAI`
4. Branch: `staging`
5. Blueprint file: `backend/render-staging.yaml`
6. Click "Apply"

### Option 3: Deploy Hook (Automated)
Once the staging service is created, get the deploy hook URL:
1. Go to service settings
2. Find "Deploy Hook" URL
3. Use: `curl -X POST <DEPLOY_HOOK_URL>`

## Post-Deployment Verification

### 1. Health Check
```bash
curl https://glowupai-backend-staging.onrender.com/api/health
```
Expected: `{"status":"healthy","checks":{"database":{"status":"healthy"}}}`

### 2. Test New Endpoints
```bash
# Test analytics endpoint
curl https://glowupai-backend-staging.onrender.com/api/analytics/summary

# Test feedback endpoint
curl -X POST https://glowupai-backend-staging.onrender.com/api/feedback \
  -H "Content-Type: application/json" \
  -d '{"rating":5,"comment":"test"}'
```

### 3. Check Rate Limiting
```bash
# Should return 429 after rate limit exceeded
for i in {1..100}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    https://glowupai-backend-staging.onrender.com/api/health
done
```

### 4. Monitor Logs
```bash
# View deployment logs in Render dashboard
# Check for:
# - Successful Docker build
# - Database migrations completed
# - Service started successfully
# - Health check passing
```

## Deployment Checklist
- [x] All changes committed
- [x] Staging branch created
- [x] Staging branch pushed to GitHub
- [x] Staging configuration created (render-staging.yaml)
- [ ] Staging service created on Render
- [ ] Database migrations run
- [ ] Health check passing
- [ ] New endpoints responding correctly
- [ ] Rate limiting active
- [ ] Analytics tracking works
- [ ] No 500 errors in logs

## Rollback Plan
If deployment fails:
1. Check deployment logs in Render dashboard
2. Verify environment variables are set correctly
3. Check database connectivity
4. Review recent commits for breaking changes
5. If needed, revert to previous commit:
   ```bash
   git revert HEAD
   git push origin staging
   ```

## Next Steps
1. Create staging service on Render
2. Wait for initial deployment (~5-10 minutes)
3. Run verification tests
4. Monitor for 15 minutes to ensure stability
5. If successful, merge staging to main for production deployment
