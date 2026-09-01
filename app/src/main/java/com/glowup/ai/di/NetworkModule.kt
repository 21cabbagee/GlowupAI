package com.glowup.ai.di

import com.glowup.ai.BuildConfig
import com.glowup.ai.data.remote.GlowUpApi
import com.glowup.ai.data.remote.NetworkFactory
import com.glowup.ai.data.remote.NetworkJson
import com.glowup.ai.data.remote.TokenProvider
import com.glowup.ai.feature.auth.FirebaseTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/** Single Hilt-owned network stack for the complete backend API surface. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = NetworkJson

    @Provides
    @Singleton
    fun provideTokenProvider(impl: FirebaseTokenProvider): TokenProvider = impl

    /**
     * Keep timeout, retry, redaction, and auth behavior in NetworkFactory. A second hand-built
     * client silently dropped those policies and made debug behavior differ from the tested stack.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(tokenProvider: TokenProvider): OkHttpClient = NetworkFactory.okHttpClient(tokenProvider, BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = NetworkFactory.retrofit(okHttpClient, json)

    @Provides
    @Singleton
    fun provideGlowUpApi(retrofit: Retrofit): GlowUpApi = retrofit.create(GlowUpApi::class.java)
}
