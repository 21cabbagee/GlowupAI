package com.glowup.ai.domain

import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.domain.model.Profile

/**
 * The app's single source of truth for "where is this session in the sequential
 * product workflow" (`backend/docs/frontend-api-map.md`, "Sequential product
 * workflow" + trap #1). Every state except [NoUser], [Authenticating] and
 * [ProfileLoading] carries the [Profile] it was derived from, because it was
 * derived from one specific authoritative `GET /profile`-shaped response
 * (`GET /profile`, `POST /api/auth/session`, `POST /api/users`, `POST /consent`
 * all return this same shape) — never from a button press or an optimistic
 * local flag (trap #1, trap #2).
 *
 * The workspace shell (`feature/shell`) must not render before this reaches
 * one of the "authoritative" states below; [NoUser], [Authenticating] and
 * [ProfileLoading] are all pre-authoritative and must show a gate, not tabs.
 */
sealed class SessionState {
    /** True while nothing derived from `GET /profile` says otherwise: the
     * shell must keep the workspace hidden. */
    abstract val isAuthoritative: Boolean

    /** Whether the capture CTA may be enabled. Only ever true when the
     * authoritative profile says `user.consentState == ConsentState.ACTIVE`
     * (frontend-api-map.md trap #1: "The capture CTA must be disabled until
     * profile.user.consent_state == 'active'."). Declining consent is a real,
     * named state ([ConsentDeclined]) rather than an error — the profile stays
     * usable for every non-photo feature, only capture is locked. */
    abstract val canCapture: Boolean

    /** Whether Premium-gated UI may be shown. Delegates to
     * [com.glowup.ai.domain.model.Entitlement.isPremium] exclusively — this
     * class never re-derives the plan/status rule itself (that duplication is
     * exactly how the web client's premium check went wrong, ANDROID_PLAN.md
     * §3 bug #2 / trap #6/#11/#12). False in every pre-authoritative and
     * error state: an unauthoritative session must never show Premium UI. */
    abstract val canUsePremium: Boolean

    /** The single next thing the UI should invite the user to do from this
     * state. Exists so feature screens never re-derive gate logic locally. */
    abstract val nextAction: NextAction

    /** No stored/authenticated identity yet, or the one we had turned out to
     * be invalid. Entry point of the whole workflow ("welcome" in
     * frontend-api-map.md's "Sequential product workflow"). */
    object NoUser : SessionState() {
        override val isAuthoritative = false
        override val canCapture = false
        override val canUsePremium = false
        override val nextAction = NextAction.SignIn
    }

    /** Firebase sign-in is in flight. Transient; not authoritative. */
    object Authenticating : SessionState() {
        override val isAuthoritative = false
        override val canCapture = false
        override val canUsePremium = false
        override val nextAction = NextAction.Wait
    }

    /** Firebase auth succeeded (or a stored user id is being re-validated);
     * waiting on the authoritative `GET /profile`-shaped response. Transient;
     * not authoritative — the shell must keep gating here. */
    object ProfileLoading : SessionState() {
        override val isAuthoritative = false
        override val canCapture = false
        override val canUsePremium = false
        override val nextAction = NextAction.Wait
    }

    /** `user.consentState == PENDING`, or an unrecognised value
     * ([com.glowup.ai.domain.model.ConsentState.UNKNOWN]) — fail closed into
     * requiring consent rather than ever opening capture on a value the
     * client doesn't recognise. */
    data class ConsentRequired(
        val profile: Profile,
    ) : SessionState() {
        override val isAuthoritative = true
        override val canCapture = false
        override val canUsePremium = profile.entitlement.isPremium
        override val nextAction = NextAction.RequestConsent
    }

    /** `user.consentState == DECLINED`. A real, first-class state per
     * frontend-api-map.md "Onboarding, profile, and consent" /
     * `POST /consent`: "On decline, keep the profile usable for non-photo
     * features but keep capture visibly locked. Never silently grant
     * consent." Only escape is re-opening the consent screen (Account/privacy),
     * exactly as trap #1 specifies. */
    data class ConsentDeclined(
        val profile: Profile,
    ) : SessionState() {
        override val isAuthoritative = true
        override val canCapture = false
        override val canUsePremium = profile.entitlement.isPremium
        override val nextAction = NextAction.ReviewConsent
    }

    /** Consent is active but no vertical has a `baseline_capture_id` yet.
     * Capture is unlocked (needed to *take* the baseline). */
    data class BaselineNeeded(
        val profile: Profile,
    ) : SessionState() {
        override val isAuthoritative = true
        override val canCapture = true
        override val canUsePremium = profile.entitlement.isPremium
        override val nextAction = NextAction.CaptureBaseline
    }

    /** Consent is active and a baseline exists. The full workspace shell may
     * render. */
    data class Ready(
        val profile: Profile,
    ) : SessionState() {
        override val isAuthoritative = true
        override val canCapture = true
        override val canUsePremium = profile.entitlement.isPremium
        override val nextAction = NextAction.None
    }

    /** A `GET /profile`-shaped call failed with something that is not the
     * documented "stale user id" case (`ApiError.NotFound`) and not an
     * unauthenticated-token case (`ApiError.Unauthorized`, both of which
     * route back to [NoUser] instead — see [SessionStateMachine]). Carries
     * the [ApiError] so the UI can render a real error/retry state instead of
     * a dead end. Not authoritative: the shell must keep gating here. */
    data class Unrecoverable(
        val reason: ApiError,
    ) : SessionState() {
        override val isAuthoritative = false
        override val canCapture = false
        override val canUsePremium = false
        override val nextAction = NextAction.Retry
    }
}

/** The one next step the UI should surface for a given [SessionState], so gate
 * logic is written once here and never duplicated per-screen. */
enum class NextAction {
    SignIn,
    Wait,
    RequestConsent,
    ReviewConsent,
    CaptureBaseline,
    Retry,

    /** Authoritative and ready: no gate action needed, render the workspace. */
    None,
}
