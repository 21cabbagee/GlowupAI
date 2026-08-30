# GlowUp AI - Bug Report Template

Last updated: 2026-08-30

---

## Quick Bug Report Template

**Copy this template for quick bug reports:**

```
TITLE: [Short description of the bug]

PRIORITY: Critical / High / Medium / Low

DESCRIPTION:
- What happened: [Describe the issue]
- What should happen: [Expected behavior]

STEPS TO REPRODUCE:
1. [First step]
2. [Second step]
3. [Bug occurs]

ENVIRONMENT:
- Device: [e.g., Pixel 5]
- Android Version: [e.g., Android 12]
- App Version: [e.g., 1.2.0]
- Network: WiFi / 4G / Offline

SCREENSHOT: [Attach if available]
```

---

## Detailed Bug Report Template

Use this for complex bugs requiring more information.

---

### Bug Information

**Bug ID:** _________ (assigned by tracking system)

**Report Date:** _________

**Reporter:** _________

**Title:** _________________________________________
*(Short, descriptive title: "Capture button freezes after 3rd photo")*

---

### Priority Classification

**Priority:** (Select one)

- [ ] **P0 - Critical (Blocker)**
  - App crashes on launch
  - Data loss
  - Security vulnerability
  - Core feature completely broken (capture, sign in)
  - Production is down
  - **Action:** Fix immediately, hotfix if in production

- [ ] **P1 - High (Major)**
  - Major feature broken but workaround exists
  - Affects many users
  - Significant UX degradation
  - Data not syncing
  - **Action:** Fix in current sprint

- [ ] **P2 - Medium (Minor)**
  - Minor feature broken
  - Affects small number of users
  - Cosmetic issues
  - Edge case bugs
  - **Action:** Fix in next release

- [ ] **P3 - Low (Trivial)**
  - Typos, minor UI polish
  - Rare edge cases
  - Nice-to-have fixes
  - **Action:** Backlog, fix when convenient

---

### Bug Category

**Category:** (Select one)

