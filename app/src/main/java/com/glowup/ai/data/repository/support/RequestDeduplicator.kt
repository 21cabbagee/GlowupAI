package com.glowup.ai.data.repository.support

import com.glowup.ai.core.util.GlowResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coalesces concurrent callers keyed by [key] into ONE in-flight request. This is the mechanism
 * behind frontend-api-map.md trap #7's "add request deduplication" for `GET /dashboard` and
 * `GET /engagement` (both have server side effects — a stampede of concurrent callers must never
 * turn into a stampede of side effects), but it is generic and used by every repository's GET
 * paths.
 *
 * The winning request runs in [scope] (the app-wide `@ApplicationScope`, not the first caller's
 * own scope) precisely so that if the caller who triggered the fetch is cancelled (e.g. a
 * ViewModel scope torn down on navigation), every OTHER caller still waiting on the same key still
 * gets a result instead of the request being cancelled out from under them.
 */
class RequestDeduplicator<T>(
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<GlowResult<T>>>()

    suspend fun run(
        key: String,
        block: suspend () -> GlowResult<T>,
    ): GlowResult<T> {
        val deferred =
            mutex.withLock {
                inFlight[key] ?: scope.async { block() }.also { inFlight[key] = it }
            }
        return try {
            deferred.await()
        } finally {
            mutex.withLock {
                if (inFlight[key] === deferred) inFlight.remove(key)
            }
        }
    }
}
