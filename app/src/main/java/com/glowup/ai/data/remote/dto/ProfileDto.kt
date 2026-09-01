package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.AppearanceProfile
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.domain.model.Entitlement
import com.glowup.ai.domain.model.EntitlementStatus
import com.glowup.ai.domain.model.ExperienceProfile
import com.glowup.ai.domain.model.Plan
import com.glowup.ai.domain.model.Profile
import com.glowup.ai.domain.model.ProfileUpdateRequest
import com.glowup.ai.domain.model.Subscription
import com.glowup.ai.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequestDto(
    @SerialName("skin_type") val skinType: String? = null,
)

@Serializable
data class ConsentRequestDto(
    @SerialName("facial_data") val facialData: Boolean,
    @SerialName("policy_version") val policyVersion: String? = null,
)

@Serializable
data class UserDto(
    val id: String = "",
    @SerialName("skin_type") val skinType: String? = null,
    @SerialName("consent_state") val consentState: String = "pending",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("firebase_uid") val firebaseUid: String? = null,
)

@Serializable
data class AppearanceProfileDto(
    val id: String = "",
    val vertical: String = "skin",
    @SerialName("baseline_capture_id") val baselineCaptureId: String? = null,
)

@Serializable
data class EntitlementDto(
    val plan: String = "free",
    val status: String = "active",
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("renews_at") val renewsAt: String? = null,
    val source: String? = null,
)

@Serializable
data class ExperienceProfileDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("skin_type") val skinType: String? = null,
    @SerialName("focus_vertical") val focusVertical: String? = null,
    val goals: List<String> = emptyList(),
    @SerialName("experience_level") val experienceLevel: String? = null,
    @SerialName("onboarding_complete") @Serializable(with = IntBooleanSerializer::class) val onboardingComplete: Boolean = false,
    @SerialName("onboarding_completed_at") val onboardingCompletedAt: String? = null,
)

@Serializable
data class ExperienceProfileUpdateRequestDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("skin_type") val skinType: String? = null,
    @SerialName("focus_vertical") val focusVertical: String? = null,
    val goals: List<String>? = null,
    @SerialName("experience_level") val experienceLevel: String? = null,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean? = null,
)

/** Shape returned by `POST /api/users`, `GET /profile`, `POST /consent`,
 * and `POST /api/auth/session` — all four are the same contract. */
@Serializable
data class ProfileResponseDto(
    val user: UserDto,
    @SerialName("appearance_profiles") val appearanceProfiles: List<AppearanceProfileDto> = emptyList(),
    val entitlement: EntitlementDto,
    val verticals: List<String> = emptyList(),
    @SerialName("experience_profile") val experienceProfile: ExperienceProfileDto? = null,
)

@Serializable
data class SubscriptionDto(
    @SerialName("user_id") val userId: String,
    val plan: String = "free",
    val status: String = "active",
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("renews_at") val renewsAt: String? = null,
    val source: String? = null,
)

@Serializable
data class UpgradeRequestDto(
    val source: String = "local_checkout",
)

fun EntitlementDto.toDomain(): Entitlement = Entitlement(
    plan = Plan.fromRaw(plan),
    status = EntitlementStatus.fromRaw(status),
    startedAt = startedAt,
    renewsAt = renewsAt,
    source = source,
)

fun UserDto.toDomain(): User = User(
    id = id,
    skinType = skinType,
    consentState = ConsentState.fromRaw(consentState),
    createdAt = createdAt,
    firebaseUid = firebaseUid,
)

fun ExperienceProfileDto.toDomain(): ExperienceProfile = ExperienceProfile(
    displayName = displayName,
    skinType = skinType,
    focusVertical = focusVertical,
    goals = goals,
    experienceLevel = experienceLevel,
    onboardingComplete = onboardingComplete || onboardingCompletedAt != null,
)

fun ProfileResponseDto.toDomain(): Profile = Profile(
    user = user.toDomain(),
    appearanceProfiles = appearanceProfiles.map {
        AppearanceProfile(id = it.id, vertical = it.vertical, baselineCaptureId = it.baselineCaptureId)
    },
    entitlement = entitlement.toDomain(),
    verticals = verticals,
    experienceProfile = experienceProfile?.toDomain(),
)

fun SubscriptionDto.toDomain(): Subscription = Subscription(
    userId = userId,
    plan = Plan.fromRaw(plan),
    status = EntitlementStatus.fromRaw(status),
    startedAt = startedAt,
    renewsAt = renewsAt,
    source = source,
)

fun ProfileUpdateRequest.toDto(): ExperienceProfileUpdateRequestDto = ExperienceProfileUpdateRequestDto(
    displayName = displayName,
    skinType = skinType,
    focusVertical = focusVertical,
    goals = goals,
    experienceLevel = experienceLevel,
    onboardingComplete = onboardingComplete,
)
