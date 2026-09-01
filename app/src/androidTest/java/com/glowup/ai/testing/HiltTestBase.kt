package com.glowup.ai.testing

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base class for Hilt instrumented tests.
 *
 * Provides:
 * - HiltAndroidRule for dependency injection
 * - MockWebServer for API mocking
 * - Common setup and teardown
 *
 * Usage:
 * ```
 * @HiltAndroidTest
 * class MyTest : HiltTestBase() {
 *     @Test
 *     fun myTest() {
 *         // Mock API response
 *         mockWebServer.enqueue(MockResponse().setBody("..."))
 *
 *         // Test code
 *     }
 * }
 * ```
 */
@HiltAndroidTest
abstract class HiltTestBase {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var mockWebServer: MockWebServer

    @Before
    open fun setUp() {
        hiltRule.inject()
    }

    @After
    open fun tearDown() {
        // MockWebServer is a singleton in tests, so we just clear the queue
        // Don't shut it down or it will break other tests
        try {
            // Drain any pending requests
            while (mockWebServer.requestCount > 0) {
                mockWebServer.takeRequest()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
