# Compose UI Testing Guide for GlowUp AI

This guide helps you write reliable, maintainable Compose UI tests.

## Overview

Our test strategy focuses on:
- **Stability**: Tests shouldn't be flaky
- **Maintainability**: Tests should survive UI text/layout changes
- **Speed**: Tests should run quickly
- **Coverage**: Tests should catch real bugs

## Test Types

### 1. Smoke Tests
Quick tests that verify the app doesn't crash and shows basic UI.

```kotlin
@Test
fun app_launches_successfully() {
    composeTestRule.assertAppLaunched()
}
```

### 2. Navigation Tests
Verify navigation flows work without crashes.

```kotlin
@Test
fun navigation_from_home_to_capture() {
    composeTestRule.skipOnboardingIfPresent()
    
    composeTestRule
        .onNodeWithText("Capture", substring = true)
        .performClick()
    
    composeTestRule.waitForIdle()
    // Verify we navigated (but don't be too specific)
}
```

### 3. Interaction Tests
Test user interactions like button clicks, form inputs.

```kotlin
@Test
fun submit_check_in_form() {
    composeTestRule.skipOnboardingIfPresent()
    
    composeTestRule
        .onNodeWithText("Check In")
        .performClick()
    
    // Fill form and submit
    composeTestRule
        .onNodeWithText("Submit")
        .performClick()
    
    composeTestRule.waitForIdle()
}
```

## Best Practices

### 1. Use Proper Wait Strategies

❌ **DON'T** use fixed delays:
```kotlin
Thread.sleep(1000) // BAD! Flaky and slow
```

✅ **DO** use `waitForIdle()` and `waitUntil()`:
```kotlin
composeTestRule.waitForIdle()

composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule
        .onAllNodesWithText("Home")
        .fetchSemanticsNodes()
        .isNotEmpty()
}
```

### 2. Use Flexible Matchers

❌ **DON'T** match exact text with newlines:
```kotlin
.onNodeWithText("Welcome to\nGlowUp AI") // BAD! Breaks if text wrapping changes
```

✅ **DO** use substring matching:
```kotlin
.onNodeWithText("Welcome", substring = true, ignoreCase = true)
```

### 3. Handle Optional UI

❌ **DON'T** assume UI elements always exist:
```kotlin
composeTestRule
    .onNodeWithText("Premium Badge")
    .assertIsDisplayed() // BAD! Crashes for free users
```

✅ **DO** handle conditional UI gracefully:
```kotlin
try {
    composeTestRule
        .onNodeWithText("Premium Badge")
        .assertIsDisplayed()
} catch (e: Exception) {
    // Badge not present, user is not premium - that's okay
}

// Or use our utility:
composeTestRule.tryAction {
    onNodeWithText("Premium Badge").assertIsDisplayed()
}
```

### 4. Add Content Descriptions

Make your composables testable by adding semantic properties:

```kotlin
@Composable
fun StreakCounter(streak: Int) {
    Text(
        text = "$streak day streak",
        modifier = Modifier.semantics {
            contentDescription = "Current streak: $streak days"
            // Add custom semantic properties if needed
        }
    )
}
```

Then test with:
```kotlin
composeTestRule
    .onNode(hasContentDescription("Current streak"))
    .assertIsDisplayed()
```

### 5. Test Behavior, Not Implementation

❌ **DON'T** test internal state:
```kotlin
// Testing specific VM state directly in UI test
assertTrue(viewModel.isLoading) // BAD!
```

✅ **DO** test user-visible behavior:
```kotlin
// Test what the user sees
composeTestRule
    .onNode(hasProgressIndicator())
    .assertExists()
```

## Utilities

We provide test utilities in `ComposeTestUtils.kt`:

### Wait for Text
```kotlin
// Wait for any of these texts to appear
val found = composeTestRule.waitForAnyText(
    texts = listOf("Home", "Welcome", "Sign In"),
    timeoutMillis = 10000
)
```

### Skip Onboarding
```kotlin
// Automatically skip onboarding if present
composeTestRule.skipOnboardingIfPresent()
```

### Assert App Launched
```kotlin
// Verify app launched to any valid screen
composeTestRule.assertAppLaunched()
```

### Semantic Matcher Combinators
```kotlin
// Combine matchers with OR
composeTestRule
    .onNode(hasText("Home") or hasContentDescription("Home screen"))
    .assertExists()

// Combine with AND
composeTestRule
    .onNode(hasText("Submit") and isEnabled())
    .performClick()
```

