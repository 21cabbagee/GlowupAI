# 🚀 Deploy Backend to Render (FREE Tier)

**Status**: render.yaml created and pushed ✅  
**Time to Deploy**: 10 minutes  
**Cost**: $0/month (free tier)

---

## ✅ WHAT'S READY:

- ✅ `backend/render.yaml` configured
- ✅ Dockerfile ready
- ✅ GitHub repo: https://github.com/piyushxpc7/GlowupAI
- ✅ Everything committed and pushed

---

## 🎯 STEP 1: CREATE RENDER ACCOUNT (2 min)

1. **Go to Render**: https://render.com/
2. **Sign up with GitHub** (click "Sign Up")
3. **Authorize Render** to access your GitHub repos

---

## 🚀 STEP 2: DEPLOY FROM RENDER DASHBOARD (5 min)

### Option A: Blueprint Deploy (Easiest - One Click)

1. **Go to**: https://dashboard.render.com/select-repo?type=blueprint

2. **Connect your repository**:
   - Select: `piyushxpc7/GlowupAI`
   - Click "Connect"

3. **Render will detect `backend/render.yaml`**:
   - Service name: `glowupai-backend`
   - Database: `glowupai-db` (PostgreSQL free tier)
   - Click "Apply"

4. **Wait for deployment** (3-5 minutes):
   - Watch the build logs
   - Wait for "Live" status

5. **Get your URL**:
   - Will be: `https://glowupai-backend.onrender.com`
   - Copy this URL!

---

### Option B: Manual Deploy (If Blueprint Doesn't Work)

1. **Create Database First**:
   - Dashboard → New → PostgreSQL
   - Name: `glowupai-db`
   - Plan: Free
   - Create Database
   - Copy the "Internal Database URL"

2. **Create Web Service**:
   - Dashboard → New → Web Service
   - Connect Repository: `piyushxpc7/GlowupAI`
   - Name: `glowupai-backend`
   - Runtime: Docker
   - Branch: `main`
   - Root Directory: `backend`
   - Plan: Free

3. **Add Environment Variables**:
   ```
   SKINPROOF_FIREBASE_PROJECT_ID=glowup-5e9ec
   SKINPROOF_PHOTOS_DIR=/app/photos
   SKINPROOF_RATE_LIMIT_ENABLED=0
   DATABASE_URL=[paste Internal Database URL]
   ```

4. **Set Health Check**:
   - Health Check Path: `/api/health`

5. **Deploy**:
   - Click "Create Web Service"
   - Wait 3-5 minutes

---

## 🎯 STEP 3: VERIFY DEPLOYMENT (1 min)

Once deployed, test your backend:

```bash
curl https://glowupai-backend.onrender.com/api/health
```

**Expected response**:
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

---

## 📱 STEP 4: UPDATE ANDROID APP (5 min)

### Update the API URL:

**File**: `app/build.gradle.kts`

**Find** (around line 125):
```kotlin
staging {
    buildConfigField("String", "API_BASE_URL", "\"$stagingApiBaseUrl\"")
}
```

**Add to** `gradle.properties`:
```properties
STAGING_API_BASE_URL=https://glowupai-backend.onrender.com/api/
RELEASE_API_BASE_URL=https://glowupai-backend.onrender.com/api/
```

### Rebuild APK:
```bash
cd ~/GlowupAI
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew clean assembleRelease
```

### Test:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🚨 RENDER FREE TIER LIMITATIONS

### What You Get (FREE):
- ✅ 750 hours/month (enough for 24/7)
- ✅ Auto-deploy from GitHub
- ✅ Free SSL certificate (HTTPS)
- ✅ PostgreSQL database (free tier)
- ✅ 100GB bandwidth/month

### Limitations:
- ⚠️ **Spins down after 15 min of inactivity**
  - First request after inactivity = 30-60 second delay
  - Subsequent requests = normal speed
  
