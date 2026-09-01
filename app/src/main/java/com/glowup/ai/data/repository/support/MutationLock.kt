package com.glowup.ai.data.repository.support

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.remote.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Guards a single non-idempotent mutation (frontend-api-map.md trap #9: `POST /users`,
 * `POST /captures`, `POST /routine-events`, subscription changes, offer clicks, `POST /products`
 * are ALL non-idempotent). [run] refuses to start a second attempt for a [key] that already has
 * one in flight instead of racing two POSTs, and [pendingKeys] lets a ViewModel disable the
 * triggering control while a mutation is outstanding.
 *
 * This class NEVER retries internally — a caller whose [run] returns
 * `GlowResult.Failure(ApiError.Network(...))` must treat that as "status unknown" and reconcile
 * against the authoritative resource (re-fetch profile/dashboard/subscription/experiment) rather
 * than calling [run] again with the same intent.
 */
class MutationLock<K> {
    private val _pendingKeys = MutableStateFlow<Set<K>>(emptySet())
    private val mutex = Mutex()
    val pendingKeys: StateFlow<Set<K>> = _pendingKeys.asStateFlow()

    fun isPending(key: K): Boolean = key in _pendingKeys.value

    suspend fun <T> run(
        key: K,
        block: suspend () -> GlowResult<T>,
    ): GlowResult<T> {
        val acquired =
            mutex.withLock {
                if (key in _pendingKeys.value) {
                    false
                } else {
                    _pendingKeys.update { it + key }
                    true
                }
            }
        if (!acquired) {
            return GlowResult.Failure(
                // Do not include the key: callers commonly key mutations by
                // user/product ids and this error can reach telemetry.
                ApiError.Unknown(IllegalStateException("mutation already pending")),
            )
        }
        return try {
            block()
        } finally {
            mutex.withLock { _pendingKeys.update { it - key } }
        }
    }
}
