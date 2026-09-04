package com.glowup.ai.di

import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.NetworkJson
import com.glowup.ai.data.remote.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Singleton
import com.glowup.ai.di.NetworkModule

/**
 * Test module that replaces NetworkModule for instrumented tests.
 *
 * Provides:
 * - MockWebServer for API responses
 * - Fake TokenProvider that doesn't require Firebase
 * - Test-configured OkHttpClient and Retrofit
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object TestNetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = NetworkJson

    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = object : TokenProvider {
        override suspend fun idToken(forceRefresh: Boolean): String = "test_token"
    }

    @Provides
    @Singleton
    fun provideMockWebServer(): MockWebServer = MockWebServer()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer test_token")
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        mockWebServer: MockWebServer
    ): Retrofit = Retrofit.Builder()
        .baseUrl(mockWebServer.url("/"))
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideGlowUpApi(retrofit: Retrofit): GlowUpApi = retrofit.create(GlowUpApi::class.java)
}
