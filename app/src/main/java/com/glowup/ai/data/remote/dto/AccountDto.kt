package com.glowup.ai.data.remote.dto

import com.glowup.ai.domain.model.ExportBundle
import com.glowup.ai.domain.model.HealthStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HealthDto(
    val status: String = "ok",
    val version: String? = null,
    val scope: String? = null,
    val features: List<String> = emptyList(),
)

fun HealthDto.toDomain(): HealthStatus = HealthStatus(status, version, scope, features)

/** Nested sections are kept as raw [JsonElement] on purpose — see
 * [ExportBundle] doc. */
@Serializable
data class ExportBundleDto(
    /** Backend currently emits this as the string `"3"`; keep it as a
     * string so a future semantic version cannot break decoding. */
    @SerialName("export_version") val exportVersion: String? = null,
    @SerialName("exported_at") val exportedAt: String? = null,
    val profile: JsonElement? = null,
    @SerialName("consent_events") val consentEvents: JsonElement? = null,
    @SerialName("appearance_profiles") val appearanceProfiles: JsonElement? = null,
    @SerialName("routine_events") val routineEvents: JsonElement? = null,
    val experiments: JsonElement? = null,
    @SerialName("captures_and_metrics") val capturesAndMetrics: JsonElement? = null,
    @SerialName("appearance_captures") val appearanceCaptures: JsonElement? = null,
    val verdicts: JsonElement? = null,
    val qna: JsonElement? = null,
    val engagement: JsonElement? = null,
    val note: String? = null,
)

fun ExportBundleDto.toDomain(): ExportBundle =
    ExportBundle(
        exportVersion = exportVersion,
        exportedAt = exportedAt,
        profile = profile,
        consentEvents = consentEvents,
        appearanceProfiles = appearanceProfiles,
        routineEvents = routineEvents,
        experiments = experiments,
        capturesAndMetrics = capturesAndMetrics,
        appearanceCaptures = appearanceCaptures,
        verdicts = verdicts,
        qna = qna,
        engagement = engagement,
        note = note,
    )
