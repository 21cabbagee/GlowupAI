# GlowUp AI - User Acceptance Testing (UAT) Guide

Last updated: 2026-08-30

## Overview
This User Acceptance Testing guide helps non-technical stakeholders, beta testers, and early users validate that GlowUp AI meets business requirements and delivers a great user experience.

**Target Audience:** Product managers, beta testers, early adopters, business stakeholders

---

## What is UAT?

UAT verifies that the app:
- Solves real user problems
- Is intuitive and easy to use
- Meets business requirements
- Is ready for real-world deployment

Unlike technical testing (which checks "does it work?"), UAT asks: "Is this what users need?"

---

## UAT Preparation

### Prerequisites

- [ ] UAT environment deployed (staging backend)
- [ ] Test APK distributed to testers
- [ ] Test accounts created (free and premium)
- [ ] Feedback collection method ready (survey, Slack channel, email)
- [ ] Testing timeframe defined (e.g., 1 week)

### Test Participants

Recruit 10-20 testers representing target users:
- **Skincare beginners:** New to tracking routines
- **Skincare enthusiasts:** Active routine trackers
- **Casual users:** Occasional app users
- **Power users:** Daily engagers, multiple products
- **Device diversity:** Various Android phones, OS versions
- **Network diversity:** WiFi, mobile data, low connectivity

---

## UAT Test Scenarios

### Scenario 1: First-Time User Onboarding

**Goal:** New user completes sign up through first capture

**Steps:**
1. Install the app
2. Open for the first time
3. Sign up with email/password or Google
4. Read and accept consent
5. Follow capture guide instructions
6. Take baseline photo
7. View initial dashboard

**Success Criteria:**
- [ ] Sign up process is clear and fast (< 2 minutes)
- [ ] Consent language is understandable
- [ ] Capture guide instructions are easy to follow
- [ ] Camera guidance helps achieve good capture
- [ ] Dashboard shows meaningful data immediately
- [ ] User understands what to do next

**Feedback Questions:**
- On a scale of 1-5, how easy was sign up?
- Did you understand what consent you were giving?
- Were the camera instructions helpful?
- Did you understand your baseline results?
- What would you do next in the app?

---

### Scenario 2: Daily Capture Habit

**Goal:** User establishes a capture routine

**Steps:**
1. Open app the next day
2. Tap "New Capture" button
3. Take photo following guidance
4. View updated metrics
5. Check streak counter
6. Return the following day and repeat

**Success Criteria:**
- [ ] User remembers to capture daily (or gets reminder)
- [ ] Capture process is quick (< 1 minute)
- [ ] Metric changes are noticeable and meaningful
- [ ] Streak mechanic is motivating
- [ ] User feels progress over time

**Feedback Questions:**
- Did you remember to capture daily without prompts?
- How long did it take to capture each day?
- Do the metrics shown feel accurate?
- Is the streak counter motivating?
- Would you continue using this daily?

---

### Scenario 3: Adding Products to Routine

**Goal:** User tracks skincare products

**Steps:**
1. Navigate to Routine tab
2. Tap "Add Product"
3. Enter product details (name, category)
4. Save product
5. Log usage event (e.g., "Applied moisturizer AM")
6. View product in routine list
7. Add 3-5 more products

**Success Criteria:**
- [ ] Adding products is intuitive
- [ ] Product categories make sense
- [ ] Logging usage is quick and easy
- [ ] Routine overview is clear
- [ ] User can find their products easily

**Feedback Questions:**
- Was adding products straightforward?
- Are the product categories sufficient?
- How often would you log product usage?
- Does the routine view help you track your skincare?
- What product features are missing?

---

### Scenario 4: Comparing Photos

**Goal:** User sees progress over time

**Steps:**
1. After 7+ days of captures, navigate to History
2. Tap "Compare" button
3. Select baseline photo
4. Select most recent photo
5. View side-by-side comparison
6. Review metric changes

**Success Criteria:**
- [ ] Comparison feature is discoverable
- [ ] Selecting photos is easy
- [ ] Visual comparison is clear
- [ ] Metric deltas are understandable
- [ ] User feels they can see progress

**Feedback Questions:**
- Did you know the compare feature existed?
- Was it easy to select which photos to compare?
- Could you see visible differences?
- Do the metric changes match what you see?
- Is this comparison useful for tracking progress?

---

### Scenario 5: Running an Experiment

