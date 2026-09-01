package com.glowup.ai.data.remote

import com.glowup.ai.core.util.GlowResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * End-to-end (MockWebServer -> Retrofit -> [ApiErrorMapper]) coverage of
 * every error shape called out in ANDROID_PLAN.md Task 2.3 and
 * frontend-api-map.md's "Error handling must preserve structured detail".
 */
class ApiErrorMapperTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GlowUpApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = testApi(server)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `403 with consent message maps to ConsentRequired`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(403).setBody(
                    """{"detail":"explicit facial-data consent is required before using photo capture"}""",
                ),
            )

            val result = apiCall { api.getCaptureGuide("user-1") }

            assertTrue(result is GlowResult.Failure)
            assertEquals(ApiError.ConsentRequired, (result as GlowResult.Failure).error)
        }

    @Test
    fun `403 with requires Premium message maps to PremiumRequired with feature`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(403).setBody(
                    """{"detail":"Experiments requires Premium; upgrade the plan to unlock it"}""",
                ),
            )

            val result = apiCall { api.listExperiments("user-1") }

            val error = (result as GlowResult.Failure).error
            assertTrue(error is ApiError.PremiumRequired)
            assertEquals("Experiments", (error as ApiError.PremiumRequired).feature)
        }

    @Test
    fun `400 with structured quality object maps to CaptureQualityRejected with coaching`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(400).setBody(
                    """
                    {"detail":{"message":"capture quality is below the acceptance threshold","quality":
                      {"face_present":true,"yaw_degrees":18.0,"pitch_degrees":0.0,"brightness":0.5,
                       "sharpness":0.8,"distance_cm":45.0,"expression_neutral":true,
                       "reference_card_present":false,"score":0.6,"accepted":false,
                       "failed_checks":["turn_head_less"],
                       "coaching":[{"check":"turn_head_less","message":"Turn 18° back toward center (left) — yaw must stay within 12°."}]}
                    }}
                    """.trimIndent(),
                ),
            )

            val result =
                apiCall {
                    api.createCapture(
                        com.glowup.ai.data.remote.dto
                            .CaptureCreateRequestDto(userId = "user-1", imageBase64 = "abc"),
                    )
                }

            val error = (result as GlowResult.Failure).error
            assertTrue(error is ApiError.CaptureQualityRejected)
            val rejected = error as ApiError.CaptureQualityRejected
            assertEquals(false, rejected.quality.accepted)
            assertEquals(listOf("turn_head_less"), rejected.quality.failedChecks)
            assertEquals(1, rejected.coaching.size)
            assertEquals("turn_head_less", rejected.coaching[0].check)
        }

    @Test
    fun `422 validation array maps loc to field name`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(422).setBody(
                    """{"detail":[{"loc":["body","question"],"msg":"field required","type":"value_error.missing"}]}""",
                ),
            )

            val result =
                apiCall {
                    api.askQna(
                        "user-1",
                        com.glowup.ai.data.remote.dto
                            .QnaCreateRequestDto(question = ""),
                    )
                }

            val error = (result as GlowResult.Failure).error
            assertTrue(error is ApiError.Validation)
            assertEquals(mapOf("question" to "field required"), (error as ApiError.Validation).fields)
        }

    @Test
    fun `400 not found string maps to NotFound`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"user not found"}"""))

            val result = apiCall { api.getProfile("missing-user") }

            val error = (result as GlowResult.Failure).error
            assertTrue(error is ApiError.NotFound)
            assertEquals("user not found", (error as ApiError.NotFound).what)
        }

    @Test
    fun `400 already exists string maps to Conflict`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"barcode already exists"}"""))

            val result =
                apiCall {
                    api.createProduct(
                        com.glowup.ai.data.remote.dto
                            .ProductCreateRequestDto(name = "Serum", barcode = "123"),
                    )
                }

            assertTrue((result as GlowResult.Failure).error is ApiError.Conflict)
        }

    @Test
    fun `204 delete finishes without parsing a body`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(204))

            val result = apiCallNoContent { api.deleteUser("user-1") }

            assertTrue(result is GlowResult.Success)
        }

    @Test
    fun `network failure maps to Network error`() =
        runTest {
            server.shutdown()

            val result = apiCall { api.health() }

            if (result !is GlowResult.Failure || result.error !is ApiError.Network) {
                fail("expected ApiError.Network, got $result")
            }
        }
}
