package com.glowup.ai.viewmodel

import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.domain.model.Dashboard
import com.glowup.ai.domain.model.Streak
import com.glowup.ai.feature.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HomeViewModel.
 *
 * Tests:
 * - Dashboard data loading
 * - Streak calculation
 * - Error handling
 * - State updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel
    private lateinit var homeRepository: HomeRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        homeRepository = mockk(relaxed = true)
        viewModel = HomeViewModel(homeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard updates state with dashboard data`() =
        runTest {
            // Given
            val mockDashboard =
                Dashboard(
                    userId = "test_user",
                    currentStreak = 5,
                    longestStreak = 10,
                    totalCaptures = 15,
                    history = emptyList(),
                )
            coEvery { homeRepository.getDashboard(any()) } returns flowOf(Result.success(mockDashboard))

            // When
            viewModel.loadDashboard("test_user")
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(5, state.currentStreak)
            assertEquals(10, state.longestStreak)
        }

    @Test
    fun `loadDashboard shows loading state`() =
        runTest {
            // Given
            coEvery { homeRepository.getDashboard(any()) } coAnswers {
                kotlinx.coroutines.delay(100)
                flowOf(Result.success(Dashboard("user", 0, 0, 0, emptyList())))
            }

            // When
            viewModel.loadDashboard("test_user")

            // Then - immediately after call, should be loading
            assertTrue(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `loadDashboard handles error gracefully`() =
        runTest {
            // Given
            val errorMessage = "Network error"
            coEvery { homeRepository.getDashboard(any()) } returns
                flowOf(
                    Result.failure(Exception(errorMessage)),
                )

            // When
            viewModel.loadDashboard("test_user")
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains(errorMessage))
        }

    @Test
    fun `refresh calls repository refresh`() =
        runTest {
            // Given
            val userId = "test_user"

            // When
            viewModel.refresh(userId)
            advanceUntilIdle()

            // Then
            coVerify { homeRepository.getDashboard(userId) }
        }

    @Test
    fun `streak calculation is correct`() =
        runTest {
            // Given - user captured for 7 consecutive days
            val mockDashboard =
                Dashboard(
                    userId = "test_user",
                    currentStreak = 7,
                    longestStreak = 7,
                    totalCaptures = 7,
                    history = emptyList(),
                )
            coEvery { homeRepository.getDashboard(any()) } returns flowOf(Result.success(mockDashboard))

            // When
            viewModel.loadDashboard("test_user")
            advanceUntilIdle()

            // Then
            assertEquals(7, viewModel.uiState.value.currentStreak)
        }

    @Test
    fun `empty dashboard shows zero streaks`() =
        runTest {
            // Given - new user with no captures
            val emptyDashboard =
                Dashboard(
                    userId = "new_user",
                    currentStreak = 0,
                    longestStreak = 0,
                    totalCaptures = 0,
                    history = emptyList(),
                )
            coEvery { homeRepository.getDashboard(any()) } returns flowOf(Result.success(emptyDashboard))

            // When
            viewModel.loadDashboard("new_user")
            advanceUntilIdle()

            // Then
            assertEquals(0, viewModel.uiState.value.currentStreak)
            assertEquals(0, viewModel.uiState.value.longestStreak)
            assertEquals(0, viewModel.uiState.value.totalCaptures)
        }

    @Test
    fun `clearError removes error state`() =
        runTest {
            // Given - error state
            coEvery { homeRepository.getDashboard(any()) } returns
                flowOf(
                    Result.failure(Exception("Error")),
                )
            viewModel.loadDashboard("test_user")
            advanceUntilIdle()

            // When
            viewModel.clearError()

            // Then
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `multiple rapid refreshes dont cause issues`() =
        runTest {
            // Given
            val mockDashboard = Dashboard("user", 1, 1, 1, emptyList())
            coEvery { homeRepository.getDashboard(any()) } returns flowOf(Result.success(mockDashboard))

            // When - rapid refreshes
            repeat(5) {
                viewModel.refresh("test_user")
            }
            advanceUntilIdle()

            // Then - should complete without crash
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `baseline comparison is available when baseline exists`() =
        runTest {
            // Given - dashboard with baseline
            val dashboardWithBaseline =
                Dashboard(
                    userId = "user",
                    currentStreak = 3,
                    longestStreak = 5,
                    totalCaptures = 10,
                    history = emptyList(),
                    hasBaseline = true,
                )
            coEvery { homeRepository.getDashboard(any()) } returns
                flowOf(
                    Result.success(dashboardWithBaseline),
                )

            // When
            viewModel.loadDashboard("user")
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.hasBaseline ?: false)
        }
}
