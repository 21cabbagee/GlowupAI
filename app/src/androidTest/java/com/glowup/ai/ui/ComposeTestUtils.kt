package com.glowup.ai.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/**
 * Compose UI test utilities and extension functions for GlowUp AI tests.
 *
 * These utilities help reduce flakiness and improve test reliability by:
 * - Providing proper waiting strategies
 * - Offering flexible matchers
 * - Handling common test scenarios
 */

// ================================================================================================
// Semantic Matcher Helpers
// ================================================================================================

/**
 * Creates a SemanticsMatcher that matches nodes that are NOT in the "on" state.
 * This is the inverse of isOn().
 */
fun isNotOn(): SemanticsMatcher {
    return SemanticsMatcher("is not on") { node ->
        !isOn().matches(node)
    }
}

// ================================================================================================
// Extension Functions for Better Readability
// ================================================================================================

/**
 * Asserts that a checkbox/toggle is not checked (off state).
 */
fun SemanticsNodeInteraction.assertIsNotChecked(): SemanticsNodeInteraction =
    assert(isNotOn())

/**
 * Asserts that a checkbox/toggle is checked (on state).
 */
fun SemanticsNodeInteraction.assertIsChecked(): SemanticsNodeInteraction =
    assert(isOn())

// ================================================================================================
// Waiting Utilities
// ================================================================================================

/**
 * Waits for any of the given text values to appear on screen.
 * Returns true if at least one text is found within the timeout.
 *
 * @param texts List of text values to search for
 * @param timeoutMillis Maximum time to wait in milliseconds
 * @param substring Whether to match substring
 * @param ignoreCase Whether to ignore case
 */
fun ComposeTestRule.waitForAnyText(
    texts: List<String>,
    timeoutMillis: Long = 10000,
    substring: Boolean = true,
    ignoreCase: Boolean = true,
): Boolean {
    return try {
        waitUntil(timeoutMillis = timeoutMillis) {
            texts.any { text ->
                onAllNodesWithText(text, substring = substring, ignoreCase = ignoreCase)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Waits for a specific text to appear on screen.
 *
 * @param text Text to search for
 * @param timeoutMillis Maximum time to wait in milliseconds
 * @param substring Whether to match substring
 * @param ignoreCase Whether to ignore case
 */
fun ComposeTestRule.waitForText(
    text: String,
    timeoutMillis: Long = 10000,
    substring: Boolean = true,
    ignoreCase: Boolean = true,
): Boolean {
    return try {
        waitUntil(timeoutMillis = timeoutMillis) {
            onAllNodesWithText(text, substring = substring, ignoreCase = ignoreCase)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Waits for content to appear and then asserts it's displayed.
 * Useful for reducing flaky tests by ensuring proper composition.
 *
 * @param text Text to wait for and assert
 * @param timeoutMillis Maximum time to wait
 */
fun ComposeTestRule.waitAndAssertDisplayed(
    text: String,
    timeoutMillis: Long = 10000,
    substring: Boolean = true,
    ignoreCase: Boolean = false,
) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText(text, substring = substring, ignoreCase = ignoreCase)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
    onNodeWithText(text, substring = substring, ignoreCase = ignoreCase)
        .assertIsDisplayed()
}

// ================================================================================================
// Semantic Matchers
// ================================================================================================

/**
 * Combines two semantic matchers with OR logic.
 */
infix fun SemanticsMatcher.or(other: SemanticsMatcher): SemanticsMatcher {
    return SemanticsMatcher("${this.description} OR ${other.description}") {
        this.matches(it) || other.matches(it)
    }
}

/**
 * Combines two semantic matchers with AND logic.
 */
infix fun SemanticsMatcher.and(other: SemanticsMatcher): SemanticsMatcher {
    return SemanticsMatcher("${this.description} AND ${other.description}") {
        this.matches(it) && other.matches(it)
    }
}

// ================================================================================================
// Common Test Scenarios
// ================================================================================================

/**
 * Attempts to skip onboarding by clicking the Skip button if present.
 * Useful for tests that need to get past onboarding quickly.
 */
fun ComposeTestRule.skipOnboardingIfPresent() {
    try {
        waitForIdle()
        val skipButton = onAllNodesWithText("Skip", ignoreCase = true, substring = true)
            .fetchSemanticsNodes()

        if (skipButton.isNotEmpty()) {
            onNodeWithText("Skip", ignoreCase = true, substring = true)
                .performClick()
            waitForIdle()
        }
    } catch (e: Exception) {
        // No skip button or already past onboarding
    }
}

/**
 * Verifies the app has launched to any valid screen without crashing.
 * Checks for common landing screens: Welcome, Home, Sign In, or GlowUp branding.
 */
fun ComposeTestRule.assertAppLaunched(timeoutMillis: Long = 15000) {
    waitUntil(timeoutMillis = timeoutMillis) {
        val hasWelcome = onAllNodesWithText("Welcome", substring = true, ignoreCase = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

        val hasHome = onAllNodesWithText("Home")
            .fetchSemanticsNodes()
            .isNotEmpty()

        val hasSignIn = onAllNodesWithText("Sign", substring = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

        val hasGlowUp = onAllNodesWithText("GlowUp", substring = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

        hasWelcome || hasHome || hasSignIn || hasGlowUp
    }
}

/**
 * Safely attempts to perform an action, catching exceptions.
 * Useful for optional UI interactions that may not always be present.
 *
 * @param action The action to attempt
 * @return true if action succeeded, false if exception was thrown
 */
inline fun <T> SemanticsNodeInteractionsProvider.tryAction(action: () -> T): Boolean {
    return try {
        action()
        true
    } catch (e: Exception) {
        false
    }
}
