package com.glowup.ai.feature.insights

import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.domain.model.Entitlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The single canonical Premium truth ([Entitlement.isPremium] — `plan == PREMIUM AND
 * status == ACTIVE`) derived reactively from [SessionStore]'s cached plan/status, so every screen
 * in this package can gate its UI without ever re-deriving the AND rule itself (the exact mistake
 * ANDROID_PLAN.md bug #2 / traps #6, #11, #12 document in the web client, and the same rule
 * [com.glowup.ai.domain.SessionState.canUsePremium] uses). This is a fast, offline-capable HINT
 * used to decide whether to render [com.glowup.ai.core.ui.LockedCard] up front and to skip a
 * doomed network call; it is never the actual enforcement. The backend's own `403` — normalised to
 * [ApiError.PremiumRequired] by `ApiErrorMapper` — remains authoritative, and every ViewModel in
 * this package still branches to the Locked state on that error even if this hint said otherwise
 * (e.g. a stale cached plan after a lapsed subscription).
 */
internal fun SessionStore.canUsePremiumFlow(): Flow<Boolean> =
    combine(planFlow, entitlementStatusFlow) { plan, status ->
        Entitlement(plan = plan, status = status, startedAt = null, renewsAt = null, source = null).isPremium
    }

/** Human-readable copy for [ApiError], reused by every screen ViewModel in this package. Premium
 * gates are branched to a distinct Locked state before this is ever called for that case. */
internal fun ApiError.toUserMessage(): String =
    when (this) {
        is ApiError.Unauthorized -> "Please sign in again to continue."
        is ApiError.ConsentRequired -> "Facial-data consent is required before using this feature."
        is ApiError.PremiumRequired -> "$feature requires Premium."
        is ApiError.CaptureQualityRejected -> "That capture didn't pass the quality check."
        is ApiError.Validation -> fields.values.firstOrNull() ?: "Check your input and try again."
        is ApiError.NotFound -> "We couldn't find that."
        is ApiError.Conflict -> message
        is ApiError.Network -> "You're offline. Check your connection and try again."
        is ApiError.Server -> "Something went wrong on our end. Try again shortly."
        is ApiError.Unknown -> "Something unexpected happened."
    }

internal val ApiError.isPremiumGate: Boolean get() = this is ApiError.PremiumRequired
