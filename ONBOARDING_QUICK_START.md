# Onboarding Quick Start Guide

Quick reference for testing and integrating the enhanced onboarding flow.

## What Was Built

A complete 9-screen onboarding experience including:
- Welcome/value prop screen
- 4 tutorial screens (streaks, photos, metrics, experiments)
- 2 permission explanation screens (camera, notifications)
- Baseline photo preparation screen
- Routine setup introduction screen

## Files Created

```
✅ EnhancedOnboardingScreen.kt       - All 9 screen composables + pager UI
✅ EnhancedOnboardingViewModel.kt    - State management & navigation logic
✅ ONBOARDING_IMPLEMENTATION.md      - Complete documentation
✅ ONBOARDING_QUICK_START.md         - This file
```

## Files Modified

```
✅ OnboardingGraph.kt                - Switched to EnhancedOnboardingRoute
```

## Testing the Flow

### 1. Reset Onboarding (for testing)

Add this helper to your debug menu or run via adb:

```kotlin
// In your debug menu or test code:
viewModelScope.launch {
    sessionStore.setOnboardingComplete(false)
    // Restart app or navigate to GlowDestination.Onboarding
}
```

Or via adb shell:
```bash
# Clear app data to reset onboarding
adb shell pm clear com.glowup.ai
```

### 2. Manual Test Flow

1. **Fresh install** or clear app data
2. **Sign in/create account** (existing auth flow)
3. **See welcome screen** with app value props
4. **Tap "Start Tour"** or "Continue" through screens
5. **Try skip button** (works until last screen)
6. **Reach final screen** showing "Get Started"
7. **Complete onboarding** → navigates to Consent or Home

### 3. Verify Behaviors

- [ ] Can't swipe between pages (must use buttons)
- [ ] Skip marks onboarding complete
- [ ] Complete flow marks onboarding complete
- [ ] Back button doesn't escape onboarding
- [ ] Second app open skips onboarding (goes to home/consent)
- [ ] Page indicators animate smoothly
- [ ] All text is readable in light and dark themes

## Integration Checklist

### Already Done ✅

- [x] Created all screen composables
- [x] Implemented ViewModel with state management
- [x] Integrated with SessionStore for persistence
- [x] Wired into navigation graph
- [x] Respects reduced motion accessibility
- [x] Uses Honey design system throughout
- [x] Documented everything

### Still Needed (Optional Enhancements)

- [ ] Add actual permission request logic (currently just educational)
- [ ] Wire baseline photo screen to actual capture flow
- [ ] Wire routine setup to actual product picker
- [ ] Add analytics events for onboarding steps
- [ ] A/B test shorter vs full flow
- [ ] Add animated illustrations (Lottie files)

## Current Flow

```
App Start
    ↓
Splash Screen
    ↓
(checks auth)
    ↓
Welcome/SignIn ← (if not authenticated)
    ↓
Enhanced Onboarding ← (if authenticated but onboarding_complete = false)
    ├─ Welcome
    ├─ Tutorial: Streaks
    ├─ Tutorial: Photos
    ├─ Tutorial: Metrics
    ├─ Tutorial: Experiments
    ├─ Camera Permission
    ├─ Notification Permission
    ├─ Baseline Photo
    └─ Routine Setup
    ↓
Consent Screen ← (after onboarding, if consent not given)
    ↓
Home Screen ← (after consent)
```

## Making Changes

### Change Screen Content

Edit the screen composable in `EnhancedOnboardingScreen.kt`:

```kotlin
@Composable
private fun TutorialStreaksScreen() {
    TutorialScreenTemplate(
        icon = Icons.Filled.LocalFireDepartment,
        title = "Build your streak",  // ← Edit this
        subtitle = "New subtitle",     // ← Or this
        contentSections = listOf(      // ← Or these
            TutorialSection("Title", "Description"),
        )
    )
}
```

### Add/Remove Screens

1. Change `pageCount` in `EnhancedOnboardingContent`:
```kotlin
val pagerState = rememberPagerState(pageCount = { 10 }) // was 9
```

2. Add to the `when (page)` block:
```kotlin
when (page) {
    // ... existing screens
    9 -> MyNewScreen()
}
```

### Disable Onboarding (for testing)

In `OnboardingGraph.kt`, temporarily bypass:
```kotlin
fun NavGraphBuilder.onboardingGraph(navController: NavController) {
    composable<GlowDestination.Onboarding> {
        // Skip directly to next step for testing
        LaunchedEffect(Unit) {
            navController.navigate(GlowDestination.Consent) {
                popUpTo(GlowDestination.Onboarding) { inclusive = true }
            }
        }
    }
    // ... rest
}
```

## Common Issues

### "Onboarding shows every time"

**Cause**: `setOnboardingComplete()` not being called.

**Fix**: Check ViewModel's `completeOnboarding()` succeeds.

### "Can't navigate after onboarding"

**Cause**: Network error loading profile.

**Fix**: Check logs for `refreshProfile()` errors.

### "Animations jittery"

**Cause**: Large images or complex layouts.

**Fix**: All screens use simple vectors, should be smooth. Check device performance.

## Quick Commands

### Build and Install
```bash
./gradlew installDebug
```

### Clear Data (reset onboarding)
```bash
adb shell pm clear com.glowup.ai
```

### View Logs
```bash
adb logcat | grep "Onboarding"
```

### Check Saved State
```bash
adb shell "run-as com.glowup.ai cat /data/data/com.glowup.ai/shared_prefs/*.xml"
```

## Next Steps

1. **Test thoroughly** with fresh install
2. **Try in different scenarios**:
   - New user signup
   - Existing user (should skip)
   - User who skips onboarding
3. **Verify persistence** across app restarts
4. **Check accessibility**:
   - TalkBack navigation
   - Reduced motion mode
   - Dark theme
5. **Get feedback** from team/users
6. **Iterate based on analytics** (once tracking added)

## Support

For questions:
- Check `ONBOARDING_IMPLEMENTATION.md` for detailed docs
- Review code comments in `EnhancedOnboardingScreen.kt`
- Test in debug mode with onboarding reset

---

**Status**: ✅ Ready to test
**Last Updated**: 2026-08-31
