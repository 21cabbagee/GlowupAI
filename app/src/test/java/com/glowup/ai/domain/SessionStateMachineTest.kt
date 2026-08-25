package com.glowup.ai.domain

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.domain.model.AppearanceProfile
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.domain.model.Entitlement
import com.glowup.ai.domain.model.EntitlementStatus
import com.glowup.ai.domain.model.Plan
import com.glowup.ai.domain.model.Profile
import com.glowup.ai.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for [SessionStateMachine] — no Android runtime, no
 * coroutines, no mocking framework. Every case is a direct assertion that
 * `(state, event) -> state` matches the sequential workflow documented in
 * `backend/docs/frontend-api-map.md` ("Sequential product workflow",
 * "Startup and session recovery", "Onboarding, profile, and consent") and
 * ANDROID_PLAN.md's non-negotiable constraints / traps #1, #2, #3, #5.
 */
class SessionStateMachineTest {

    // ---- test fixtures ------------------------------------------------

    private fun user(consentState: ConsentState) = User(
        id = "user-1",
        skinType = "combination",
        consentState = consentState,
        createdAt = "2026-01-01T00:00:00Z",
        firebaseUid = "firebase-uid-1",
    )

    private fun entitlement(plan: Plan, status: EntitlementStatus) = Entitlement(
        plan = plan,
        status = status,
        startedAt = null,
        renewsAt = null,
        source = null,
    )

    private fun appearanceProfile(vertical: String, baselineCaptureId: String?) = AppearanceProfile(
        id = "ap-$vertical",
        vertical = vertical,
        baselineCaptureId = baselineCaptureId,
    )

    private fun profile(
        consentState: ConsentState = ConsentState.ACTIVE,
        plan: Plan = Plan.FREE,
        status: EntitlementStatus = EntitlementStatus.ACTIVE,
        appearanceProfiles: List<AppearanceProfile> = listOf(appearanceProfile("skin", null)),
        verticals: List<String> = listOf("skin"),
    ) = Profile(
        user = user(consentState),
        appearanceProfiles = appearanceProfiles,
        entitlement = entitlement(plan, status),
        verticals = verticals,
        experienceProfile = null,
    )

    // ---- initial state --------------------------------------------------

    @Test
    fun `initial state is NoUser`() {
        assertEquals(SessionState.NoUser, SessionStateMachine.initial())
    }

    // ---- pre-profile transitions -----------------------------------------

    @Test
    fun `sign-in requested from NoUser moves to Authenticating`() {
        assertEquals(
            SessionState.Authenticating,
            SessionStateMachine.onSignInRequested(SessionState.NoUser),
        )
    }

    @Test
    fun `sign-in requested from Unrecoverable moves to Authenticating (allows retry)`() {
        val unrecoverable = SessionState.Unrecoverable(ApiError.Network(RuntimeException("offline")))
        assertEquals(SessionState.Authenticating, SessionStateMachine.onSignInRequested(unrecoverable))
    }

    @Test
    fun `sign-in requested is a no-op from every other state`() {
        val ready = SessionState.Ready(profile())
        val states = listOf(
            SessionState.Authenticating,
            SessionState.ProfileLoading,
            SessionState.ConsentRequired(profile(consentState = ConsentState.PENDING)),
            SessionState.ConsentDeclined(profile(consentState = ConsentState.DECLINED)),
            SessionState.BaselineNeeded(profile()),
            ready,
        )
        states.forEach { state ->
            assertEquals("no-op expected from $state", state, SessionStateMachine.onSignInRequested(state))
        }
    }

    @Test
    fun `authentication failed returns to NoUser only from Authenticating`() {
        assertEquals(SessionState.NoUser, SessionStateMachine.onAuthenticationFailed(SessionState.Authenticating))
        assertEquals(SessionState.NoUser, SessionStateMachine.onAuthenticationFailed(SessionState.NoUser))
        val ready = SessionState.Ready(profile())
        assertEquals(ready, SessionStateMachine.onAuthenticationFailed(ready))
    }

    @Test
    fun `authentication succeeded moves Authenticating to ProfileLoading`() {
        assertEquals(
            SessionState.ProfileLoading,
            SessionStateMachine.onAuthenticationSucceeded(SessionState.Authenticating),
        )
    }

    @Test
    fun `authentication succeeded is a no-op outside Authenticating`() {
        assertEquals(SessionState.NoUser, SessionStateMachine.onAuthenticationSucceeded(SessionState.NoUser))
        val ready = SessionState.Ready(profile())
        assertEquals(ready, SessionStateMachine.onAuthenticationSucceeded(ready))
    }

