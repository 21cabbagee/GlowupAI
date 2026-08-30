# GlowUp AI - End-to-End Testing Checklist

Last updated: 2026-08-30

## Overview
This checklist covers complete user journeys from app install through core feature usage. Each test should be executed on multiple devices and Android versions.

## Test Environment Setup

### Prerequisites
- [ ] Backend running and accessible (staging environment)
- [ ] Firebase Auth configured and working
- [ ] Test devices: Minimum Android 8.0, recommended Android 12+
- [ ] Test accounts: Fresh user, existing user, premium user
- [ ] Network conditions: WiFi, 4G, 3G, offline mode
- [ ] Test data: Sample images, known good/bad captures

---

## 1. New User Onboarding Flow

### 1.1 App Install and Launch
- [ ] Install APK on clean device
- [ ] Launch app for first time
- [ ] Verify splash screen displays
- [ ] Check Firebase initialization (no crashes)
- [ ] Verify health check call to backend succeeds
- [ ] Verify welcome screen appears

### 1.2 Account Creation
- [ ] **Email/Password Sign Up**
  - [ ] Enter valid email and password
  - [ ] Verify password requirements shown
  - [ ] Submit and verify account created
  - [ ] Check Firebase Auth user created
  - [ ] Verify user_id stored locally
  - [ ] Check backend user record created (POST /api/users)

- [ ] **Google Sign In**
  - [ ] Tap Google Sign In button
  - [ ] Select Google account
  - [ ] Verify authentication succeeds
  - [ ] Check user_id stored and backend record created

- [ ] **Error Handling**
  - [ ] Try invalid email format
  - [ ] Try weak password
  - [ ] Try existing email (should fail gracefully)
  - [ ] Test network failure during sign up
  - [ ] Verify error messages are user-friendly

### 1.3 Consent Flow
- [ ] After account creation, consent screen appears
- [ ] Verify policy version displayed
- [ ] Read full consent text (scrollable)
- [ ] Checkbox for facial_data consent
- [ ] Submit consent (POST /api/users/{user_id}/consent)
- [ ] Verify consent stored in backend
- [ ] Verify can't proceed without consent
- [ ] Test rejecting consent (should block app usage)

### 1.4 Baseline Capture
- [ ] After consent, capture guide screen appears
- [ ] Display capture requirements:
  - [ ] Good lighting
  - [ ] Neutral expression
  - [ ] Front-facing camera
  - [ ] Distance guidelines
- [ ] Tap "Take Baseline Photo" button
- [ ] Camera permission requested
- [ ] Grant camera permission
- [ ] Camera preview launches
- [ ] ML Kit face detection overlay shown
- [ ] Face guidance indicators work:
  - [ ] Too far away warning
  - [ ] Too close warning
  - [ ] Face not centered warning
  - [ ] Good position indicator
- [ ] Capture photo when guidance is green
- [ ] Photo preview shown with "Retake" and "Use Photo" options
- [ ] Tap "Use Photo"
- [ ] Upload progress shown
- [ ] Backend processes capture (POST /api/users/{user_id}/captures)
- [ ] Quality check passes (or shows retry if quality insufficient)
- [ ] First capture marked as baseline (is_baseline=true)
- [ ] Dashboard loads with baseline metrics

### 1.5 Onboarding Completion
- [ ] After baseline, user sees dashboard for first time
- [ ] "First Capture" achievement unlocked
- [ ] Streak counter initialized (Day 1)
- [ ] Empty routine state shown
- [ ] Navigation drawer accessible
- [ ] All tabs enabled (Home, Routine, Insights, Discover, Account)

---

## 2. Capture Flow (Post-Onboarding)

### 2.1 Standard Capture
- [ ] From dashboard, tap "New Capture" button
- [ ] Capture guide reminder shown
- [ ] Camera launches
- [ ] Face detection guidance active
- [ ] Capture photo
- [ ] Preview and confirm
- [ ] Upload to backend
- [ ] Quality check passes
- [ ] New snapshot created
- [ ] Metrics calculated and displayed
- [ ] Capture added to history timeline
- [ ] Streak updated if applicable

