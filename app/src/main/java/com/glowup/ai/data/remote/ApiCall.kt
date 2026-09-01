package com.glowup.ai.data.remote

import com.glowup.ai.core.util.GlowResult
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Runs a suspend Retrofit call and normalises any failure through
 * [ApiErrorMapper]. Repository authors should call this (or
 * [apiCallNoContent]) instead of touching [GlowUpApi] try/catch blocks
 * directly, so every call in the app fails the same way.
 *
 * Example:
 * ```
 * suspend fun profile(userId: String): GlowResult<Profile> =
 *     apiCall { api.getProfile(userId).toDomain() }
 * ```
 */
suspend fun <T> apiCall(block: suspend () -> T): GlowResult<T> = try {
    GlowResult.Success(block())
} catch (exception: HttpException) {
    GlowResult.Failure(ApiErrorMapper.map(exception))
} catch (exception: IOException) {
    GlowResult.Failure(ApiErrorMapper.map(exception))
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    // Log the full exception details for debugging parsing errors
    android.util.Log.e("ApiCall", "API call failed with exception: ${exception.message}", exception)
    if (exception is kotlinx.serialization.SerializationException) {
        android.util.Log.e("ApiCall", "JSON parsing error. This usually means a required field is missing or has wrong type in the API response.")
    }
    GlowResult.Failure(ApiErrorMapper.map(exception))
}

/**
 * For the one true `204 No Content` route in this API (`DELETE
 * /api/users/{userId}`). Deliberately takes a `Response<Unit>`-returning
 * block and never touches `.body()` — a 204 has no body to parse, and
 * Retrofit would throw if this tried to decode one.
 */
suspend fun apiCallNoContent(block: suspend () -> Response<Unit>): GlowResult<Unit> = try {
    val response = block()
    if (response.isSuccessful) {
        GlowResult.Success(Unit)
    } else {
        GlowResult.Failure(ApiErrorMapper.map(HttpException(response)))
    }
} catch (exception: HttpException) {
    GlowResult.Failure(ApiErrorMapper.map(exception))
} catch (exception: IOException) {
    GlowResult.Failure(ApiErrorMapper.map(exception))
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    GlowResult.Failure(ApiErrorMapper.map(exception))
}