    @Test
    fun `profile refresh requested is a no-op before an identity exists`() {
        assertEquals(SessionState.NoUser, SessionStateMachine.onProfileRefreshRequested(SessionState.NoUser))
        assertEquals(
            SessionState.Authenticating,
            SessionStateMachine.onProfileRefreshRequested(SessionState.Authenticating),
        )
    }

    @Test
    fun `profile refresh requested moves any authoritative or loading state to ProfileLoading`() {
        val ready = SessionState.Ready(profile())
        val declined = SessionState.ConsentDeclined(profile(consentState = ConsentState.DECLINED))
        val unrecoverable = SessionState.Unrecoverable(ApiError.Server(500, "boom"))
        listOf(SessionState.ProfileLoading, ready, declined, unrecoverable).forEach { state ->
            assertEquals(SessionState.ProfileLoading, SessionStateMachine.onProfileRefreshRequested(state))
        }
    }

    // ---- classification from a successful profile fetch -------------------

    @Test
    fun `pending consent classifies as ConsentRequired`() {
        val p = profile(consentState = ConsentState.PENDING)
        assertEquals(SessionState.ConsentRequired(p), SessionStateMachine.onProfileResult(GlowResult.Success(p)))
    }

    @Test
    fun `unrecognised consent value fails closed into ConsentRequired, not Ready`() {
        // Simulates a future backend value this client build doesn't know about yet.
        val p = profile(consentState = ConsentState.UNKNOWN)
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p))
        assertTrue(result is SessionState.ConsentRequired)
        assertFalse(result.canCapture)
    }

    @Test
    fun `declined consent classifies as ConsentDeclined and keeps capture locked`() {
        val p = profile(consentState = ConsentState.DECLINED)
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p))
        assertEquals(SessionState.ConsentDeclined(p), result)
        assertFalse((result as SessionState.ConsentDeclined).canCapture)
    }

    @Test
    fun `declined consent still allows premium UI if entitled (consent and billing are independent gates)`() {
        val p = profile(
            consentState = ConsentState.DECLINED,
            plan = Plan.PREMIUM,
            status = EntitlementStatus.ACTIVE,
        )
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p)) as SessionState.ConsentDeclined
        assertTrue(result.canUsePremium)
        assertFalse(result.canCapture)
    }

    @Test
    fun `active consent with no baseline anywhere classifies as BaselineNeeded`() {
        val p = profile(
            consentState = ConsentState.ACTIVE,
            appearanceProfiles = listOf(appearanceProfile("skin", null), appearanceProfile("hair", null)),
        )
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p))
        assertEquals(SessionState.BaselineNeeded(p), result)
        assertTrue((result as SessionState.BaselineNeeded).canCapture)
    }

    @Test
    fun `active consent with an empty appearance-profile list classifies as BaselineNeeded`() {
        val p = profile(consentState = ConsentState.ACTIVE, appearanceProfiles = emptyList())
        assertEquals(
            SessionState.BaselineNeeded(p),
            SessionStateMachine.onProfileResult(GlowResult.Success(p)),
        )
    }

    @Test
    fun `active consent with a baseline in only ONE vertical classifies as Ready (baseline counts across verticals)`() {
        // frontend-api-map.md: "the first accepted capture for the user becomes a
        // baseline even if is_baseline is false" + ANDROID_PLAN trap #10: "the rule
        // counts captures across verticals" — so a baseline recorded for "hair"
        // must satisfy the gate even though "skin" itself has none.
        val p = profile(
            consentState = ConsentState.ACTIVE,
            appearanceProfiles = listOf(
                appearanceProfile("skin", null),
                appearanceProfile("hair", "capture-42"),
            ),
        )
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p))
        assertEquals(SessionState.Ready(p), result)
        assertTrue((result as SessionState.Ready).canCapture)
    }

    @Test
    fun `active consent with a baseline in the only vertical classifies as Ready`() {
        val p = profile(
            consentState = ConsentState.ACTIVE,
            appearanceProfiles = listOf(appearanceProfile("skin", "capture-1")),
        )
        assertEquals(SessionState.Ready(p), SessionStateMachine.onProfileResult(GlowResult.Success(p)))
    }

    @Test
    fun `re-fetching after consent changes reclassifies from scratch, not from the previous state`() {
        val declined = profile(consentState = ConsentState.DECLINED)
        val afterDecline = SessionStateMachine.onProfileResult(GlowResult.Success(declined))
        assertTrue(afterDecline is SessionState.ConsentDeclined)

        val acceptedWithBaseline = profile(
            consentState = ConsentState.ACTIVE,
            appearanceProfiles = listOf(appearanceProfile("skin", "capture-1")),
        )
        val afterAccept = SessionStateMachine.onProfileResult(GlowResult.Success(acceptedWithBaseline))
        assertEquals(SessionState.Ready(acceptedWithBaseline), afterAccept)
    }

    // ---- stale-user-id / auth recovery -------------------------------------

    @Test
    fun `400 user not found (ApiError NotFound) restarts at NoUser regardless of prior state`() {
        val priorStates = listOf(
            SessionState.ProfileLoading,
            SessionState.Ready(profile()),
            SessionState.ConsentDeclined(profile(consentState = ConsentState.DECLINED)),
        )
        val failure = GlowResult.Failure(ApiError.NotFound("user"))
        priorStates.forEach { prior ->
            // onProfileResult never consults `prior` — the assertion documents
            // that fact explicitly rather than assuming it.
            assertEquals(SessionState.NoUser, SessionStateMachine.onProfileResult(failure))
        }
    }

    @Test
    fun `unauthorized after token refresh forces re-authentication at NoUser`() {
        assertEquals(
            SessionState.NoUser,
            SessionStateMachine.onProfileResult(GlowResult.Failure(ApiError.Unauthorized)),
        )
    }

    // ---- everything else is Unrecoverable, with the error preserved -------

    @Test
    fun `network failure becomes Unrecoverable carrying the original error`() {
        val error = ApiError.Network(RuntimeException("offline"))
        val result = SessionStateMachine.onProfileResult(GlowResult.Failure(error))
        assertEquals(SessionState.Unrecoverable(error), result)
        assertFalse(result.isAuthoritative)
        assertFalse(result.canCapture)
        assertFalse(result.canUsePremium)
    }

    @Test
    fun `server failure becomes Unrecoverable`() {
        val error = ApiError.Server(500, "internal error")
        assertEquals(SessionState.Unrecoverable(error), SessionStateMachine.onProfileResult(GlowResult.Failure(error)))
    }

    @Test
    fun `validation failure becomes Unrecoverable`() {
        val error = ApiError.Validation(mapOf("skin_type" to "must be a string"))
        assertEquals(SessionState.Unrecoverable(error), SessionStateMachine.onProfileResult(GlowResult.Failure(error)))
    }

    @Test
    fun `conflict failure becomes Unrecoverable`() {
        val error = ApiError.Conflict("duplicate")
        assertEquals(SessionState.Unrecoverable(error), SessionStateMachine.onProfileResult(GlowResult.Failure(error)))
    }

    @Test
    fun `unknown failure becomes Unrecoverable`() {
        val error = ApiError.Unknown(IllegalStateException("unexpected body"))
        assertEquals(SessionState.Unrecoverable(error), SessionStateMachine.onProfileResult(GlowResult.Failure(error)))
    }

    @Test
    fun `an ApiError ConsentRequired on a profile fetch is a transport error, not the session ConsentRequired state`() {
        // ApiError.ConsentRequired means a 403 was returned from some other call
        // that got funneled through this same reducer; it must not be confused
        // with SessionState.ConsentRequired, which is derived only from a
        // *successful* fetch whose consentState field is PENDING/UNKNOWN.
        val result = SessionStateMachine.onProfileResult(GlowResult.Failure(ApiError.ConsentRequired))
        assertTrue(result is SessionState.Unrecoverable)
        assertEquals(ApiError.ConsentRequired, (result as SessionState.Unrecoverable).reason)
    }

    @Test
    fun `an ApiError PremiumRequired on a profile fetch becomes Unrecoverable, not a silent premium grant`() {
        val error = ApiError.PremiumRequired("ingredient explainer")
        val result = SessionStateMachine.onProfileResult(GlowResult.Failure(error))
        assertEquals(SessionState.Unrecoverable(error), result)
        assertFalse(result.canUsePremium)
    }

    // ---- premium boundary (delegated to Entitlement.isPremium, never re-derived) --

    @Test
    fun `premium plus active status enables canUsePremium`() {
        val p = profile(
            appearanceProfiles = listOf(appearanceProfile("skin", "capture-1")),
            plan = Plan.PREMIUM,
            status = EntitlementStatus.ACTIVE,
        )
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p)) as SessionState.Ready
        assertTrue(result.canUsePremium)
    }

    @Test
    fun `premium plan with cancelled status is NOT premium`() {
        // The exact web-client bug (ANDROID_PLAN.md §3 bug #2 / trap #6): checking
        // plan alone would wrongly show Premium UI here.
        val p = profile(
            appearanceProfiles = listOf(appearanceProfile("skin", "capture-1")),
            plan = Plan.PREMIUM,
            status = EntitlementStatus.CANCELLED,
        )
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p)) as SessionState.Ready
        assertFalse(result.canUsePremium)
    }

    @Test
    fun `free plan with active status is NOT premium`() {
        val p = profile(
            appearanceProfiles = listOf(appearanceProfile("skin", "capture-1")),
            plan = Plan.FREE,
            status = EntitlementStatus.ACTIVE,
        )
        val result = SessionStateMachine.onProfileResult(GlowResult.Success(p)) as SessionState.Ready
        assertFalse(result.canUsePremium)
    }

    @Test
    fun `unknown plan or status is NOT premium (fails closed)`() {
        val unknownPlan = profile(
            appearanceProfiles = listOf(appearanceProfile("skin", "capture-1")),
            plan = Plan.UNKNOWN,
            status = EntitlementStatus.ACTIVE,
        )
        val unknownStatus = profile(
            appearanceProfiles = listOf(appearanceProfile("skin", "capture-1")),
            plan = Plan.PREMIUM,
            status = EntitlementStatus.UNKNOWN,
        )
        listOf(unknownPlan, unknownStatus).forEach { p ->
            val result = SessionStateMachine.onProfileResult(GlowResult.Success(p)) as SessionState.Ready
            assertFalse(result.canUsePremium)
        }
    }

    // ---- gate surface: canCapture / isAuthoritative / nextAction per state --------

    @Test
    fun `only BaselineNeeded and Ready allow capture`() {
        val gatedFalse = listOf(
            SessionState.NoUser,
            SessionState.Authenticating,
            SessionState.ProfileLoading,
            SessionState.ConsentRequired(profile(consentState = ConsentState.PENDING)),
            SessionState.ConsentDeclined(profile(consentState = ConsentState.DECLINED)),
            SessionState.Unrecoverable(ApiError.Server(500, "x")),
        )
        gatedFalse.forEach { assertFalse("$it should not allow capture", it.canCapture) }

        assertTrue(SessionState.BaselineNeeded(profile()).canCapture)
        assertTrue(
            SessionState.Ready(profile(appearanceProfiles = listOf(appearanceProfile("skin", "c1")))).canCapture,
        )
    }

    @Test
    fun `only ConsentRequired, ConsentDeclined, BaselineNeeded and Ready are authoritative`() {
        val notAuthoritative = listOf(
            SessionState.NoUser,
            SessionState.Authenticating,
            SessionState.ProfileLoading,
            SessionState.Unrecoverable(ApiError.Network(RuntimeException())),
        )
        notAuthoritative.forEach { assertFalse("$it should not be authoritative", it.isAuthoritative) }

        val authoritative = listOf(
            SessionState.ConsentRequired(profile(consentState = ConsentState.PENDING)),
            SessionState.ConsentDeclined(profile(consentState = ConsentState.DECLINED)),
            SessionState.BaselineNeeded(profile()),
            SessionState.Ready(profile(appearanceProfiles = listOf(appearanceProfile("skin", "c1")))),
        )
        authoritative.forEach { assertTrue("$it should be authoritative", it.isAuthoritative) }
    }

    @Test
    fun `nextAction names the one thing the UI should invite next`() {
        assertEquals(NextAction.SignIn, SessionState.NoUser.nextAction)
        assertEquals(NextAction.Wait, SessionState.Authenticating.nextAction)
        assertEquals(NextAction.Wait, SessionState.ProfileLoading.nextAction)
        assertEquals(
            NextAction.RequestConsent,
            SessionState.ConsentRequired(profile(consentState = ConsentState.PENDING)).nextAction,
        )
        assertEquals(
            NextAction.ReviewConsent,
            SessionState.ConsentDeclined(profile(consentState = ConsentState.DECLINED)).nextAction,
        )
        assertEquals(NextAction.CaptureBaseline, SessionState.BaselineNeeded(profile()).nextAction)
        assertEquals(
            NextAction.None,
            SessionState.Ready(profile(appearanceProfiles = listOf(appearanceProfile("skin", "c1")))).nextAction,
        )
        assertEquals(NextAction.Retry, SessionState.Unrecoverable(ApiError.Server(500, "x")).nextAction)
    }
}
