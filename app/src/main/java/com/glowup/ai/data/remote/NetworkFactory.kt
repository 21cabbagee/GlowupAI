package com.glowup.ai.data.remote

import com.glowup.ai.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds the OkHttp/Retrofit stack for [GlowUpApi]. The DI agent (owns
 * the `di` package) should call this from a Hilt `@Provides` function rather than
 * duplicating this wiring — this file is the single source of truth for
 * timeouts, interceptor order, and the base URL.
 *
 * `BuildConfig.API_BASE_URL` is the API root (the checked-in build types end in
 * `/api/`). Retrofit resolves relative paths against it, so [GlowUpApi] uses
 * paths without a second `api/` segment.
 */
object NetworkFactory {
    /** Safe GETs get one retry with a short backoff; mutations never do —
     * this is enforced by [RetryPolicyInterceptor], not by caller discipline. */
    fun okHttpClient(
        tokenProvider: TokenProvider,
        debugLogging: Boolean,
        debugLogger: (String) -> Unit = { message -> println(message) },
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(RetryPolicyInterceptor())
                .addInterceptor(AuthInterceptor(tokenProvider))

        if (debugLogging) {
            builder.addInterceptor(RedactingLoggingInterceptor(debugLogger))
        }
        return builder.build()
    }

    fun retrofit(
        okHttpClient: OkHttpClient,
        json: Json = NetworkJson,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun api(okHttpClient: OkHttpClient): GlowUpApi = retrofit(okHttpClient).create(GlowUpApi::class.java)

    /** Retrofit requires a trailing slash and treats a missing slash as a file
     * path. Keep that normalization here so every build type and test server
     * joins `/api/` + `users/...` predictably. */
    internal fun normalizeBaseUrl(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "API_BASE_URL must not be blank" }
        return if (value.endsWith('/')) value else "$value/"
    }
}
