package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.apiCall
import com.glowup.ai.data.remote.dto.toDomain
import com.glowup.ai.data.repository.support.MutationLock
import com.glowup.ai.domain.model.Discover
import com.glowup.ai.domain.model.Offer
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns: `GET /discover`, `GET /commerce/offers`, `POST /commerce/offers/{id}/click`.
 *
 * Commerce offers are free for EVERY plan (ANDROID_PLAN.md non-negotiable constraint) — never
 * gate [getOffers] on entitlement here or in a caller. `click` is not idempotent
 * (frontend-api-map.md trap #9: "offer clicks are not idempotent") — [clickOffer] is
 * [MutationLock]-guarded per offer id so a retry (e.g. the user tapping again while the browser
 * intent is still launching) can't double-record a click.
 */
@Singleton
class DiscoverRepository
    @Inject
    constructor(
        private val api: GlowUpApi,
    ) {
        private val mutations = MutationLock<String>()
        val pendingKeys: StateFlow<Set<String>> = mutations.pendingKeys

        suspend fun getDiscover(userId: String): GlowResult<Discover> = apiCall { api.getDiscover(userId).toDomain() }

        /** Never gated on plan/entitlement — see class doc. */
        suspend fun getOffers(
            userId: String,
            productId: String? = null,
        ): GlowResult<List<Offer>> = apiCall { api.getOffers(userId, productId).map { it.toDomain() } }

        suspend fun clickOffer(
            userId: String,
            offerId: String,
        ): GlowResult<Offer> =
            mutations.run("click_offer:$offerId") {
                apiCall { api.clickOffer(userId, offerId).toDomain() }
            }
    }
