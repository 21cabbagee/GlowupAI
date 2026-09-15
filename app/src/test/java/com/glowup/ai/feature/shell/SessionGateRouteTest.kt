package com.glowup.ai.feature.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionGateRouteTest {
    @Test
    fun `auth entry routes reset admission state before a new workspace user`() {
        assertTrue(shouldResetSessionGate(null))
        assertTrue(shouldResetSessionGate(GlowDestination.Welcome::class.qualifiedName))
        assertTrue(shouldResetSessionGate(GlowDestination.SignIn::class.qualifiedName))
        assertTrue(shouldResetSessionGate("${GlowDestination.SignIn::class.qualifiedName}?from=logout"))
        assertTrue(shouldResetSessionGate(GlowDestination.Onboarding::class.qualifiedName))
        assertTrue(shouldResetSessionGate(GlowDestination.Consent::class.qualifiedName))
        assertFalse(shouldResetSessionGate(GlowDestination.Home::class.qualifiedName))
    }
}
