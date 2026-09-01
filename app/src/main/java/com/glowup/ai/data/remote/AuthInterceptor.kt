package com.glowup.ai.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Supplies the Firebase ID token. Firebase itself is wired up later (Phase
 * 1/3.1) — this interface exists so the network layer has zero compile-time
 * dependency on the Firebase SDK. Implement it once `FirebaseAuth` is
 * available and bind it in a Hilt module (owned by the DI agent).
 */
interface TokenProvider {
    /**
     * Returns the current user's Firebase ID token, or `null` if there is no
     * signed-in user. Pass `forceRefresh = true` after a `401` to obtain a
     * fresh token before failing the request permanently.
     */
    suspend fun idToken(forceRefresh: Boolean = false): String?
}

/**
 * Attaches `Authorization: Bearer <idToken>` to every request. On a `401`
 * it refreshes the token exactly once and retries the same request; a
 * second `401` is returned to the caller as-is (mapped to
 * [ApiError.Unauthorized] by [ApiErrorMapper]) rather than looping forever.
 *
 * A request that already carries an explicit `Authorization` header (the
 * three `/api/admin` routes, which use a static admin bearer token instead
 * of a Firebase ID token) is left untouched.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header("Authorization") != null) {
            return chain.proceed(original)
        }

        val token = runBlockingIo { tokenProvider.idToken(forceRefresh = false) }
        val requestWithAuth =
            if (token != null) {
                original.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else {
                original
            }

        val response = chain.proceed(requestWithAuth)
        if (response.code != 401) {
            return response
        }

        // Refresh once and retry. If the refreshed token is the same or
        // still absent, return the original 401 rather than issuing a second
        // request with the same expired credential.
        val refreshed =
            synchronized(refreshLock) {
                runBlockingIo { tokenProvider.idToken(forceRefresh = true) }
            }
        if (refreshed == null || refreshed == token) {
            return response
        }
        response.close()
        val retried =
            requestWithAuth
                .newBuilder()
                .removeHeader("Authorization")
                .addHeader("Authorization", "Bearer $refreshed")
                .build()
        return chain.proceed(retried)
    }
}

/**
 * OkHttp interceptors are synchronous; [TokenProvider.idToken] is a suspend
 * function backed by the Firebase SDK's Task API. This bridges the two
 * without pulling a coroutine dispatcher/scope dependency into the
 * interceptor's constructor — OkHttp already runs interceptors off the
 * caller's thread for suspend Retrofit calls.
 */
private fun <T> runBlockingIo(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
