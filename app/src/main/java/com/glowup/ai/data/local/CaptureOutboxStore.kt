package com.glowup.ai.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A capture taken while offline (or whose upload failed with an ambiguous network error) queues
 * here instead of being dropped. [com.glowup.ai.data.work.CaptureUploadWorker] drains this table.
 *
 * `status`:
 * - `pending` — safe to attempt (or re-attempt) an upload.
 * - `failed_permanent` — the server explicitly rejected this exact payload (quality/validation);
 *   never auto-retried again, surfaced to the user for a manual retake/discard.
 *
 * A row is deleted ONLY on confirmed success or confirmed prior acceptance (see
 * `com.glowup.ai.data.work.CaptureOutboxProcessor` — reconciliation-before-retry is what keeps a
 * duplicate accepted capture from ever reaching the server twice). It is never removed merely
 * because an upload attempt was *made*.
 */
@Entity(tableName = "capture_outbox")
data class CaptureOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val vertical: String,
    /** Path to a local file holding the (already downscaled, per ANDROID_PLAN.md 3.2) base64
     * image payload — the outbox never keeps the image bytes in the SQLite row itself. */
    val imagePath: String,
    /** Serialized `CaptureQualityInputDto` (client-measured pose) — null if the capture screen
     * had no preflight reading. */
    val qualityJson: String?,
    val isBaseline: Boolean,
    val experimentId: String?,
    /** Client-stamped capture timestamp. Doubles as the reconciliation key: if a retry finds a
     * history item at this same `capturedAt` for this user/vertical, the earlier attempt must
     * have already been accepted server-side, and this row is dropped instead of re-uploaded. */
    val capturedAt: String,
    val deviceMetaJson: String?,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val status: String = "pending",
    val createdAtMillis: Long,
)

@Dao
interface CaptureOutboxDao {
    @Insert
    suspend fun insert(entity: CaptureOutboxEntity): Long

    @Update
    suspend fun update(entity: CaptureOutboxEntity)

    @Query("DELETE FROM capture_outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM capture_outbox WHERE status = 'pending' ORDER BY createdAtMillis ASC")
    suspend fun pending(): List<CaptureOutboxEntity>

    @Query("SELECT * FROM capture_outbox ORDER BY createdAtMillis ASC")
    fun allFlow(): Flow<List<CaptureOutboxEntity>>

    @Query("SELECT COUNT(*) FROM capture_outbox WHERE status = 'pending'")
    fun pendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM capture_outbox WHERE userId = :userId")
    suspend fun forUser(userId: String): List<CaptureOutboxEntity>

    @Query("DELETE FROM capture_outbox WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    @Query("DELETE FROM capture_outbox WHERE id = :id")
    suspend fun discard(id: Long)
}

/**
 * Persists a capture's base64 payload to a private file so [CaptureOutboxEntity] rows stay small.
 * Files live under `filesDir/capture_outbox/` and are deleted once the outbox row that references
 * them is removed (uploaded, confirmed-already-accepted, or the user discards a permanently
 * failed one).
 */
@Singleton
class CaptureImageStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val dir: File by lazy {
        File(context.filesDir, "capture_outbox").apply { mkdirs() }
    }

    suspend fun save(base64: String): String = withContext(Dispatchers.IO) {
        val file = File(dir, "${UUID.randomUUID()}.b64")
        file.writeText(base64)
        file.absolutePath
    }

    suspend fun read(path: String): String = withContext(Dispatchers.IO) {
        File(path).readText()
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        runCatching { File(path).delete() }
        Unit
    }

    suspend fun deleteAll(paths: Iterable<String>) = withContext(Dispatchers.IO) {
        paths.forEach { path -> runCatching { File(path).delete() } }
    }
}
