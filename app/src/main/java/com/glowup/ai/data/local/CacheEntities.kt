package com.glowup.ai.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Cache entities for [GlowUpDatabase]. Every one stores its payload as a single JSON-blob column
 * (`json`) encoded with the SAME `NetworkJson` instance / DTO `@Serializable` shapes the network
 * layer already uses (see each repository's `toEntity`/`fromEntity`) — this file intentionally
 * does not re-model every nested field as SQL columns, only the columns needed to key and
 * invalidate a row:
 *
 * - `userId`/`plan`/`vertical` implement the "cache keyed by {user_id, plan}" rule from
 *   frontend-api-map.md trap #7 — a plan change must miss the cache, never serve stale free-tier
 *   (or stale Premium) data.
 * - `valid` is the persisted half of the offline-first staleness marker: `false` means "this row
 *   is a known-stale cache entry invalidated by a mutation elsewhere" and callers must show a
 *   "showing cached data" affordance while a background refresh is in flight. It is NOT a TTL —
 *   dashboard/engagement rows are never invalidated by time, only by the explicit trigger list in
 *   trap #7 (never poll either endpoint).
 */

@Entity(tableName = "dashboard_cache")
data class DashboardCacheEntity(
    /** "<userId>:<plan>:<vertical>" */
    @PrimaryKey val cacheKey: String,
    val userId: String,
    val plan: String,
    val vertical: String,
    val json: String,
    val fetchedAtMillis: Long,
    val valid: Boolean,
)

@Dao
interface DashboardCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DashboardCacheEntity)

    @Query("SELECT * FROM dashboard_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): DashboardCacheEntity?

    @Query("UPDATE dashboard_cache SET valid = 0 WHERE userId = :userId")
    suspend fun invalidateForUser(userId: String)

    @Query("DELETE FROM dashboard_cache WHERE userId = :userId AND plan != :currentPlan")
    suspend fun dropOtherPlans(userId: String, currentPlan: String)

    @Query("DELETE FROM dashboard_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Entity(tableName = "history_cache")
data class HistoryCacheEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val vertical: String,
    val capturedAt: String,
    val json: String,
    val fetchedAtMillis: Long,
    val valid: Boolean = true,
)

@Dao
interface HistoryCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HistoryCacheEntity>)

    @Query("SELECT * FROM history_cache WHERE userId = :userId AND vertical = :vertical ORDER BY capturedAt DESC")
    suspend fun forUser(userId: String, vertical: String): List<HistoryCacheEntity>

    @Query("UPDATE history_cache SET valid = 0 WHERE userId = :userId AND vertical = :vertical")
    suspend fun invalidateForUser(userId: String, vertical: String)

    @Query("DELETE FROM history_cache WHERE userId = :userId AND vertical = :vertical")
    suspend fun clearForUser(userId: String, vertical: String)

    @Query("DELETE FROM history_cache WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

/** Product rows are GLOBAL, not per-user (frontend-api-map.md trap: `POST /api/products` takes no
 * `user_id`) — this table has no `userId` column on purpose. */
@Entity(tableName = "product_cache")
data class ProductCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val barcode: String? = null,
    val json: String,
    val fetchedAtMillis: Long,
)

@Dao
interface ProductCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProductCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ProductCacheEntity>)

    @Query("SELECT * FROM product_cache WHERE id = :id")
    suspend fun get(id: String): ProductCacheEntity?

    @Query("SELECT * FROM product_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductCacheEntity?

    @Query("SELECT * FROM product_cache WHERE name LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<ProductCacheEntity>

    @Query("DELETE FROM product_cache")
    suspend fun clearAll()
}

@Entity(tableName = "product_detail_cache")
data class ProductDetailCacheEntity(
    @PrimaryKey val cacheKey: String,
    val productId: String,
    val userId: String,
    val plan: String,
    val json: String,
    val fetchedAtMillis: Long,
)

@Dao
interface ProductDetailCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProductDetailCacheEntity)

    @Query("SELECT * FROM product_detail_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): ProductDetailCacheEntity?

    @Query("DELETE FROM product_detail_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM product_detail_cache")
    suspend fun clearAll()
}

@Entity(tableName = "routine_event_cache")
data class RoutineEventCacheEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String,
    val json: String,
    val fetchedAtMillis: Long,
)

