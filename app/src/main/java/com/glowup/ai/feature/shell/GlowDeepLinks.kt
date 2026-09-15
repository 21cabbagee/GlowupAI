package com.glowup.ai.feature.shell

import android.content.Intent

/**
 * Converts only the app's allow-listed external links into type-safe destinations. SessionGate
 * still decides whether the destination is currently admissible.
 */
fun destinationFromIntent(intent: Intent?): GlowDestination? {
    if (intent?.action == ACTION_OPEN_CAPTURE) return GlowDestination.Capture

    val uri = intent?.data ?: return null
    if (uri.scheme != DEEP_LINK_SCHEME) return null

    val host = uri.host?.lowercase() ?: return null
    if (host == "auth" && uri.path?.trimEnd('/') == "/callback") {
        val callback =
            if (!uri.fragment.isNullOrBlank()) android.net.Uri.parse("https://callback.invalid/?${uri.fragment}") else uri
        return authCallbackDestination(callback.getQueryParameter("type"))
    }
    val id = uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
    return when (host) {
        "home" -> GlowDestination.Home
        "routine" -> GlowDestination.Routine
        "capture" -> GlowDestination.Capture
        "insights" -> GlowDestination.Insights
        "discover" -> GlowDestination.Discover
        "account" -> GlowDestination.Account
        "product" -> id?.let(GlowDestination::ProductDetail)
        "experiment" -> id?.let(GlowDestination::ExperimentDetail)
        else -> null
    }
}

/** Maps only known Supabase auth callback types to safe app entry routes. */
internal fun authCallbackDestination(type: String?): GlowDestination? =
    when (type) {
        "recovery" -> GlowDestination.ResetPassword
        "signup" -> GlowDestination.Splash
        else -> null
    }

const val ACTION_OPEN_CAPTURE = "com.glowup.ai.action.OPEN_CAPTURE"

private const val DEEP_LINK_SCHEME = "glowup"
