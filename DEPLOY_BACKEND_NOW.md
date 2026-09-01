# 🚀 Deploy Backend to Railway (30 Minutes)

**Why This First**: Your app can't work without a backend. The auth errors in your screenshots are because there's no server running.

---

## Step 1: Install Railway CLI (2 min)

```bash
brew install railway
```

---

## Step 2: Login to Railway (1 min)

```bash
railway login
```

**Browser will open** → Sign in with GitHub

---

## Step 3: Initialize Project (2 min)

```bash
cd ~/GlowupAI/backend
railway init
```

**Choose**:
- Create new project: "glowupai-backend"
- Environment: "production"

---

## Step 4: Add PostgreSQL Database (3 min)

```bash
railway add
```

**Select**: PostgreSQL

**Railway will**:
- Create database
- Generate connection string
- Auto-inject as `DATABASE_URL` env var

---

## Step 5: Set Environment Variables (5 min)

```bash
railway variables set SKINPROOF_FIREBASE_PROJECT_ID="glowup-5e9ec"
railway variables set SKINPROOF_PHOTOS_DIR="/app/photos"
railway variables set SKINPROOF_RATE_LIMIT_ENABLED="1"
```

**Check all env vars**:
```bash
railway variables list
```

**Should see**:
- DATABASE_URL (auto-set by Railway)
- SKINPROOF_FIREBASE_PROJECT_ID
- SKINPROOF_PHOTOS_DIR
- SKINPROOF_RATE_LIMIT_ENABLED

---

## Step 6: Deploy! (5 min)

```bash
railway up
```

**Railway will**:
1. Detect Python app (sees Dockerfile)
2. Build Docker image
3. Deploy to production
4. Give you a URL

**Wait for**: "✅ Deployment successful"

---

## Step 7: Generate Public URL (1 min)

```bash
railway domain
```

**Choose**: Generate domain

**You'll get**: `glowupai-backend-production.up.railway.app`

---

## Step 8: Test Backend (2 min)

```bash
curl https://glowupai-backend-production.up.railway.app/api/health
```

**Expected**:
```json
{
  "status": "ok",
  "version": "0.1.0",
  "scope": "cosmetic_tracking"
}
```

---

## Step 9: Update Android App (10 min)

### Option A: For Debug Testing (Temporary)

**File**: `app/build.gradle.kts`

**Find** (around line 120):
```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/\"")
}
```

**Change to**:
```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"https://glowupai-backend-production.up.railway.app/api/\"")
}
```

### Option B: For Production (Recommended)

**Add to** `gradle.properties`:
```properties
STAGING_API_BASE_URL=https://glowupai-backend-production.up.railway.app/api/
RELEASE_API_BASE_URL=https://glowupai-backend-production.up.railway.app/api/
```

---

## Step 10: Rebuild APK (5 min)

```bash
cd ~/GlowupAI
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew clean assembleDebug
```

---

## Step 11: Install & Test (5 min)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Now try**:
1. Open app
2. Create account: `test@glowupai.app` / `Test123!`
3. Should work! ✅

---

## 🎯 SUCCESS CHECKLIST

- [ ] Railway CLI installed
- [ ] Railway project created
- [ ] PostgreSQL database added
- [ ] Environment variables set
- [ ] Backend deployed
- [ ] Public URL generated
- [ ] Health endpoint works
- [ ] Android app updated
- [ ] APK rebuilt
- [ ] Auth works in app

---

## 🚨 TROUBLESHOOTING

### Issue: "railway: command not found"
```bash
# Make sure Homebrew is up to date
brew update
brew install railway
```

### Issue: Deploy fails
```bash
# Check logs
railway logs

# Common fix: Make sure Dockerfile exists
ls backend/Dockerfile
```

### Issue: Database connection fails
```bash
# Check DATABASE_URL is set
railway variables get DATABASE_URL

# Should start with: postgresql://
```

### Issue: App still can't connect
```bash
# Check the URL you set in build.gradle
grep API_BASE_URL app/build.gradle.kts

# Make sure it ends with /api/
# ✅ https://your-app.railway.app/api/
# ❌ https://your-app.railway.app/
```

---

## 💰 RAILWAY COST

**Free Tier**:
- $5 credit per month
- Good for testing (should last 1-2 months)

**Paid Tier** (when you scale):
- ~$10-20/month for production
- Scales automatically
- Worth it when you have users

---

## ✅ DONE?

Once backend is deployed and working:

**Next**: Test the app thoroughly
- Read: `APP_TESTING_POLISH_PLAN.md`
- Follow 7-day testing checklist

**After That**: Build landing page
- Simple 1-page site
- Email capture
- APK download

**Then**: Launch!
- Follow: `AI_SEARCH_LAUNCH_STRATEGY.md`
- Or: `CAL_AI_LAUNCH_BLUEPRINT.md`

---

**Questions?**
- Type `"railway help"` if stuck
- Check Railway docs: https://docs.railway.app
- Or DM me the error message

**Let's get this backend live!** 🚀
