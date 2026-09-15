package com.glowup.ai.domain.model

import java.util.UUID

/**
 * Typealias for StreakCalculator - uses HistoryItem which has capturedAt field
 */
typealias Capture = HistoryItem

/** Client-measured pose/face fields sent to the server. Brightness and
 * sharpness are always overwritten by the server, so they are not part of
 * the outgoing request shape at all (see [CaptureCreateRequest]). */
data class CapturePose(
    val facePresent: Boolean,
    val yawDegrees: Double,
    val pitchDegrees: Double,
    val distanceCm: Double,
    val expressionNeutral: Boolean,
)

data class CaptureCreateRequest(
    val userId: String,
    val imageBase64: String,
    val pose: CapturePose?,
    val isBaseline: Boolean = false,
    val vertical: String = "skin",
    val experimentId: String? = null,
    val capturedAt: String? = null,
    val deviceMeta: Map<String, String>? = null,
    /** Stable across an ambiguous network failure and every outbox retry. */
    val idempotencyKey: String = UUID.randomUUID().toString(),
)

data class CoachingTip(
    val check: String,
    val message: String,
)

/** Full quality object as returned by the server (and as embedded in a
 * `400` rejection's `detail.quality`). Server-authoritative: a client
 * preflight is only ever a hint, never an acceptance. */
data class CaptureQuality(
    val facePresent: Boolean,
    val yawDegrees: Double,
    val pitchDegrees: Double,
    val brightness: Double,
    val sharpness: Double,
    val distanceCm: Double,
    val expressionNeutral: Boolean,
    val referenceCardPresent: Boolean,
    val score: Double,
    val accepted: Boolean,
    val failedChecks: List<String>,
    val coaching: List<CoachingTip>,
)

data class AppearanceMetric(
    val confidence: Double?,
    val rednessScore: Double?,
    val blemishCount: Double?,
    val darkspotArea: Double?,
    val textureScore: Double?,
    val modelVersion: String?,
    val confidenceLabel: String?,
)

/** A bounded, qualitative observation returned by the vision provider.  All
 * fields are strings from a server-controlled enum; keeping them as strings
 * lets the app safely display a newly added server value instead of failing
 * deserialization. */
data class CosmeticObservation(
    val region: String,
    val concern: String,
    val visibility: String,
    val extent: String,
    val description: String,
)

data class AnalysisQuality(
    val usable: Boolean,
    val issues: List<String>,
)

data class AnalysisImageInput(
    val preprocessingVersion: String,
    val width: Int,
    val height: Int,
    val bytes: Int,
    val detail: String,
)

data class AnalysisEnvelope(
    val schemaVersion: String,
    val analysisId: String?,
    val status: String,
    val unavailableReason: String?,
    val retryAfterSeconds: Int?,
    val visionProvider: String?,
    val visionModelId: String?,
    val visionReasoningEffort: String?,
    val languageProvider: String?,
    val languageModelId: String?,
    val languageReasoningEffort: String?,
    val languageMode: String?,
    val languageFallbackProvider: String?,
    val languageFallbackModelId: String?,
    val languageFallbackReasoningEffort: String?,
    val imageInput: AnalysisImageInput?,
    val quality: AnalysisQuality?,
    val observations: List<CosmeticObservation>,
    val limitations: List<String>,
    val summary: String?,
    val languageAnswer: String?,
)

data class BaselineComparison(
    val hasBaseline: Boolean,
    val rednessChangePct: Double?,
    val blemishChangePct: Double?,
    val darkspotChangePct: Double?,
    val textureChangePct: Double?,
)

data class CaptureResult(
    val id: String,
    val capturedAt: String,
    val isBaseline: Boolean,
    val status: String,
    val captureQuality: CaptureQuality,
    val analysisJobId: String?,
    val metric: AppearanceMetric,
    val vertical: String,
    val baselineComparison: BaselineComparison? = null,
    val analysis: AnalysisEnvelope? = null,
)

data class HistoryItem(
    val id: String,
    val capturedAt: String,
    val isBaseline: Boolean,
    val rednessScore: Double?,
    val blemishCount: Double?,
    val rednessDelta: Double?,
    val darkspotArea: Double?,
    val textureScore: Double?,
    val confidence: Double?,
    val modelVersion: String?,
    val captureQuality: CaptureQuality?,
    val noiseFloor: Map<String, Double>,
    val appearanceMetrics: Map<String, Double>,
    val confidenceLabel: String?,
    val baselineComparison: BaselineComparison? = null,
    val photoPath: String? = null,
    val analysis: AnalysisEnvelope? = null,
)

data class CaptureGuide(
    val vertical: String,
    val state: CaptureGuideState,
    val message: String,
    val nextWindowStart: String?,
    val nextWindowEnd: String?,
    val lastCapture: String?,
)

data class MeasurementFeedbackRequest(
    val captureId: String,
    val agreement: MeasurementAgreement,
    val note: String? = null,
)

data class MeasurementFeedback(
    val id: String,
    val captureId: String,
    val agreement: MeasurementAgreement,
    val note: String?,
    val createdAt: String?,
)
