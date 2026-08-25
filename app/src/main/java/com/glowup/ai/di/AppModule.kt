package com.glowup.ai.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * App-wide bindings that don't belong to network (`NetworkModule`) or dispatchers
 * (`DispatcherModule`).
 *
 * Deliberately thin: persistence (`SessionStore`, Room, WorkManager) belongs to Phase 2.4's
 * `data/local` / `data/work` packages and is expected to bring its own Hilt module(s) rather than
 * being declared here.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
}
