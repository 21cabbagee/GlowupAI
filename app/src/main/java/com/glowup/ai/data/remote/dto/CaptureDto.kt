package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.AppearanceMetric
import com.glowup.ai.domain.model.AnalysisEnvelope
import com.glowup.ai.domain.model.AnalysisQuality
import com.glowup.ai.domain.model.AnalysisImageInput
import com.glowup.ai.domain.model.BaselineComparison
import com.glowup.ai.domain.model.CaptureCreateRequest
import com.glowup.ai.domain.model.CaptureGuide
import com.glowup.ai.domain.model.CaptureGuideState
import com.glowup.ai.domain.model.CaptureQuality
import com.glowup.ai.domain.model.CaptureResult
import com.glowup.ai.domain.model.CoachingTip
import com.glowup.ai.domain.model.CosmeticObservation
import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.MeasurementAgreement
import com.glowup.ai.domain.model.MeasurementFeedback
import com.glowup.ai.domain.model.MeasurementFeedbackRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Pose/face fields the client measures. `brightness`/`sharpness` are
 * deliberately absent — the server always overwrites them from the uploaded
 * image, so sending client values for them would be misleading. */
@Serializable
data class CaptureQualityInputDto(
    @SerialName("face_present") val facePresent: Boolean = true,
    @SerialName("yaw_degrees") val yawDegrees: Double = 0.0,
    @SerialName("pitch_degrees") val pitchDegrees: Double = 0.0,
    @SerialName("distance_cm") val distanceCm: Double = 45.0,
    @SerialName("expression_neutral") val expressionNeutral: Boolean = true,
)

@Serializable
data class CaptureCreateRequestDto(
    @SerialName("user_id") val userId: String,
    @SerialName("image_base64") val imageBase64: String,
    val quality: CaptureQualityInputDto? = null,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("device_meta") val deviceMeta: Map<String, String>? = null,
    @SerialName("is_baseline") val isBaseline: Boolean = false,
    val vertical: String = "skin",
    @SerialName("experiment_id") val experimentId: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
)

fun CaptureCreateRequest.toDto(): CaptureCreateRequestDto =
    CaptureCreateRequestDto(
        userId = userId,
        imageBase64 = imageBase64,
        quality =
            pose?.let {
                CaptureQualityInputDto(
                    facePresent = it.facePresent,
                    yawDegrees = it.yawDegrees,
                    pitchDegrees = it.pitchDegrees,
                    distanceCm = it.distanceCm,
                    expressionNeutral = it.expressionNeutral,
                )
            },
        capturedAt = capturedAt,
        deviceMeta = deviceMeta,
        isBaseline = isBaseline,
        vertical = vertical,
        experimentId = experimentId,
        idempotencyKey = idempotencyKey,
    )

@Serializable
data class CoachingTipDto(
    val check: String = "",
    val message: String = "",
)

fun CoachingTipDto.toDomain(): CoachingTip = CoachingTip(check, message)

/** Full server quality object — also the shape embedded in a `400`
 * rejection's `detail.quality` (see [com.glowup.ai.data.remote.ApiErrorMapper]). */
@Serializable
data class CaptureQualityDto(
    @SerialName("face_present") val facePresent: Boolean = false,
    @SerialName("yaw_degrees") val yawDegrees: Double = 0.0,
    @SerialName("pitch_degrees") val pitchDegrees: Double = 0.0,
    val brightness: Double = 0.0,
    val sharpness: Double = 0.0,
    @SerialName("distance_cm") val distanceCm: Double = 0.0,
    @SerialName("expression_neutral") val expressionNeutral: Boolean = false,
    @SerialName("reference_card_present") val referenceCardPresent: Boolean = false,
    val score: Double = 0.0,
    val accepted: Boolean = false,
    @SerialName("failed_checks") val failedChecks: List<String> = emptyList(),
    val coaching: List<CoachingTipDto> = emptyList(),
)

fun CaptureQualityDto.toDomain(): CaptureQuality =
    CaptureQuality(
        facePresent = facePresent,
        yawDegrees = yawDegrees,
        pitchDegrees = pitchDegrees,
        brightness = brightness,
        sharpness = sharpness,
        distanceCm = distanceCm,
        expressionNeutral = expressionNeutral,
        referenceCardPresent = referenceCardPresent,
        score = score,
        accepted = accepted,
        failedChecks = failedChecks,
        coaching = coaching.map { it.toDomain() },
    )

@Serializable
data class MetricDto(
    val confidence: Double? = null,
    @SerialName("redness_score") val rednessScore: Double? = null,
    @SerialName("blemish_count") val blemishCount: Double? = null,
    @SerialName("darkspot_area") val darkspotArea: Double? = null,
    @SerialName("texture_score") val textureScore: Double? = null,
    @SerialName("model_version") val modelVersion: String? = null,
)