- ⚠️ **Database**: 1GB storage, 97 connection limit

- ⚠️ **No custom domain** on free tier
  - You get: `your-app.onrender.com`
  - Custom domain requires paid plan ($7/month)

---

## 🔥 WORKAROUND FOR SPIN-DOWN (Optional)

### Option A: Ping Service (Keep Alive)

Use a free service to ping your backend every 14 minutes:

1. **Go to**: https://cron-job.org/en/
2. **Create free account**
3. **Add cronjob**:
   - URL: `https://glowupai-backend.onrender.com/api/health`
   - Interval: Every 14 minutes
   - Save

**Result**: Your backend stays awake 24/7 on free tier!

### Option B: Upgrade to Paid ($7/month)

- No spin-down
- Faster performance
- Custom domain support
- Priority support

---

## ✅ SUCCESS CHECKLIST

After deployment:

- [ ] Render account created
- [ ] Backend deployed successfully
- [ ] Health check passes
- [ ] Database connected (PostgreSQL)
- [ ] URL copied: `https://glowupai-backend.onrender.com`
- [ ] Android app updated with new URL
- [ ] APK rebuilt and tested
- [ ] Authentication works
- [ ] (Optional) Cron-job.org set up for keep-alive

---

## 🐛 TROUBLESHOOTING

### Issue: "Deploy Failed"

**Check logs**:
- Dashboard → Your Service → Logs
- Look for error messages

**Common fixes**:
1. Make sure `render.yaml` is in `backend/` directory
2. Check Dockerfile exists at `backend/Dockerfile`
3. Verify environment variables are set

### Issue: "Database Connection Failed"

**Fix**:
1. Go to Database settings
2. Copy "Internal Database URL"
3. Paste into `DATABASE_URL` env var
4. Redeploy service

### Issue: "App Can't Connect"

**Check**:
1. URL ends with `/api/` not just `/`
   - ✅ `https://glowupai-backend.onrender.com/api/`
   - ❌ `https://glowupai-backend.onrender.com/`

2. Rebuild APK after changing URL:
   ```bash
   ./gradlew clean assembleRelease
   ```

### Issue: "First Request Takes Forever"

**This is normal on free tier**:
- Service spins down after 15 min inactivity
- First request wakes it up (30-60 seconds)
- Use cron-job.org to keep it awake (see above)

---

## 💰 UPGRADE TO PAID (When Ready)

**When you should upgrade**:
- You have 100+ daily active users
- Spin-down is hurting UX
- You need custom domain
- You're generating revenue

**Cost**: $7/month for web service + $7/month for database = $14/month total

**To upgrade**:
- Dashboard → Your Service → Settings
- Change Plan → Starter ($7/month)
- Same for database

---

## 🎯 NEXT STEPS

Once Render is deployed:

### Today:
1. ✅ Deploy to Render
2. ✅ Update Android app
3. ✅ Test end-to-end
4. ✅ Set up cron-job.org (optional)

### This Week:
- Follow `APP_TESTING_POLISH_PLAN.md`
- Test all features thoroughly
- Fix any bugs found

### Next Week:
- Soft launch to 20 users
- Build landing page
- Prep Product Hunt launch

---

## 📞 HELP

**Render Docs**: https://render.com/docs  
**Render Status**: https://status.render.com/  
**Render Support**: https://render.com/support

**Common Questions**:
- "How much does Render cost?" → Free for hobbyists, $7/mo for production
- "Will it scale?" → Yes, up to thousands of users before needing upgrade
- "Can I switch to Railway later?" → Yes, both support Docker

---

## ✅ YOU'RE READY!

**Current Status**:
- ✅ Backend running locally (for testing)
- ✅ `render.yaml` ready (for deployment)
- ✅ GitHub repo pushed
- ✅ APK built

**Next Action**: Deploy on Render (follow Step 1-4 above)

**Time Required**: 10 minutes

**Let's ship this!** 🚀
