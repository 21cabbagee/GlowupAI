package com.glowup.ai.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Retries GETs only, with a short bounded backoff on a network failure or a
 * transient HTTP response. NEVER retries POST/PATCH/DELETE — every mutation
 * in this API is documented as non-idempotent (profile creation, capture,
 * routine events, subscription changes, offer clicks — see
 * frontend-api-map.md trap #9), so an automatic retry could double-submit.
 *
 * `GET /dashboard` and `GET /engagement` are side-effecting reads (they
 * recompute verdicts / write a reminder row) but are still safe to retry
 * on a transport failure: a retry after no response reached the server (or
 * a 5xx before any write completed) does not risk a duplicate mutation the
 * way retrying a POST would. Callers are still responsible for not *polling*
 * either endpoint (that's a caching concern, not a retry-safety one).
 */
class RetryPolicyInterceptor(
    private val maxRetries: Int = 2,
    private val initialBackoffMillis: Long = 300L,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") {
            return chain.proceed(request)
        }

        var attempt = 0
        var lastException: IOException? = null
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                if (!isTransient(response.code) || attempt == maxRetries) {
                    return response
                }
                response.close()
            } catch (exc: IOException) {
                lastException = exc
                if (attempt == maxRetries) throw exc
            }
            try {
                Thread.sleep((initialBackoffMillis * (1L shl attempt)).coerceAtMost(MAX_BACKOFF_MILLIS))
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("retry interrupted", interrupted)
            }
            attempt++
        }
        throw lastException ?: IOException("retry loop exhausted without a response")
    }

    private fun isTransient(code: Int): Boolean = code == 408 || code == 429 || code in 500..599

    private companion object {
        const val MAX_BACKOFF_MILLIS = 5_000L
    }
}
