package com.glowup.ai.feature.auth

import com.glowup.ai.data.remote.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Supplies the current Supabase access JWT to the trusted GlowUp API layer. */
@Singleton
class SupabaseTokenProvider
    @Inject
    constructor(
        private val authGateway: SupabaseAuthGateway,
    ) : TokenProvider {
        override suspend fun idToken(forceRefresh: Boolean): String? =
            authGateway.accessToken(forceRefresh)
    }
