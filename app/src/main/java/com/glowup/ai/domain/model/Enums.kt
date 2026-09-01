package com.glowup.ai.domain.model

/**
 * Unknown-tolerant enums for every backend status vocabulary.
 *
 * Every enum here carries an `UNKNOWN` fallback so a new backend value never
 * crashes deserialization/mapping — it just degrades to a state the UI can
 * treat conservatively. Each companion's `fromRaw` is the single place that
 * knows the backend's exact string spelling.
 */

/** Real values emitted by the backend: `planned|running|paused|completed|cancelled`.
 * `"active"` is NEVER emitted — do not add it here, and do not filter on it. */
enum class ExperimentStatus {
    PLANNED, RUNNING, PAUSED, COMPLETED, CANCELLED, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): ExperimentStatus = when (raw) {
            "planned" -> PLANNED
            "running" -> RUNNING
            "paused" -> PAUSED
            "completed" -> COMPLETED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

/** Async job lifecycle shared by reprocess and shelf-scan jobs. */
enum class JobStatus {
    QUEUED, RUNNING, COMPLETED, FAILED, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): JobStatus = when (raw) {
            "queued" -> QUEUED
            "running" -> RUNNING
            "completed" -> COMPLETED
            "failed" -> FAILED
            else -> UNKNOWN
        }
    }
}

enum class ConsentState {
    PENDING, ACTIVE, DECLINED, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): ConsentState = when (raw) {
            "pending" -> PENDING
            "active" -> ACTIVE
            "declined" -> DECLINED
            else -> UNKNOWN
        }
    }
}

enum class Plan {
    FREE, PREMIUM, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): Plan = when (raw) {
            "free" -> FREE
            "premium" -> PREMIUM
            else -> UNKNOWN
        }
    }
}

enum class EntitlementStatus {
    ACTIVE, CANCELLED, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): EntitlementStatus = when (raw) {
            "active" -> ACTIVE
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

/** States from `GET /capture-guide`. */
enum class CaptureGuideState {
    BASELINE_NEEDED, SCHEDULED, DUE, OVERDUE, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): CaptureGuideState = when (raw) {
            "baseline_needed" -> BASELINE_NEEDED
            "scheduled" -> SCHEDULED
            "due" -> DUE
            "overdue" -> OVERDUE
            else -> UNKNOWN
        }
    }
}

/** Dashboard verdict label. `LOCKED` is the one-free-lifetime-unlock upsell
 * shape and must render as a distinct upsell card, never as a normal verdict. */
enum class VerdictLabel {
    KEEP, LIKELY_USEFUL, EVIDENCE_UNCLEAR, INVESTIGATE, LOCKED, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): VerdictLabel = when (raw) {
            "keep" -> KEEP
            "likely_useful" -> LIKELY_USEFUL
            "evidence_unclear" -> EVIDENCE_UNCLEAR
            "investigate" -> INVESTIGATE
            "locked" -> LOCKED
            else -> UNKNOWN
        }
    }
}

/** `POST /api/triage` and Q&A `scope`. Backend returns `scope`, never `route`. */
enum class SafetyScope {
    COSMETIC_TRACKING, DERMATOLOGY_REVIEW, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): SafetyScope = when (raw) {
            "cosmetic_tracking" -> COSMETIC_TRACKING
            "dermatology_review" -> DERMATOLOGY_REVIEW
            else -> UNKNOWN
        }
    }
}

/** `POST /api/routine-events` accepts ONLY these three values. */
enum class RoutineAction {
    START, STOP, CHANGE, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): RoutineAction = when (raw) {
            "start" -> START
            "stop" -> STOP
            "change" -> CHANGE
            else -> UNKNOWN
        }
    }

    fun toWire(): String = when (this) {
        START -> "start"
        STOP -> "stop"
        CHANGE -> "change"
        UNKNOWN -> throw IllegalArgumentException("RoutineAction.UNKNOWN cannot be sent to the server")
    }
}

enum class CheckInRoutineState {
    STEADY, CHANGED, MISSED, NOT_SURE, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): CheckInRoutineState = when (raw) {
            "steady" -> STEADY
            "changed" -> CHANGED
            "missed" -> MISSED
            "not_sure" -> NOT_SURE
            else -> UNKNOWN
        }
    }

    fun toWire(): String = name.lowercase()
}

enum class CheckInSkinFeel {
    BETTER, SAME, WORSE, NOT_SURE, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): CheckInSkinFeel = when (raw) {
            "better" -> BETTER
            "same" -> SAME
            "worse" -> WORSE
            "not_sure" -> NOT_SURE
            else -> UNKNOWN
        }
    }

    fun toWire(): String = name.lowercase()
}

enum class ContextEventType {
    SLEEP, TRAVEL, WEATHER, CYCLE, STRESS, DIET, CUSTOM, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): ContextEventType = when (raw) {
            "sleep" -> SLEEP
            "travel" -> TRAVEL
            "weather" -> WEATHER
            "cycle" -> CYCLE
            "stress" -> STRESS
            "diet" -> DIET
            "custom" -> CUSTOM
            else -> UNKNOWN
        }
    }

    fun toWire(): String = name.lowercase()
}

enum class MeasurementAgreement {
    FAIR, UNCERTAIN, OFF, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): MeasurementAgreement = when (raw) {
            "fair" -> FAIR
            "uncertain" -> UNCERTAIN
            "off" -> OFF
            else -> UNKNOWN
        }
    }

    fun toWire(): String = name.lowercase()
}

/** `primary_metric` on experiments. */
enum class PrimaryMetric {
    BLEMISH_COUNT, REDNESS_SCORE, DARKSPOT_AREA, TEXTURE_SCORE, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): PrimaryMetric = when (raw) {
            "blemish_count" -> BLEMISH_COUNT
            "redness_score" -> REDNESS_SCORE
            "darkspot_area" -> DARKSPOT_AREA
            "texture_score" -> TEXTURE_SCORE
            else -> UNKNOWN
        }
    }

    fun toWire(): String = when (this) {
        BLEMISH_COUNT -> "blemish_count"
        REDNESS_SCORE -> "redness_score"
        DARKSPOT_AREA -> "darkspot_area"
        TEXTURE_SCORE -> "texture_score"
        UNKNOWN -> "redness_score"
    }

    val displayName: String
        get() = when (this) {
            BLEMISH_COUNT -> "Blemishes"
            REDNESS_SCORE -> "Redness"
            DARKSPOT_AREA -> "Dark Spots"
            TEXTURE_SCORE -> "Texture"
            UNKNOWN -> "Unknown"
        }
}
