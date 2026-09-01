# Render Staging Deployment - Quick Start

## ⚡ Quick Deploy (5 minutes)

### Step 1: Go to Render Dashboard
🔗 https://dashboard.render.com

### Step 2: Create Web Service
1. Click **"New +"** → **"Blueprint"**
2. Select repository: **piyushxpc7/GlowupAI**
3. Branch: **staging**
4. Blueprint file: **backend/render-staging.yaml**
5. Click **"Apply"**

That's it! Render will:
- Create `glowupai-backend-staging` web service
- Create `glowupai-db-staging` PostgreSQL database
- Deploy from the `staging` branch
- Set up all environment variables
- Configure health checks

### Step 3: Wait for Deployment
⏱️ First deployment takes ~5-10 minutes
- Watch the logs in Render dashboard
- Wait for "Live" status

### Step 4: Verify Deployment
```bash
# Run the verification script
cd /Users/21cabbage/GlowupAI/backend
./scripts/verify-staging.sh

# Or manually test the health endpoint
curl https://glowupai-backend-staging.onrender.com/api/health
```

Expected response:
```json
{
  "status": "healthy",
  "checks": {
    "database": {
      "status": "healthy",
      "backend": "postgresql"
    }
  },
  "version": "3.0.0"
}
```

## 🔄 Trigger Re-deployment

After pushing new changes to the `staging` branch:

### Option A: Automatic (Render watches the branch)
Render automatically deploys when you push to `staging`. No action needed!

### Option B: Manual Trigger
1. Go to service page in Render dashboard
2. Click **"Manual Deploy"** → **"Deploy latest commit"**

### Option C: Deploy Hook (Automated CI/CD)
1. Get deploy hook URL from service settings
2. Add to GitHub Actions or use manually:
```bash
curl -X POST <YOUR_DEPLOY_HOOK_URL>
```

## 🧪 Testing New Features

### Test Analytics
```bash
curl https://glowupai-backend-staging.onrender.com/api/analytics/summary
```

### Test Feedback
```bash
curl -X POST https://glowupai-backend-staging.onrender.com/api/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test-user",
    "capture_id": "test-capture",
    "rating": 5,
    "comment": "Test feedback",
    "feedback_type": "quality"
  }'
```

### Test Rate Limiting
```bash
# Should eventually return 429 (Too Many Requests)
for i in {1..100}; do
  curl -s -o /dev/null -w "%{http_code} " \
    https://glowupai-backend-staging.onrender.com/api/health
done
```

## 📊 Monitoring

### View Logs
```bash
# In Render dashboard:
# Service → Logs → Select time range
```

### Check Metrics
```bash
# In Render dashboard:
# Service → Metrics
# - CPU usage
# - Memory usage
# - Response time
# - Request count
```

## ⚠️ Troubleshooting

### Service won't start
1. Check logs for errors
2. Verify environment variables are set
3. Check database connection
4. Verify Dockerfile builds locally:
   ```bash
   cd /Users/21cabbage/GlowupAI/backend
   docker build -t test-build .
   docker run -p 8000:8000 test-build
   ```

### Health check failing
1. Check if service is running
2. Verify `/api/health` endpoint exists
3. Check database connectivity
4. Review recent code changes

### Database connection issues
1. Verify database is created
2. Check DATABASE_URL is set correctly
3. Run migrations manually if needed

## 🚀 Promote to Production

Once staging is verified:

```bash
# Option 1: Merge to main
cd /Users/21cabbage/GlowupAI
git checkout main
git merge staging
git push origin main

# Option 2: Create PR for review
gh pr create --base main --head staging \
  --title "Deploy staging to production" \
  --body "Staging verification complete. Ready for production deployment."
```

## 📝 Current Status

- ✅ Staging branch created and pushed
- ✅ Staging configuration (render-staging.yaml) created
- ✅ Deployment guide created
- ✅ Verification script created
- ⏳ **Next: Create staging service on Render**

## 🔗 Useful Links

- Staging Branch: https://github.com/piyushxpc7/GlowupAI/tree/staging
- Production URL: https://glowupai-20ca.onrender.com
- Staging URL (once deployed): https://glowupai-backend-staging.onrender.com
- Render Dashboard: https://dashboard.render.com
