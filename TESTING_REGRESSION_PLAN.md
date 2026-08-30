# GlowUp AI - Regression Test Plan

Last updated: 2026-08-30

## Purpose
This regression test plan ensures that new code changes, features, or bug fixes don't break existing functionality. Run this test suite before every release.

---

## Test Execution Strategy

### When to Run Regression Tests
- [ ] Before every production release
- [ ] After major feature additions
- [ ] After bug fixes that touch core flows
- [ ] After backend API changes
- [ ] After dependency updates (Android, Firebase, Retrofit, etc.)
- [ ] After database schema migrations

### Test Levels
- **P0 (Critical):** Core user journeys - must pass before release
- **P1 (High):** Important features - should pass, release notes if fail
- **P2 (Medium):** Nice-to-have features - can defer fix
- **P3 (Low):** Edge cases - document and schedule

### Test Devices Matrix
Test on minimum 3 devices representing:
- Low-end device (Android 8.0, 2GB RAM)
- Mid-range device (Android 11, 4GB RAM)
- High-end device (Android 13+, 6GB+ RAM)

---

## P0: Critical Path Tests

### 1. Authentication (P0)
**Impact:** Users cannot access app without auth

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Sign up with email/password | Account created, user_id stored | | |
| Sign in with existing account | Dashboard loads with user data | | |
| Sign in with Google | Google auth succeeds, user_id stored | | |
| Invalid credentials | Error message shown, no crash | | |
| Firebase Auth failure | Graceful error, retry option | | |

**Backend Endpoints:**
- POST /api/users
- GET /api/users/{user_id}/profile

**Automation:** Android Espresso + Firebase Test Lab

---

### 2. Consent Flow (P0)
**Impact:** Required for legal compliance

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| View consent after sign up | Consent screen shown, policy version visible | | |
| Accept consent | Consent saved, user proceeds | | |
| Reject consent | Cannot proceed past consent | | |
| Consent already given | Skip consent screen | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/consent
- GET /api/users/{user_id}/profile (consent field)

**Automation:** Espresso UI test

---

### 3. Baseline Capture (P0)
**Impact:** Users cannot use app without baseline

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| First capture after consent | Capture guide shown | | |
| Camera permission granted | Camera launches, face detection active | | |
| Good quality capture | Photo accepted, metrics calculated | | |
| Poor quality capture | Retry prompt shown | | |
| Upload succeeds | Baseline saved, dashboard unlocked | | |
| Upload fails | Retry option, capture stays in outbox | | |

**Backend Endpoints:**
- GET /api/users/{user_id}/capture-guide
- POST /api/users/{user_id}/captures

**Automation:** Espresso + Mockito for camera, mocked backend responses

---

### 4. Dashboard Load (P0)
**Impact:** Primary screen users see

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Load dashboard after baseline | Metrics shown, streak = 1 | | |
| Load with existing data | History, stats, recent captures | | |
| Offline mode | Cached data shown, offline indicator | | |
| Empty state handling | Appropriate empty messages | | |

**Backend Endpoints:**
- GET /api/users/{user_id}/dashboard
- GET /api/users/{user_id}/history

**Automation:** Espresso + Mockito

---

### 5. Subsequent Captures (P0)
**Impact:** Core daily engagement flow

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Take 2nd capture | New snapshot, metrics updated | | |
| Streak increments | Streak += 1 on next calendar day | | |
| Same day multiple captures | All saved, streak increments once | | |
| Capture quality validation | Low quality rejected | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/captures
- GET /api/users/{user_id}/dashboard

**Automation:** Espresso

---

### 6. Streak Tracking (P0)
**Impact:** Key retention mechanic

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Day 1 baseline | Streak = 1 | | |
| Day 2 capture | Streak = 2 | | |
| Miss day 3 | Streak resets to 0 | | |
| New capture after reset | Streak = 1 again | | |
| Freeze day used (premium) | Missed day doesn't break streak | | |
| Multiple captures same day | Streak increments only once | | |

**Backend Endpoints:**
- GET /api/users/{user_id}/dashboard (streak field)

**Automation:** Unit test StreakCalculator.kt, integration test with mocked time

---

### 7. Offline Capture (P0)
**Impact:** Users can capture without internet

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Capture while offline | Saved to local outbox (Room) | | |
| Offline indicator shown | Top bar shows "Offline" | | |
| Reconnect to internet | WorkManager syncs pending captures | | |
| Sync succeeds | Capture updated with server data | | |
| Sync fails | Retry scheduled, notification shown | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/captures (when online)

**Automation:** Espresso + toggle device connectivity

---

### 8. Sign Out (P0)
**Impact:** Security and session management

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Sign out from account screen | Firebase session cleared | | |
| Local data cleared | user_id removed | | |
| Redirect to welcome | Login screen shown | | |
| Sign in again | Dashboard loads previous user data | | |

