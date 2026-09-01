# Compose UI Test Verification Checklist

## Pre-Test Setup

- [ ] Android device or emulator is running
- [ ] Device has API level 24 or higher
- [ ] App builds successfully: `./gradlew assembleDebug`
- [ ] Test APK builds successfully: `./gradlew assembleDebugAndroidTest`

## Test Execution

### Run All Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Expected Results
All tests in the following files should pass:
- [ ] `HomeScreenTest.kt` - All 5 tests pass
- [ ] `OnboardingFlowTest.kt` - All 5 tests pass

### Individual Test Verification

#### HomeScreenTest.kt
- [ ] `app_launches_and_shows_initial_screen()` - Verifies app launches
- [ ] `navigation_does_not_crash()` - Verifies navigation works
- [ ] `bottom_navigation_integrates_correctly()` - Verifies bottom nav
- [ ] `theme_applies_without_errors()` - Verifies theme loads
- [ ] `app_survives_configuration_changes()` - Verifies stability

#### OnboardingFlowTest.kt
- [ ] `onboarding_welcome_screen_displays()` - Verifies welcome screen
- [ ] `onboarding_has_navigation_controls()` - Verifies nav controls exist
- [ ] `onboarding_skip_navigates_forward()` - Verifies skip works
- [ ] `app_handles_orientation_changes()` - Verifies orientation handling
- [ ] `accessibility_elements_present()` - Verifies accessibility

## Common Issues and Solutions

### Issue: "No connected devices"
**Solution**: Start emulator or connect physical device
```bash
adb devices
```

### Issue: "Task :app:connectedDebugAndroidTest FAILED"
**Check**:
1. Are tests timing out? Increase timeout in test code
2. Is the device locked? Unlock it
3. Are animations disabled? (Recommended for tests)
   - Settings → Developer Options → Animation scale → Off

### Issue: "java.lang.RuntimeException: No instrumentation registered"
**Solution**: Clean and rebuild
```bash
./gradlew clean
./gradlew assembleDebugAndroidTest
```

### Issue: Tests pass locally but fail in CI
**Check**:
1. CI emulator has sufficient resources
2. CI has animation scale set to 0
3. CI waits for emulator to fully boot
4. Increase timeout values for CI environment

## Test Quality Metrics

### Stability
Run tests 5 times consecutively:
```bash
for i in {1..5}; do ./gradlew connectedDebugAndroidTest; done
```

- [ ] All 5 runs pass without failures
- [ ] No intermittent failures
- [ ] Consistent timing (no outliers)

### Coverage
Check that tests cover:
- [ ] App launch
- [ ] Navigation flows
- [ ] Onboarding skip
- [ ] Basic UI rendering
- [ ] Theme application

### Performance
Tests should complete in reasonable time:
- [ ] `HomeScreenTest` completes in < 2 minutes
- [ ] `OnboardingFlowTest` completes in < 2 minutes
- [ ] Full suite completes in < 5 minutes

## Files to Verify

### Test Files
- [ ] `app/src/androidTest/java/com/glowup/ai/ui/HomeScreenTest.kt` - Updated
- [ ] `app/src/androidTest/java/com/glowup/ai/ui/OnboardingFlowTest.kt` - Updated
- [ ] `app/src/androidTest/java/com/glowup/ai/ui/ComposeTestUtils.kt` - Created

### Documentation
- [ ] `app/src/androidTest/java/com/glowup/ai/ui/TESTING_GUIDE.md` - Created
- [ ] `COMPOSE_TEST_FIXES.md` - Created
- [ ] `TEST_VERIFICATION_CHECKLIST.md` - This file

## Integration with CI/CD

### GitHub Actions Example
```yaml
- name: Run Android Tests
  run: |
    ./gradlew connectedDebugAndroidTest
    
- name: Upload Test Reports
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: test-reports
    path: app/build/reports/androidTests/
```

### Gradle Configuration
Verify in `app/build.gradle.kts`:
- [ ] `testInstrumentationRunner = "com.glowup.ai.HiltTestRunner"` is set
- [ ] Test dependencies are included
- [ ] Debug build type is configured

## Smoke Test (Quick Verification)

Run the fastest test to verify setup:
```bash
./gradlew connectedDebugAndroidTest --tests "*.HomeScreenTest.app_launches_and_shows_initial_screen"
```

- [ ] Test completes in < 30 seconds
- [ ] Test passes
- [ ] No errors in output

## Full Verification (Before Merging)

1. **Clean Build**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```
   - [ ] Completes without errors

2. **Run All Tests**
   ```bash
   ./gradlew connectedDebugAndroidTest
   ```
   - [ ] All tests pass
   - [ ] No flaky failures

3. **Check Reports**
   ```bash
   open app/build/reports/androidTests/connected/index.html
   ```
   - [ ] All tests are green
   - [ ] Test execution times are reasonable
   - [ ] No skipped tests

4. **Check Logs**
   ```bash
   adb logcat -d | grep -i "test\|error\|crash"
   ```
   - [ ] No unexpected errors
   - [ ] No crashes during tests
   - [ ] No memory issues

## Test Maintenance

### When to Update Tests

Update tests when:
- [ ] UI text changes (use `substring = true` to minimize changes)
- [ ] Navigation flow changes
- [ ] New features are added
- [ ] Tests become flaky (investigate and fix root cause)

### Test Review Checklist

For new/modified tests:
- [ ] Test has clear, descriptive name
- [ ] Test uses proper wait strategies (no `Thread.sleep()`)
- [ ] Test uses flexible matchers (`substring = true`)
- [ ] Test handles optional UI gracefully
- [ ] Test has comments explaining complex logic
- [ ] Test follows patterns in `TESTING_GUIDE.md`

## Success Criteria

All of the following must be true:
- ✅ All tests pass on first run
- ✅ All tests pass when run 5 times consecutively
- ✅ Tests complete in reasonable time (< 5 minutes total)
- ✅ No flaky failures
- ✅ Test reports show 100% success rate
- ✅ No crashes or memory issues during tests

## Sign-off

- [ ] All tests verified locally
- [ ] Test reports reviewed
- [ ] No flaky tests observed
- [ ] Documentation complete
- [ ] Ready for code review

**Tested by**: _________________  
**Date**: _________________  
**Device/Emulator**: _________________  
**Test Results**: [ ] Pass [ ] Fail  

## Next Steps

After verification:
1. [ ] Commit changes
2. [ ] Create pull request
3. [ ] Run tests in CI
4. [ ] Get code review
5. [ ] Merge to main

## Notes

Add any observations or issues encountered during verification:

```
[Space for notes]
```