fun MetricDto?.toDomain(confidenceLabel: String? = null): AppearanceMetric =
    AppearanceMetric(
        confidence = this?.confidence,
        rednessScore = this?.rednessScore,
        blemishCount = this?.blemishCount,
        darkspotArea = this?.darkspotArea,
        textureScore = this?.textureScore,
        modelVersion = this?.modelVersion,
        confidenceLabel = confidenceLabel,
    )

@Serializable
data class MeasurementExplanationDto(
    @SerialName("confidence_label") val confidenceLabel: String? = null,
)

@Serializable
data class BaselineComparisonDto(
    @SerialName("has_baseline") val hasBaseline: Boolean = false,
    @SerialName("redness_change_pct") val rednessChangePct: Double? = null,
    @SerialName("blemish_change_pct") val blemishChangePct: Double? = null,
    @SerialName("darkspot_change_pct") val darkspotChangePct: Double? = null,
    @SerialName("texture_change_pct") val textureChangePct: Double? = null,
)

@Serializable
data class AnalysisQualityDto(
    val usable: Boolean = false,
    val issues: List<String> = emptyList(),
)

@Serializable
data class CosmeticObservationDto(
    val region: String = "",
    val concern: String = "",
    val visibility: String = "",
    val extent: String = "",
    val description: String = "",
)

@Serializable
data class AnalysisImageInputDto(
    @SerialName("preprocessing_version") val preprocessingVersion: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val bytes: Int = 0,
    val detail: String = "low",
)

@Serializable
data class AnalysisEnvelopeDto(
    @SerialName("schema_version") val schemaVersion: String = "",
    @SerialName("analysis_id") val analysisId: String? = null,
    val status: String = "unavailable",
    @SerialName("unavailable_reason") val unavailableReason: String? = null,
    @SerialName("retry_after_seconds") val retryAfterSeconds: Int? = null,
    @SerialName("vision_provider") val visionProvider: String? = null,
    @SerialName("vision_model_id") val visionModelId: String? = null,
    @SerialName("vision_reasoning_effort") val visionReasoningEffort: String? = null,
    @SerialName("language_provider") val languageProvider: String? = null,
    @SerialName("language_model_id") val languageModelId: String? = null,
    @SerialName("language_reasoning_effort") val languageReasoningEffort: String? = null,
    @SerialName("language_mode") val languageMode: String? = null,
    @SerialName("language_fallback_provider") val languageFallbackProvider: String? = null,
    @SerialName("language_fallback_model_id") val languageFallbackModelId: String? = null,
    @SerialName("language_fallback_reasoning_effort") val languageFallbackReasoningEffort: String? = null,
    @SerialName("image_input") val imageInput: AnalysisImageInputDto? = null,
    val quality: AnalysisQualityDto? = null,
    val observations: List<CosmeticObservationDto> = emptyList(),
    val limitations: List<String> = emptyList(),
    val summary: String? = null,
    val language: LanguageResponseDto? = null,
)

@Serializable
data class LanguageResponseDto(
    val answer: String? = null,
)

fun AnalysisEnvelopeDto.toDomain(): AnalysisEnvelope = AnalysisEnvelope(
    schemaVersion = schemaVersion,
    analysisId = analysisId,
    status = status,
    unavailableReason = unavailableReason,
    retryAfterSeconds = retryAfterSeconds,
    visionProvider = visionProvider,
    visionModelId = visionModelId,
    visionReasoningEffort = visionReasoningEffort,
    languageProvider = languageProvider,
    languageModelId = languageModelId,
    languageReasoningEffort = languageReasoningEffort,
    languageMode = languageMode,
    languageFallbackProvider = languageFallbackProvider,
    languageFallbackModelId = languageFallbackModelId,
    languageFallbackReasoningEffort = languageFallbackReasoningEffort,
    imageInput = imageInput?.let { AnalysisImageInput(it.preprocessingVersion, it.width, it.height, it.bytes, it.detail) },
    quality = quality?.let { AnalysisQuality(it.usable, it.issues) },
    observations = observations.map { CosmeticObservation(it.region, it.concern, it.visibility, it.extent, it.description) },
    limitations = limitations,
    summary = summary,
    languageAnswer = language?.answer,
)

fun BaselineComparisonDto?.toDomain(): BaselineComparison? =
    this?.let {
        BaselineComparison(
            hasBaseline = hasBaseline,
            rednessChangePct = rednessChangePct,
            blemishChangePct = blemishChangePct,
            darkspotChangePct = darkspotChangePct,
            textureChangePct = textureChangePct,
        )
    }