### 2.2 Quality Validation
- [ ] **Good quality capture**
  - [ ] Proper lighting, centered face
  - [ ] Quality score above threshold
  - [ ] Capture accepted
  
- [ ] **Poor quality capture**
  - [ ] Too dark/blurry image
  - [ ] Quality check fails
  - [ ] Error message: "Capture quality below threshold"
  - [ ] Retry option provided
  - [ ] Original image not saved

- [ ] **Edge cases**
  - [ ] No face detected
  - [ ] Multiple faces
  - [ ] Partial face (profile view)
  - [ ] Occluded face (sunglasses, mask)

### 2.3 Capture Metadata
- [ ] Captured_at timestamp recorded
- [ ] Device metadata included (device model, OS version)
- [ ] Photo encrypted before upload
- [ ] Local capture saved to outbox if offline

---

## 3. Streak Tracking

### 3.1 Daily Streak Flow
- [ ] Day 1: Baseline capture, streak = 1
- [ ] Day 2: New capture within 24 hours, streak = 2
- [ ] Day 3: New capture, streak = 3
- [ ] Streak counter visible on dashboard
- [ ] Streak animation on increment
- [ ] Check GET /api/users/{user_id}/dashboard for correct streak

### 3.2 Streak Maintenance
- [ ] **Within capture window**
  - [ ] Capture at 8am Day 1, 10am Day 2: streak continues
  - [ ] Capture at 11pm Day 1, 1am Day 2: streak continues (24hr window)

- [ ] **Missed day**
  - [ ] Skip capture for 24+ hours
  - [ ] Streak resets to 0
  - [ ] Notification: "Your streak ended"
  - [ ] Next capture starts new streak at 1

### 3.3 Freeze Day Feature (Premium)
- [ ] Premium user with active streak
- [ ] Navigate to streak settings
- [ ] See available freeze days (starts with some allocated)
- [ ] Choose to use freeze day
- [ ] Miss capture that day
- [ ] Verify streak NOT reset
- [ ] Freeze day count decremented
- [ ] Free user: freeze day option not available

### 3.4 Streak Edge Cases
- [ ] **Timezone changes**
  - [ ] User travels across timezones
  - [ ] Capture at 11pm PST, travel to EST (2am EST)
  - [ ] Next capture at 11am EST same calendar day
  - [ ] Verify streak logic handles this (should continue)

- [ ] **Multiple captures same day**
  - [ ] Take 2+ captures in one day
  - [ ] Streak only increments once per day
  - [ ] All captures saved to history

- [ ] **Streak recovery**
  - [ ] Break streak after 10+ days
  - [ ] Start new streak
  - [ ] Verify longest_streak preserved in profile
  - [ ] Check if "Bounce Back" achievement unlocks

---

## 4. Achievement System

### 4.1 Achievement Unlocks
- [ ] **First Capture** - Complete baseline
- [ ] **Week Warrior** - 7 day streak
- [ ] **Fortnight Focus** - 14 day streak
- [ ] **Month Master** - 30 day streak
- [ ] **Century Club** - 100 day streak
- [ ] **First Product** - Add product to routine
- [ ] **First Experiment** - Create experiment
- [ ] **Scientist** - Complete experiment
- [ ] **Consistent** - 5 consecutive weeks of 3+ captures
- [ ] **Early Bird** - Capture before 7am
- [ ] **Night Owl** - Capture after 11pm

### 4.2 Achievement Display
- [ ] Achievements screen accessible from Account tab
- [ ] Unlocked achievements show date earned
- [ ] Locked achievements show requirements
- [ ] Achievement badge animations on unlock
- [ ] Push notification on achievement unlock (if enabled)

### 4.3 Achievement Edge Cases
- [ ] Unlock multiple achievements at once
- [ ] Achievement unlocks while offline (sync on reconnect)
- [ ] Premium-only achievements locked for free users

---

## 5. Offline Mode

