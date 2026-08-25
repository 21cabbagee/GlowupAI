package com.glowup.ai.data.remote

import com.glowup.ai.data.remote.dto.CaptureQualityDto
import com.glowup.ai.data.remote.dto.toDomain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import retrofit2.HttpException
import java.io.IOException

/**
 * Normalises every network failure into an [ApiError]. This is THE hard part
 * of Task 2.3 — see ANDROID_PLAN.md Task 2.3 and frontend-api-map.md's
 * "Error handling must preserve structured detail" trap. Nothing else in
 * the app should inspect a raw HTTP status code or error body; route
 * everything through [map] (normally via [apiCall]).
 */
object ApiErrorMapper {

    fun map(throwable: Throwable): ApiError = when (throwable) {
        is HttpException -> mapHttp(throwable)
        is IOException -> ApiError.Network(throwable)
        else -> ApiError.Unknown(throwable)
    }

    private fun mapHttp(exception: HttpException): ApiError {
        val code = exception.code()
        val bodyText = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
        val detail = bodyText?.let { extractDetail(it) }

        return when (code) {
            401 -> ApiError.Unauthorized
            403 -> mapForbidden(detail)
            400 -> mapBadRequest(detail)
            422 -> mapValidation(detail)
            404 -> ApiError.NotFound(detailMessage(detail) ?: "resource not found")
            in 500..599 -> ApiError.Server(code, detailMessage(detail) ?: "server error ($code)")
            else -> ApiError.Server(code, detailMessage(detail) ?: "unexpected response ($code)")
        }
    }

    /** `{"detail": ...}` — `detail` is a string, an object, or (422 only) an array. */
    private fun extractDetail(bodyText: String): JsonElement? = runCatching {
        NetworkJson.parseToJsonElement(bodyText).jsonObject["detail"]
    }.getOrNull()

    private fun detailMessage(detail: JsonElement?): String? = when (detail) {
        null, is JsonNull -> null
        is JsonPrimitive -> detail.contentOrNull
        is JsonObject -> (detail["message"] as? JsonPrimitive)?.contentOrNull
        is JsonArray -> detail.joinToString("; ") { element ->
            (element as? JsonObject)?.get("msg")?.let { (it as? JsonPrimitive)?.contentOrNull } ?: element.toString()
        }
    }

    /**
     * 403 messages are always English sentences built from one of two
     * f-strings on the backend:
     *  - `require_premium`:  "<feature> requires Premium; upgrade the plan to unlock it"
     *  - `require_consent`:  "explicit facial-data consent is required before using photo capture"
     * Anything else (e.g. an ownership-mismatch 403 under
     * `SKINPROOF_AUTH_REQUIRED=1`) falls back to [ApiError.Unauthorized] —
     * the safest generic treatment for "you may not do this as the current
     * identity."
     */
    private fun mapForbidden(detail: JsonElement?): ApiError {
        val message = detailMessage(detail) ?: return ApiError.Server(403, "forbidden")
        return when {
            message.contains("consent", ignoreCase = true) -> ApiError.ConsentRequired
            message.contains("requires Premium", ignoreCase = true) -> {
                val feature = message.substringBefore(" requires Premium", missingDelimiterValue = message).trim()
                ApiError.PremiumRequired(feature.ifBlank { "This feature" })
            }
            // A valid token that owns a different user is forbidden, not
            // unauthenticated. Do not make SessionStateMachine clear a good
            // session for this response.
            else -> ApiError.Server(403, message)
        }
    }

    /**
     * 400s split three ways:
     *  - a structured `{"message": ..., "quality": {...}}` object -> capture rejection
     *  - a string ending in "not found" -> [ApiError.NotFound]
     *  - a string signalling a duplicate ("already exists") -> [ApiError.Conflict]
     *  - anything else -> a single-field [ApiError.Validation] fallback, since the
     *    backend's plain domain-validation 400s (e.g. "action must be start,
     *    stop, or change") don't carry a field name at all.
     */
    private fun mapBadRequest(detail: JsonElement?): ApiError {
        if (detail is JsonObject && detail["quality"] is JsonObject) {
            val qualityDto = runCatching {
                NetworkJson.decodeFromJsonElement(CaptureQualityDto.serializer(), detail["quality"] as JsonObject)
            }.getOrNull()
            if (qualityDto != null) {
                return ApiError.CaptureQualityRejected(qualityDto.toDomain(), qualityDto.coaching.map { it.toDomain() })
            }
        }

        val message = detailMessage(detail) ?: "request could not be completed"
        return when {
            message.contains("not found", ignoreCase = true) -> ApiError.NotFound(message)
            message.contains("already exists", ignoreCase = true) -> ApiError.Conflict(message)
            else -> ApiError.Validation(mapOf("_general" to message))
        }
    }

    /** FastAPI 422 body: `[{"loc":["body","field"],"msg":"...","type":"..."}]`. */
    private fun mapValidation(detail: JsonElement?): ApiError {
        if (detail !is JsonArray) {
            return ApiError.Validation(mapOf("_general" to (detailMessage(detail) ?: "invalid request")))
        }
        val fields = detail.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val loc = (obj["loc"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                ?.filterNot { it == "body" || it == "query" || it == "path" }
                ?.joinToString(".")
                .takeUnless { it.isNullOrBlank() } ?: "_general"
            val msg = (obj["msg"] as? JsonPrimitive)?.contentOrNull ?: "invalid value"
            loc to msg
        }.toMap()
        return ApiError.Validation(fields.ifEmpty { mapOf("_general" to "invalid request") })
    }
}
