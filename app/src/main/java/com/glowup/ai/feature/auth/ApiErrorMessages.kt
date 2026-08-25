package com.glowup.ai.feature.auth

import com.glowup.ai.data.remote.ApiError

/**
 * Shared user-facing copy for [ApiError] — used across `feature/auth` and `feature/onboarding`
 * (both owned by this task) so a session/profile/consent failure never surfaces a raw exception
 * message or a bare HTTP status to the user.
 */
fun ApiError.toMessage(): String = when (this) {
    is ApiError.Network -> "No connection. Check your network and try again."
    is ApiError.Server -> "Something went wrong on our end. Please try again."
    is ApiError.Unauthorized -> "Your session expired. Please sign in again."
    is ApiError.ConsentRequired -> "Photo tracking requires consent first."
    is ApiError.PremiumRequired -> "$feature requires Premium."
    is ApiError.CaptureQualityRejected -> "That photo didn't meet the quality checks. Please retry."
    is ApiError.Validation -> fields.values.firstOrNull() ?: "Please check your details and try again."
    is ApiError.Conflict -> message
    is ApiError.NotFound -> "We couldn't find your account. Please sign in again."
    is ApiError.Unknown -> "Something went wrong. Please try again."
}
