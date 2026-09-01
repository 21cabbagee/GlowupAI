package com.glowup.ai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.glowup.ai.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import com.glowup.ai.data.local.LocalModule

/**
 * Test module that replaces LocalModule for instrumented tests.
 *
 * Provides:
 * - In-memory Room databases for fast, isolated tests
 * - Test DataStore with unique name to avoid conflicts
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [LocalModule::class]
)
object TestLocalModule {
    @Provides
    @Singleton
    fun provideSessionDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("test_glowup_session")
    }

    // -- Cache database (in-memory for tests)
    @Provides
    @Singleton
    fun provideGlowUpDatabase(
        @ApplicationContext context: Context
    ): GlowUpDatabase = Room.inMemoryDatabaseBuilder(
        context,
        GlowUpDatabase::class.java
    )
        .allowMainThreadQueries() // OK for tests
        .build()

    @Provides
    fun provideDashboardCacheDao(db: GlowUpDatabase): DashboardCacheDao = db.dashboardCacheDao()

    @Provides
    fun provideHistoryCacheDao(db: GlowUpDatabase): HistoryCacheDao = db.historyCacheDao()

    @Provides
    fun provideProductCacheDao(db: GlowUpDatabase): ProductCacheDao = db.productCacheDao()

    @Provides
    fun provideRoutineEventCacheDao(db: GlowUpDatabase): RoutineEventCacheDao = db.routineEventCacheDao()

    @Provides
    fun provideExperimentCacheDao(db: GlowUpDatabase): ExperimentCacheDao = db.experimentCacheDao()

    @Provides
    fun provideVerdictCacheDao(db: GlowUpDatabase): VerdictCacheDao = db.verdictCacheDao()

    @Provides
    fun provideOfferCacheDao(db: GlowUpDatabase): OfferCacheDao = db.offerCacheDao()

    @Provides
    fun provideProductDetailCacheDao(db: GlowUpDatabase): ProductDetailCacheDao = db.productDetailCacheDao()

    @Provides
    fun provideEngagementCacheDao(db: GlowUpDatabase): EngagementCacheDao = db.engagementCacheDao()

    @Provides
    fun provideDiscoverCacheDao(db: GlowUpDatabase): DiscoverCacheDao = db.discoverCacheDao()

    @Provides
    fun provideLabelCacheDao(db: GlowUpDatabase): LabelCacheDao = db.labelCacheDao()

    @Provides
    fun provideContextEventCacheDao(db: GlowUpDatabase): ContextEventCacheDao = db.contextEventCacheDao()

    // -- Outbox database (in-memory for tests)
    @Provides
    @Singleton
    fun provideGlowUpOutboxDatabase(
        @ApplicationContext context: Context
    ): GlowUpOutboxDatabase = Room.inMemoryDatabaseBuilder(
        context,
        GlowUpOutboxDatabase::class.java
    )
        .allowMainThreadQueries() // OK for tests
        .build()

    @Provides
    fun provideCaptureOutboxDao(db: GlowUpOutboxDatabase): CaptureOutboxDao = db.captureOutboxDao()
}
