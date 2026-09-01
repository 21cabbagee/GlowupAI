package com.glowup.ai.core.util

import com.glowup.ai.data.remote.ApiError

/**
 * Uniform result wrapper for every network-backed operation in the app.
 *
 * Repositories and ViewModels branch on this instead of catching exceptions
 * directly. [ApiError] carries the normalised failure shape produced by
 * `ApiErrorMapper` in `data.remote`.
 */
sealed class GlowResult<out T> {
    data class Success<T>(
        val data: T,
    ) : GlowResult<T>()

    data class Failure(
        val error: ApiError,
    ) : GlowResult<Nothing>()
}

/** Returns the success value or `null` if this is a [GlowResult.Failure]. */
fun <T> GlowResult<T>.dataOrNull(): T? =
    when (this) {
        is GlowResult.Success -> data
        is GlowResult.Failure -> null
    }

inline fun <T, R> GlowResult<T>.map(transform: (T) -> R): GlowResult<R> =
    when (this) {
        is GlowResult.Success -> GlowResult.Success(transform(data))
        is GlowResult.Failure -> this
    }

inline fun <T> GlowResult<T>.onSuccess(block: (T) -> Unit): GlowResult<T> {
    if (this is GlowResult.Success) block(data)
    return this
}

inline fun <T> GlowResult<T>.onFailure(block: (ApiError) -> Unit): GlowResult<T> {
    if (this is GlowResult.Failure) block(error)
    return this
}
