# 🚀 Deploy to Staging NOW - 3 Simple Steps

## Step 1: Open Render Dashboard (30 seconds)
Go to: **https://dashboard.render.com**

## Step 2: Deploy with Blueprint (2 minutes)
1. Click **"New +"** button (top right)
2. Select **"Blueprint"**
3. Choose repository: **piyushxpc7/GlowupAI**
4. Select branch: **staging**
5. Blueprint file will auto-detect: **backend/render-staging.yaml**
6. Click **"Apply"**

✨ Render will automatically:
- Create the web service `glowupai-backend-staging`
- Create the database `glowupai-db-staging`
- Set all environment variables
- Deploy the Docker container
- Run health checks

## Step 3: Wait & Verify (5-10 minutes)
Watch the deployment logs in Render dashboard.

When status shows **"Live"**, run:
```bash
cd /Users/21cabbage/GlowupAI/backend
./scripts/verify-staging.sh
```

## 🎯 What's Being Deployed

### New Features
✅ Analytics tracking system
✅ Rate limiting (protects API from abuse)
✅ Data collection with user consent
✅ ML monitoring and performance tracking
✅ Feedback collection
✅ Enhanced API endpoints

### Testing Improvements
✅ Unit tests
✅ Integration tests
✅ Load tests
✅ Android UI tests
✅ Security scanning

### Configuration
- **Service**: glowupai-backend-staging
- **Database**: PostgreSQL (staging)
- **Branch**: staging (auto-deploy on push)
- **Health Check**: /api/health
- **Environment**: Docker

## 📊 Expected Deployment Time

| Phase | Time | Status |
|-------|------|--------|
| Blueprint Apply | 1 min | Manual |
| Docker Build | 3-5 min | Automatic |
| Database Setup | 1 min | Automatic |
| Service Start | 1-2 min | Automatic |
| Health Check | 30 sec | Automatic |
| **Total** | **6-10 min** | |

## ✅ Success Criteria

Your deployment is successful when:
- [ ] Service status shows "Live"
- [ ] Health check returns 200 OK
- [ ] Database connected
- [ ] No errors in logs
- [ ] Verification script passes

## 🔗 Quick Links

- **Render Dashboard**: https://dashboard.render.com
- **GitHub Staging Branch**: https://github.com/piyushxpc7/GlowupAI/tree/staging
- **Production (for reference)**: https://glowupai-20ca.onrender.com
- **Expected Staging URL**: https://glowupai-backend-staging.onrender.com

## 📚 Need More Details?

- **Quick Start**: See `RENDER_STAGING_QUICKSTART.md`
- **Detailed Guide**: See `STAGING_DEPLOYMENT.md`
- **Full Status**: See `DEPLOYMENT_STATUS.md`

## 🆘 Troubleshooting

### "Blueprint not found"
→ Make sure you selected branch: **staging** (not main)

### "Build failed"
→ Check logs for error messages
→ Verify Dockerfile is in backend/ directory
→ Ensure all dependencies in pyproject.toml

### "Health check failing"
→ Wait 2-3 minutes after "Live" status
→ Render needs time to start the service
→ Check logs for startup errors

### "Database connection failed"
→ Verify database was created
→ Check DATABASE_URL is set
→ Wait for database to be fully provisioned

## 🎉 After Successful Deployment

Test the new features:
```bash
# Test health
curl https://glowupai-backend-staging.onrender.com/api/health

# Test analytics
curl https://glowupai-backend-staging.onrender.com/api/analytics/summary

# Test rate limiting (run multiple times)
for i in {1..50}; do
  curl https://glowupai-backend-staging.onrender.com/api/health
done
```

## 🚀 Next: Production Deployment

Once staging is verified (tested for 15+ minutes with no issues):
```bash
cd /Users/21cabbage/GlowupAI
git checkout main
git merge staging
git push origin main
```

Production will auto-deploy from the main branch.

---

**Ready to deploy?** Go to https://dashboard.render.com and click "New +" → "Blueprint"!