@Dao
interface RoutineEventCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<RoutineEventCacheEntity>)

    @Query("SELECT * FROM routine_event_cache WHERE userId = :userId ORDER BY fetchedAtMillis DESC")
    fun forUserFlow(userId: String): Flow<List<RoutineEventCacheEntity>>

    @Query("DELETE FROM routine_event_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Entity(tableName = "experiment_cache")
data class ExperimentCacheEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val status: String,
    val plan: String,
    val json: String,
    val fetchedAtMillis: Long,
    val valid: Boolean,
)

@Dao
interface ExperimentCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExperimentCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExperimentCacheEntity>)

    @Query("SELECT * FROM experiment_cache WHERE userId = :userId AND plan = :plan AND valid = 1")
    suspend fun forUser(userId: String, plan: String): List<ExperimentCacheEntity>

    @Query("SELECT * FROM experiment_cache WHERE id = :id")
    suspend fun get(id: String): ExperimentCacheEntity?

    @Query("SELECT * FROM experiment_cache WHERE id = :id AND plan = :plan AND valid = 1")
    suspend fun getForPlan(id: String, plan: String): ExperimentCacheEntity?

    @Query("UPDATE experiment_cache SET valid = 0 WHERE userId = :userId")
    suspend fun invalidateForUser(userId: String)

    @Query("DELETE FROM experiment_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

/** [Verdict][com.glowup.ai.domain.model.Verdict] has no server id, so rows are keyed by an
 * autogenerated local id plus the `{userId, plan, vertical}` triple used to select them. */
@Entity(tableName = "verdict_cache")
data class VerdictCacheEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val userId: String,
    val plan: String,
    val vertical: String,
    val json: String,
    val fetchedAtMillis: Long,
    val valid: Boolean,
)

@Dao
interface VerdictCacheDao {
    @Insert
    suspend fun insertAll(entities: List<VerdictCacheEntity>)

    @Query("SELECT * FROM verdict_cache WHERE userId = :userId AND plan = :plan AND vertical = :vertical AND valid = 1")
    suspend fun current(userId: String, plan: String, vertical: String): List<VerdictCacheEntity>

    @Query("DELETE FROM verdict_cache WHERE userId = :userId AND vertical = :vertical")
    suspend fun clearForUser(userId: String, vertical: String)

    @Query("UPDATE verdict_cache SET valid = 0 WHERE userId = :userId")
    suspend fun invalidateForUser(userId: String)

    @Query("DELETE FROM verdict_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Entity(tableName = "offer_cache")
data class OfferCacheEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String?,
    val json: String,
    val fetchedAtMillis: Long,
)

@Dao
interface OfferCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OfferCacheEntity>)

    @Query("SELECT * FROM offer_cache WHERE userId = :userId")
    suspend fun forUser(userId: String): List<OfferCacheEntity>

    @Query("DELETE FROM offer_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Entity(tableName = "engagement_cache")
data class EngagementCacheEntity(
    @PrimaryKey val cacheKey: String,
    val userId: String,
    val plan: String,
    val json: String,
    val fetchedAtMillis: Long,
    val valid: Boolean,
)

@Dao
interface EngagementCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EngagementCacheEntity)

    @Query("SELECT * FROM engagement_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): EngagementCacheEntity?

    @Query("UPDATE engagement_cache SET valid = 0 WHERE userId = :userId")
    suspend fun invalidateForUser(userId: String)

    @Query("DELETE FROM engagement_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Entity(tableName = "discover_cache")
data class DiscoverCacheEntity(
    @PrimaryKey val cacheKey: String,
    val userId: String,
    val plan: String,
    val json: String,
    val fetchedAtMillis: Long,
    val valid: Boolean,
)

@Dao
interface DiscoverCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DiscoverCacheEntity)

    @Query("SELECT * FROM discover_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): DiscoverCacheEntity?

    @Query("DELETE FROM discover_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Entity(tableName = "label_cache")
data class LabelCacheEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val json: String,
    val fetchedAtMillis: Long,
)

@Dao
interface LabelCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LabelCacheEntity>)

    @Query("SELECT * FROM label_cache WHERE userId = :userId ORDER BY fetchedAtMillis DESC")
    suspend fun forUser(userId: String): List<LabelCacheEntity>

    @Query("DELETE FROM label_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Entity(tableName = "context_event_cache")
data class ContextEventCacheEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val occurredAt: String,
    val json: String,
    val fetchedAtMillis: Long,
)

@Dao
interface ContextEventCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ContextEventCacheEntity>)

    @Query("SELECT * FROM context_event_cache WHERE userId = :userId ORDER BY occurredAt DESC")
    suspend fun forUser(userId: String): List<ContextEventCacheEntity>

    @Query("DELETE FROM context_event_cache WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
