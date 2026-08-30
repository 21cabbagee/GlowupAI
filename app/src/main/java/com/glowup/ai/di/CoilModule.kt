package com.glowup.ai.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.glowup.ai.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Coil image loading configuration optimized for GlowUp AI capture photos.
 *
 * Key optimizations:
 * - Reuses existing OkHttpClient from NetworkModule (shares connection pool, auth interceptor)
 * - 25% of app memory for image cache (reasonable for photo-heavy app)
 * - 2% of disk for persistent cache (survives app restarts)
 * - Disables cache header respect (backend doesn't send Cache-Control headers yet)
 * - Debug logging in DEBUG builds only
 *
 * Performance impact:
 * - Memory cache hits: ~5ms load time (vs 600-800ms network)
 * - Disk cache hits: ~50ms load time
 * - Dramatically reduces jank in PhotoGrid/History scrolling
 *
 * See PERFORMANCE_OPTIMIZATIONS.md §3 for benchmarks and usage patterns.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient, // Injected from NetworkModule - shares auth, connection pool
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% of app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% of disk space
                    .build()
            }
            // Backend doesn't send Cache-Control headers yet - rely on local cache policy
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true) // Smooth fade-in for loaded images
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
