package com.glowup.ai.data.repository.support

import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.domain.model.Plan
import java.util.concurrent.ConcurrentHashMap

/**
 * A value handed back to a repository caller: the data plus an offline-first staleness marker.
 * [stale] `true` means "this is cached data being shown while a refresh may or may not be in
 * flight" — UI wires this to a "showing cached data" affordance rather than silently pretending
 * it is fresh (ANDROID_PLAN.md 2.4 "Offline-first reads"). [refreshError] is populated when a
 * background refresh behind a stale value failed, so the UI can show BOTH the cached content and
 * a small "couldn't refresh" affordance instead of one hiding the other.
 */
data class Cached<T>(val data: T, val stale: Boolean, val fetchedAtMillis: Long, val refreshError: ApiError? = null)

private data class Entry<T>(val value: T, val plan: Plan, val fetchedAtMillis: Long, val valid: Boolean)

/**
 * In-memory cache keyed by an arbitrary string (repositories key it `"$userId:$vertical"` etc.)
 * that also remembers the [Plan] the cached value was fetched under.
 *
 * This is what makes "cache keyed by {user_id, plan}" (frontend-api-map.md trap #7) real: [get]
 * takes the CURRENT plan and treats a plan mismatch as a cache miss even if the entry is
 * otherwise [Entry.valid] — a downgrade/upgrade must never serve the other tier's cached shape.
 *
 * [invalidate] does not delete data; it flips [Entry.valid] to `false` so a caller can still show
 * the stale copy immediately (per the offline-first requirement) while a fresh fetch runs behind
 * it — see [Cached].
 */
class KeyedMemoryCache<T> {

    private val map = ConcurrentHashMap<String, Entry<T>>()

    /** Returns the cached value for [key] if present, regardless of validity/plan — used to seed
     * the "stale" copy shown alongside a refresh. */
    fun peek(key: String, currentPlan: Plan? = null): Cached<T>? = map[key]
        ?.takeIf { currentPlan == null || it.plan == currentPlan }
        ?.let { Cached(it.value, stale = true, fetchedAtMillis = it.fetchedAtMillis) }

    /** Returns a FRESH (valid, plan-matching) cached value for [key], or `null` on any miss —
     * wrong plan, invalidated, or never populated. */
    fun getFresh(key: String, currentPlan: Plan): Cached<T>? {
        val entry = map[key] ?: return null
        if (!entry.valid || entry.plan != currentPlan) return null
        return Cached(entry.value, stale = false, fetchedAtMillis = entry.fetchedAtMillis)
    }

    fun put(key: String, value: T, plan: Plan, fetchedAtMillis: Long = System.currentTimeMillis()) {
        map[key] = Entry(value, plan, fetchedAtMillis, valid = true)
    }

    /** Marks every entry for keys matching [keyPredicate] stale without discarding the data. */
    fun invalidate(keyPredicate: (String) -> Boolean) {
        map.replaceAll { key, entry -> if (keyPredicate(key)) entry.copy(valid = false) else entry }
    }

    fun invalidateAll() {
        map.replaceAll { _, entry -> entry.copy(valid = false) }
    }

    fun isValid(key: String, currentPlan: Plan): Boolean {
        val entry = map[key] ?: return false
        return entry.valid && entry.plan == currentPlan
    }
}
