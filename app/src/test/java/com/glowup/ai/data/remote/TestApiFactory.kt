package com.glowup.ai.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds a [GlowUpApi] pointed at a local [MockWebServer] — no
 * `BuildConfig`/DI dependency, so these tests exercise exactly the
 * serialization + [ApiErrorMapper] contract this file owns. */
fun testApi(server: MockWebServer): GlowUpApi {
    val client = OkHttpClient.Builder().build()
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .build()
    return retrofit.create(GlowUpApi::class.java)
}