**Goal:** User tests a new product scientifically

**Steps:**
1. Add new product to routine
2. Navigate to Insights → Experiments
3. Create new experiment:
   - Name: "Testing Vitamin C Serum"
   - Product: Select newly added product
   - Primary metric: Redness score
   - Duration: 14 days
4. Take captures over 14 days
5. Log product usage
6. View experiment progress
7. Complete experiment
8. Review results

**Success Criteria:**
- [ ] Experiment creation is clear
- [ ] Tracking progress is visible
- [ ] User remembers to log product usage
- [ ] Results show clear before/after data
- [ ] User can determine if product worked

**Feedback Questions:**
- Did you understand how experiments work?
- Was it easy to track the experiment?
- Did you remember to log product usage?
- Were the results helpful in evaluating the product?
- Would you run more experiments?

---

### Scenario 6: Asking Questions (Q&A)

**Goal:** User gets skincare advice

**Steps:**
1. Navigate to Insights → Q&A
2. Tap "Ask Question"
3. Enter question: "Should I use retinol and vitamin C together?"
4. Submit and wait for answer
5. Read answer and citations
6. Ask follow-up question

**Success Criteria:**
- [ ] Q&A feature is discoverable
- [ ] Question submission is simple
- [ ] Answer is provided quickly (< 30 seconds)
- [ ] Answer is helpful and accurate
- [ ] Citations add credibility
- [ ] Disclaimer is clear

**Feedback Questions:**
- Did you find the Q&A feature useful?
- Was the answer helpful?
- Did you trust the information provided?
- Would you ask more questions?
- What questions would you want to ask?

---

### Scenario 7: Exploring Premium Features

**Goal:** User understands premium value

**Steps:**
1. As free user, navigate app
2. Encounter premium-locked features:
   - Freeze days
   - Additional product verdicts
   - Shelf scan
   - Advanced insights
3. Tap "Upgrade to Premium"
4. View pricing and benefits
5. (Optional) Complete purchase

**Success Criteria:**
- [ ] Free tier is useful enough
- [ ] Premium features are clearly marked
- [ ] Value proposition is compelling
- [ ] Pricing is reasonable
- [ ] Upgrade process is smooth

**Feedback Questions:**
- Is the free version useful on its own?
- Which premium features are most appealing?
- Is the pricing fair for the value?
- Would you upgrade to premium?
- What would convince you to upgrade?

---

### Scenario 8: Offline Usage

**Goal:** User can capture without internet

**Steps:**
1. Enable airplane mode
2. Open app (should load cached data)
3. Take new capture
4. Verify "saved locally" message
5. Disable airplane mode
6. Wait for sync
7. Verify capture uploaded and processed

