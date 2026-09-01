package com.glowup.ai.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glowup.ai.MainActivity
import com.glowup.ai.testing.HiltTestBase
import com.glowup.ai.testing.MockResponses
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example test showing how to test UI with mocked API responses.
 *
 * This demonstrates:
 * - Using HiltTestBase for dependency injection
 * - Mocking API responses with MockWebServer
 * - Testing Compose UI with injected dependencies
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenWithMockApiTest : HiltTestBase() {

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreen_loadsDataFromMockedApi() {
        // Given - mock API returns successful dashboard
        mockWebServer.enqueue(MockResponses.successfulDashboard())

        // Wait for app to load
        composeTestRule.waitForIdle()

        // The home screen should eventually load the mocked data
        // Note: This is a simplified example - actual implementation may need
        // navigation to home screen and proper state management
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Home", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun homeScreen_handlesApiError() {
        // Given - mock API returns error
        mockWebServer.enqueue(MockResponses.errorResponse("Server error"))

        // Wait for app to load
        composeTestRule.waitForIdle()

        // The app should handle the error gracefully
        // (specific behavior depends on implementation)
    }

    @Test
    fun homeScreen_displaysEmptyStateForNewUser() {
        // Given - mock API returns empty dashboard
        mockWebServer.enqueue(MockResponses.emptyDashboard())

        // Wait for app to load
        composeTestRule.waitForIdle()

        // The home screen should show empty state
        // (specific assertions depend on implementation)
    }
}
