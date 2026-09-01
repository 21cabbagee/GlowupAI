package com.glowup.ai.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glowup.ai.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for onboarding flow.
 *
 * Tests critical user journey:
 * - Onboarding screens
 * - Sign in
 * - Consent
 * - First capture
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun onboardingFlow_completeJourney() {
        // Step 1: Welcome screen should be visible
        composeTestRule
            .onNodeWithText("Welcome to GlowUp AI")
            .assertIsDisplayed()

        // Step 2: Click Next through onboarding
        composeTestRule
            .onNodeWithText("Next")
            .performClick()

        // Step 3: Second onboarding screen
        composeTestRule
            .onNodeWithText("Track Your Progress")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Next")
            .performClick()

        // Step 4: Final onboarding screen
        composeTestRule
            .onNodeWithText("Get Started")
            .assertIsDisplayed()
            .performClick()

        // Step 5: Should reach sign in screen
        composeTestRule
            .onNodeWithText("Sign In")
            .assertIsDisplayed()
    }

    @Test
    fun onboardingFlow_canSkipToSignIn() {
        // Given - on welcome screen
        composeTestRule
            .onNodeWithText("Welcome to GlowUp AI")
            .assertIsDisplayed()

        // When - click skip
        composeTestRule
            .onNodeWithText("Skip", ignoreCase = true)
            .performClick()

        // Then - should reach sign in
        composeTestRule
            .onNodeWithText("Sign In")
            .assertIsDisplayed()
    }

    @Test
    fun consentScreen_requiresAgreement() {
        // Navigate to consent screen (after sign in)
        navigateToConsentScreen()

        // Verify consent text is shown
        composeTestRule
            .onNodeWithText("Data Privacy & Consent", substring = true)
            .assertIsDisplayed()

        // Verify facial data consent checkbox
        composeTestRule
            .onNode(hasContentDescription("Facial data consent checkbox"))
            .assertIsNotChecked()

        // Try to continue without consent
        composeTestRule
            .onNodeWithText("Continue")
            .assertIsNotEnabled()

        // Check consent
        composeTestRule
            .onNode(hasContentDescription("Facial data consent checkbox"))
            .performClick()

        // Now continue should be enabled
        composeTestRule
            .onNodeWithText("Continue")
            .assertIsEnabled()
    }

    @Test
    fun signInScreen_showsProviders() {
        // Navigate to sign in
        navigateToSignInScreen()

        // Verify sign in options
        composeTestRule
            .onNodeWithText("Sign in with Google", substring = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Sign in with Email", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun firstCapturePrompt_appearsAfterOnboarding() {
        // Complete onboarding flow
        completeOnboardingFlow()

        // Should see first capture prompt
        composeTestRule
            .onNodeWithText("Take Your First Capture", substring = true)
            .assertIsDisplayed()

        // Should have capture button
        composeTestRule
            .onNodeWithText("Start Capture", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun accessibility_onboardingScreens() {
        // Verify content descriptions for accessibility
        composeTestRule
            .onNode(hasContentDescription("Onboarding illustration"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Next")
            .assertHasClickAction()

        // Verify text is readable
        composeTestRule
            .onNodeWithText("Welcome to GlowUp AI")
            .assertTextContains("Welcome", substring = true)
    }

    // Helper functions

    private fun navigateToSignInScreen() {
        // Skip onboarding
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Skip", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("Skip", ignoreCase = true)
            .performClick()
    }

    private fun navigateToConsentScreen() {
        // This would require actual sign in flow
        // For now, just navigate to sign in
        navigateToSignInScreen()
    }

    private fun completeOnboardingFlow() {
        // Click through all onboarding screens
        repeat(3) {
            composeTestRule.waitForIdle()
            try {
                composeTestRule
                    .onNodeWithText("Next")
                    .performClick()
            } catch (e: Exception) {
                // Already past onboarding
            }
        }

        // Click Get Started
        try {
            composeTestRule
                .onNodeWithText("Get Started")
                .performClick()
        } catch (e: Exception) {
            // Already started
        }
    }
}

// Extension functions
fun SemanticsNodeInteraction.assertIsNotChecked(): SemanticsNodeInteraction {
    return assert(isNotOn())
}

fun SemanticsNodeInteraction.assertIsChecked(): SemanticsNodeInteraction {
    return assert(isOn())
}
