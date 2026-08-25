package com.glowup.ai.domain.model

data class User(
    val id: String,
    val skinType: String?,
    val consentState: ConsentState,
    val createdAt: String?,
    val firebaseUid: String?,
)

data class AppearanceProfile(
    val id: String,
    val vertical: String,
    val baselineCaptureId: String?,
)

/**
 * Premium is decided in exactly ONE place: [isPremium]. Never re-derive it
 * ad hoc from `plan` alone anywhere else in the app (that is bug #2 in the
 * web client — see ANDROID_PLAN.md section 3).
 */
data class Entitlement(
    val plan: Plan,
    val status: EntitlementStatus,
    val startedAt: String?,
    val renewsAt: String?,
    val source: String?,
) {
    val isPremium: Boolean
        get() = plan == Plan.PREMIUM && status == EntitlementStatus.ACTIVE
}

data class ExperienceProfile(
    val displayName: String?,
    val skinType: String?,
    val focusVertical: String?,
    val goals: List<String>,
    val experienceLevel: String?,
    val onboardingComplete: Boolean,
)

/**
 * The full session snapshot returned by `GET /profile`, `POST /api/auth/session`,
 * `POST /api/users`, and `POST /consent`. This is the single authoritative
 * source for [com.glowup.ai.domain.SessionStateMachine] — never infer session
 * state from a button press.
 */
data class Profile(
    val user: User,
    val appearanceProfiles: List<AppearanceProfile>,
    val entitlement: Entitlement,
    val verticals: List<String>,
    val experienceProfile: ExperienceProfile?,
)

data class ProfileUpdateRequest(
    val displayName: String? = null,
    val skinType: String? = null,
    val focusVertical: String? = null,
    val goals: List<String>? = null,
    val experienceLevel: String? = null,
    val onboardingComplete: Boolean? = null,
)

data class Subscription(
    val userId: String,
    val plan: Plan,
    val status: EntitlementStatus,
    val startedAt: String?,
    val renewsAt: String?,
    val source: String?,
) {
    val isPremium: Boolean
        get() = plan == Plan.PREMIUM && status == EntitlementStatus.ACTIVE
}
