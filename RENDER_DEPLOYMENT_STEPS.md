# GlowUp AI Backend - Render Deployment Guide

**Estimated Time: 10 minutes**

This guide will walk you through deploying the GlowUp AI backend to Render's free tier. All code is already committed and pushed to GitHub.

---

## Prerequisites

- GitHub account with access to: https://github.com/piyushxpc7/GlowupAI
- A Gemini API key (get one at https://aistudio.google.com/apikey)

---

## Part 1: Sign Up and Connect GitHub (2 minutes)

### Step 1: Create Render Account

1. Go to https://render.com
2. Click **"Get Started"** or **"Sign Up"**
3. Select **"Sign up with GitHub"**
4. Authorize Render to access your GitHub account
5. Complete your profile if prompted

---

## Part 2: Deploy from Dashboard (5 minutes)

### Step 2: Create New Blueprint

1. Once logged in, click **"New +"** button in the top right
2. Select **"Blueprint"** from the dropdown menu
3. In the repository list, find and click **"piyushxpc7/GlowupAI"**
   - If you don't see it, click **"Configure account"** and grant access to the repository
4. Click **"Connect"**

### Step 3: Configure Blueprint

Render will automatically detect the `render.yaml` file in the `backend/` directory.

1. You'll see a preview showing:
   - **Web Service**: `glowupai-backend`
   - **Database**: `glowupai-db` (PostgreSQL)

2. **Service Group Name**: Leave as default or rename to something memorable (e.g., "glowupai-production")

3. Click **"Apply"** at the bottom

### Step 4: Set Environment Variables

Before the deployment starts, you need to add your Gemini API key:

1. While on the Blueprint deployment screen, find the **"glowupai-backend"** service
2. Click on **"Environment"** or **"Environment Variables"**
3. Add the following REQUIRED environment variables:

   ```
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```

4. OPTIONAL but RECOMMENDED variables (add these for production):

   ```
   SKINPROOF_ALLOWED_ORIGINS=https://your-frontend-domain.com
   SKINPROOF_AUTH_REQUIRED=0
   SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
   ```

5. Click **"Save Changes"**

### Step 5: Deploy

1. Click **"Apply"** or **"Create Resources"**
2. Render will now:
   - Create a PostgreSQL database
   - Build your Docker container
   - Deploy the web service
   - This takes about 5-7 minutes for the first deployment

3. Watch the build logs:
   - Click on the **"glowupai-backend"** service to see live logs
   - You should see Docker build steps, then "uvicorn" starting

---

## Part 3: Verify Deployment (2 minutes)

### Step 6: Get Your Deployed URL

1. On your service dashboard, look for the URL at the top
2. It will be in the format: `https://glowupai-backend-XXXX.onrender.com`
3. **Save this URL** - this is your production API endpoint

### Step 7: Test the Health Endpoint

1. Open a new browser tab
2. Visit: `https://glowupai-backend-XXXX.onrender.com/api/health`
3. You should see a JSON response like:

   ```json
   {
     "status": "ok",
     "timestamp": "2026-08-31T12:34:56.789Z",
     "version": "deterministic-3.0"
   }
   ```

4. If you see this, YOUR BACKEND IS LIVE!

### Step 8: Test the API Documentation

1. Visit: `https://glowupai-backend-XXXX.onrender.com/docs`
2. You should see the FastAPI Swagger UI with all API endpoints
3. Try the `/api/health` endpoint from the interactive docs

---

## Your Render Configuration (render.yaml)

Here's what gets deployed (from `backend/render.yaml`):

```yaml
services:
  - type: web
    name: glowupai-backend
    env: docker
    dockerfilePath: ./Dockerfile
    dockerContext: .
    plan: free
    healthCheckPath: /api/health
    envVars:
      - key: SKINPROOF_FIREBASE_PROJECT_ID
        value: glowup-ai-38ae7
      - key: SKINPROOF_PHOTOS_DIR
        value: /app/photos
      - key: SKINPROOF_RATE_LIMIT_ENABLED
        value: "0"
      - key: SKINPROOF_SKIP_QUALITY_CHECKS
        value: "1"
      - key: DATABASE_URL
        fromDatabase:
          name: glowupai-db
          property: connectionString

databases:
  - name: glowupai-db
    plan: free
    databaseName: glowupai
    user: glowupai
```

**What this means:**
- **Web Service**: Runs your FastAPI backend in a Docker container
- **Free Plan**: 750 hours/month (enough for 24/7 operation)
- **Health Checks**: Render pings `/api/health` to ensure service is running
- **Database**: PostgreSQL (free tier: 90-day data retention, then deleted)
- **Auto-Connect**: Database URL is automatically injected as `DATABASE_URL`

---

## How to Update Environment Variables

### After Deployment:

1. Go to your Render Dashboard: https://dashboard.render.com
2. Click on **"glowupai-backend"** service
3. Click **"Environment"** in the left sidebar
4. Click **"Add Environment Variable"**
5. Add key-value pairs:
   - For example: `SKINPROOF_AUTH_REQUIRED=1`
6. Click **"Save Changes"**
7. Service will automatically redeploy (takes 2-3 minutes)

### Important Environment Variables:

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | YES | Your Google Gemini API key |
| `DATABASE_URL` | AUTO | Auto-injected by Render from database |
| `SKINPROOF_ALLOWED_ORIGINS` | RECOMMENDED | Frontend domain (e.g., `https://app.glowup.ai`) |
| `SKINPROOF_AUTH_REQUIRED` | NO | Set to `1` to enforce Firebase auth |
| `SKINPROOF_FIREBASE_PROJECT_ID` | AUTO | Already set in render.yaml |
| `SKINPROOF_ADMIN_TOKEN` | NO | Random string to enable admin routes |

---

## Troubleshooting

### Issue 1: Build Fails

**Symptom**: Build logs show errors during Docker build

**Solutions**:
1. Check that your `Dockerfile` is in `backend/` directory
2. Verify all code is committed and pushed to GitHub
3. Check build logs for specific error messages
4. Common fix: Ensure `pyproject.toml` and `README.md` exist in backend/

### Issue 2: Service Won't Start

**Symptom**: Build succeeds but service crashes on startup

**Solutions**:
1. Check **Logs** tab for error messages
2. Most common: Missing `GEMINI_API_KEY`
3. Verify database connection: Look for "DATABASE_URL" in environment
4. Check health endpoint is responding: `/api/health`

### Issue 3: Health Check Failing

**Symptom**: Service shows as "Unhealthy" in dashboard

**Solutions**:
1. Verify `/api/health` endpoint exists in your code
2. Check that PORT environment variable is set (auto-set by Render)
3. Look at logs: Search for "uvicorn" startup messages
4. Check firewall/CORS settings aren't blocking health checks

### Issue 4: Database Connection Error

**Symptom**: Logs show "database connection failed"

**Solutions**:
1. Verify `glowupai-db` database was created
2. Check that `DATABASE_URL` appears in Environment tab
3. Database takes 2-3 minutes to provision on first deploy
4. Free tier databases sleep after 90 days of inactivity

### Issue 5: "Free Instance Will Spin Down"

**Symptom**: Warning about instance spinning down

**Explanation**: 
- Free tier instances sleep after 15 minutes of inactivity
- First request after sleep takes 30-60 seconds to wake up
- This is normal for Render free tier
- Upgrade to paid plan ($7/month) for always-on service

### Issue 6: 502 Bad Gateway

**Symptom**: Browser shows "502 Bad Gateway"

**Solutions**:
1. Service is likely still deploying (check dashboard)
2. Service may have crashed (check logs)
3. Wait 1-2 minutes and refresh
4. Check service status indicator (green = healthy)

### Issue 7: CORS Errors from Frontend

**Symptom**: Frontend can't connect, browser console shows CORS error

**Solutions**:
1. Add frontend domain to `SKINPROOF_ALLOWED_ORIGINS`
2. Format: `https://yourdomain.com` (no trailing slash)
3. Multiple origins: `https://app.glowup.ai,https://www.glowup.ai`
4. Save and wait for auto-redeploy

---

## Free Tier Limitations

Be aware of these Render free tier limits:

| Resource | Limit |
|----------|-------|
| **Uptime** | 750 hours/month (31 days = 744 hours) |
| **Inactivity** | Spins down after 15 minutes, cold start ~30s |
| **Database** | 1GB storage, 90-day data retention |
| **Bandwidth** | 100GB/month |
| **Build Minutes** | 500 minutes/month |
| **Concurrent Builds** | 1 at a time |

**If you exceed limits**: Service will be paused. Upgrade to paid plan.

---

## Updating Your Code

### To Deploy Changes:

1. Make changes to your code locally
2. Commit and push to GitHub:
   ```bash
   git add .
   git commit -m "Your change description"
   git push
   ```
3. Render auto-deploys on every push to `main` branch
4. Watch deployment progress in Render dashboard
5. Takes 3-5 minutes per deployment

### To Disable Auto-Deploy:

1. Go to service settings
2. Find **"Auto-Deploy"** toggle
3. Turn it off
4. Manually deploy by clicking **"Manual Deploy"** > **"Deploy latest commit"**

---

## Monitoring Your Service

### View Logs:

1. Dashboard > **"glowupai-backend"** > **"Logs"** tab
2. Real-time logs stream here
3. Search logs with Ctrl+F
4. Download logs with **"Download"** button

### View Metrics:

1. Dashboard > **"glowupai-backend"** > **"Metrics"** tab
2. See CPU, memory, and request stats
3. Free tier has limited metrics retention

### Set Up Alerts:

1. Dashboard > **"glowupai-backend"** > **"Settings"**
2. Scroll to **"Health Check"** section
3. Add email for deployment notifications
4. Render emails you when deploy succeeds/fails

---

## Quick Reference

### Your URLs:

- **API Base**: `https://glowupai-backend-XXXX.onrender.com`
- **Health Check**: `/api/health`
- **API Docs**: `/docs`
- **Admin Panel** (if enabled): `/api/admin/*`

### Key Commands (for reference):

```bash
# Local development
cd ~/GlowupAI/backend
source venv/bin/activate
uvicorn skinproof.api:app --reload --port 8000

# Test local health
curl http://localhost:8000/api/health

# Test production health
curl https://glowupai-backend-XXXX.onrender.com/api/health
```

---

## Next Steps

After successful deployment:

1. **Update Frontend**: Point your app to the new Render URL
2. **Set CORS**: Add frontend domain to `SKINPROOF_ALLOWED_ORIGINS`
3. **Enable Auth**: Set `SKINPROOF_AUTH_REQUIRED=1` when ready
4. **Monitor**: Check logs daily for first week
5. **Backup**: Export database regularly (Render free tier = 90-day retention)

---

## Support Resources

- **Render Docs**: https://render.com/docs
- **Render Status**: https://status.render.com
- **Community Forum**: https://community.render.com
- **GlowUp AI GitHub**: https://github.com/piyushxpc7/GlowupAI

---

## Success Checklist

Before considering deployment complete:

- [ ] Service shows as "Live" in Render dashboard
- [ ] Health endpoint returns 200 OK: `/api/health`
- [ ] API docs load successfully: `/docs`
- [ ] Database connection is working (check logs for "Connected to database")
- [ ] Gemini API key is set (test an AI endpoint)
- [ ] Frontend can reach backend (no CORS errors)
- [ ] Logs show no error messages
- [ ] You've saved your deployment URL

---

## Estimated Costs

**Free Tier**: $0/month
- Includes everything you need to start
- Perfect for MVP and testing
- Limitations listed above

**Upgrade Options**:
- **Starter Plan**: $7/month (always-on, no spin-down)
- **Database Plan**: $7/month (persistent storage, no 90-day limit)
- **Total for small production**: ~$14/month

---

**Deployment completed successfully?** Update your frontend `.env` with the new backend URL and you're ready to go live!

---

**Created**: 2026-08-31
**Last Updated**: 2026-08-31
**Deployment Platform**: Render.com (Free Tier)
**GitHub Repository**: https://github.com/piyushxpc7/GlowupAI