**Backend Endpoints:** None (client-side only)

**Automation:** Espresso

---

## P1: High Priority Features

### 9. Add Product to Routine (P1)
**Impact:** Core feature for tracking

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Add new product | Product saved to backend | | |
| Product appears in list | Routine screen shows product | | |
| Product details displayed | Name, category, added date | | |
| Invalid product name | Validation error shown | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/products
- GET /api/users/{user_id}/products

**Automation:** Espresso + Mockito

---

### 10. Log Routine Event (P1)
**Impact:** Tracks product usage

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Log event for product | Event saved to backend | | |
| Event appears in history | Product detail shows event | | |
| Timestamp recorded | Defaults to now, can customize | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/routine-events
- GET /api/users/{user_id}/products/{id}/history

**Automation:** Espresso

---

### 11. Create Experiment (P1)
**Impact:** Premium feature, advanced tracking

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Create new experiment | Experiment saved, status=active | | |
| Experiment appears in list | Insights tab shows experiment | | |
| Product linked | Experiment tied to specific product | | |
| Progress tracking | Days completed updated daily | | |
| Complete experiment | Status changes to completed | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/experiments
- GET /api/users/{user_id}/experiments
- POST /api/users/{user_id}/experiments/{id}/status

**Automation:** Espresso

---

### 12. Q&A Thread (P1)
**Impact:** User engagement and support

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Ask question | Thread created, answer generated | | |
| View answer | Response with disclaimer shown | | |
| Follow-up question | Appended to existing thread | | |
| Triage response | Dermatology scope triggers referral | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/qna
- GET /api/users/{user_id}/qna

**Automation:** Espresso + mocked Gemini responses

---

### 13. Achievements (P1)
**Impact:** Gamification and retention

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| First capture achievement | Unlocked after baseline | | |
| Week Warrior achievement | Unlocked after 7 day streak | | |
| Achievement notification | Push notification sent (if enabled) | | |
| View achievements screen | Shows locked and unlocked | | |

**Backend Endpoints:**
- GET /api/users/{user_id}/dashboard (achievements field)

**Automation:** Unit test + Espresso

---

### 14. Photo Comparison (P1)
**Impact:** Core value proposition

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Select 2 photos | Comparison view loads | | |
| Side-by-side display | Both photos visible | | |
| Metric deltas shown | Percentage changes calculated | | |
| Export comparison | Image saved to gallery | | |

**Backend Endpoints:**
- GET /api/users/{user_id}/history (for photo list)

**Automation:** Espresso

---

### 15. Premium Upgrade (P1)
**Impact:** Monetization

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| View premium features | Lock icons shown for free users | | |
| Tap upgrade button | Pricing screen shown | | |
| Complete purchase | Entitlement updated | | |
| Premium features unlocked | All locks removed | | |
| Premium status persists | After app restart | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/subscriptions/upgrade
- GET /api/users/{user_id}/dashboard (entitlement field)

**Automation:** Manual (in-app purchase testing)

---

## P2: Medium Priority Features

### 16. Shelf Scan OCR (P2)
**Impact:** Premium feature, convenience

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Take shelf photo | Image uploaded | | |
| Poll job status | Job completes, products detected | | |
| Confirm selections | Selected products added to routine | | |
| No products detected | Error handled gracefully | | |

**Backend Endpoints:**
- POST /api/users/{user_id}/shelf-scan
- GET /api/users/{user_id}/shelf-scan/{job_id}
- POST /api/users/{user_id}/shelf-scan/{job_id}/confirm

**Automation:** Manual (requires Gemini API)

---

### 17. Commerce Offers (P2)
**Impact:** Affiliate revenue

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| View Discover tab | Offers loaded and displayed | | |
| Tap offer | Opens external link | | |
| Click tracked | Engagement event logged | | |

**Backend Endpoints:**
- GET /api/users/{user_id}/commerce/offers
- POST /api/users/{user_id}/commerce/offers/{id}/click

**Automation:** Espresso

---

### 18. Account Settings (P2)
**Impact:** User preferences

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Update profile | Display name, skin type changed | | |
| Notification preferences | Settings saved | | |
| Privacy settings | Consent preferences editable | | |

**Backend Endpoints:**
- PATCH /api/users/{user_id}/profile

**Automation:** Espresso

---

### 19. Push Notifications (P2)
**Impact:** Re-engagement

| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Daily reminder enabled | Notification sent at set time | | |
| Achievement notification | Sent on unlock | | |
| Tap notification | Opens relevant screen | | |
| Disable notifications | No notifications sent | | |

**Backend Endpoints:** None (Firebase Cloud Messaging)

**Automation:** Manual + Firebase Test Lab

---

## P3: Low Priority / Edge Cases

