package com.glowup.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The capture outbox lives in its OWN Room database, separate from [GlowUpDatabase], for exactly
 * one reason: [GlowUpDatabase] is allowed to use `fallbackToDestructiveMigration` because it is a
 * pure cache, and Room's destructive-migration fallback is database-wide, not per-table. Keeping
 * the outbox physically separate means a future cache schema change can never accidentally wipe a
 * queued, not-yet-uploaded capture.
 *
 * Migration policy for THIS database: NEVER `fallbackToDestructiveMigration`. Any future schema
 * change here must ship a real `Migration` object added to `LocalModule`'s
 * `Room.databaseBuilder(...).addMigrations(...)` call. Version 1 has no prior version to migrate
 * from, so the migration list starts empty — that is not a license to add destructive fallback
 * later.
 */
@Database(
    entities = [CaptureOutboxEntity::class],
    version = 1,
    // See GlowUpDatabase's identical comment — flip to true once `room.schemaLocation` is
    // configured in app/build.gradle.kts (owned by Task 0.3).
    exportSchema = false,
)
abstract class GlowUpOutboxDatabase : RoomDatabase() {
    abstract fun captureOutboxDao(): CaptureOutboxDao
}
