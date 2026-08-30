# GlowUp AI Launch Day Playbook

**Purpose**: Step-by-step operational guide for launching GlowUp AI  
**Audience**: Launch team executing the soft launch and public launch  
**Last Updated**: August 30, 2026

This playbook focuses on the **operational execution** of launch day. For technical deployment, see `DEPLOY.md`. For production requirements, see `PRODUCTION_READINESS.md`.

---

## Table of Contents

1. [Pre-Launch Checklist](#pre-launch-checklist)
2. [Launch Day Timeline](#launch-day-timeline)
3. [Monitoring Plan](#monitoring-plan)
4. [Support Response Templates](#support-response-templates)
5. [Social Media Launch Content](#social-media-launch-content)
6. [Feedback Collection Plan](#feedback-collection-plan)
7. [Emergency Rollback Procedures](#emergency-rollback-procedures)

---

## Pre-Launch Checklist

### 48 Hours Before Launch

**Backend Infrastructure:**
- [ ] Backend deployed to production Railway/Render
- [ ] PostgreSQL database created and backed up
- [ ] Database backup restore tested successfully
- [ ] Photo storage configured (volume or S3)
- [ ] All production environment variables set correctly
- [ ] Health check endpoint returning 200: `curl https://api.yourapp.com/api/health`
- [ ] Firebase Auth tested with test account
- [ ] Admin token secured and documented

**Android App:**
- [ ] Release APK/AAB built and signed with production keystore
- [ ] Production API URL configured (not staging/localhost)
- [ ] `google-services.json` for production Firebase installed
- [ ] App tested on at least 2 physical devices (different manufacturers)
- [ ] Offline capture and upload tested
- [ ] Firebase Crashlytics initialized and tested
- [ ] Version code and version name finalized

**Legal & Compliance:**
- [ ] Privacy policy published and accessible
- [ ] Privacy policy URL added to app settings
- [ ] Google Play Data Safety form completed
- [ ] Terms of service finalized
- [ ] Support email configured (e.g., support@glowup.ai)
- [ ] Support email auto-responder set up

**Analytics & Monitoring:**
- [ ] Firebase Analytics events logging correctly
- [ ] Firebase Crashlytics receiving test crashes
- [ ] Backend health check monitoring configured
- [ ] Error rate alerts configured (>5% errors triggers alert)
- [ ] Database connection monitoring enabled
- [ ] Signup funnel events tracked

**Marketing Assets:**
- [ ] Social media accounts created (Twitter, LinkedIn, Instagram)
- [ ] Launch posts drafted (see templates below)
- [ ] Product Hunt draft created (if applicable)
- [ ] Landing page deployed and tested
- [ ] Screenshots prepared for social media
- [ ] Demo video recorded (optional but recommended)

### 24 Hours Before Launch

**Final Smoke Tests:**
- [ ] Complete end-to-end user journey on release build:
  - [ ] Sign up with email
  - [ ] Sign up with Google
  - [ ] Complete onboarding
  - [ ] Grant consent
  - [ ] Take first photo
  - [ ] Add a product
  - [ ] Log routine event
  - [ ] Navigate all tabs
  - [ ] Sign out and sign back in
- [ ] Test account deletion flow completely
- [ ] Verify exported data is correct
- [ ] Test support email responsiveness

**Team Preparation:**
- [ ] Launch day schedule shared with team
- [ ] Monitoring dashboard URLs shared
- [ ] Support response templates accessible to team
- [ ] Rollback procedure reviewed
- [ ] Hotfix deployment process confirmed
- [ ] Communication channels established (Slack, Discord, etc.)

**Soft Launch Preparation:**
- [ ] List of 5 friends prepared with names and contact info
- [ ] Personal invitation messages drafted
- [ ] Feedback survey created (Google Forms or Typeform)
- [ ] Feedback collection spreadsheet set up

---

## Launch Day Timeline

### Soft Launch (5 Friends First)

**Hour 0: Deploy & Verify (9:00 AM)**
- [ ] Final backend health check
- [ ] Verify database connectivity
- [ ] Check all environment variables
- [ ] Confirm photo storage is working
- [ ] Test one full signup and capture flow yourself
- [ ] Screenshot successful health check for records

**Hour 1: Distribute to Friends (10:00 AM)**
- [ ] Send APK/TestFlight links to 5 friends via personalized messages:
  ```
  Hey [Name]! I've been working on GlowUp AI - a skincare tracking app 
  that helps you scientifically test what products actually work for your skin.
  
  You're one of 5 people getting early access. Would love your honest feedback!
  
  Download: [link]
  Feedback form: [link]
  
  Let me know if you hit any issues - I'm monitoring closely today!
  ```

**Hour 2-3: Initial Monitoring (11:00 AM - 12:00 PM)**
- [ ] Watch for first signups in Firebase Analytics
- [ ] Monitor Crashlytics for any crashes
- [ ] Check backend logs for errors
- [ ] Monitor database for new user records
- [ ] Track capture upload success rate
- [ ] Check support email every 30 minutes

**Hour 4: First Check-In (1:00 PM)**
- [ ] Message friends asking about first impressions
- [ ] Note any reported issues
- [ ] Check if all 5 friends have signed up
- [ ] Review Firebase Analytics funnel:
  - How many completed onboarding?
  - How many granted consent?
  - How many took first photo?

**Hour 6: Mid-Day Review (3:00 PM)**
- [ ] Analyze any crash reports
- [ ] Review error logs for patterns
- [ ] Check capture upload success rate (target: >95%)
- [ ] Verify routine event logging
- [ ] Test any reported bugs on your own device
- [ ] Document issues in a launch log

**Hour 12: Evening Check (9:00 PM)**
- [ ] Send feedback survey to all friends who signed up
- [ ] Compile initial feedback notes
- [ ] Prioritize critical bugs vs. nice-to-haves
- [ ] Plan fixes for next 24-48 hours
- [ ] Update launch log with lessons learned

**Hour 24: First Day Retrospective (9:00 AM Next Day)**
- [ ] Calculate key metrics:
  - Signup completion rate (target: 100% of 5)
  - Onboarding completion rate (target: >80%)
  - First capture rate (target: >60%)
  - Crash-free rate (target: >99%)
- [ ] Review all feedback
- [ ] Create bug fix backlog
- [ ] Decide: GO/NO-GO for public launch
- [ ] If GO: Set public launch date (recommend 3-7 days buffer)
- [ ] If NO-GO: Create fix plan and retest date

---

### Public Launch Timeline

**Day -1: Pre-Launch (Day Before)**
- [ ] All soft launch critical bugs fixed
- [ ] Updated APK/AAB deployed
- [ ] Final smoke test completed
- [ ] Social media posts scheduled
- [ ] Product Hunt launch scheduled (if applicable)
- [ ] Support team briefed
- [ ] Monitoring alerts re-verified

**Launch Day - Hour 0: Deploy (6:00 AM)**
- [ ] Final backend health check
- [ ] Final database backup
- [ ] APK uploaded to Google Play Console
- [ ] Submit for review (or publish if pre-approved)
- [ ] Confirm app store listing is complete
- [ ] Test download link works

**Hour 1: Soft Announce (7:00 AM)**
- [ ] Post to personal social media accounts
- [ ] Share in close communities/Discord servers
- [ ] Email soft launch testers about public release

**Hour 2: Social Media Launch (8:00 AM)**
- [ ] Post launch tweet (see templates below)
- [ ] Post to LinkedIn
- [ ] Post to relevant Reddit communities
- [ ] Post to Product Hunt (if applicable)
- [ ] Engage with early responses

**Hour 3-6: Monitor & Engage (9:00 AM - 12:00 PM)**
- [ ] Respond to social media comments within 15 minutes
- [ ] Monitor signup rate (track every hour)
- [ ] Watch Crashlytics for new crash patterns
- [ ] Check support email every 30 minutes
- [ ] Monitor backend error rate
- [ ] Track capture upload success rate

**Hour 8: Mid-Day Assessment (2:00 PM)**
- [ ] Calculate early metrics:
  - Total signups
  - Signup rate (per hour)
  - Onboarding completion rate
  - First capture rate
  - Crash-free sessions percentage
  - Support ticket count
- [ ] Triage any critical issues
- [ ] Update social media with engagement thank-yous

**Hour 12: Evening Check (6:00 PM)**
- [ ] Review full day metrics
- [ ] Respond to all support emails
- [ ] Post day-end social media update if metrics are good
- [ ] Document lessons learned
- [ ] Plan next day's responses

**Day 1-7: Post-Launch Monitoring**
- [ ] Daily metrics review (morning)
- [ ] Daily support email check (3x per day minimum)
- [ ] Daily crash report review
- [ ] Track retention cohorts:
  - Day 1 retention
  - Day 3 retention
  - Day 7 retention
- [ ] Collect and categorize user feedback
- [ ] Iterate on critical issues weekly

---

## Monitoring Plan

### Critical Health Metrics

**Backend Health (Check Every Hour During Launch)**
1. **API Health Endpoint**
   - URL: `https://api.yourapp.com/api/health`
   - Expected: HTTP 200 with `{"status":"ok","database_ready":true}`
   - Alert if: Non-200 response or `database_ready=false`

2. **Error Rate**
   - Monitor: Backend logs for 5xx errors
   - Target: <1% error rate
   - Alert if: >5% error rate in any 5-minute window

3. **Response Time**
   - Monitor: p95 response time for key endpoints
   - Target: <2 seconds for /api/auth/session
   - Target: <5 seconds for capture upload
   - Alert if: p95 >10 seconds

4. **Database Connections**
   - Monitor: Active PostgreSQL connections
   - Target: <50% of connection pool max
   - Alert if: >80% of connection pool used

5. **Photo Upload Success Rate**
   - Monitor: Success vs. failure for capture uploads
   - Target: >95% success rate
   - Alert if: <90% success rate

### User Metrics (Dashboard to Watch)

**Firebase Analytics - Real-Time Events:**
1. **Signup Funnel**
   - app_open
   - begin_signup
   - signup_complete
   - onboarding_complete
   - first_capture

2. **Critical User Actions**
   - capture_taken (target: average 1 per user in first 24h)
   - product_added
   - routine_logged
   - consent_granted

3. **Engagement Metrics**
   - session_start
   - average session duration (target: >2 minutes)
   - screen_view per tab

**Firebase Crashlytics - Stability:**
1. **Crash-Free Users**
   - Target: >99% crash-free users
   - Alert if: <95% crash-free

2. **Top Crashes**
   - Review top 3 crashes by volume
   - Triage: Critical if >1% of users affected

### Monitoring Dashboard Setup

**Create a Launch Dashboard with:**
1. Real-time signup count (Firebase Analytics)
2. Current active users (Firebase Analytics)
3. Crash-free rate (Firebase Crashlytics)
4. Backend health check status (uptime monitor)
5. Error rate chart (backend logs)
6. Capture upload success rate (backend metrics)
7. Support ticket count (support email)

**Tools:**
- Firebase Console open in one tab
- Backend hosting dashboard (Railway/Render) in another tab
- Support email open in third tab
- Launch metrics spreadsheet for manual tracking

---

## Support Response Templates

### Template 1: Can't Sign Up / Auth Error

**Subject**: Re: Sign up issue with GlowUp AI

Hi [Name],

Thanks for reporting this! I'm sorry you're having trouble signing up.

Can you help me troubleshoot:
1. Are you trying to sign up with email or Google?
2. What error message do you see exactly?
3. What device and Android version are you using? (Settings > About Phone)

In the meantime, try these steps:
- Make sure you have a stable internet connection
- Try the alternate signup method (Google if you tried email, or vice versa)
- Restart the app and try again

I'm actively monitoring and will fix any bugs ASAP. I really appreciate your patience!

Best,
[Your Name]
GlowUp AI Team

---

### Template 2: App Crashed

**Subject**: Re: App crash issue

Hi [Name],

I'm sorry the app crashed on you! I take stability seriously.

Good news: I receive automatic crash reports, so I likely already have details about what happened. I'm investigating now.

To help me fix this faster, can you share:
1. What were you doing right before it crashed?
2. Does it crash every time you try that action?
3. Device model and Android version?

If it's blocking you from using the app, I'll prioritize a fix today.

Thanks for your patience!

Best,
[Your Name]
GlowUp AI Team

---

### Template 3: Photo Won't Upload

**Subject**: Re: Photo upload issue

Hi [Name],

Thanks for letting me know about the upload issue. Let's troubleshoot:

1. Are you on WiFi or mobile data? (Large photos upload better on WiFi)
2. Do you see an error message, or does it just seem stuck?
3. Have you granted the app camera and storage permissions?

Try this:
- Go to Settings > Apps > GlowUp AI > Permissions
- Make sure Camera and Storage are allowed
- Try taking the photo again

If it still doesn't work, the photo might be queued to upload automatically when you have a better connection. Check the Insights tab to see if it appears there.

Let me know if this helps!

Best,
[Your Name]
GlowUp AI Team

---

### Template 4: Feature Request

**Subject**: Re: Feature idea for GlowUp AI

Hi [Name],

Thank you for the feature suggestion! I love hearing ideas from users.

[Acknowledge their specific idea]

I'm keeping a list of feature requests from early users, and I'll definitely consider this for a future update. Right now I'm focused on stability and core functionality, but I'll keep you posted on what makes it into the roadmap.

Is there anything else you'd like to see improved?

Thanks for being an early supporter!

Best,
[Your Name]
GlowUp AI Team

---

### Template 5: General Positive Feedback

**Subject**: Re: Love GlowUp AI!

Hi [Name],

This made my day! Thank you so much for the kind words.

As an early user, your feedback really matters. If you're enjoying the app, I'd be grateful if you could:
1. Leave a review on the Play Store (helps others discover it!)
2. Share with friends who are into skincare

And please don't hesitate to reach out if you ever run into issues or have ideas.

Thanks again for your support!

Best,
[Your Name]
GlowUp AI Team

---

### Template 6: Critical Bug Auto-Response

**Auto-Responder during high-volume launch:**

Subject: We received your GlowUp AI support request

Hi there,

Thanks for reaching out! We're receiving a lot of messages during our launch (exciting!).

I'll respond personally within 24 hours. In the meantime:
- For crashes: Check if there's an app update available
- For login issues: Try signing out and back in
- For upload issues: Make sure you have a stable internet connection

You can also check our FAQ: [link]

Thanks for your patience!

Best,
GlowUp AI Team

---

## Social Media Launch Content

### Launch Tweet Template

**Version 1: Problem-Focused**
```
I built GlowUp AI because I was tired of guessing if my skincare products actually work.

Track your routine. Take photos. See what's actually improving your skin.

No subscriptions. Your photos stay private. Just you vs. the mirror, backed by data.

Download: [link]

#skincare #indieapp #androiddev
```

**Version 2: Story-Focused**
```
For the past 6 months I've been building GlowUp AI - a skincare tracking app for people who want to know what ACTUALLY works.

Features:
- Daily photo timeline
- Routine tracking
- Product experimentation
- AI-powered insights
- Privacy-first (your face, your data)

It's free and live on Android: [link]

#buildinpublic #skincare
```

**Version 3: Pain Point**
```
Spent $200 on skincare products this month?

Cool. Do any of them actually work?

GlowUp AI helps you track your routine + take consistent photos so you can see what's working (and what's just marketing).

Free on Android: [link]

#skincare #beauty #tech
```

### LinkedIn Post Template

```
Excited to share that GlowUp AI is now live!

Over the past 6 months, I've been building a privacy-first skincare tracking app that helps users scientifically test their products through photo timelines and routine tracking.

Why this matters:
- The skincare market is $100B+ but most people buy based on marketing, not data
- Clinical trials are expensive, but personal experimentation can be structured
- Photo evidence beats memory every time

Built with:
- Kotlin + Jetpack Compose for Android
- Python + FastAPI for the backend
- Firebase for auth
- PostgreSQL for storage
- Gemini API for AI insights

Early feedback from beta testers has been incredible. If you're into skincare or just curious about indie app development, I'd love your thoughts!

Download: [link]

#buildinpublic #skincare #android #indiehacking
```

### Reddit Post Template

**r/SkincareAddiction**
```
Title: [App] I built GlowUp AI - track your routine + take consistent photos to see what actually works

Body:
Hey SCA! I've been a lurker here for years, and I built an app to solve a problem I had: 
I never knew if my products were actually working.

**GlowUp AI** is a free Android app that helps you:
- Take consistent progress photos with guides
- Track exactly what products you're using when
- See side-by-side comparisons over time
- Run "experiments" (A/B testing products)

Privacy was important to me - your photos stay on your device, and I'm not selling data to beauty brands.

It's free, no subscription, and I'd love feedback from this community since you all inspired it!

Download: [link]

Happy to answer questions!
```

**r/Android**
```
Title: I spent 6 months building a skincare tracking app with Kotlin + Compose. Here's what I learned.

Body:
Hey r/Android! I just launched GlowUp AI - a skincare tracking app built entirely in Kotlin with Jetpack Compose.

**Tech Stack:**
- Kotlin + Jetpack Compose (100% Compose, no XML)
- CameraX for consistent photo capture
- Room for local caching
- Firebase Auth
- Encrypted photo storage
- Offline-first architecture with sync queue

**Interesting technical challenges:**
- Building consistent lighting guides for photo quality
- Implementing offline capture with upload queue
- Managing encrypted photo storage
- Handling consent/privacy flows for facial photos

It's free and open for feedback. Would love to hear from other Android devs!

Download: [link]

Happy to discuss any technical details!
```

### Product Hunt Launch Copy

**Tagline:**
Track your skincare scientifically with photos, routines, and AI insights

**Description:**
GlowUp AI helps you figure out what skincare products actually work for YOUR skin.

Stop guessing. Start tracking.

**How it works:**
1. Take consistent photos with guided capture
2. Log your routine (what products, when)
3. See side-by-side comparisons over time
4. Get AI-powered insights on what's working

**Why it's different:**
- Privacy-first: Your face photos stay private, encrypted, and under your control
- Scientific: Built-in A/B testing for products
- No subscriptions: Free core features, optional Premium for power users
- Actually useful AI: Gemini-powered insights that help, not hype

Perfect for:
- People with acne tracking treatment effectiveness
- Anyone anti-aging and testing serums/retinols
- Skincare enthusiasts who love data
- Anyone tired of buying products that don't work

Built by an indie developer who got tired of wasting money on products that didn't work.

---

## Feedback Collection Plan

### In-App Feedback (Build After Launch)

**Future Feature - Post-Launch:**
- Add feedback button in Settings > Help & Feedback
- Simple form: "What would make GlowUp AI better for you?"
- Capture device info automatically
- Send to support email or Firebase

**For Launch: Use External Form**

### Google Form for Soft Launch

**Title:** GlowUp AI Early Feedback

**Questions:**

1. **What's your name?** (Optional)
   - Short answer

2. **How would you feel if you could no longer use GlowUp AI?**
   - Very disappointed
   - Somewhat disappointed
   - Not disappointed

3. **What did you like most about the app?**
   - Long answer

4. **What was confusing or frustrating?**
   - Long answer

5. **Did you successfully take your first photo?**
   - Yes
   - No - why not? [follow-up]

6. **Would you recommend this to a friend?** (0-10 NPS)
   - Linear scale 0-10

7. **What feature is missing that you wish existed?**
   - Long answer

8. **Any bugs or crashes?**
   - Long answer

9. **What device are you using?**
   - Short answer

10. **Any other thoughts?**
    - Long answer

**Share Link After Hour 12 of Soft Launch**

### Post-Public Launch Survey (Day 7)

Send to all users who completed onboarding:

**Subject:** You've been using GlowUp AI for a week - how's it going?

**Body:**
```
Hi [Name],

It's been a week since you started using GlowUp AI! I'd love to hear how it's going.

Quick 2-minute survey: [link]

Your feedback directly shapes what I build next. Plus, everyone who completes the 
survey gets early access to new features.

Thanks for being an early user!

Best,
[Your Name]
```

### Ongoing Feedback Collection

**Weekly:**
- Review Play Store reviews (respond to all)
- Review support emails for patterns
- Track most requested features
- Document common bugs

**Monthly:**
- Send NPS survey to active users
- Compile feedback report
- Prioritize feature roadmap
- Share product updates with users

---

## Emergency Rollback Procedures

### When to Rollback

**Immediate Rollback Triggers:**
- Crash rate >5% of users
- Complete service outage >15 minutes
- Data loss or corruption detected
- Critical security vulnerability discovered
- Auth system completely broken

**Consider Rollback (Evaluate First):**
- Crash rate 2-5% of users (might be device-specific)
- Non-critical feature broken
- Performance degradation but app still usable
- High error rate but intermittent

### Backend Rollback

**Railway Rollback (Fast):**
1. Go to Railway dashboard
2. Select your service
3. Go to Deployments tab
4. Find previous successful deployment
5. Click three dots > "Redeploy"
6. Confirm rollback
7. Wait 2-3 minutes for deployment
8. Test health endpoint immediately

**Rollback Time: ~3 minutes**

### Android App Rollback

**Play Store Internal Testing:**
1. Go to Play Console
2. Navigate to Release > Testing > Internal Testing
3. Go to Releases tab
4. Create new release with previous working APK
5. Upload and submit
6. Notify testers to update

**Rollback Time: ~5 minutes for submission, users must update manually**

**Public Release:**
- Cannot instantly rollback
- Must submit new release with previous version code +1
- Takes 2-24 hours for review
- Update release notes: "Fixing critical bug from v1.X.X"

**Mitigation While Waiting:**
- Post to social media about known issue
- Update Play Store description with known issues
- Send email to users if you have addresses
- Respond to all support requests immediately

### Communication Templates for Outages

**Twitter/Social Media:**
```
We're aware of an issue causing [problem] in GlowUp AI. 

We've identified the cause and are deploying a fix now. 

Expected resolution: [time]

Sorry for the disruption - we'll make this right.
```

**Email to Users (If Critical):**
```
Subject: GlowUp AI Service Issue - We're On It

Hi,

We're aware that some users are experiencing [issue description] with GlowUp AI.

What happened: [brief explanation]
Current status: [we're fixing it / fix deployed / monitoring]
What you should do: [update app / wait / nothing]

We take reliability seriously and we're sorry for the disruption.

Updates: [Twitter link or status page]

Thanks for your patience,
[Your Name]
GlowUp AI Team
```

---

## Launch Day Checklist Summary

**T-Minus 48 Hours:**
- Complete Pre-Launch Checklist (all items)

**T-Minus 24 Hours:**
- Complete final smoke tests
- Prepare team and assets

**Launch Day (Soft):**
- Hour 0: Deploy & verify
- Hour 1: Send to 5 friends
- Hour 2-6: Monitor closely
- Hour 12: Collect initial feedback
- Hour 24: Go/No-Go decision

**Launch Day (Public):**
- Hour 0: Deploy APK/AAB
- Hour 1: Soft announce
- Hour 2: Full social media launch
- Hour 3-12: Monitor and engage
- Day 1-7: Daily monitoring and iteration

**Monitoring:**
- Watch Firebase Analytics real-time
- Monitor Crashlytics for crashes
- Check backend health every hour
- Respond to support within 2 hours

**Communication:**
- Use templates for common issues
- Be transparent about bugs
- Thank users for feedback
- Share early wins on social media

---

## Post-Launch: First Week

### Daily Tasks

**Every Morning (9 AM):**
- [ ] Check overnight crashes in Crashlytics
- [ ] Review overnight signups
- [ ] Respond to support emails
- [ ] Check Play Store reviews
- [ ] Update launch metrics spreadsheet

**Every Afternoon (2 PM):**
- [ ] Check backend health and error logs
- [ ] Review social media engagement
- [ ] Respond to comments/DMs
- [ ] Triage bug reports

**Every Evening (9 PM):**
- [ ] Final support email check
- [ ] Review day's metrics
- [ ] Update team on progress
- [ ] Plan next day's priorities

### Week 1 Metrics to Track

**Acquisition:**
- Total signups
- Signups per day
- Traffic sources (organic, Reddit, Twitter, etc.)

**Activation:**
- Onboarding completion rate
- First capture rate
- Time to first capture

**Retention:**
- Day 1 retention
- Day 3 retention
- Day 7 retention

**Engagement:**
- Average captures per user
- Average session duration
- Repeat capture rate

**Quality:**
- Crash-free rate
- Error rate
- Support ticket volume
- Average resolution time

**Feedback:**
- NPS score
- Top feature requests
- Top bug reports
- Play Store rating

### Week 1 Goals

**Success Indicators:**
- 40%+ Day 1 retention
- 99%+ crash-free rate
- <5% error rate
- Average 2+ captures per user
- NPS >30
- 4+ star Play Store rating

**If Below Goals:**
- Schedule retrospective
- Identify biggest drop-off point
- Plan sprint to fix critical issues
- Consider limited rollout vs. full launch

---

## Appendix: Quick Reference

### Key URLs
- Production Backend: https://api.yourapp.com
- Health Check: https://api.yourapp.com/api/health
- Firebase Console: https://console.firebase.google.com
- Railway Dashboard: https://railway.app/dashboard
- Play Console: https://play.google.com/console

### Key Commands
- Backend health: `curl https://api.yourapp.com/api/health`
- Railway logs: `railway logs`
- Railway rollback: Via dashboard > Deployments > Redeploy

### Emergency Contacts
- [Your Name]: [Your Contact]
- [Tech Co-Founder]: [Contact]
- [Support Person]: [Contact]

### Launch Day War Room
- Communication: [Slack/Discord Link]
- Monitoring Dashboard: [Link]
- Metrics Spreadsheet: [Link]
- Support Email: support@glowup.ai

---

**Last Updated**: August 30, 2026  
**Document Owner**: [Your Name]  
**Next Review**: Post-launch retrospective after Day 7