### 5.1 Offline Capture
- [ ] Disable device internet (airplane mode)
- [ ] Navigate to capture screen
- [ ] Verify "Offline" indicator shown
- [ ] Take capture photo
- [ ] Photo saved to local outbox (Room database)
- [ ] Message: "Saved locally, will sync when online"
- [ ] Capture appears in history with "pending upload" badge

### 5.2 Offline to Online Sync
- [ ] Enable internet connection
- [ ] WorkManager triggers sync automatically
- [ ] Pending captures upload in background
- [ ] Upload progress notification shown
- [ ] On success: capture status updates to "synced"
- [ ] Backend processes capture and returns metrics
- [ ] Local capture updated with server data
- [ ] Streak updated if applicable

### 5.3 Offline Limitations
- [ ] Can view cached dashboard data
- [ ] Can view previously loaded history
- [ ] Cannot add products (requires backend)
- [ ] Cannot ask Q&A questions
- [ ] Cannot fetch new insights
- [ ] Offline indicator shown throughout app
- [ ] Queue indicator shows pending actions (e.g., "2 pending uploads")

### 5.4 Sync Failures
- [ ] **Network failure during upload**
  - [ ] WorkManager retries with exponential backoff
  - [ ] Up to 3 retry attempts
  - [ ] Persistent notification if all retries fail
  - [ ] User can manually retry from history screen

- [ ] **Backend 500 error**
  - [ ] Capture remains in outbox
  - [ ] Error logged to Crashlytics
  - [ ] Retry scheduled

---

## 6. Photo Comparison

### 6.1 Compare Two Photos
- [ ] Navigate to History tab
- [ ] Tap "Compare" button
- [ ] Select first photo from timeline
- [ ] Select second photo from timeline
- [ ] Comparison view loads:
  - [ ] Side-by-side or slider view
  - [ ] Date labels on each photo
  - [ ] Days between captures shown
- [ ] Metric changes displayed:
  - [ ] Delta values (e.g., "Redness: -12%")
  - [ ] Green for improvement, red for worsening
  - [ ] Neutral grey for no change

### 6.2 Comparison Filters
- [ ] Filter by date range
- [ ] Filter by experiment (if photos tagged to experiment)
- [ ] Compare baseline to current
- [ ] Compare first/last capture in time range

### 6.3 Comparison Export
- [ ] Export comparison as image
- [ ] Share comparison (Android share sheet)
- [ ] Save to device gallery (with storage permission)

---

## 7. Routine Tracking

### 7.1 Add Product
- [ ] Navigate to Routine tab
- [ ] Tap "Add Product" button
- [ ] Enter product details:
  - [ ] Product name (required)
  - [ ] Category (dropdown: cleanser, moisturizer, serum, etc.)
  - [ ] Barcode (optional)
  - [ ] Ingredients (optional list or string)
  - [ ] Stabilization days (default 14)
- [ ] Submit (POST /api/users/{user_id}/products)
- [ ] Product appears in routine list
- [ ] Product card shows name, category, added date

### 7.2 Log Routine Event
- [ ] Tap on product in routine list
- [ ] Product detail screen opens
- [ ] Tap "Log Use" button
- [ ] Event creation form:
  - [ ] Action: start, stop, continue (default "continue")
  - [ ] Slot: am, pm, unspecified
  - [ ] Dose/Frequency (optional)
  - [ ] Notes (optional)
  - [ ] Timestamp (defaults to now)
- [ ] Submit (POST /api/users/{user_id}/routine-events)
- [ ] Event appears in product history
- [ ] Recent events shown on dashboard routine widget

### 7.3 Shelf Scan (Premium, Gemini-enabled)
- [ ] Premium user only
- [ ] Tap "Scan Shelf" button
- [ ] Camera launches
- [ ] Take photo of skincare shelf
- [ ] Upload image (POST /api/users/{user_id}/shelf-scan)
- [ ] Backend returns job_id, status="queued"
- [ ] Poll GET /api/users/{user_id}/shelf-scan/{job_id}
- [ ] Loading indicator during processing
- [ ] OCR extracts product names
- [ ] Confirmation screen shows detected products
- [ ] User selects which products to add
- [ ] Selected products added to routine in batch

