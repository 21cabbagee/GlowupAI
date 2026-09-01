package com.glowup.ai.di

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.OkHttpClient
import javax.inject.Singleton
import com.glowup.ai.di.CoilModule

/**
 * Test module that replaces CoilModule for instrumented tests.
 *
 * Provides a simplified ImageLoader without disk caching to avoid
 * file system operations in tests.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoilModule::class]
)
object TestCoilModule {
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.10) // Smaller cache for tests
                .build()
        }
        // No disk cache for tests
        .build()
}
