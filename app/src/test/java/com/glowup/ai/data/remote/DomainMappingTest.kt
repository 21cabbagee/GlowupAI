package com.glowup.ai.data.remote

import com.glowup.ai.data.remote.dto.ProductDto
import com.glowup.ai.data.remote.dto.parseIngredientsJson
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.domain.model.EntitlementStatus
import com.glowup.ai.domain.model.ExperimentStatus
import com.glowup.ai.domain.model.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit coverage for the two web-client bugs Task 2.3 must not reproduce
 * (ANDROID_PLAN.md section 3, items 1/2/4): the `ingredients_json` read
 * shape, the experiment status vocabulary, and the single-source
 * `Entitlement.isPremium` rule.
 */
class DomainMappingTest {

    @Test
    fun `ingredients_json is parsed from a JSON array string on read`() {
        val dto = ProductDto(
            id = "p1",
            name = "Barrier serum",
            category = "moisturizer",
            ingredientsJson = """["Niacinamide","Ceramide NP"]""",
        )

        val product = dto.toDomain()

        assertEquals(listOf("Niacinamide", "Ceramide NP"), product.ingredients)
    }

    @Test
    fun `ingredients_json falls back to comma splitting for a malformed value`() {
        assertEquals(listOf("Niacinamide", "Retinol"), parseIngredientsJson("Niacinamide, Retinol"))
    }

    @Test
    fun `ingredients_json null or blank yields an empty list, never a crash`() {
        assertEquals(emptyList<String>(), parseIngredientsJson(null))
        assertEquals(emptyList<String>(), parseIngredientsJson(""))
    }

    @Test
    fun `experiment status running is recognised and active is never produced`() {
        assertEquals(ExperimentStatus.RUNNING, ExperimentStatus.fromRaw("running"))
        // The backend NEVER emits "active" for an experiment; an app that
        // filtered on it (like the web client) would see an always-empty
        // list. Confirm it falls back to UNKNOWN rather than silently
        // matching some other state.
        assertEquals(ExperimentStatus.UNKNOWN, ExperimentStatus.fromRaw("active"))
    }

    @Test
    fun `experiment status tolerates an unrecognised future value`() {
        assertEquals(ExperimentStatus.UNKNOWN, ExperimentStatus.fromRaw("archived"))
    }

    @Test
    fun `isPremium requires both plan premium and status active`() {
        val premium = com.glowup.ai.domain.model.Entitlement(
            plan = Plan.PREMIUM,
            status = EntitlementStatus.ACTIVE,
            startedAt = null,
            renewsAt = null,
            source = null,
        )
        assertTrue(premium.isPremium)

        val cancelledPremium = premium.copy(status = EntitlementStatus.CANCELLED)
        assertFalse(cancelledPremium.isPremium)

        val activeFree = premium.copy(plan = Plan.FREE)
        assertFalse(activeFree.isPremium)
    }
}
