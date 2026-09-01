# GlowUp AI - Production Launch Checklist

**Last Updated**: September 1, 2026  
**Target Launch Date**: TBD  
**Status**: Pre-Launch Preparation

---

## Overview

This is a comprehensive checklist for launching GlowUp AI to production. Complete each item and check the box when done.

---

## Phase 0: Pre-Deployment Setup (Do First!)

### A. Firebase Configuration ⚠️ CRITICAL

- [ ] **Create Firebase Project**
  - [ ] Go to [Firebase Console](https://console.firebase.google.com)
  - [ ] Create new project: `glowup-ai-38ae7` (or verify existing)
  - [ ] Enable Google Analytics for Firebase

- [ ] **Enable Firebase Authentication**
  - [ ] Go to Authentication → Sign-in method
  - [ ] Enable Google provider
  - [ ] Add authorized domains for production

- [ ] **Enable Firebase Crashlytics**
  - [ ] Go to Crashlytics in Firebase console
  - [ ] Enable Crashlytics for Android

- [ ] **Download Configuration Files**
  - [ ] Download `google-services.json` for Android
  - [ ] Save to `/Users/21cabbage/GlowupAI/app/google-services.json`
  - [ ] **DO NOT** commit to git (already in .gitignore)

- [ ] **Verify Firebase Project ID**
  - [ ] Confirm: `glowup-ai-38ae7`
  - [ ] Update backend environment variable if different

### B. Android Release Signing ⚠️ CRITICAL

- [ ] **Generate Release Keystore**
  ```bash
  keytool -genkey -v -keystore glowup-ai-release.keystore \
    -alias glowup-ai -keyalg RSA -keysize 2048 -validity 10000
  ```
  - [ ] Save keystore file securely
  - [ ] **NEVER** commit keystore to git
  - [ ] Store in password manager

- [ ] **Create keystore.properties**
  ```properties
  storeFile=../glowup-ai-release.keystore
  storePassword=YOUR_STORE_PASSWORD
  keyAlias=glowup-ai
  keyPassword=YOUR_KEY_PASSWORD
  ```
  - [ ] Save to `/Users/21cabbage/GlowupAI/app/keystore.properties`
  - [ ] **NEVER** commit to git (already in .gitignore)

### C. Third-Party Service Setup

- [ ] **Sentry (Error Tracking)**
  - [ ] Create account at [sentry.io](https://sentry.io)
  - [ ] Create new project: "GlowUp AI Backend"
  - [ ] Copy DSN for environment variables
  - [ ] Set up alerts for error notifications

- [ ] **Google Gemini API**
  - [ ] Get API key from [Google AI Studio](https://ai.google.dev/)
  - [ ] Test API key with sample request
  - [ ] Note usage limits and quotas

- [ ] **Railway or Render (Optional but Recommended)**
  - [ ] Create account at [Railway](https://railway.app) or [Render](https://render.com)
  - [ ] Add payment method if needed
  - [ ] Install CLI tool for deployments

---

## Phase 1: Backend Deployment

### A. Database Setup

- [ ] **Provision PostgreSQL Database**
  - Option 1: Railway
    - [ ] `railway add --database postgres`
    - [ ] Note down connection string
  - Option 2: Render
    - [ ] Create new PostgreSQL instance
    - [ ] Choose region close to your users
  - Option 3: Neon, Supabase, or other managed Postgres

- [ ] **Configure Database Connection**
  - [ ] Railway automatically sets `DATABASE_URL`
  - [ ] For others, set `GLOWUPAI_DATABASE_URL` manually

### B. Photo Storage Setup

- [ ] **Choose Storage Option**
  - Option 1: Volume (Recommended for MVP)
    - [ ] Create volume: `/data/photos`
    - [ ] Mount to backend service
  - Option 2: Object Storage (Recommended for scale)
    - [ ] Set up S3/GCS bucket
    - [ ] Configure backend integration (code changes needed)

- [ ] **Generate Encryption Key** (if using volume)
  ```bash
  python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
  ```
  - [ ] Save key securely
  - [ ] Set as `GLOWUPAI_PHOTO_KEY`

### C. Environment Variables

- [ ] **Set Required Variables**
  ```bash
  # Required
  GLOWUPAI_ENV=production
  GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
  GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
  GEMINI_API_KEY=your_key_here
  GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com
  
  # Security
  GLOWUPAI_AUTH_REQUIRED=0  # Start with 0, flip to 1 once tested
  GLOWUPAI_ADMIN_TOKEN=generate_secure_token_here
  
  # Photo storage (if using volume)
  GLOWUPAI_PHOTO_DIR=/data/photos
  GLOWUPAI_PHOTO_KEY=your_base64_key_here
  
  # Monitoring
  SENTRY_DSN=your_sentry_dsn_here
  SENTRY_TRACES_SAMPLE_RATE=0.1
  SENTRY_PROFILES_SAMPLE_RATE=0.1
  
  # Auto-set by Railway/Render (don't set manually)
  # DATABASE_URL
  # PORT
  ```

- [ ] **Generate Admin Token**
  ```bash
  python3 -c "import secrets; print(secrets.token_urlsafe(32))"
  ```

### D. Deploy Backend

- [ ] **Option 1: Railway**
  ```bash
  cd backend
  railway login
  railway init
  railway add --database postgres
  railway up
  railway domain
  ```

- [ ] **Option 2: Render**
  - [ ] Connect GitHub repository
  - [ ] Create Web Service
  - [ ] Set environment variables
  - [ ] Deploy

- [ ] **Option 3: Docker Compose (Local/VPS)**
  ```bash
  cd backend
  docker compose up -d --build
  ```

### E. Verify Backend Deployment

- [ ] **Health Check**
  ```bash
  curl https://your-backend-url.com/api/health
  ```
  - [ ] Status: `ok` or `healthy`
  - [ ] Database: `postgresql`
  - [ ] `database_ready`: `true`

- [ ] **Test Authentication**
  - [ ] Create test Firebase user
  - [ ] Get ID token
  - [ ] Test `/api/auth/session` endpoint
  - [ ] Verify user profile created

- [ ] **Check Logs**
  - [ ] No critical errors
  - [ ] Database migrations completed
  - [ ] All services initialized

---

## Phase 2: Android App Build

### A. Pre-Build Configuration

- [ ] **Update API Endpoint**
  - [ ] Open `app/build.gradle.kts`
  - [ ] Set `releaseApiBaseUrl` to production URL
  - [ ] Verify HTTPS (not HTTP)

- [ ] **Verify Dependencies**
  - [ ] All Firebase SDKs present
  - [ ] `google-services.json` in place
  - [ ] `keystore.properties` configured

### B. Build Release APK

- [ ] **Clean Build**
  ```bash
  ./gradlew clean
  ```

- [ ] **Build Release APK**
  ```bash
  ./gradlew assembleRelease
  ```
  - [ ] Build succeeds without errors
  - [ ] APK created: `app/build/outputs/apk/release/app-release.apk`
  - [ ] APK size reasonable (< 50MB)

- [ ] **Verify APK Signing**
  ```bash
  jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
  ```
  - [ ] Shows: "jar verified"
  - [ ] Certificate matches your keystore

### C. Test APK on Device

- [ ] **Install on Physical Device**
  ```bash
  adb install app/build/outputs/apk/release/app-release.apk
  ```

- [ ] **Basic Functionality Test**
  - [ ] App launches without crash
  - [ ] Google sign-in works
  - [ ] Can create account
  - [ ] Can upload photo
  - [ ] Dashboard loads
  - [ ] No obvious bugs

---

## Phase 3: Internal Testing (Week 1)

### A. Smoke Tests

- [ ] **User Onboarding Flow**
  - [ ] Fresh install
  - [ ] Sign up with Google
  - [ ] Complete onboarding
  - [ ] Profile created successfully

- [ ] **Core Features**
  - [ ] Take first photo
  - [ ] View dashboard
  - [ ] Check metrics
  - [ ] View history
  - [ ] Navigation works

- [ ] **Offline Mode**
  - [ ] Enable airplane mode
  - [ ] App doesn't crash
  - [ ] Shows offline message
  - [ ] Local data persists

### B. Monitor Production Systems

- [ ] **Sentry Dashboard**
  - [ ] No new errors
  - [ ] Performance within limits
  - [ ] No memory leaks

- [ ] **Firebase Console**
  - [ ] Analytics events flowing
  - [ ] No crashes reported
  - [ ] User sessions tracked

- [ ] **Backend Metrics**
  - [ ] Response times < 500ms
  - [ ] Error rate < 1%
  - [ ] Database healthy
  - [ ] No resource exhaustion

### C. Fix Critical Issues

- [ ] **Prioritize Bugs**
  - [ ] P0: Crashes, data loss → Fix immediately
  - [ ] P1: Major features broken → Fix within 24h
  - [ ] P2: Minor issues → Fix before beta

- [ ] **Deploy Hotfixes**
  - [ ] Test fix locally
  - [ ] Deploy to production
  - [ ] Verify fix in production
  - [ ] Monitor for new issues

---

## Phase 4: Closed Alpha (Week 2-3)

### A. Recruit Alpha Testers

- [ ] **Target**: 25-50 users
  - [ ] Friends and family
  - [ ] Early supporters
  - [ ] Skincare enthusiasts

- [ ] **Distribute APK**
  - Option 1: Google Play Internal Testing
    - [ ] Upload APK to Play Console
    - [ ] Create internal testing track
    - [ ] Add testers by email
  - Option 2: Direct Distribution
    - [ ] Share APK link
    - [ ] Provide installation instructions

### B. Collect Feedback

- [ ] **Set Up Feedback Channels**
  - [ ] Google Form for structured feedback
  - [ ] Discord/Slack for discussions
  - [ ] In-app feedback button (optional)

- [ ] **Track Key Metrics**
  - [ ] Daily active users (DAU)
  - [ ] Photos uploaded per user
  - [ ] Feature usage rates
  - [ ] Crash-free rate
  - [ ] Session length

### C. Iterate Based on Feedback

- [ ] **Weekly Reviews**
  - [ ] Review user feedback
  - [ ] Prioritize improvements
  - [ ] Plan next sprint

- [ ] **Bug Fixes & Improvements**
  - [ ] Fix reported issues
  - [ ] Improve UX based on feedback
  - [ ] Add missing features

---

## Phase 5: Closed Beta (Week 4-6)

### A. Scale to 100-500 Users

- [ ] **Google Play Closed Testing**
  - [ ] Upload to Play Console
  - [ ] Create closed testing track
  - [ ] Set up opt-in URL
  - [ ] Share with community

### B. Stress Test Infrastructure

- [ ] **Monitor Scaling**
  - [ ] Database performance under load
  - [ ] API response times
  - [ ] Photo storage usage
  - [ ] Bandwidth consumption

- [ ] **Optimize as Needed**
  - [ ] Add database indexes
  - [ ] Enable Redis caching
  - [ ] Optimize slow queries
  - [ ] Compress images

### C. Polish & Prepare for Launch

- [ ] **UI/UX Polish**
  - [ ] Fix all visual bugs
  - [ ] Improve animations
  - [ ] Add loading states
  - [ ] Improve error messages

- [ ] **Documentation**
  - [ ] User guide / FAQ
  - [ ] Privacy policy
  - [ ] Terms of service
  - [ ] Support email

---

## Phase 6: Public Launch Preparation

### A. Google Play Store Listing

- [ ] **Store Listing**
  - [ ] App title
  - [ ] Short description (80 chars)
  - [ ] Full description (4000 chars)
  - [ ] Keywords for ASO
  - [ ] Screenshots (at least 2)
  - [ ] Feature graphic
  - [ ] App icon

- [ ] **Privacy & Content**
  - [ ] Privacy policy URL
  - [ ] Data safety form
  - [ ] Content rating questionnaire
  - [ ] Target audience

- [ ] **Upload Production APK**
  - [ ] Build final production APK
  - [ ] Version code increment
  - [ ] Upload to production track
  - [ ] Submit for review

### B. Marketing Preparation

- [ ] **Landing Page**
  - [ ] Clear value proposition
  - [ ] Screenshots/demo
  - [ ] App Store links
  - [ ] Email capture

- [ ] **Social Media**
  - [ ] Create accounts (Twitter, Instagram, TikTok)
  - [ ] Prepare launch posts
  - [ ] Engage with skincare community

- [ ] **Press Kit**
  - [ ] App description
  - [ ] High-res screenshots
  - [ ] Founder story
  - [ ] Press release

### C. Support Infrastructure

- [ ] **Support Email**
  - [ ] Set up support@glowup.ai
  - [ ] Create canned responses
  - [ ] Set up ticketing system (optional)

- [ ] **Documentation**
  - [ ] Help center / FAQ
  - [ ] Video tutorials
  - [ ] Troubleshooting guides

---

## Phase 7: Public Launch

### A. Gradual Rollout

- [ ] **Day 1-3: 10% Rollout**
  - [ ] Enable for 10% of users
  - [ ] Monitor closely
  - [ ] Fix any critical issues

- [ ] **Day 4-7: 50% Rollout**
  - [ ] Increase to 50%
  - [ ] Monitor metrics
  - [ ] Optimize performance

- [ ] **Day 8+: 100% Rollout**
  - [ ] Release to all users
  - [ ] Continue monitoring
  - [ ] Respond to reviews

### B. Launch Day Activities

- [ ] **Social Media Blast**
  - [ ] Announce on all channels
  - [ ] Share screenshots/videos
  - [ ] Engage with comments

- [ ] **Community Outreach**
  - [ ] Post in relevant subreddits
  - [ ] Share in skincare communities
  - [ ] Reach out to influencers

- [ ] **Monitor Everything**
  - [ ] Sentry errors
  - [ ] Firebase crashes
  - [ ] Play Store reviews
  - [ ] Social media mentions
  - [ ] Server metrics

### C. Post-Launch

- [ ] **Respond to Users**
  - [ ] Reply to reviews (especially negative)
  - [ ] Answer support emails promptly
  - [ ] Engage on social media

- [ ] **Collect Feedback**
  - [ ] In-app surveys
  - [ ] Monitor app reviews
  - [ ] Track feature requests

- [ ] **Plan Next Iteration**
  - [ ] Prioritize improvements
  - [ ] Plan v1.1 features
  - [ ] Set roadmap

---

## Ongoing Maintenance

### Daily

- [ ] Check Sentry for new errors
- [ ] Review Firebase Crashlytics
- [ ] Monitor server uptime
- [ ] Respond to support emails
- [ ] Check app reviews

### Weekly

- [ ] Review analytics dashboard
- [ ] Analyze user behavior
- [ ] Plan bug fixes
- [ ] Update documentation
- [ ] Engage with community

### Monthly

- [ ] Review all metrics vs. goals
- [ ] Plan feature updates
- [ ] Optimize costs
- [ ] Review security
- [ ] Update dependencies

---

## Emergency Procedures

### If Backend Goes Down

1. Check Sentry for errors
2. Check Railway/Render dashboard
3. Restart service if needed
4. Check database health
5. Review recent deployments
6. Rollback if necessary
7. Post status update

### If Critical Bug Found

1. Assess severity (data loss? crashes?)
2. Disable feature via remote config if possible
3. Prepare hotfix
4. Test thoroughly
5. Deploy ASAP
6. Monitor closely
7. Notify affected users

### If Database Issues

1. Check connection pool
2. Check slow queries
3. Add indexes if needed
4. Scale up if needed
5. Consider read replicas
6. Backup database

---

## Success Criteria

### Week 1 (Internal)
- [ ] 0 critical bugs
- [ ] All core features work
- [ ] < 1% error rate

### Week 2-3 (Alpha)
- [ ] 25+ active testers
- [ ] Positive feedback
- [ ] < 5% crash rate

### Week 4-6 (Beta)
- [ ] 100+ active users
- [ ] 4.0+ star rating
- [ ] 50%+ D7 retention

### Public Launch
- [ ] 1000+ downloads in Week 1
- [ ] 4.5+ star rating
- [ ] < 2% crash rate
- [ ] Positive reviews

---

## Notes

- **Don't rush!** Better to launch a polished product late than a buggy product early.
- **Monitor everything** in the first 48 hours after each phase.
- **Communicate proactively** with users about issues.
- **Be ready to rollback** if something goes wrong.
- **Celebrate small wins** along the way!

---

**Document Owner**: Development Team  
**Last Review**: September 1, 2026  
**Next Review**: After Phase 1 completion
