package com.glowup.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Pure CACHE database: dashboard snapshot, history/captures, products, routine events,
 * experiments, verdicts, offers. Every row here is a copy of something the backend remains the
 * source of truth for and can always re-derive from a fresh `GET`.
 *
 * Migration strategy: [fallbackToDestructiveMigration] is deliberately used for THIS database in
 * `LocalModule` — losing a schema-mismatched cache on an app upgrade is harmless, the next screen
 * visit just refetches and repopulates it. This is intentionally NOT true of
 * [GlowUpOutboxDatabase]: the capture outbox holds locally-queued, not-yet-uploaded user data, so
 * it is a SEPARATE Room database specifically so a destructive migration here can never silently
 * delete a pending capture. Do not merge the outbox table into this database.
 */
@Database(
    entities = [
        DashboardCacheEntity::class,
        HistoryCacheEntity::class,
        ProductCacheEntity::class,
        RoutineEventCacheEntity::class,
        ExperimentCacheEntity::class,
        VerdictCacheEntity::class,
        OfferCacheEntity::class,
        ProductDetailCacheEntity::class,
        EngagementCacheEntity::class,
        DiscoverCacheEntity::class,
        LabelCacheEntity::class,
        ContextEventCacheEntity::class,
    ],
    version = 2, // Bumped from 1 to 2 for index additions (PERFORMANCE_OPTIMIZATIONS.md §4)
    // `false` because the Task 0.3 build-config agent owns app/build.gradle.kts and
    // `room.schemaLocation` is not wired there yet; true (with an unconfigured location) is a
    // noisy KSP warning, not a hard error, but there's no reason to ship it. Flip to `true` once
    // that ksp arg exists.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GlowUpDatabase : RoomDatabase() {
    abstract fun dashboardCacheDao(): DashboardCacheDao

    abstract fun historyCacheDao(): HistoryCacheDao

    abstract fun productCacheDao(): ProductCacheDao

    abstract fun routineEventCacheDao(): RoutineEventCacheDao

    abstract fun experimentCacheDao(): ExperimentCacheDao

    abstract fun verdictCacheDao(): VerdictCacheDao

    abstract fun offerCacheDao(): OfferCacheDao

    abstract fun productDetailCacheDao(): ProductDetailCacheDao

    abstract fun engagementCacheDao(): EngagementCacheDao

    abstract fun discoverCacheDao(): DiscoverCacheDao

    abstract fun labelCacheDao(): LabelCacheDao

    abstract fun contextEventCacheDao(): ContextEventCacheDao
}
