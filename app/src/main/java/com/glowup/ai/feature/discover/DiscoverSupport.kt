package com.glowup.ai.feature.discover

import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.domain.model.Entitlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Shared, feature-local presentation helpers for the discover feature. Kept out of the core package (not
 * owned by this task) — every other feature package has its own equivalent (e.g.
 * `feature/insights/InsightsSupport.kt`).
 */

/**
 * The single canonical Premium truth ([Entitlement.isPremium]) derived reactively from
 * [SessionStore]'s cached plan/status — the same AND rule
 * [com.glowup.ai.domain.SessionState.canUsePremium] uses, never re-derived ad hoc. This is a fast,
 * offline-capable HINT used to decide up front whether to render [com.glowup.ai.core.ui.LockedCard]
 * and skip a doomed network call for recommendations/prediction/purchase-guidance; it is never the
 * actual enforcement. The backend's own `403` — normalised to [ApiError.PremiumRequired] — remains
 * authoritative, and [DiscoverViewModel] always branches to [SectionState.Locked] on that error
 * even if this hint said otherwise (e.g. a stale cached plan after a lapsed subscription).
 *
 * Offers/click NEVER consult this — they are free for every plan (ANDROID_PLAN.md non-negotiable
 * constraint) — see [com.glowup.ai.data.repository.DiscoverRepository].
 */
internal fun SessionStore.canUsePremiumFlow(): Flow<Boolean> =
    combine(planFlow, entitlementStatusFlow) { plan, status ->
        Entitlement(plan = plan, status = status, startedAt = null, renewsAt = null, source = null).isPremium
    }

internal fun ApiError.toDisplayMessage(): String =
    when (this) {
        is ApiError.Unauthorized -> "Please sign in again to continue."
        is ApiError.ConsentRequired -> "You need to accept the facial-data consent before this works."
        is ApiError.PremiumRequired -> "$feature requires Premium."
        is ApiError.CaptureQualityRejected -> "That photo didn't pass the quality check."
        is ApiError.Validation -> fields.values.firstOrNull() ?: "Check the form and try again."
        is ApiError.NotFound -> what
        is ApiError.Conflict -> message
        is ApiError.Network -> "You're offline. Check your connection and try again."
        is ApiError.Server -> "Something went wrong on our end ($code). Try again."
        is ApiError.Unknown -> "Something went wrong. Try again."
    }

/** `true` only for the specific 403 shape that means "you're not Premium" — used to decide whether
 * to render [com.glowup.ai.core.ui.LockedCard] instead of a generic [com.glowup.ai.core.ui.ErrorState]. */
internal val ApiError.isPremiumGate: Boolean get() = this is ApiError.PremiumRequired
