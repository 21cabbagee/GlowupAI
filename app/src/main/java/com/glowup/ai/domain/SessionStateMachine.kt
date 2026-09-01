package com.glowup.ai.domain

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.domain.model.Profile

/**
 * Pure, side-effect-free session gate.
 *
 * No Android dependency, no Compose, no network/DataStore access: every
 * function here is `(SessionState, event) -> SessionState`. Callers
 * (`feature/shell/SessionGate.kt`, repositories) perform the actual I/O —
 * Firebase sign-in, `GET /profile` / `POST /api/auth/session` / `POST
 * /consent`, and clearing DataStore keys — and feed the results in here. That
 * split is what makes the state trustworthy: it can only ever be what the
 * backend's authoritative profile response says it is
 * (`frontend-api-map.md`, "Sequential product workflow" + trap #1), never
 * what a button press or a locally-cached flag optimistically claims.
 *
 * State diagram (see [SessionState] for the authoritative/gate meaning of
 * each node):
 *
 * ```text
 *                 onSignInRequested
 *   NoUser ───────────────────────────► Authenticating
 *     ▲                                      │
 *     │ auth failed                          │ auth succeeded
 *     └──────────────────────────────────────┤
 *                                             ▼
 *                                      ProfileLoading
 *                                             │
 *                              onProfileResult(GET /profile-shaped call)
 *                     ┌───────────────────────┼───────────────────────┐
 *                     │                       │                       │
 *         Success, consentState        Success, consentState   Failure: NotFound
 *         PENDING or UNKNOWN           DECLINED                (stale/deleted user id)
 *                     ▼                       ▼                       │
 *            ConsentRequired          ConsentDeclined                 │
 *                     │                       │                       │
 *      (either can re-enter profile load  ────┘                       │
 *       after POST /consent, which returns                            │
 *       the same profile shape — see below)                           │
 *                     │                                                │
 *         Success, consentState ACTIVE                                │
 *                     ▼                                                │
 *          hasBaseline(profile)?                                       │
 *          ┌────────────────┬───────────────┐                          │
 *          │ no             │ yes            │                          │
 *          ▼                ▼                ▼                          ▼
 *   BaselineNeeded        Ready      Failure: Unauthorized ──────► NoUser
 *          │                │        (401 survives AuthInterceptor's
 *          │ (first accepted│         refresh attempt -> force re-auth)
 *          │  capture makes │
 *          └──► onProfileResult again, same branch as above
 *
 *   Any state ── Failure: anything else (Network/Server/Validation/
 *                Conflict/ConsentRequired/PremiumRequired/CaptureQualityRejected/
 *                Unknown) ──► Unrecoverable(reason)
 *   Unrecoverable ── onSignInRequested / onProfileRefreshRequested ──► retry
 * ```
 *
 * `ConsentRequired`/`ConsentDeclined`/`BaselineNeeded` are not dead ends: a
 * later `onProfileResult` (fed by the repository re-fetching after
 * `POST /consent` or `POST /captures`) reclassifies from scratch every time,
 * exactly like the very first fetch — there is deliberately no "remembered"
 * transition edge between them, because remembering would be exactly the
 * optimistic-flag bug this machine exists to prevent.
 */
object SessionStateMachine {
    /** Cold-start state before anything has run. */
    fun initial(): SessionState = SessionState.NoUser

    /** UI/repository begins a Firebase sign-in (or is re-validating a stored
     * candidate user id from `SessionStore` — frontend-api-map.md "Startup
     * and session recovery": "load a locally persisted user_id only as a
     * candidate"). Only meaningful from [SessionState.NoUser] or
     * [SessionState.Unrecoverable]; otherwise a no-op so a duplicate signal
     * can never regress an already-authoritative state. */
    fun onSignInRequested(current: SessionState): SessionState =
        when (current) {
            is SessionState.NoUser, is SessionState.Unrecoverable -> SessionState.Authenticating
            else -> current
        }

    /** Firebase auth failed (bad credentials, cancelled, etc). Back to
     * [SessionState.NoUser] so the user can retry sign-in; only meaningful
     * while [SessionState.Authenticating]. */
    fun onAuthenticationFailed(current: SessionState): SessionState =
        when (current) {
            SessionState.Authenticating -> SessionState.NoUser
            else -> current
        }

