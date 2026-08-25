package com.glowup.ai.domain.model

import kotlinx.serialization.json.JsonElement

/**
 * Full data-export bundle. Nested sections are intentionally kept as raw
 * [JsonElement] — the export shape is a deep, evolving dump of nearly every
 * table for a privacy/data-control screen that just needs to serialize the
 * whole thing back out to a file, not bind individual fields to UI.
 */
data class ExportBundle(
    val exportVersion: String?,
    val exportedAt: String?,
    val profile: JsonElement?,
    val consentEvents: JsonElement?,
    val appearanceProfiles: JsonElement?,
    val routineEvents: JsonElement?,
    val experiments: JsonElement?,
    val capturesAndMetrics: JsonElement?,
    val appearanceCaptures: JsonElement?,
    val verdicts: JsonElement?,
    val qna: JsonElement?,
    val engagement: JsonElement?,
    val note: String?,
)

data class HealthStatus(
    val status: String,
    val version: String?,
    val scope: String?,
    val features: List<String>,
)
