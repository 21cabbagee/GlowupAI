package com.glowup.ai.domain.model

/** `action` must be exactly `start`/`stop`/`change` — never a daily "applied" tick. */
data class RoutineEventRequest(
    val userId: String,
    val productId: String,
    val action: RoutineAction,
    val timestamp: String? = null,
    val slot: String = "unspecified",
    val dose: String? = null,
    val frequency: String? = null,
    val notes: String? = null,
    val experimentId: String? = null,
)

data class RoutineEvent(
    val id: String,
    val productId: String,
    val productName: String?,
    val action: RoutineAction,
    val timestamp: String?,
    val slot: String?,
    val dose: String?,
    val frequency: String?,
    val notes: String?,
    /** Inline warning on `start`/`change` responses; `null` when nothing is at risk. */
    val confoundWarning: ConfoundCheck?,
)

data class ActiveProductWindow(
    val productId: String,
    val productName: String,
    val startedAt: String,
    val stableAt: String,
)

data class ConfoundCheck(
    val confounded: Boolean,
    val activeWindows: List<ActiveProductWindow>,
    val message: String?,
)
