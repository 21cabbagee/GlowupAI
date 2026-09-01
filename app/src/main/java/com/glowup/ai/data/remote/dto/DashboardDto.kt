package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.CheckInRoutineState
import com.glowup.ai.domain.model.CheckInSkinFeel
import com.glowup.ai.domain.model.Dashboard
import com.glowup.ai.domain.model.DashboardAnalytics
import com.glowup.ai.domain.model.DashboardFeatures
import com.glowup.ai.domain.model.DashboardRoutineEvent
import com.glowup.ai.domain.model.RoutineAction
import com.glowup.ai.domain.model.Verdict
import com.glowup.ai.domain.model.VerdictEvidence
import com.glowup.ai.domain.model.VerdictLabel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class VerdictEvidenceDto(
    @SerialName("n_after") val nAfter: Int? = null,
    val confidence: Double? = null,
)

/** `label == "locked"` is the one-free-lifetime-unlock upsell shape, not a
 * normal verdict — render it as a distinct card, never mixed into the list. */
@Serializable
data class VerdictDto(
    val label: String = "evidence_unclear",
    @SerialName("generated_text") val generatedText: String = "",
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val evidence: VerdictEvidenceDto? = null,
)

fun VerdictDto.toDomain(): Verdict =
    Verdict(
        label = VerdictLabel.fromRaw(label),
        generatedText = generatedText,
        productId = productId,
        productName = productName,
        evidence = evidence?.let { VerdictEvidence(it.nAfter, it.confidence) },
    )

@Serializable
data class DashboardRoutineEventDto(
    val action: String = "start",
    @SerialName("product_name") val productName: String? = null,
    val timestamp: String? = null,
    val slot: String? = null,
    val notes: String? = null,
)

fun DashboardRoutineEventDto.toDomain(): DashboardRoutineEvent =
    DashboardRoutineEvent(
        action = RoutineAction.fromRaw(action),
        productName = productName,
        timestamp = timestamp,
        slot = slot,
        notes = notes,
    )

@Serializable
data class DashboardAnalyticsDto(
    val activation: Boolean? = null,
    @SerialName("baseline_capture") val baselineCapture: Boolean? = null,
    @SerialName("first_three_captures") val firstThreeCaptures: Boolean? = null,
    @SerialName("median_history_days") val medianHistoryDays: Double? = null,
    @SerialName("weekly_verdict_open_rate") val weeklyVerdictOpenRate: Double? = null,
    @SerialName("verdict_action_rate") val verdictActionRate: Double? = null,
    @SerialName("evidence_unclear_engagement_rate") val evidenceUnclearEngagementRate: Double? = null,
    @SerialName("raw_events") val rawEvents: JsonElement? = null,
)

@Serializable
data class DashboardFeaturesDto(
    @SerialName("product_verdicts_unlocked") @Serializable(with = IntBooleanSerializer::class) val productVerdictsUnlocked: Boolean = false,
    @Serializable(with = IntBooleanSerializer::class) val experiments: Boolean = false,
    @SerialName("ingredient_analysis") @Serializable(with = IntBooleanSerializer::class) val ingredientAnalysis: Boolean = false,
    @SerialName("long_history") @Serializable(with = IntBooleanSerializer::class) val longHistory: Boolean = false,
    @Serializable(with = IntBooleanSerializer::class) val qna: Boolean = false,
    @Serializable(with = IntBooleanSerializer::class) val discover: Boolean = false,
    @SerialName("root_cause") @Serializable(with = IntBooleanSerializer::class) val rootCause: Boolean = false,
    @SerialName("budget_optimizer") @Serializable(with = IntBooleanSerializer::class) val budgetOptimizer: Boolean = false,
    @SerialName("derm_export") @Serializable(with = IntBooleanSerializer::class) val dermExport: Boolean = false,
    @SerialName("product_prediction") @Serializable(with = IntBooleanSerializer::class) val productPrediction: Boolean = false,
)

fun DashboardFeaturesDto.toDomain(): DashboardFeatures =
    DashboardFeatures(
        productVerdictsUnlocked = productVerdictsUnlocked,
        raw =
            mapOf(
                "experiments" to experiments,
                "ingredient_analysis" to ingredientAnalysis,
                "long_history" to longHistory,
                "qna" to qna,
                "discover" to discover,
                "root_cause" to rootCause,
                "budget_optimizer" to budgetOptimizer,
                "derm_export" to dermExport,
                "product_prediction" to productPrediction,
            ),
    )

@Serializable
data class DashboardDto(
    val profile: ProfileResponseDto,
    val vertical: String = "skin",
    val history: List<HistoryItemDto> = emptyList(),
    val verdicts: List<VerdictDto> = emptyList(),
    val experiments: List<ExperimentDto> = emptyList(),
    val engagement: EngagementDto? = null,
    val analytics: DashboardAnalyticsDto? = null,
    @SerialName("weekly_recap") val weeklyRecap: WeeklyRecapDto? = null,
    @SerialName("check_ins") val checkIns: List<CheckInDto> = emptyList(),
    @SerialName("routine_events") val routineEvents: List<DashboardRoutineEventDto> = emptyList(),
    val features: DashboardFeaturesDto = DashboardFeaturesDto(),
    val disclaimer: String = "Cosmetic tracking only; SkinProof does not diagnose, treat, or rule out medical conditions.",
)

fun DashboardDto.toDomain(): Dashboard =
    Dashboard(
        profile = profile.toDomain(),
        vertical = vertical,
        history = history.map { it.toDomain() },
        verdicts = verdicts.map { it.toDomain() },
        experiments = experiments.map { it.toDomain() },
        engagement = engagement?.toDomain(),
        analytics =
            analytics?.let {
                DashboardAnalytics(
                    medianHistoryDays = it.medianHistoryDays,
                    baselineCapture = it.baselineCapture,
                    firstThreeCaptures = it.firstThreeCaptures,
                    activation = it.activation?.toString(),
                )
            },
        weeklyRecap = weeklyRecap?.toDomain(),
        checkIns = checkIns.map { it.toDomain() },
        routineEvents = routineEvents.map { it.toDomain() },
        features = features.toDomain(),
        disclaimer = disclaimer,
    )

@Serializable
data class CheckInCreateRequestDto(
    @SerialName("routine_state") val routineState: String = "steady",
    @SerialName("skin_feel") val skinFeel: String = "not_sure",
    val note: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
)

fun com.glowup.ai.domain.model.CheckInCreateRequest.toDto(): CheckInCreateRequestDto =
    CheckInCreateRequestDto(
        routineState = routineState.toWire(),
        skinFeel = skinFeel.toWire(),
        note = note,
        occurredAt = occurredAt,
    )

@Serializable
data class CheckInDto(
    val id: String = "",
    @SerialName("routine_state") val routineState: String = "steady",
    @SerialName("skin_feel") val skinFeel: String = "not_sure",
    val note: String? = null,
    @SerialName("occurred_at") val occurredAt: String = "",
)

fun CheckInDto.toDomain(): com.glowup.ai.domain.model.CheckIn =
    com.glowup.ai.domain.model.CheckIn(
        id = id,
        routineState = CheckInRoutineState.fromRaw(routineState),
        skinFeel = CheckInSkinFeel.fromRaw(skinFeel),
        note = note,
        occurredAt = occurredAt,
    )