### 20. Timezone Changes (P3)
| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| User travels across timezones | Streak logic handles correctly | | |
| Capture timestamps in UTC | Server stores UTC, displays local | | |

---

### 21. Device Rotation (P3)
| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Rotate during capture | Camera state preserved | | |
| Rotate during form input | Form data preserved | | |
| Rotate on any screen | No data loss | | |

---

### 22. Low Storage (P3)
| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Capture with low storage | Clear error message | | |
| App cache cleared | Graceful degradation | | |

---

### 23. Slow Network (P3)
| Test Case | Expected Result | Status | Notes |
|-----------|----------------|--------|-------|
| Upload on 3G | Progress indicator, longer timeout | | |
| Fetch dashboard on slow network | Loading state, retry on timeout | | |

---

## Backend Regression Tests

Run backend test suite before deployment:

```bash
cd backend
pytest tests/ -v
```

**Expected:** All 58+ tests passing

Key backend regression tests:
- Authentication (test_auth.py)
- Core API endpoints (test_complete.py)
- Capture processing (test_core.py)
- Experiment logic (test_experience.py)
- Growth features (test_growth_features.py)
- Gemini integration (test_google_ai.py)

---

## Database Integrity Checks

After schema migrations:

```sql
-- Check no orphaned captures
SELECT COUNT(*) FROM captures WHERE user_id NOT IN (SELECT user_id FROM users);

-- Check no orphaned products
SELECT COUNT(*) FROM products WHERE user_id NOT IN (SELECT user_id FROM users);

-- Check no orphaned experiments
SELECT COUNT(*) FROM experiments WHERE user_id NOT IN (SELECT user_id FROM users);

-- Check consent required
SELECT COUNT(*) FROM users WHERE consent_given IS NULL;

-- Check baseline captures exist for all active users
SELECT user_id FROM users WHERE user_id NOT IN (SELECT DISTINCT user_id FROM captures WHERE is_baseline = true);
```

All queries should return 0 or expected results.

---

## API Contract Validation

Verify API endpoints return expected schemas:

```bash
# Health check
curl https://api.glowup.ai/api/health

# Dashboard schema (with test user_id)
curl https://api.glowup.ai/api/users/{test_user_id}/dashboard

# History schema
curl https://api.glowup.ai/api/users/{test_user_id}/history
```

Validate:
- Response status 200
- Expected fields present
- Field types match documentation
- Timestamps in ISO-8601 format
- Nested objects structured correctly

---

## Automated Test Suite

### Android Unit Tests
```bash
./gradlew test
```

Tests in: `app/src/test/java/com/glowup/ai/`
- Domain logic (StreakCalculator, SessionStateMachine)
- ViewModels
- Repository layer
- Utility functions

### Android Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

Tests in: `app/src/androidTest/java/com/glowup/ai/`
- UI flows (Espresso)
- Database (Room)
- WorkManager background jobs

### Backend Unit Tests
```bash
cd backend
pytest tests/
```

Tests:
- Service layer logic
- API endpoint contracts
- Data validation
- Authentication flow
- Quality checks

---

## Performance Regression

Check for performance degradation:

| Metric | Baseline | Current | Status |
|--------|----------|---------|--------|
| App cold start time | < 2s | | |
| Dashboard load time | < 1s | | |
| Capture upload time (WiFi) | < 3s | | |
| History screen load | < 1.5s | | |
| APK size | < 30MB | | |
| Memory usage (idle) | < 150MB | | |
| Memory usage (active) | < 300MB | | |

Tools:
- Android Profiler
- Firebase Performance Monitoring
- Crashlytics for crash-free rate

---

## Regression Test Checklist

Before releasing version X.Y.Z:

- [ ] All P0 tests passing
- [ ] At least 90% of P1 tests passing (document failures)
- [ ] Backend test suite passing (58+ tests)
- [ ] Android unit tests passing
- [ ] Android instrumented tests passing
- [ ] Database integrity checks clean
- [ ] API contract validation successful
- [ ] Performance metrics within acceptable range
- [ ] No new crashes reported in test build
- [ ] Firebase Test Lab passes on multiple devices
- [ ] Manual smoke test on 3 physical devices
- [ ] Known issues documented in release notes

---

## Issue Tracking

When regression test fails:

1. Document in issue tracker:
   - Test case name
   - Expected vs actual result
   - Steps to reproduce
   - Device/OS version
   - Backend version
   - Screenshots/logs

2. Prioritize based on P0/P1/P2/P3

3. Fix or defer:
   - P0: Block release until fixed
   - P1: Fix or document in release notes
   - P2/P3: Schedule for next sprint

---

## Sign-off

- [ ] All required regression tests completed
- [ ] Test report generated
- [ ] Known issues documented
- [ ] Release approved

**QA Lead:** ___________
**Date:** ___________
**Build Version:** ___________
