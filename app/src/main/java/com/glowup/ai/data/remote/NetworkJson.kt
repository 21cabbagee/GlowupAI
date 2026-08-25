package com.glowup.ai.data.remote

import kotlinx.serialization.json.Json

/**
 * Single shared [Json] instance for the whole network layer: the Retrofit
 * converter factory, and [ApiErrorMapper] when it has to parse an error body
 * by hand. `ignoreUnknownKeys` is required — the backend returns raw DB rows
 * with extra columns by design (see frontend-api-map.md's contract
 * conventions).
 */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