### 7.4 Product Verdicts (Free Tier Limitation)
- [ ] Free user with 1 product: verdict shown
- [ ] Free user with 2+ products and definitive verdicts:
  - [ ] First product verdict unlocked (shown)
  - [ ] Additional verdicts show "locked" card
  - [ ] Locked card: "Upgrade to Premium for unlimited verdicts"
- [ ] Premium user: all verdicts always shown
- [ ] Evidence_unclear verdicts never locked (no data to withhold)

---

## 8. Experiments

### 8.1 Create Experiment
- [ ] Navigate to Insights tab → Experiments
- [ ] Tap "New Experiment" button
- [ ] Experiment creation form:
  - [ ] Name (required, max 160 chars)
  - [ ] Hypothesis (optional)
  - [ ] Select product from routine (required)
  - [ ] Primary metric: redness_score, texture_score, etc. (default redness)
  - [ ] Target days (default 14, range 1-180)
- [ ] Submit (POST /api/users/{user_id}/experiments)
- [ ] Experiment created with status="active"
- [ ] Experiment card appears in experiments list

### 8.2 Track Experiment
- [ ] During active experiment, captures auto-tagged with experiment_id
- [ ] Routine events for experiment product tagged with experiment_id
- [ ] Experiment detail screen shows:
  - [ ] Progress bar (days completed / target days)
  - [ ] Current metric value vs baseline
  - [ ] Timeline of captures during experiment
  - [ ] Associated routine events

### 8.3 Complete Experiment
- [ ] Navigate to experiment detail
- [ ] Tap "Complete Experiment" button
- [ ] Confirmation dialog
- [ ] Update status (POST /api/users/{user_id}/experiments/{id}/status)
- [ ] Status changed to "completed"
- [ ] Final result summary shown:
  - [ ] Metric change over experiment period
  - [ ] Capture count
  - [ ] Product adherence (routine events logged)
- [ ] "Scientist" achievement unlocks if first completed experiment

### 8.4 Experiment Edge Cases
- [ ] Abandon experiment mid-way (status="abandoned")
- [ ] Multiple active experiments at once
- [ ] Experiment with insufficient captures (< 2 data points)
- [ ] Experiment product removed from routine mid-experiment
- [ ] Premium-only experiments (if applicable)

---

## 9. Q&A Feature

### 9.1 Ask Question
- [ ] Navigate to Insights tab → Q&A
- [ ] Tap "Ask Question" button
- [ ] Enter question text (max 2000 chars)
- [ ] Submit (POST /api/users/{user_id}/qna)
- [ ] New thread created
- [ ] Loading indicator while backend generates answer
- [ ] Answer appears with:
  - [ ] Response text
  - [ ] Citations (if Gemini-enabled)
  - [ ] Scope indicator (cosmetic_tracking vs dermatology_review)
  - [ ] Disclaimer text (required for compliance)

### 9.2 Follow-up Questions
- [ ] In existing thread, tap "Follow up" button
- [ ] Enter follow-up question
- [ ] Submit with thread_id (POST /api/users/{user_id}/qna)
- [ ] Answer appended to thread
- [ ] Conversation history preserved

### 9.3 Triage Responses
- [ ] **Cosmetic tracking scope**
  - [ ] Question about routine, products, metrics
  - [ ] App provides guidance
  - [ ] scope="cosmetic_tracking"

- [ ] **Dermatology review scope**
  - [ ] Question about medical concern, diagnosis, treatment
  - [ ] Response: "This requires professional evaluation"
  - [ ] scope="dermatology_review"
  - [ ] No follow-up allowed in that thread
  - [ ] Disclaimer shown prominently

### 9.4 Q&A Limitations
- [ ] Premium feature (if gated)
- [ ] Free users: limited questions per month
- [ ] Without Gemini API key: generic responses only
- [ ] Rate limiting: max N questions per hour

---

## 10. Account Deletion