    /** Firebase auth succeeded. Moves to [SessionState.ProfileLoading] —
     * still not authoritative until `POST /api/auth/session` (or
     * `GET /profile`) actually returns. */
    fun onAuthenticationSucceeded(current: SessionState): SessionState =
        when (current) {
            SessionState.Authenticating -> SessionState.ProfileLoading
            else -> current
        }

    /** A repository is about to (re-)fetch the authoritative profile — e.g.
     * after `POST /consent`, `POST /captures`, a subscription mutation, or a
     * pull-to-refresh. No-op from [SessionState.NoUser]/[SessionState.Authenticating]
     * (nothing to refresh yet); otherwise moves to [SessionState.ProfileLoading]
     * so the UI can show a transient loading affordance without losing the
     * fact that a request is in flight. */
    fun onProfileRefreshRequested(current: SessionState): SessionState =
        when (current) {
            SessionState.NoUser, SessionState.Authenticating -> current
            else -> SessionState.ProfileLoading
        }

    /**
     * The one authoritative transition. Always call this with the result of a
     * `GET /profile`, `POST /api/auth/session`, `POST /api/users`, or
     * `POST /consent` call — they all return the same profile shape
     * (`frontend-api-map.md` "Onboarding, profile, and consent") — and this
     * function alone decides the next [SessionState] from it. It ignores the
     * previous state on success: the new profile is authoritative regardless
     * of what we thought before (trap #1/#2 — never assume a mutation result
     * proves consent, baseline, or Premium; always re-derive).
     */
    fun onProfileResult(result: GlowResult<Profile>): SessionState =
        when (result) {
            is GlowResult.Success -> {
                classify(result.data)
            }

            is GlowResult.Failure -> {
                when (val error = result.error) {
                    // frontend-api-map.md "Startup and session recovery": "Call
                    // GET /api/users/{user_id}/profile; if it returns 400 user not
                    // found, clear only the GlowUpAI session key and restart at
                    // welcome." Missing entities are 400, not 404, in this API
                    // (frontend-api-map.md line ~38-39 / ANDROID_PLAN.md trap #5),
                    // and the only documented error for this call is exactly this
                    // "stale/deleted user id" case, so any ApiError.NotFound here
                    // means restart at NoUser. The actual key-clearing (never a
                    // blanket storage wipe) is the caller's job, not this pure
                    // function's — see SessionGate.kt.
                    is ApiError.NotFound -> SessionState.NoUser

                    // A bearer token that is still rejected after AuthInterceptor's
                    // 401 refresh attempt means the Firebase session itself is dead;
                    // force re-authentication rather than stranding the user on an
                    // error screen with a "retry" button that can never succeed.
                    is ApiError.Unauthorized -> SessionState.NoUser

                    else -> SessionState.Unrecoverable(error)
                }
            }
        }

    private fun classify(profile: Profile): SessionState =
        when (profile.user.consentState) {
            ConsentState.DECLINED -> {
                SessionState.ConsentDeclined(profile)
            }

            ConsentState.ACTIVE -> {
                if (hasBaseline(profile)) SessionState.Ready(profile) else SessionState.BaselineNeeded(profile)
            }

            // PENDING is the documented default for a freshly created profile.
            // UNKNOWN is any value this client doesn't recognise. Both fail
            // closed into requiring consent: the capture gate must never open on
            // an unrecognised consent_state (frontend-api-map.md trap #1: "The
            // capture CTA must be disabled until profile.user.consent_state ==
            // 'active'" — read literally, anything else than exactly ACTIVE keeps
            // it disabled).
            ConsentState.PENDING, ConsentState.UNKNOWN -> {
                SessionState.ConsentRequired(profile)
            }
        }

    /**
     * frontend-api-map.md `POST /api/captures` prerequisites: "The first
     * accepted capture for the user becomes a baseline even if `is_baseline`
     * is false" (also ANDROID_PLAN.md trap #10: "...and the rule counts
     * captures across verticals"). That means baseline-existence is a
     * per-*user* fact, not a per-vertical one: the first capture accepted in
     * *any* vertical retroactively satisfies "has a baseline" for the whole
     * account, and the backend records that by stamping a
     * `baseline_capture_id` onto that vertical's appearance profile. So we
     * must not require every vertical (or specifically the "current")
     * vertical to carry a `baselineCaptureId` — we only need to see that it
     * has happened at all, anywhere in [Profile.appearanceProfiles].
     */
    private fun hasBaseline(profile: Profile): Boolean = profile.appearanceProfiles.any { it.baselineCaptureId != null }
}