## Common Pitfalls

### 1. Race Conditions
**Problem**: Test fails randomly because composition hasn't finished.

**Solution**: Always call `waitForIdle()` after navigation or state changes:
```kotlin
composeTestRule
    .onNodeWithText("Next")
    .performClick()

composeTestRule.waitForIdle() // Wait for recomposition
```

### 2. Text with Newlines
**Problem**: Text appears on multiple lines in UI.

**Solution**: Use `substring = true`:
```kotlin
// UI shows "Welcome to\nGlowUp AI"
composeTestRule
    .onNodeWithText("Welcome to", substring = true)
    .assertIsDisplayed()
```

### 3. Ambiguous Node Matches
**Problem**: Multiple nodes match the same text.

**Solution**: Use more specific matchers or `onFirst()`:
```kotlin
composeTestRule
    .onAllNodesWithText("Capture")
    .onFirst()
    .performClick()
```

### 4. Testing Navigation
**Problem**: Can't verify navigation happened.

**Solution**: Look for screen-specific content:
```kotlin
// Click navigate button
composeTestRule
    .onNodeWithText("Go to Settings")
    .performClick()

composeTestRule.waitForIdle()

// Verify we're on settings screen by looking for settings-specific content
composeTestRule
    .onNodeWithText("Theme", substring = true)
    .assertExists()
```

## Debugging Tests

### Print Semantics Tree
```kotlin
composeTestRule.onRoot().printToLog("UI_TREE")
```

This prints the entire UI hierarchy with semantic properties.

### Check Node Existence
```kotlin
val exists = composeTestRule
    .onAllNodesWithText("Home")
    .fetchSemanticsNodes()
    .isNotEmpty()

println("Home node exists: $exists")
```

### Take Screenshots (if screenshot testing is enabled)
```kotlin
composeTestRule.onRoot().captureToImage()
```

## Test Organization

### File Structure
```
androidTest/
  java/com/glowup/ai/
    ui/
      ComposeTestUtils.kt          # Shared utilities
      HomeScreenTest.kt            # Home screen tests
      OnboardingFlowTest.kt        # Onboarding tests
      CaptureScreenTest.kt         # Capture screen tests
      TESTING_GUIDE.md             # This file
    di/
      Test*Module.kt               # Test dependency injection
    testing/
      HiltTestBase.kt              # Base test class
      MockResponses.kt             # Mock API responses
```

### Naming Conventions
- Test classes: `[Feature]Test.kt` (e.g., `HomeScreenTest.kt`)
- Test methods: `[component]_[action]_[expectedResult]`
  - `homeScreen_captureButton_navigatesToCapture()`
  - `onboarding_skipButton_skipsToHome()`

## Running Tests

### Run all tests
```bash
./gradlew connectedAndroidTest
```

### Run specific test class
```bash
./gradlew connectedAndroidTest --tests "*.HomeScreenTest"
```

### Run specific test method
```bash
./gradlew connectedAndroidTest --tests "*.HomeScreenTest.app_launches_successfully"
```

### Run with test coverage
```bash
./gradlew createDebugCoverageReport
```

## Mocking

For tests that need API responses, use `MockWebServer`:

```kotlin
@HiltAndroidTest
class MyFeatureTest : HiltTestBase() {
    @Test
    fun test_loads_data() {
        // Enqueue mock response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(MockResponses.DASHBOARD_SUCCESS)
        )
        
        // Test your feature
        composeTestRule
            .onNodeWithText("Dashboard")
            .assertIsDisplayed()
    }
}
```

## Performance

### Test Speed Tips
1. Use `@Before` to set up common state once
2. Don't make unnecessary API calls
3. Use `waitForIdle()` instead of fixed delays
4. Skip animations with reduced motion
5. Run tests in parallel when possible

### Flakiness Prevention
1. Always wait for composition with `waitForIdle()`
2. Use `waitUntil()` for async operations
3. Use flexible matchers (`substring = true`)
4. Don't test timing-sensitive animations
5. Mock external dependencies

## Resources

- [Compose Testing Docs](https://developer.android.com/jetpack/compose/testing)
- [Semantics in Compose](https://developer.android.com/jetpack/compose/semantics)
- [Testing Best Practices](https://developer.android.com/training/testing/fundamentals)

## Questions?

If you're unsure how to test something:
1. Check this guide
2. Look at existing tests for examples
3. Check `ComposeTestUtils.kt` for helpful utilities
4. Ask in team chat or PR review
