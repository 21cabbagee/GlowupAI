package com.glowup.ai.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.testing.HiltTestBase
import com.glowup.ai.testing.MockResponses
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration test for HomeRepository with mocked API.
 *
 * This demonstrates:
 * - Injecting real repositories with test dependencies
 * - Mocking API responses
 * - Testing repository logic with real implementations
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeRepositoryIntegrationTest : HiltTestBase() {

    @Inject
    lateinit var homeRepository: HomeRepository

    @Test
    fun getDashboard_withSuccessfulResponse_returnsData() = runTest {
        // Given - mock successful dashboard response
        mockWebServer.enqueue(MockResponses.successfulDashboard("user_123"))

        // When - fetch dashboard
        val result = homeRepository.getDashboard("user_123")

        // Then - should succeed with data
        assertTrue(result is GlowResult.Success)
        val dashboard = (result as GlowResult.Success).data.data
        assertEquals("user_123", dashboard.profile.user.id)
        assertEquals(5, dashboard.engagement?.captureStreak)
        assertEquals(15, dashboard.engagement?.captureCount)
        assertNotNull(dashboard.profile.appearanceProfiles.firstOrNull()?.baselineCaptureId)
    }

    @Test
    fun getDashboard_withErrorResponse_returnsFailure() = runTest {
        // Given - mock error response
        mockWebServer.enqueue(MockResponses.errorResponse("Server error"))

        // When - fetch dashboard
        val result = homeRepository.getDashboard("user_123")

        // Then - should fail
        assertTrue(result is GlowResult.Failure)
    }

    @Test
    fun getDashboard_withEmptyDashboard_returnsZeroStreaks() = runTest {
        // Given - mock empty dashboard for new user
        mockWebServer.enqueue(MockResponses.emptyDashboard("new_user"))

        // When - fetch dashboard
        val result = homeRepository.getDashboard("new_user")

        // Then - should succeed with zero values
        assertTrue(result is GlowResult.Success)
        val dashboard = (result as GlowResult.Success).data.data
        assertEquals(0, dashboard.engagement?.captureStreak)
        assertEquals(0, dashboard.engagement?.captureCount)
        assertNull(dashboard.profile.appearanceProfiles.firstOrNull()?.baselineCaptureId)
    }

    @Test
    fun getDashboard_withUnauthorized_returnsFailure() = runTest {
        // Given - mock unauthorized response
        mockWebServer.enqueue(MockResponses.unauthorizedResponse())

        // When - fetch dashboard
        val result = homeRepository.getDashboard("user_123")

        // Then - should fail with auth error
        assertTrue(result is GlowResult.Failure)
    }
}
