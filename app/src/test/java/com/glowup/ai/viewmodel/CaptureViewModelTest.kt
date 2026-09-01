package com.glowup.ai.viewmodel

import com.glowup.ai.data.repository.CaptureRepository
import com.glowup.ai.domain.model.Capture
import com.glowup.ai.domain.model.CaptureQuality
import com.glowup.ai.feature.capture.CaptureViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for CaptureViewModel.
 *
 * Tests:
 * - Image capture flow
 * - Quality validation
 * - Upload handling
 * - Error states
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {
    private lateinit var viewModel: CaptureViewModel
    private lateinit var captureRepository: CaptureRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        captureRepository = mockk(relaxed = true)
        viewModel = CaptureViewModel(captureRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitCapture with valid quality succeeds`() =
        runTest {
            // Given
            val mockCapture =
                Capture(
                    id = "capture_123",
                    userId = "user_123",
                    timestamp = System.currentTimeMillis(),
                    metrics =
                        mapOf(
                            "smoothness_score" to 75.5,
                            "clarity_score" to 80.0,
                        ),
                )
            val goodQuality =
                CaptureQuality(
                    facePresent = true,
                    yawDegrees = 0f,
                    pitchDegrees = 0f,
                    distanceCm = 45f,
                    expressionNeutral = true,
                )

            coEvery {
                captureRepository.submitCapture(any(), any(), any())
            } returns Result.success(mockCapture)

            // When
            viewModel.submitCapture("user_123", File("test.jpg"), goodQuality)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isUploading)
            assertNull(state.error)
            assertNotNull(state.captureResult)
        }

    @Test
    fun `submitCapture with bad quality fails validation`() =
        runTest {
            // Given - face not present
            val badQuality =
                CaptureQuality(
                    facePresent = false,
                    yawDegrees = 0f,
                    pitchDegrees = 0f,
                    distanceCm = 45f,
                    expressionNeutral = true,
                )

            // When
            viewModel.submitCapture("user_123", File("test.jpg"), badQuality)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("quality") || state.error!!.contains("face"))
        }

    @Test
    fun `submitCapture shows uploading state`() =
        runTest {
            // Given
            coEvery {
                captureRepository.submitCapture(any(), any(), any())
            } coAnswers {
                kotlinx.coroutines.delay(100)
                Result.success(mockk(relaxed = true))
            }

            // When
            viewModel.submitCapture(
                "user_123",
                File("test.jpg"),
                CaptureQuality(true, 0f, 0f, 45f, true),
            )

            // Then - immediately should be uploading
            assertTrue(viewModel.uiState.value.isUploading)
        }

    @Test
    fun `submitCapture handles network error`() =
        runTest {
            // Given
            coEvery {
                captureRepository.submitCapture(any(), any(), any())
            } returns Result.failure(Exception("Network error"))

            // When
            viewModel.submitCapture(
                "user_123",
                File("test.jpg"),
                CaptureQuality(true, 0f, 0f, 45f, true),
            )
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isUploading)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Network"))
        }

    @Test
    fun `retry after error works`() =
        runTest {
            // Given - first call fails, second succeeds
            coEvery {
                captureRepository.submitCapture(any(), any(), any())
            } returnsMany
                listOf(
                    Result.failure(Exception("Temporary error")),
                    Result.success(mockk(relaxed = true)),
                )

            val quality = CaptureQuality(true, 0f, 0f, 45f, true)

            // When - first attempt fails
            viewModel.submitCapture("user_123", File("test.jpg"), quality)
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.error)

            // When - retry succeeds
            viewModel.retry()
            advanceUntilIdle()

            // Then
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `quality validation checks all parameters`() =
        runTest {
            // Test each quality parameter
            val invalidQualities =
                listOf(
                    CaptureQuality(false, 0f, 0f, 45f, true), // No face
                    CaptureQuality(true, 45f, 0f, 45f, true), // Too much yaw
                    CaptureQuality(true, 0f, 45f, 45f, true), // Too much pitch
                    CaptureQuality(true, 0f, 0f, 20f, true), // Too close
                    CaptureQuality(true, 0f, 0f, 100f, true), // Too far
                )

            for (badQuality in invalidQualities) {
                // When
                viewModel.submitCapture("user_123", File("test.jpg"), badQuality)
                advanceUntilIdle()

                // Then
                assertNotNull(viewModel.uiState.value.error)

                // Reset
                viewModel.clearError()
            }
        }

    @Test
    fun `baseline capture is marked correctly`() =
        runTest {
            // Given
            val mockCapture =
                Capture(
                    id = "baseline_123",
                    userId = "user_123",
                    timestamp = System.currentTimeMillis(),
                    metrics = mapOf("smoothness_score" to 75.0),
                    isBaseline = true,
                )

            coEvery {
                captureRepository.submitCapture(any(), any(), any(), true)
            } returns Result.success(mockCapture)

            // When
            viewModel.submitCapture(
                "user_123",
                File("test.jpg"),
                CaptureQuality(true, 0f, 0f, 45f, true),
                isBaseline = true,
            )
            advanceUntilIdle()

            // Then
            coVerify {
                captureRepository.submitCapture(any(), any(), any(), true)
            }
        }

    @Test
    fun `clearError removes error state`() =
        runTest {
            // Given - error state
            coEvery {
                captureRepository.submitCapture(any(), any(), any())
            } returns Result.failure(Exception("Error"))

            viewModel.submitCapture(
                "user_123",
                File("test.jpg"),
                CaptureQuality(true, 0f, 0f, 45f, true),
            )
            advanceUntilIdle()

            // When
            viewModel.clearError()

            // Then
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `multiple rapid submissions are handled safely`() =
        runTest {
            // Given
            coEvery {
                captureRepository.submitCapture(any(), any(), any())
            } returns Result.success(mockk(relaxed = true))

            // When - rapid submissions
            repeat(3) {
                viewModel.submitCapture(
                    "user_123",
                    File("test$it.jpg"),
                    CaptureQuality(true, 0f, 0f, 45f, true),
                )
            }
            advanceUntilIdle()

            // Then - should complete without crash
            assertFalse(viewModel.uiState.value.isUploading)
        }
}
