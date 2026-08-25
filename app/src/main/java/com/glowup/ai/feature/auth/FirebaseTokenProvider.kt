package com.glowup.ai.feature.auth

import com.glowup.ai.data.remote.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real, Firebase-backed [TokenProvider]. Closes the binding gap `di/NetworkModule.kt` called
 * out with its deliberate no-op `TokenProvider { null }`: [AuthInterceptor] calls [idToken] on
 * every outgoing request (and again, with `forceRefresh = true`, after a `401`), and this is what
 * turns that into a real Firebase ID token once a user is signed in.
 *
 * Degrades to `null` — never throws — when Firebase isn't configured
 * ([FirebaseAuthGateway.instanceOrNull]) or no user is signed in, so a request simply goes out
 * unauthenticated in either case rather than crashing the network layer.
 */
@Singleton
class FirebaseTokenProvider @Inject constructor() : TokenProvider {
    override suspend fun idToken(forceRefresh: Boolean): String? {
        val user = FirebaseAuthGateway.currentUser() ?: return null
        return try {
            user.getIdToken(forceRefresh).await().token
        } catch (t: Throwable) {
            null
        }
    }
}