### 10.1 Initiate Deletion
- [ ] Navigate to Account tab → Settings
- [ ] Tap "Delete Account" button
- [ ] Warning screen appears:
  - [ ] "This action cannot be undone"
  - [ ] List of data to be deleted
  - [ ] Confirmation required
- [ ] Enter email or type "DELETE" to confirm
- [ ] Submit (DELETE /api/users/{user_id})

### 10.2 Deletion Process
- [ ] Backend marks user for deletion
- [ ] All user data queued for removal:
  - [ ] User profile
  - [ ] Captures and metrics
  - [ ] Photos (encrypted files)
  - [ ] Routine data
  - [ ] Experiments
  - [ ] Q&A threads
  - [ ] Achievements
  - [ ] Subscription records
  - [ ] Engagement events
- [ ] User logged out of app
- [ ] Firebase Auth user deleted
- [ ] Local app data cleared
- [ ] User redirected to welcome screen

### 10.3 Deletion Verification
- [ ] **Backend verification**
  - [ ] Run SQL query: SELECT * FROM users WHERE user_id='{deleted_id}'
  - [ ] Result: No rows (or deleted_at timestamp set)
  - [ ] Check photos table: no rows for user_id
  - [ ] Check captures table: no rows for user_id
  - [ ] Check products table: no rows for user_id

- [ ] **Photo storage verification**
  - [ ] Check SKINPROOF_PHOTO_DIR or S3 bucket
  - [ ] No files matching user_id prefix

- [ ] **Firebase verification**
  - [ ] User not in Firebase Auth users list

- [ ] **Re-creation test**
  - [ ] Try to sign up with same email
  - [ ] Should succeed as new user (no old data)

### 10.4 Deletion Edge Cases
- [ ] Delete account with active premium subscription
- [ ] Delete account with pending uploads in outbox
- [ ] Delete account while offline (should queue deletion)

---

## 11. Premium Features

### 11.1 Premium Status Check
- [ ] GET /api/users/{user_id}/dashboard
- [ ] Check entitlement.plan == "premium"
- [ ] Check entitlement.status == "active"
- [ ] Both must be true for premium access

### 11.2 Free User Experience
- [ ] Premium features show lock icon
- [ ] Tapping locked feature shows upgrade prompt
- [ ] Locked features:
  - [ ] Freeze days
  - [ ] Unlimited product verdicts (after first free)
  - [ ] Shelf scan OCR
  - [ ] Advanced insights
  - [ ] Experiments (if gated)
  - [ ] Unlimited Q&A

### 11.3 Premium Upgrade Flow
- [ ] Tap "Upgrade to Premium" button
- [ ] Pricing screen shown (via in-app billing or web link)
- [ ] User completes purchase
- [ ] POST /api/users/{user_id}/subscriptions/upgrade
- [ ] Backend updates entitlement
- [ ] App refreshes dashboard
- [ ] Premium features now unlocked
- [ ] Confirmation: "Welcome to Premium!"

### 11.4 Premium Downgrade
- [ ] Subscription expires or cancelled
- [ ] Backend updates entitlement.status to "inactive"
- [ ] App fetches updated dashboard
- [ ] Premium features locked again
- [ ] Data preserved (not deleted)
- [ ] Freeze days stop regenerating
- [ ] Product verdicts locked again (except 1 free)

---

## 12. Navigation and UI

### 12.1 Bottom Navigation
- [ ] Five tabs: Home, Routine, Insights, Discover, Account
- [ ] Active tab highlighted with honey accent color
- [ ] Smooth transitions between tabs
- [ ] Tab state preserved on rotation

### 12.2 Home Screen
- [ ] Streak counter prominent
- [ ] Days since last capture
- [ ] Quick stats: total captures, current streak, achievements
- [ ] Recent insights widget
- [ ] "New Capture" FAB (Floating Action Button)
- [ ] Capture history timeline (recent 5-10)

### 12.3 Routine Screen
- [ ] List of all products
- [ ] Recent routine events
- [ ] "Add Product" button
- [ ] "Scan Shelf" button (premium)
- [ ] Filter by product category
- [ ] Search products

