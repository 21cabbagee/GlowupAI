package com.glowup.ai.data.repository

import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.repository.support.KeyedMemoryCache
import com.glowup.ai.domain.model.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [KeyedMemoryCache] — the mechanism behind frontend-api-map.md trap #7's cache
 * invalidation matrix ("cache keyed by {user_id, plan}", "invalidate on capture, routine,
 * experiment, consent, and subscription mutations", offline-first staleness marker).
 */
class KeyedMemoryCacheTest {
    @Test
    fun `a fresh put is returned by getFresh for the same plan`() {
        val cache = KeyedMemoryCache<String>()
        cache.put("user-1:skin", "dashboard-v1", Plan.FREE)

        val result = cache.getFresh("user-1:skin", Plan.FREE)

        assertEquals("dashboard-v1", result?.data)
        assertFalse(result!!.stale)
    }

    @Test
    fun `a plan change misses the cache even though the entry is still valid`() {
        // This is the exact bug trap #7 calls out: "a plan change must not serve stale free-tier
        // data" — a Premium upgrade must never see the free-tier cached dashboard, and vice versa.
        val cache = KeyedMemoryCache<String>()
        cache.put("user-1:skin", "free-tier-dashboard", Plan.FREE)

        assertNull(cache.getFresh("user-1:skin", Plan.PREMIUM))
        // The FREE-plan lookup still hits, proving the miss above was plan-specific, not a bug
        // that dropped the entry entirely.
        assertEquals("free-tier-dashboard", cache.getFresh("user-1:skin", Plan.FREE)?.data)
    }

    @Test
    fun `invalidate marks entries stale without discarding the data`() {
        val cache = KeyedMemoryCache<String>()
        cache.put("user-1:skin", "dashboard-v1", Plan.FREE)

        cache.invalidate { it.startsWith("user-1:") }

        assertNull(cache.getFresh("user-1:skin", Plan.FREE))
        val peeked = cache.peek("user-1:skin")
        assertEquals("dashboard-v1", peeked?.data)
        assertTrue(peeked!!.stale)
    }

    @Test
    fun `invalidate only affects matching keys - the invalidation matrix is per-user`() {
        val cache = KeyedMemoryCache<String>()
        cache.put("user-1:skin", "user-1-dashboard", Plan.FREE)
        cache.put("user-2:skin", "user-2-dashboard", Plan.FREE)

        cache.invalidate { it.startsWith("user-1:") }

        assertNull(cache.getFresh("user-1:skin", Plan.FREE))
        assertEquals("user-2-dashboard", cache.getFresh("user-2:skin", Plan.FREE)?.data)
    }

    @Test
    fun `a never-populated key is a clean miss`() {
        val cache = KeyedMemoryCache<String>()
        assertNull(cache.getFresh("user-1:skin", Plan.FREE))
        assertNull(cache.peek("user-1:skin"))
    }

    @Test
    fun `stale copy carries a refresh error without losing the data`() {
        val cache = KeyedMemoryCache<String>()
        cache.put("user-1:skin", "dashboard-v1", Plan.FREE)
        cache.invalidate { true }

        val stale = cache.peek("user-1:skin")!!
        val error = ApiError.Network(RuntimeException("offline"))
        val withError = stale.copy(refreshError = error)

        assertEquals("dashboard-v1", withError.data)
        assertEquals(error, withError.refreshError)
        assertTrue(withError.stale)
    }
}
