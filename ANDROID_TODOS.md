# Android TODOs/FIXMEs - Issue Tracker

Generated: 2026-09-01
Total TODOs Found: 17
GitHub Issues Created: 10 (consolidated related items)

## Summary by Priority

### High Priority (1 item)
- Issue #2: Cloud backup implementation

### Medium Priority (6 items)
- Issue #3: Image loading with Coil
- Issue #4: Share functionality
- Issue #5: Analytics export (PDF/CSV)
- Issue #6: Navigate to capture detail
- Issue #9: Log viewer and export
- Issue #10: API endpoint switching

### Low Priority (3 items)
- Issue #7: Experiment tracking
- Issue #8: Open source licenses screen
- Issue #11: Achievement tracking for comparison/sharing

## Detailed Issue List

### Issue #2: Implement cloud backup functionality
**Priority:** High  
**Category:** Feature Implementation  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/2

**Location:**
- `app/src/main/java/com/glowup/ai/feature/account/SettingsViewModel.kt:209`

**Description:**
Implement actual cloud backup logic for user data. Currently only logs when enabled but doesn't perform any backup operations.

**Impact:** User data protection and sync across devices

---

### Issue #3: Implement image loading with Coil library
**Priority:** Medium  
**Category:** UI Enhancement  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/3

**Locations (5 occurrences):**
- `app/src/main/java/com/glowup/ai/feature/home/MonthlyRecapScreen.kt:319`
- `app/src/main/java/com/glowup/ai/feature/home/MonthlyRecapScreen.kt:345`
- `app/src/main/java/com/glowup/ai/feature/capture/PhotoGridScreen.kt:249`
- `app/src/main/java/com/glowup/ai/feature/capture/PhotoComparisonScreen.kt:249`
- `app/src/main/java/com/glowup/ai/feature/capture/PhotoComparisonScreen.kt:297`

**Description:**
Replace placeholder image displays with actual image loading using Coil library across multiple screens (Monthly Recap, Photo Grid, Photo Comparison).

**Impact:** Proper image display for user captures

---

### Issue #4: Implement share functionality for comparisons
**Priority:** Medium  
**Category:** Feature Implementation  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/4

**Locations (2 occurrences):**
- `app/src/main/java/com/glowup/ai/feature/comparison/ComparisonScreen.kt:89` (top bar icon)
- `app/src/main/java/com/glowup/ai/feature/comparison/ComparisonScreen.kt:173` (share button)

**Description:**
Add share functionality to allow users to share their progress comparisons via social media and other channels.

**Impact:** User engagement and app virality

---

### Issue #5: Implement PDF and CSV export for analytics
**Priority:** Medium  
**Category:** Feature Implementation  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/5

**Locations (2 occurrences):**
- `app/src/main/java/com/glowup/ai/feature/analytics/AnalyticsViewModel.kt:363` (PDF export)
- `app/src/main/java/com/glowup/ai/feature/analytics/AnalyticsViewModel.kt:374` (CSV export)

**Description:**
Add export functionality for analytics data in both PDF (formatted reports with charts) and CSV (raw data) formats.

**Impact:** Data portability and advanced user analytics

---

### Issue #6: Navigate to specific capture detail from calendar
**Priority:** Medium  
**Category:** Navigation Enhancement  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/6

**Location:**
- `app/src/main/java/com/glowup/ai/feature/home/HomeScreen.kt:385`

**Description:**
When user taps on a calendar date with a capture, navigate to that specific capture detail instead of the generic Capture screen.

**Impact:** Better user experience and navigation flow

---

### Issue #7: Add experiment tracking to HistoryItem
**Priority:** Low  
**Category:** Feature Enhancement  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/7

**Location:**
- `app/src/main/java/com/glowup/ai/feature/capture/PhotoGridScreen.kt:337`

**Description:**
Implement experiment tracking in HistoryItem to enable filtering by experiments in photo grid. Currently returns emptyList().

**Impact:** Enhanced filtering capabilities for power users

---

### Issue #8: Implement open source licenses screen
**Priority:** Low  
**Category:** Legal/Compliance  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/8

**Location:**
- `app/src/main/java/com/glowup/ai/feature/account/SettingsScreen.kt:561`

**Description:**
Add navigation to open source licenses screen with proper attribution for all libraries used.

**Impact:** Legal compliance and transparency

---

### Issue #9: Implement log viewer and export functionality
**Priority:** Medium  
**Category:** Developer Tools  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/9

**Location:**
- `app/src/main/java/com/glowup/ai/feature/account/SettingsViewModel.kt:305`

**Description:**
Add log viewer screen and log export functionality for debugging and support purposes.

**Impact:** Better debugging and customer support

---

### Issue #10: Implement API endpoint switching for debug builds
**Priority:** Medium  
**Category:** Developer Tools  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/10

**Location:**
- `app/src/main/java/com/glowup/ai/feature/account/SettingsViewModel.kt:311`

**Description:**
Add ability to switch between API endpoints (dev/staging/production) in debug builds for testing.

**Impact:** Development efficiency and testing flexibility

---

### Issue #11: Track comparison and sharing usage for achievements
**Priority:** Low  
**Category:** Gamification  
**GitHub:** https://github.com/piyushxpc7/GlowupAI/issues/11

**Locations (2 occurrences):**
- `app/src/main/java/com/glowup/ai/domain/calculator/AchievementCalculator.kt:46` (usedComparison)
- `app/src/main/java/com/glowup/ai/domain/calculator/AchievementCalculator.kt:47` (sharedProgress)

**Description:**
Add tracking for comparison feature usage and progress sharing to unlock related achievements. Currently hardcoded to false.

**Impact:** Achievement system completeness

---

## Quick Wins Analysis

After reviewing all 17 TODOs, no items qualified as "quick wins" (< 5 minutes to implement). All items require:
- Feature implementation with proper architecture
- UI/UX considerations
- Testing and validation
- Potential dependency additions

**Recommendation:** Prioritize based on user impact and business value. Start with Issue #2 (cloud backup) as it affects data security, then Issue #3 (image loading) as it impacts core functionality.

## Next Steps

1. Review and prioritize issues based on current sprint goals
2. Assign issues to appropriate team members
3. Add issues to project board/milestone
4. Consider bundling related issues (e.g., #4 and #11 - sharing + tracking)
5. Estimate effort for each issue during sprint planning

## Statistics by Category

- **Feature Implementation:** 4 issues (2, 4, 5, 7)
- **UI Enhancement:** 2 issues (3, 6)
- **Developer Tools:** 2 issues (9, 10)
- **Legal/Compliance:** 1 issue (8)
- **Gamification:** 1 issue (11)

## Files with Most TODOs

1. **AnalyticsViewModel.kt** - 2 TODOs (export features)
2. **SettingsViewModel.kt** - 3 TODOs (backup, logs, API switching)
3. **ComparisonScreen.kt** - 2 TODOs (share functionality)
4. **PhotoComparisonScreen.kt** - 2 TODOs (image loading)
5. **AchievementCalculator.kt** - 2 TODOs (tracking)
6. **MonthlyRecapScreen.kt** - 2 TODOs (image loading)
