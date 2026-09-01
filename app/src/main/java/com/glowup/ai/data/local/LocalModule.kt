package com.glowup.ai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.glowUpSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "glowup_session")

/**
 * Hilt bindings for everything in `data/local` — DataStore, both Room databases and their DAOs.
 * Owned here (Task 2.4) rather than in the `di` package per `AppModule`'s own doc comment.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Provides
    @Singleton
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.glowUpSessionDataStore

    // -- Cache database (destructive fallback OK — see GlowUpDatabase doc) ----------------------

    @Provides
    @Singleton
    fun provideGlowUpDatabase(
        @ApplicationContext context: Context,
    ): GlowUpDatabase =
        Room
            .databaseBuilder(context, GlowUpDatabase::class.java, "glowup_cache.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
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

    // -- Outbox database (NEVER destructive — see GlowUpOutboxDatabase doc) ---------------------

    @Provides
    @Singleton
    fun provideGlowUpOutboxDatabase(
        @ApplicationContext context: Context,
    ): GlowUpOutboxDatabase =
        Room
            .databaseBuilder(context, GlowUpOutboxDatabase::class.java, "glowup_outbox.db")
            // Deliberately no fallbackToDestructiveMigration() and no addMigrations() yet:
            // version 1 has nothing to migrate from. The next schema bump MUST add a real
            // Migration here instead of reaching for a destructive fallback.
            .build()

    @Provides
    fun provideCaptureOutboxDao(db: GlowUpOutboxDatabase): CaptureOutboxDao = db.captureOutboxDao()
}
