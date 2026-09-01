package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.ActiveProductWindow
import com.glowup.ai.domain.model.ConfoundCheck
import com.glowup.ai.domain.model.RoutineAction
import com.glowup.ai.domain.model.RoutineEvent
import com.glowup.ai.domain.model.RoutineEventRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutineEventCreateRequestDto(
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    val action: String,
    val timestamp: String? = null,
    val slot: String = "unspecified",
    val dose: String? = null,
    val frequency: String? = null,
    val notes: String? = null,
    @SerialName("experiment_id") val experimentId: String? = null,
)

fun RoutineEventRequest.toDto(): RoutineEventCreateRequestDto = RoutineEventCreateRequestDto(
    userId = userId,
    productId = productId,
    action = action.toWire(),
    timestamp = timestamp,
    slot = slot,
    dose = dose,
    frequency = frequency,
    notes = notes,
    experimentId = experimentId,
)

@Serializable
data class ActiveProductWindowDto(
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("stable_at") val stableAt: String = "",
)

@Serializable
data class ConfoundCheckDto(
    val confounded: Boolean = false,
    @SerialName("active_windows") val activeWindows: List<ActiveProductWindowDto> = emptyList(),
    val message: String? = null,
)

fun ConfoundCheckDto.toDomain(): ConfoundCheck = ConfoundCheck(
    confounded = confounded,
    activeWindows = activeWindows.map { ActiveProductWindow(it.productId, it.productName, it.startedAt, it.stableAt) },
    message = message,
)

@Serializable
data class RoutineEventDto(
    val id: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String? = null,
    val action: String = "start",
    val timestamp: String? = null,
    val slot: String? = null,
    val dose: String? = null,
    val frequency: String? = null,
    val notes: String? = null,
    @SerialName("confound_warning") val confoundWarning: ConfoundCheckDto? = null,
)

fun RoutineEventDto.toDomain(): RoutineEvent = RoutineEvent(
    id = id,
    productId = productId,
    productName = productName,
    action = RoutineAction.fromRaw(action),
    timestamp = timestamp,
    slot = slot,
    dose = dose,
    frequency = frequency,
    notes = notes,
    confoundWarning = confoundWarning?.toDomain(),
)
