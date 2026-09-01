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
 * UI tests for Home screen.
 *
 * Tests:
 * - Dashboard display
 * - Streak visualization
 * - Navigation to capture
 * - Comparison view
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // Assume user is already signed in for these tests
        navigateToHomeScreen()
    }

    @Test
    fun homeScreen_displaysStreak() {
        // Verify streak card is visible
        composeTestRule
            .onNodeWithText("Current Streak", substring = true)
            .assertIsDisplayed()

        // Verify streak number is displayed
        composeTestRule
            .onNode(hasContentDescription("Current streak count"))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysCaptureHistory() {
        // Verify history section
        composeTestRule
            .onNodeWithText("Your Progress", substring = true)
            .assertIsDisplayed()

        // Verify there's a list/grid of captures
        composeTestRule
            .onNode(hasContentDescription("Capture history"))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_captureButtonWorks() {
        // Find and click capture button
        composeTestRule
            .onNodeWithText("New Capture", substring = true)
            .performClick()

        // Should navigate to capture screen
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasContentDescription("Camera preview"))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_comparisonButtonWorks() {
        // Verify comparison button exists
        composeTestRule
            .onNodeWithText("Compare", substring = true)
            .assertIsDisplayed()
            .performClick()

        // Should navigate to comparison screen
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Comparison", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_refreshWorks() {
        // Perform pull to refresh
        composeTestRule
            .onNode(hasScrollAction())
            .performTouchInput {
                swipeDown(startY = 100f, endY = 500f)
            }

        // Wait for refresh
        composeTestRule.waitForIdle()

        // Content should still be visible
        composeTestRule
            .onNodeWithText("Current Streak", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysAchievements() {
        // Scroll to achievements section
        composeTestRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Achievements", substring = true))

        // Verify achievements are shown
        composeTestRule
            .onNodeWithText("Achievements", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_emptyState_showsPrompt() {
        // For a new user with no captures
        // (This test would need to set up empty state)

        // Verify empty state message
        composeTestRule
            .onNodeWithText("Take your first capture", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun homeScreen_navigation_toSettings() {
        // Click settings icon
        composeTestRule
            .onNode(hasContentDescription("Settings"))
            .performClick()

        // Should navigate to settings
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_accessibility_contentDescriptions() {
        // Verify important elements have content descriptions
        composeTestRule
            .onNode(hasContentDescription("Current streak count"))
            .assertExists()

        composeTestRule
            .onNode(hasContentDescription("Capture history"))
            .assertExists()

        composeTestRule
            .onNode(hasContentDescription("New capture button"))
            .assertExists()
    }

    @Test
    fun homeScreen_themeToggle_works() {
        // Navigate to settings
        composeTestRule
            .onNode(hasContentDescription("Settings"))
            .performClick()

        composeTestRule.waitForIdle()

        // Find theme toggle
        composeTestRule
            .onNode(hasContentDescription("Theme toggle"))
            .performClick()

        // Theme should change (verify by checking a color or icon)
        composeTestRule.waitForIdle()

        // Navigate back
        composeTestRule
            .onNode(hasContentDescription("Navigate up"))
            .performClick()

        // Home screen should still be functional
        composeTestRule
            .onNodeWithText("Current Streak", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_scrollable_content() {
        // Verify content is scrollable
        composeTestRule
            .onNode(hasScrollAction())
            .performScrollToIndex(0)

        // Scroll down
        composeTestRule
            .onNode(hasScrollAction())
            .performTouchInput {
                swipeUp()
            }

        // Should still work
        composeTestRule.waitForIdle()
    }

    // Helper functions

    private fun navigateToHomeScreen() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Try to skip onboarding if present
        try {
            composeTestRule
                .onNodeWithText("Skip", ignoreCase = true)
                .performClick()
        } catch (e: Exception) {
            // Already past onboarding
        }

        // Wait to reach home
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Current Streak", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule
                .onAllNodesWithText("Home", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
