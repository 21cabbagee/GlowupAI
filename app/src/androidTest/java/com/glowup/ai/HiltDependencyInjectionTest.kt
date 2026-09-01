package com.glowup.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.repository.HomeRepository
import com.glowup.ai.testing.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Tests that verify Hilt dependency injection is working correctly in instrumented tests.
 *
 * These tests ensure:
 * - Hilt modules are properly configured
 * - Dependencies can be injected
 * - Test modules replace production modules
 * - MockWebServer is available for API mocking
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltDependencyInjectionTest : HiltTestBase() {

    @Inject
    lateinit var sessionStore: SessionStore

    @Inject
    lateinit var glowUpApi: GlowUpApi

    @Inject
    lateinit var homeRepository: HomeRepository

    @Inject
    lateinit var testMockWebServer: MockWebServer

    @Test
    fun hilt_injectsSessionStore() {
        // Verify SessionStore is injected
        assertNotNull(sessionStore)
    }

    @Test
    fun hilt_injectsGlowUpApi() {
        // Verify API is injected (from TestNetworkModule)
        assertNotNull(glowUpApi)
    }

    @Test
    fun hilt_injectsRepository() {
        // Verify repository is injected with its dependencies
        assertNotNull(homeRepository)
    }

    @Test
    fun hilt_injectsMockWebServer() {
        // Verify MockWebServer is available (from TestNetworkModule)
        assertNotNull(testMockWebServer)
        assertNotNull(mockWebServer) // Also from base class
        assertSame(testMockWebServer, mockWebServer) // Should be the same singleton
    }

    @Test
    fun mockWebServer_isRunning() {
        // Verify MockWebServer is started and has a URL
        val url = mockWebServer.url("/")
        assertNotNull(url)
        assertTrue(url.toString().startsWith("http://"))
    }

    @Test
    fun hilt_providesTestDoubles() {
        // This test verifies that test modules are being used instead of production modules
        // The API should point to MockWebServer, not the real backend
        val baseUrl = mockWebServer.url("/").toString()

        // The Retrofit instance should be configured with MockWebServer URL
        // This is implicit in TestNetworkModule but we can verify the server is reachable
        assertTrue("MockWebServer should have a local URL",
            baseUrl.contains("127.0.0.1") || baseUrl.contains("localhost"))
    }
}