**Success Criteria:**
- [ ] App works offline (doesn't crash)
- [ ] User can capture without internet
- [ ] Offline state is clearly indicated
- [ ] Sync happens automatically when online
- [ ] User doesn't lose data

**Feedback Questions:**
- Did the app work well offline?
- Was it clear that your capture would sync later?
- Did you feel confident your data was safe?

---

### Scenario 9: Account Management

**Goal:** User can manage account and privacy

**Steps:**
1. Navigate to Account tab
2. Review profile information
3. Check achievements earned
4. View subscription status
5. Review privacy policy
6. (Optional) Test account deletion flow

**Success Criteria:**
- [ ] Account information is accessible
- [ ] Privacy settings are clear
- [ ] Achievements are visible and motivating
- [ ] Account deletion is possible but not too easy

**Feedback Questions:**
- Is your account information clear?
- Do you understand what data is collected?
- Are achievements motivating?
- Do you trust the app with your data?

---

### Scenario 10: End-to-End User Journey

**Goal:** Complete user journey over 2 weeks

**Day 1:**
- Sign up
- Baseline capture
- Add 3 products to routine

**Days 2-7:**
- Daily captures
- Log product usage
- Achieve "Week Warrior" achievement

**Day 8:**
- Compare Day 1 vs Day 7 photos
- Create experiment for new product
- Ask Q&A question

**Days 9-14:**
- Continue daily captures
- Track experiment progress
- Explore Discover tab (commerce offers)

**Day 14:**
- Complete experiment
- Achieve "Fortnight Focus" achievement
- Review overall progress

**Success Criteria:**
- [ ] User completes full 2-week journey
- [ ] No major bugs or blockers
- [ ] User finds value in the app
- [ ] User intends to continue using

**Feedback Questions:**
- What was your overall experience?
- What did you find most valuable?
- What frustrated you the most?
- Would you recommend this to a friend?
- Would you continue using after the test period?

---

## UAT Feedback Collection

### Feedback Form Template

**Tester Information:**
- Name: ___________
- Device: ___________
- Android Version: ___________
- Testing Period: ___________

**Overall Experience (1-5):**
- Ease of use: ___
- Visual design: ___
- Feature usefulness: ___
- Performance (speed): ___
- Would recommend: ___

**Feature Feedback:**

| Feature | Used? | Rating (1-5) | Comments |
|---------|-------|--------------|----------|
| Onboarding | | | |
| Daily Capture | | | |
| Streak Tracking | | | |
| Routine Tracking | | | |
| Experiments | | | |
| Photo Comparison | | | |
| Q&A | | | |
| Achievements | | | |
| Discover | | | |
| Account Settings | | | |

**Open Questions:**

1. What did you like most about the app?
   - ___________

2. What frustrated you the most?
   - ___________

3. What features are missing?
   - ___________

4. Did anything confuse you?
   - ___________

5. Would you pay for premium? Why or why not?
   - ___________

6. What would make you use this app daily?
   - ___________

7. Any bugs or issues encountered?
   - ___________

8. Final thoughts?
   - ___________

---

## UAT Issue Logging

When testers find issues, log them in this format:

**Issue Title:** ___________ (e.g., "Capture button doesn't respond")

**Priority:**
- [ ] Critical (blocks testing)
- [ ] High (major feature broken)
- [ ] Medium (annoying but workaround exists)
- [ ] Low (polish issue)

**Category:**
- [ ] Bug (something broken)
- [ ] UX Issue (confusing or frustrating)
- [ ] Feature Request (something missing)
- [ ] Content Issue (unclear text)

**Description:**
- What happened: ___________
- What you expected: ___________
- Steps to reproduce: ___________

**Environment:**
- Device: ___________
- Android Version: ___________
- App Version: ___________
- Network: WiFi / 4G / Offline

**Screenshot/Video:** (attach if possible)

**Reporter:** ___________
**Date:** ___________

---

## UAT Success Metrics

### Quantitative Metrics

| Metric | Target | Result |
|--------|--------|--------|
| Completion rate (full 2-week test) | > 70% | |
| Daily capture rate (days 2-14) | > 60% | |
| Average daily captures | > 0.8 | |
| Products added per user | > 3 | |
| Experiments created | > 0.5 | |
| Q&A questions asked | > 1 | |
| Crash-free rate | > 99% | |
| Average session duration | > 3 min | |

### Qualitative Metrics

| Metric | Target | Result |
|--------|--------|--------|
| Overall satisfaction (1-5) | > 4.0 | |
| Would recommend (%) | > 70% | |
| Ease of use (1-5) | > 4.0 | |
| Feature usefulness (1-5) | > 3.5 | |
| Premium upgrade interest (%) | > 30% | |

### Go/No-Go Criteria

**GO (release approved) if:**
- [ ] Completion rate > 70%
- [ ] Overall satisfaction > 4.0
- [ ] No P0 bugs found
- [ ] < 3 P1 bugs unresolved
- [ ] Would recommend > 70%

**NO-GO (more work needed) if:**
- [ ] Completion rate < 50%
- [ ] Overall satisfaction < 3.5
- [ ] Any P0 bugs
- [ ] > 5 P1 bugs unresolved
- [ ] Major feature requests from >50% of testers

---

## UAT Schedule

### Week 1: Preparation
- [ ] Deploy staging environment
- [ ] Build and distribute test APK
- [ ] Recruit testers (10-20 people)
- [ ] Create test accounts
- [ ] Send welcome email with instructions

### Week 2-3: Active Testing
- [ ] Testers use app daily
- [ ] Daily check-ins in Slack/Discord
- [ ] Bug reports triaged daily
- [ ] Mid-test survey (Day 7)

### Week 4: Analysis
- [ ] Collect final feedback forms
- [ ] Analyze quantitative metrics
- [ ] Review qualitative feedback
- [ ] Prioritize issues and feature requests
- [ ] Make go/no-go decision

### Week 5: Iteration (if NO-GO)
- [ ] Fix critical issues
- [ ] Run shortened UAT round 2
- [ ] Re-evaluate

---

## Post-UAT Action Items

After UAT completes:

1. **Issue Triage:**
   - P0: Fix before launch (blockers)
   - P1: Fix before launch or document in known issues
   - P2: Schedule for v1.1
   - P3: Backlog

2. **Feature Requests:**
   - Evaluate against roadmap
   - High-demand features → prioritize
   - Nice-to-haves → backlog

3. **Product Improvements:**
   - Update onboarding based on confusion points
   - Clarify unclear UI copy
   - Add missing help text

4. **Documentation:**
   - Update user guide
   - Create FAQ based on common questions
   - Document known limitations

5. **Tester Recognition:**
   - Thank testers (email, credits)
   - Offer free premium for beta testers (optional)
   - Share launch announcement

---

## Beta Tester Recruitment

### Ideal Beta Tester Profile

- **Active in skincare community:** Reddit r/SkincareAddiction, beauty forums
- **Regular social media users:** Can provide feedback and spread word
- **Device diversity:** Various Android models
- **Tech-savvy but not developers:** Representative of real users
- **Time to commit:** 2-3 weeks, 10-15 min/day

### Recruitment Channels

- [ ] Reddit r/SkincareAddiction post
- [ ] Instagram/TikTok skincare influencers
- [ ] Friends & family
- [ ] BetaList / Product Hunt beta programs
- [ ] Android beta community
- [ ] Existing email list (if applicable)

### Tester Incentives

- Early access to product
- Free premium for 3 months (after launch)
- Name in credits (with permission)
- Input on future features
- Beta tester badge in app

---

## UAT Communication

### Welcome Email Template

**Subject:** You're invited to test GlowUp AI (Beta)

Hi [Name],

Thanks for joining the GlowUp AI beta test!

**What is GlowUp AI?**
GlowUp AI helps you track your skincare journey with daily photo captures, routine tracking, and progress insights.

**What we need from you:**
- Use the app daily for 2 weeks (10-15 min/day)
- Take daily captures
- Track your routine
- Try all features
- Report bugs and feedback

**Getting Started:**
1. Download the beta APK: [link]
2. Install on your Android device
3. Sign up with test code: BETA2026
4. Follow the onboarding

**Support:**
- Slack channel: #glowup-beta
- Email: beta@glowup.ai
- FAQ: [link]

**Timeline:**
- Testing period: [dates]
- Mid-test survey: [date]
- Final survey: [date]

**Rewards:**
- 3 months free premium (after launch)
- Beta tester badge
- Your feedback shapes the product!

Let's build something great together!

The GlowUp Team

---

### Mid-Test Check-In Email

**Subject:** How's your GlowUp beta experience going?

Hi [Name],

You're halfway through the beta test! We'd love to hear how it's going.

**Quick Survey (5 minutes):** [survey link]

**Hot Topics:**
- Is the daily capture working well?
- Are you finding value in tracking your routine?
- Any features missing?
- Any bugs we should know about?

Reply to this email or jump in the #glowup-beta Slack channel.

Thanks for testing!

---

### Final Survey Email

**Subject:** Final GlowUp Beta Survey - Your Feedback Matters!

Hi [Name],

The beta test is wrapping up! Your final feedback will shape our launch.

**Final Survey (10 minutes):** [survey link]

**Key Questions:**
- Would you continue using GlowUp?
- Would you recommend it to friends?
- What's missing before launch?
- Any final bugs or issues?

**What's Next:**
- We'll analyze all feedback this week
- Launch planned for [date]
- You'll get 3 months free premium as thanks!

Thank you for being an early believer!

---

## UAT Sign-Off

After UAT, get sign-off from stakeholders:

- [ ] Product Manager: Features meet requirements
- [ ] QA Lead: Quality standards met
- [ ] Engineering Lead: No major tech debt
- [ ] Design Lead: UX meets standards
- [ ] Business Lead: Metrics align with goals

**UAT Summary:**
- Testers: _____ completed / _____ recruited
- Overall satisfaction: _____/5
- Would recommend: _____%
- Critical bugs: _____
- Go/No-Go decision: _____

**Sign-off:**

Product Manager: ___________ Date: _____
QA Lead: ___________ Date: _____
Engineering Lead: ___________ Date: _____

**Next Steps:**
- [ ] Fix P0/P1 bugs
- [ ] Prepare for launch
- [ ] Plan marketing rollout
- [ ] Production deployment
