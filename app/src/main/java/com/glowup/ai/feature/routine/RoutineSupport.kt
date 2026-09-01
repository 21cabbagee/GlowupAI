package com.glowup.ai.feature.routine

import com.glowup.ai.data.remote.ApiError

/**
 * Shared, feature-local presentation helpers for the routine feature. Kept out of the core package (which
 * this task does not own) — every other feature package is expected to have its own equivalent.
 */
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

/** `true` only for the specific 403 shape that means "you're not Premium" — used to decide
 * whether to render [com.glowup.ai.core.ui.LockedCard] instead of a generic [ErrorState]. */
internal fun ApiError.isPremiumRequired(): Boolean = this is ApiError.PremiumRequired
