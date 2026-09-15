package com.glowup.ai.feature.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlowDeepLinksTest {
    @Test
    fun `signup confirmation callback returns to splash for session admission`() {
        assertEquals(
            GlowDestination.Splash,
            authCallbackDestination("signup"),
        )
    }

    @Test
    fun `recovery callback opens password reset`() {
        assertEquals(
            GlowDestination.ResetPassword,
            authCallbackDestination("recovery"),
        )
    }

    @Test
    fun `unknown auth callback type is not admitted`() {
        assertNull(authCallbackDestination("unexpected"))
    }
}
