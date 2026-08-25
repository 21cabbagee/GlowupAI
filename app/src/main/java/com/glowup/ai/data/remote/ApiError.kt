package com.glowup.ai.data.remote

import com.glowup.ai.domain.model.CaptureQuality
import com.glowup.ai.domain.model.CoachingTip

/**
 * Normalised failure shape for every network call. Produced exclusively by
 * [ApiErrorMapper] — nothing else in the app should construct these except
 * tests.
 */
sealed class ApiError {
    /** Missing/invalid/expired bearer token (401), or an unrecognised 403
     * whose message matches neither the consent nor the Premium pattern. */
    object Unauthorized : ApiError()

    /** 403 whose message indicates missing facial-data consent. */
    object ConsentRequired : ApiError()

    /** 403 whose message reads "<feature> requires Premium...". */
    data class PremiumRequired(val feature: String) : ApiError()

    /** 400 capture-quality rejection: `detail` was a structured object with
     * a `quality` sub-object (and, since the coaching growth feature,
     * `quality.coaching[]`). */
    data class CaptureQualityRejected(
        val quality: CaptureQuality,
        val coaching: List<CoachingTip>,
    ) : ApiError()

    /** 422 (FastAPI body/query validation) or an otherwise field-shaped 400. */
    data class Validation(val fields: Map<String, String>) : ApiError()

    /** 400 "<entity> not found" — the backend uses 400, not 404, for this. */
    data class NotFound(val what: String) : ApiError()

    /** 400 "already exists" / duplicate-style conflicts (e.g. barcode reuse). */
    data class Conflict(val message: String) : ApiError()

    /** No HTTP response at all (timeout, DNS, offline, etc). */
    data class Network(val cause: Throwable) : ApiError()

    /** Any other non-2xx status, including 5xx. */
    data class Server(val code: Int, val message: String) : ApiError()

    /** Anything that doesn't fit the above (unexpected body shape, etc). */
    data class Unknown(val cause: Throwable) : ApiError()
}