@Serializable
data class CaptureResponseDto(
    val id: String = "",
    @SerialName("captured_at") val capturedAt: String = "",
    @SerialName("is_baseline") @Serializable(with = IntBooleanSerializer::class) val isBaseline: Boolean = false,
    val status: String = "accepted",
    @SerialName("capture_quality") val captureQuality: CaptureQualityDto = CaptureQualityDto(),
    @SerialName("analysis_job_id") val analysisJobId: String? = null,
    val metric: MetricDto? = null,
    val measurement: MeasurementExplanationDto? = null,
    val vertical: String = "skin",
    @SerialName("baseline_comparison") val baselineComparison: BaselineComparisonDto? = null,
    val analysis: AnalysisEnvelopeDto? = null,
    @SerialName("analysis_status") val analysisStatus: String? = null,
)

fun CaptureResponseDto.toDomain(): CaptureResult =
    CaptureResult(
        id = id,
        capturedAt = capturedAt,
        isBaseline = isBaseline,
        status = status,
        captureQuality = captureQuality.toDomain(),
        analysisJobId = analysisJobId,
        metric = metric.toDomain(measurement?.confidenceLabel),
        vertical = vertical,
        baselineComparison = baselineComparison.toDomain(),
        analysis = analysis?.toDomain() ?: analysisStatus?.let { AnalysisEnvelopeDto(status = it).toDomain() },
    )

@Serializable
data class HistoryItemDto(
    val id: String = "",
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("captured_at") val capturedAt: String = "",
    @SerialName("is_baseline") @Serializable(with = IntBooleanSerializer::class) val isBaseline: Boolean = false,
    @SerialName("redness_score") val rednessScore: Double? = null,
    @SerialName("blemish_count") val blemishCount: Double? = null,
    @SerialName("redness_delta") val rednessDelta: Double? = null,
    @SerialName("darkspot_area") val darkspotArea: Double? = null,
    @SerialName("texture_score") val textureScore: Double? = null,
    val confidence: Double? = null,
    @SerialName("model_version") val modelVersion: String? = null,
    @SerialName("capture_quality") val captureQuality: CaptureQualityDto? = null,
    @SerialName("noise_floor") val noiseFloor: Map<String, Double>? = null,
    @SerialName("appearance_metrics") val appearanceMetrics: Map<String, Double>? = null,
    @SerialName("confidence_label") val confidenceLabel: String? = null,
    @SerialName("baseline_comparison") val baselineComparison: BaselineComparisonDto? = null,
    val analysis: AnalysisEnvelopeDto? = null,
)

fun HistoryItemDto.toDomain(): HistoryItem =
    HistoryItem(
        id = id,
        photoPath = photoPath,
        capturedAt = capturedAt,
        isBaseline = isBaseline,
        rednessScore = rednessScore,
        blemishCount = blemishCount,
        rednessDelta = rednessDelta,
        darkspotArea = darkspotArea,
        textureScore = textureScore,
        confidence = confidence,
        modelVersion = modelVersion,
        captureQuality = captureQuality?.toDomain(),
        noiseFloor = noiseFloor ?: emptyMap(),
        appearanceMetrics = appearanceMetrics ?: emptyMap(),
        confidenceLabel = confidenceLabel,
        baselineComparison = baselineComparison.toDomain(),
        analysis = analysis?.toDomain(),
    )

@Serializable
data class CaptureGuideDto(
    val vertical: String = "skin",
    val state: String = "baseline_needed",
    val message: String = "",
    @SerialName("next_window_start") val nextWindowStart: String? = null,
    @SerialName("next_window_end") val nextWindowEnd: String? = null,
    @SerialName("last_capture") val lastCapture: String? = null,
)

fun CaptureGuideDto.toDomain(): CaptureGuide =
    CaptureGuide(
        vertical = vertical,
        state = CaptureGuideState.fromRaw(state),
        message = message,
        nextWindowStart = nextWindowStart,
        nextWindowEnd = nextWindowEnd,
        lastCapture = lastCapture,
    )

@Serializable
data class MeasurementFeedbackCreateRequestDto(
    @SerialName("capture_id") val captureId: String,
    val agreement: String,
    val note: String? = null,
)

fun MeasurementFeedbackRequest.toDto(): MeasurementFeedbackCreateRequestDto =
    MeasurementFeedbackCreateRequestDto(
        captureId = captureId,
        agreement = agreement.toWire(),
        note = note,
    )

@Serializable
data class MeasurementFeedbackDto(
    val id: String,
    @SerialName("capture_id") val captureId: String,
    val agreement: String,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

fun MeasurementFeedbackDto.toDomain(): MeasurementFeedback =
    MeasurementFeedback(
        id = id,
        captureId = captureId,
        agreement = MeasurementAgreement.fromRaw(agreement),
        note = note,
        createdAt = createdAt,
    )