### 12.4 Insights Screen
- [ ] Experiments section
- [ ] Q&A section
- [ ] Metric trends (charts)
- [ ] Product verdicts
- [ ] Premium insights (locked for free)

### 12.5 Discover Screen
- [ ] Commerce offers (GET /api/users/{user_id}/commerce/offers)
- [ ] Recommended products
- [ ] Educational content
- [ ] No upgrade gate (per API docs)

### 12.6 Account Screen
- [ ] Profile information
- [ ] Achievements
- [ ] Settings
- [ ] Subscription status
- [ ] Privacy policy
- [ ] Sign out
- [ ] Delete account

### 12.7 UI State Management
- [ ] Loading states (shimmer skeletons)
- [ ] Error states (with retry button)
- [ ] Empty states (with CTA)
- [ ] Polling indicators (for async jobs)
- [ ] Offline indicator in top bar

---

## 13. Push Notifications (Optional)

### 13.1 Daily Reminder
- [ ] User enables notifications in settings
- [ ] Set reminder time preference
- [ ] Notification triggers at set time
- [ ] Message: "Don't break your streak! Take today's capture"
- [ ] Tap notification opens app to capture screen

### 13.2 Achievement Notifications
- [ ] Achievement unlocks
- [ ] Notification sent immediately
- [ ] Message: "Achievement Unlocked: {name}"
- [ ] Tap opens achievements screen

### 13.3 Experiment Notifications
- [ ] Experiment reaches target day
- [ ] Notification: "Your experiment is complete! View results"
- [ ] Tap opens experiment detail screen

---

## 14. Error Handling

### 14.1 Network Errors
- [ ] No internet connection: clear offline indicator, queue actions
- [ ] Timeout: retry with backoff
- [ ] 500 error: user-friendly message, retry option
- [ ] 403 Forbidden: check premium status, show upgrade prompt if needed
- [ ] 422 Validation: display field-specific errors

### 14.2 Camera Errors
- [ ] Permission denied: show rationale, link to settings
- [ ] Camera unavailable: fallback to gallery picker
- [ ] Low light: warning before capture
- [ ] Storage full: clear error message

### 14.3 Firebase Auth Errors
- [ ] Token expired: refresh token automatically
- [ ] Refresh failed: re-authenticate user
- [ ] Sign out and redirect to login

---

## 15. App Lifecycle

### 15.1 First Launch
- [ ] Welcome screen
- [ ] Sign up / Sign in

### 15.2 Subsequent Launch
- [ ] Check local user_id
- [ ] Verify user exists (GET /api/users/{user_id}/profile)
- [ ] If exists: load dashboard
- [ ] If not found: clear session, show welcome

### 15.3 Background/Foreground
- [ ] App backgrounded: save state
- [ ] App foregrounded: refresh dashboard
- [ ] Check for pending syncs
- [ ] Update streak status

### 15.4 App Update
- [ ] New version installed
- [ ] Database migrations run (Room)
- [ ] Check backend API version compatibility
- [ ] Force update if backend requires newer client

---

## Test Result Summary

| Category | Total Tests | Passed | Failed | Blocked | Notes |
|----------|-------------|--------|--------|---------|-------|
| Onboarding | | | | | |
| Capture | | | | | |
| Streaks | | | | | |
| Achievements | | | | | |
| Offline Mode | | | | | |
| Comparison | | | | | |
| Routine | | | | | |
| Experiments | | | | | |
| Q&A | | | | | |
| Account Deletion | | | | | |
| Premium | | | | | |
| Navigation | | | | | |
| Notifications | | | | | |
| Error Handling | | | | | |
| Lifecycle | | | | | |

---

## Test Environment Details

**Date Tested:** ___________
**Tester:** ___________
**App Version:** ___________
**Backend Version:** ___________
**Device Model:** ___________
**Android Version:** ___________
**Network:** ___________

---

## Sign-off

- [ ] All critical paths tested and passing
- [ ] Known issues documented
- [ ] Ready for next testing phase

**Tester Signature:** ___________
**Date:** ___________