- [ ] Crash / ANR (Application Not Responding)
- [ ] Functionality (feature doesn't work)
- [ ] UI/UX (visual or interaction issue)
- [ ] Performance (slow, laggy, battery drain)
- [ ] Data Loss / Corruption
- [ ] Network / API
- [ ] Authentication / Authorization
- [ ] Database / Storage
- [ ] Camera / Media
- [ ] Notification
- [ ] Offline Mode
- [ ] Third-party integration (Firebase, Gemini)
- [ ] Backend Issue
- [ ] Other: _________

---

### Affected Feature

**Feature:** (Select one or more)

- [ ] Sign Up / Sign In
- [ ] Onboarding (Consent, Baseline)
- [ ] Capture Flow
- [ ] Dashboard
- [ ] History / Timeline
- [ ] Photo Comparison
- [ ] Streak Tracking
- [ ] Achievements
- [ ] Routine Tracking
- [ ] Products
- [ ] Experiments
- [ ] Q&A
- [ ] Insights
- [ ] Discover / Commerce
- [ ] Account / Settings
- [ ] Notifications
- [ ] Offline Sync
- [ ] Premium Features
- [ ] Other: _________

---

### Description

**What Happened:**
*(Describe what went wrong. Be specific and factual.)*

Example:
> When I tap the "New Capture" button on the dashboard, the camera opens but the face detection overlay doesn't appear. The screen shows the camera preview but no guidance indicators. After 5 seconds, the app freezes and I see an ANR dialog.

---

**What Should Happen:**
*(Describe the expected behavior.)*

Example:
> The camera should open with face detection active. Green/red indicators should guide me to position my face correctly. The capture button should be enabled once face is detected.

---

**Impact:**
*(How does this affect users? How many users are impacted?)*

- [ ] All users
- [ ] Premium users only
- [ ] Free users only
- [ ] Specific device models: _________
- [ ] Specific Android versions: _________
- [ ] Users on WiFi / 4G / Offline
- [ ] First-time users only
- [ ] Existing users only

**Severity:**
- [ ] Blocks primary use case
- [ ] Workaround available
- [ ] Minor inconvenience
- [ ] Cosmetic only

---

### Steps to Reproduce

**Reproducibility:** (Select one)
- [ ] Always (100%)
- [ ] Often (75%+)
- [ ] Sometimes (25-75%)
- [ ] Rarely (<25%)
- [ ] Once (cannot reproduce)

**Steps:**

1. _________________________________________
2. _________________________________________
3. _________________________________________
4. _________________________________________
5. **Bug occurs:** _________________________

**Prerequisites:**
*(Any setup needed before reproducing? E.g., "User must have 10+ captures")*

- _________________________________________
- _________________________________________

---

### Environment Details

**Device Information:**
- Device Model: _________ (e.g., Samsung Galaxy S21, Pixel 6 Pro)
- Manufacturer: _________
- Screen Size: _________ (e.g., 6.2", 1080x2400)
- RAM: _________ (e.g., 8GB)
- Storage Available: _________ (e.g., 50GB free)
- Rooted: Yes / No

**Software:**
- Android Version: _________ (e.g., Android 12)
- OS Build: _________ (e.g., SKQ1.211103.001)
- App Version: _________ (e.g., 1.2.0 build 42)
- Backend Version: _________ (if known)

**Network:**
- Connection Type: WiFi / 4G / 3G / Offline
- Network Speed: Fast / Moderate / Slow
- VPN Active: Yes / No

**Account Type:**
- Free / Premium
- Days since signup: _________
- Total captures: _________

---

### Logs and Diagnostics

**Android Logcat:**
*(Attach full logcat if crash occurred)*

```
# To get logcat:
adb logcat -d > bug_report.txt

# Or filter for app:
adb logcat -d | grep "com.glowup.ai" > bug_report.txt
```

**Paste relevant logs here:**
```
[Paste logs]
```

**Stack Trace:**
*(If app crashed, paste crash stack trace)*

```
[Paste stack trace from Crashlytics or logcat]
```

**Network Logs:**
*(If API-related, include request/response)*

Request:
```
POST /api/users/{user_id}/captures
Authorization: Bearer [token]
Content-Type: application/json

{
  "user_id": "...",
  "image_base64": "...",
  "is_baseline": false
}
```

Response:
```
Status: 500 Internal Server Error
{
  "detail": "Internal server error"
}
```

---

### Visual Evidence

**Screenshots:**
- [ ] Attached (screenshot_1.png, screenshot_2.png)

**Screen Recording:**
- [ ] Attached (bug_recording.mp4)

**Before/After:**
- [ ] Before (expected state)
- [ ] After (buggy state)

**Instructions for capturing:**

Screenshots:
- Power + Volume Down (most Android devices)

Screen Recording:
- Swipe down → Quick Settings → Screen Record
- Or: `adb shell screenrecord /sdcard/bug.mp4`

---

### Additional Context

**Frequency:**
- First occurrence: _________ (date/time)
- How often: Daily / Weekly / Rarely
- Occurs on: Every action / Specific conditions

**Related Issues:**
- Is this related to any existing bugs? _________
- Did this start after a recent update? _________

**User Actions Before Bug:**
*(What were you doing in the app before the bug occurred?)*

- _________________________________________
- _________________________________________

**Workaround:**
*(Is there a way to work around this bug?)*

- [ ] No workaround
- [ ] Workaround: _____________________________

---

### Backend Investigation (For API/Backend Bugs)

**Backend Logs:**
*(If backend team, include server logs)*

```
[Paste backend logs showing error]
```

**Database State:**
*(If data corruption, show DB state)*

```sql
-- Query to investigate
SELECT * FROM captures WHERE user_id = '...' ORDER BY captured_at DESC LIMIT 5;
```

Result:
```
[Paste query result]
```

**API Response Time:**
- Expected: < 2s
- Actual: _________ seconds

**Backend Metrics:**
- CPU usage: _________%
- Memory usage: _________%
- Database query time: _________ ms
- Queue depth: _________

---

### Regression Information

**Did this ever work?**
- [ ] Yes, worked in version: _________
- [ ] No, never worked
- [ ] Unknown

**Recent Changes:**
*(If yes, what changed between working and broken?)*

- Code commit: _________
- Deployment date: _________
- Related PR: _________

---

## Bug Report Examples

### Example 1: Crash Bug

```
TITLE: App crashes when uploading photo on Android 11

PRIORITY: P0 - Critical

CATEGORY: Crash

FEATURE: Capture Flow

DESCRIPTION:
What happened: After taking a capture photo and tapping "Use Photo", 
the app immediately crashes with OutOfMemoryError. This happens every 
time on my device.

What should happen: Photo should upload to backend and metrics should 
be calculated.

STEPS TO REPRODUCE:
1. Open app, tap "New Capture"
2. Camera opens, take photo
3. Preview screen appears
4. Tap "Use Photo"
5. App crashes immediately

ENVIRONMENT:
- Device: OnePlus 7T
- Android Version: Android 11
- App Version: 1.1.0
- Network: WiFi
- RAM: 8GB

LOGCAT:
java.lang.OutOfMemoryError: Failed to allocate a 52428800 byte allocation 
with 16777216 free bytes and 16MB until OOM, target footprint 268435456, 
growth limit 268435456
    at com.glowup.ai.capture.CaptureViewModel.encodeImage(CaptureViewModel.kt:142)
    ...

REPRODUCIBILITY: Always (100%)

SCREENSHOT: [crash_screenshot.png]
```

---

### Example 2: UI Bug

```
TITLE: Streak counter shows wrong number after timezone change

PRIORITY: P2 - Medium

CATEGORY: UI/UX

FEATURE: Streak Tracking

DESCRIPTION:
What happened: I traveled from PST to EST. After timezone change, my 
streak counter shows "0" even though I captured yesterday.

What should happen: Streak should remain intact across timezone changes.

STEPS TO REPRODUCE:
1. Have active streak (e.g., 5 days)
2. Capture in PST timezone
3. Change device timezone to EST
4. Open app next day
5. Streak shows 0 instead of 6

ENVIRONMENT:
- Device: Pixel 5
- Android Version: Android 13
- App Version: 1.2.0
- Network: WiFi

REPRODUCIBILITY: Always (100%)

SCREENSHOT: [streak_bug.png]

WORKAROUND: Capture again to restart streak
```

---

### Example 3: Performance Bug

```
TITLE: History screen takes 10+ seconds to load with 100+ captures

PRIORITY: P1 - High

CATEGORY: Performance

FEATURE: History / Timeline

DESCRIPTION:
What happened: When I open the History tab, it takes 10-15 seconds to 
load all my captures. The screen shows a loading spinner for a long time 
and the app feels frozen.

What should happen: History should load in < 2 seconds with progressive 
rendering.

STEPS TO REPRODUCE:
1. Have account with 100+ captures
2. Open app
3. Tap History tab
4. Observe long loading time

ENVIRONMENT:
- Device: Samsung Galaxy A52
- Android Version: Android 12
- App Version: 1.2.0
- Network: WiFi
- Captures: 127

REPRODUCIBILITY: Always (100%)

PERFORMANCE METRICS:
- Time to first item: 8 seconds
- Time to fully loaded: 14 seconds
- Memory usage spike: +200MB

VIDEO: [slow_loading.mp4]
```

---

## Bug Triage Process

### Initial Triage (Within 24 hours)

1. **Validate Bug:**
   - [ ] Can we reproduce it?
   - [ ] Is this a duplicate of existing bug?
   - [ ] Is this a feature request in disguise?

2. **Assign Priority:**
   - Based on impact and severity
   - Consider number of users affected

3. **Assign Owner:**
   - Android engineer for app bugs
   - Backend engineer for API bugs
   - Product manager for UX issues

4. **Update Status:**
   - New → Confirmed → In Progress → Fixed → Closed

---

### Priority SLA (Service Level Agreement)

| Priority | Response Time | Resolution Time |
|----------|---------------|-----------------|
| P0 (Critical) | 1 hour | 24 hours (or hotfix) |
| P1 (High) | 1 day | 1 week |
| P2 (Medium) | 3 days | 2-4 weeks |
| P3 (Low) | 1 week | Backlog |

---

## Bug Tracking

### Status Workflow

```
New → Needs Triage → Confirmed → In Progress → In Review → 
Fixed (in staging) → Verified → Closed
```

**Alternative paths:**
- New → Duplicate → Closed
- New → Cannot Reproduce → Needs More Info → (back to Confirmed or Closed)
- New → Won't Fix → Closed

---

### Bug Tracking Fields

| Field | Description |
|-------|-------------|
| Bug ID | Unique identifier (auto-assigned) |
| Title | Short description |
| Status | Current workflow status |
| Priority | P0 / P1 / P2 / P3 |
| Category | Type of bug |
| Feature | Affected feature area |
| Reporter | Who reported the bug |
| Assignee | Who is fixing the bug |
| Date Reported | When bug was reported |
| Date Fixed | When bug was fixed |
| Fix Version | App version with fix |
| Related PR | Pull request with fix |

---

## Bug Reporting Best Practices

### DO:
- ✅ Include clear steps to reproduce
- ✅ Attach screenshots/videos when relevant
- ✅ Provide device and environment details
- ✅ Report one bug per report
- ✅ Check for duplicates before reporting
- ✅ Be specific and factual
- ✅ Include logs and stack traces for crashes

### DON'T:
- ❌ Assume the cause ("It's probably the database")
- ❌ Mix multiple bugs in one report
- ❌ Report feature requests as bugs
- ❌ Be vague ("It doesn't work")
- ❌ Skip environment details
- ❌ Report without trying to reproduce
- ❌ Include user_ids or sensitive data in public reports

---

## Bug Report Submission

### Where to Report Bugs

**Internal Team:**
- GitHub Issues: https://github.com/org/glowup-ai/issues
- Jira: [Project key]
- Slack: #glowup-bugs channel

**Beta Testers:**
- Beta feedback form: [URL]
- Email: beta@glowup.ai
- Slack: #glowup-beta channel

**End Users (Post-Launch):**
- In-app feedback: Account → Report Bug
- Support email: support@glowup.ai
- Help center: https://help.glowup.ai

---

## Automated Bug Reporting

### Crashlytics Integration

Crashes automatically reported to Firebase Crashlytics with:
- Stack trace
- Device info
- App version
- Custom keys (user_id, screen, action)

**Check Crashlytics:**
- Firebase Console → Crashlytics
- Group by crash type
- Review top crashes weekly

### ANR (Application Not Responding)

ANRs automatically reported with:
- Main thread trace
- All thread traces
- Heap dump (if available)

---

## Bug Metrics

Track bug health metrics:

| Metric | Target |
|--------|--------|
| Crash-free rate | > 99.5% |
| ANR rate | < 0.1% |
| P0 bugs in production | 0 |
| P1 resolution time | < 7 days |
| Bug backlog size | < 50 |
| Bug resolution rate | > 90% closed/month |

---

## Bug Report Template Checklist

Before submitting, verify:

- [ ] Title is clear and descriptive
- [ ] Priority is assigned
- [ ] Category and feature are selected
- [ ] Description includes "what happened" and "what should happen"
- [ ] Steps to reproduce are detailed
- [ ] Reproducibility is noted
- [ ] Environment details are complete
- [ ] Screenshots/video attached (if applicable)
- [ ] Logs included for crashes
- [ ] Searched for duplicates
- [ ] One bug per report

---

## Contact

**Bug Reporting Issues:**
If you have questions about reporting bugs, contact:
- Email: qa@glowup.ai
- Slack: @qa-team

**Urgent Production Issues:**
- On-call rotation: [PagerDuty/phone]
- Slack: #glowup-incidents

---

**Thank you for helping us build a better GlowUp AI!**

Every bug report makes the app more stable and reliable for all users.
