package com.glowup.ai.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.testing.HiltTestBase
import com.glowup.ai.testing.MockResponses
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
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
        val result = homeRepository.getDashboard("user_123").first()

        // Then - should succeed with data
        assertTrue(result.isSuccess)
        val dashboard = result.getOrNull()
        assertNotNull(dashboard)
        assertEquals("user_123", dashboard?.userId)
        assertEquals(5, dashboard?.currentStreak)
        assertEquals(10, dashboard?.longestStreak)
        assertEquals(15, dashboard?.totalCaptures)
        assertTrue(dashboard?.hasBaseline == true)
    }

    @Test
    fun getDashboard_withErrorResponse_returnsFailure() = runTest {
        // Given - mock error response
        mockWebServer.enqueue(MockResponses.errorResponse("Server error"))

        // When - fetch dashboard
        val result = homeRepository.getDashboard("user_123").first()

        // Then - should fail
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getDashboard_withEmptyDashboard_returnsZeroStreaks() = runTest {
        // Given - mock empty dashboard for new user
        mockWebServer.enqueue(MockResponses.emptyDashboard("new_user"))

        // When - fetch dashboard
        val result = homeRepository.getDashboard("new_user").first()

        // Then - should succeed with zero values
        assertTrue(result.isSuccess)
        val dashboard = result.getOrNull()
        assertNotNull(dashboard)
        assertEquals(0, dashboard?.currentStreak)
        assertEquals(0, dashboard?.longestStreak)
        assertEquals(0, dashboard?.totalCaptures)
        assertFalse(dashboard?.hasBaseline == true)
    }

    @Test
    fun getDashboard_withUnauthorized_returnsFailure() = runTest {
        // Given - mock unauthorized response
        mockWebServer.enqueue(MockResponses.unauthorizedResponse())

        // When - fetch dashboard
        val result = homeRepository.getDashboard("user_123").first()

        // Then - should fail with auth error
        assertTrue(result.isFailure)
    }
}
