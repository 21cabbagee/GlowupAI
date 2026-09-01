package com.glowup.ai.domain.model

data class VerdictEvidence(
    val nAfter: Int?,
    val confidence: Double?,
)

/** `label == LOCKED` is a distinct upsell card — the free one-lifetime-unlock
 * model, not a boolean. Never render it as a normal verdict. */
data class Verdict(
    val label: VerdictLabel,
    val generatedText: String,
    val productId: String?,
    val productName: String?,
    val evidence: VerdictEvidence?,
)

data class Reminder(
    val id: String,
    val kind: String,
    val nextAt: String?,
    val enabled: Boolean,
    val cadenceDays: Int?,
    val lastSentAt: String?,
)

data class Engagement(
    val captureStreak: Int,
    val captureCount: Int,
    /** Up to the most recent 30 distinct calendar days with an accepted capture. */
    val captureDays: List<String>,
    val guide: CaptureGuide?,
    val reminders: List<Reminder>,
)

data class EngagementEventRequest(
    val eventType: String,
    val referenceId: String? = null,
    val metadata: Map<String, String>? = null,
)

data class DashboardRoutineEvent(
    val action: RoutineAction,
    val productName: String?,
    val timestamp: String?,
    val slot: String?,
    val notes: String?,
)

data class DashboardAnalytics(
    val medianHistoryDays: Double?,
    val baselineCapture: Boolean?,
    val firstThreeCaptures: Boolean?,
    val activation: String?,
)

/** Free-plan `verdicts`/`experiments` are empty by DESIGN, not "no data" —
 * branch on [features]/entitlement before rendering an empty state. */
data class DashboardFeatures(
    val productVerdictsUnlocked: Boolean,
    val raw: Map<String, Boolean>,
)

data class Dashboard(
    val profile: Profile,
    val vertical: String,
    val history: List<HistoryItem>,
    val verdicts: List<Verdict>,
    val experiments: List<Experiment>,
    val engagement: Engagement?,
    val analytics: DashboardAnalytics?,
    val weeklyRecap: WeeklyRecap?,
    val checkIns: List<CheckIn>,
    val routineEvents: List<DashboardRoutineEvent>,
    val features: DashboardFeatures,
    val disclaimer: String,
)

data class CheckIn(
    val id: String,
    val routineState: CheckInRoutineState,
    val skinFeel: CheckInSkinFeel,
    val note: String?,
    val occurredAt: String,
)

data class CheckInCreateRequest(
    val routineState: CheckInRoutineState = CheckInRoutineState.STEADY,
    val skinFeel: CheckInSkinFeel = CheckInSkinFeel.NOT_SURE,
    val note: String? = null,
    val occurredAt: String? = null,
)

data class MetricSummary(
    val metric: String,
    val label: String,
    val direction: String,
    val delta: Double?,
    val noiseFloor: Double?,
    val sentence: String?,
)

data class WeeklyRecapPeriod(val start: String?, val end: String?)

data class WeeklyRecap(
    val status: String,
    val headline: String,
    val body: String,
    val nextAction: String?,
    val captureCount: Int,
    val totalCaptureCount: Int?,
    val checkInCount: Int,
    val comparisonMode: String?,
    val confidenceLabel: String,
    val metricSummaries: List<MetricSummary>,
    val period: WeeklyRecapPeriod,
    val disclaimer: String,
)

data class Analytics(
    val activation: String?,
    val baselineCapture: Boolean?,
    val firstThreeCaptures: Boolean?,
    val medianHistoryDays: Double?,
    val weeklyVerdictOpenRate: Double?,
    val verdictActionRate: Double?,
    val evidenceUnclearEngagementRate: Double?,
    val rawEvents: List<Map<String, String>>,
    val rawEventCount: Int? = null,
)
