package com.glowup.ai.feature.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.glowup.ai.data.remote.NetworkJson
import com.glowup.ai.domain.model.ExportBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Turns a `GET /export` [ExportBundle] into a shareable file.
 *
 * frontend-api-map.md is explicit: "Do not put the export in a shared cache or log its
 * contents." This writer never logs the bundle, and it deliberately does NOT use
 * [Context.getCacheDir] or any Room/[com.glowup.ai.data.repository.support.KeyedMemoryCache]
 * layer — it writes straight to a private `filesDir/exports/` directory that nothing else in
 * the app reads from, clears any previous export before writing a new one (so at most one copy
 * ever exists on disk), and only ever becomes reachable by another app for the lifetime of the
 * single share [Intent] the user explicitly triggers, via a scoped [FileProvider] grant — never
 * a raw `file://` Uri, external storage, or MediaStore entry.
 *
 * Raw photo bytes are never part of this file: the backend response itself does not embed them
 * (see [ExportBundle] and the `GET /export` contract), and this writer only ever serializes what
 * the response already contained.
 */
private val PrettyExportJson = Json(NetworkJson) { prettyPrint = true }

object ExportFileWriter {
    /** Writes [bundle] to `filesDir/exports/glowup-export-<timestamp>.json` and returns a
     * content:// [Uri] usable in a share [Intent]. Runs off the main thread. */
    suspend fun write(
        context: Context,
        userId: String,
        bundle: ExportBundle,
    ): Uri =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "exports").apply { mkdirs() }
            // Only one export file ever exists on disk at a time.
            dir.listFiles()?.forEach { it.delete() }

            val stamp =
                SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                    .apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date())
            val file = File(dir, "glowup-export-$stamp.json")

            val json =
                buildJsonObject {
                    put("export_version", bundle.exportVersion)
                    put("exported_at", bundle.exportedAt)
                    put("profile", bundle.profile ?: JsonNull)
                    put("consent_events", bundle.consentEvents ?: JsonNull)
                    put("appearance_profiles", bundle.appearanceProfiles ?: JsonNull)
                    put("routine_events", bundle.routineEvents ?: JsonNull)
                    put("experiments", bundle.experiments ?: JsonNull)
                    put("captures_and_metrics", bundle.capturesAndMetrics ?: JsonNull)
                    put("appearance_captures", bundle.appearanceCaptures ?: JsonNull)
                    put("verdicts", bundle.verdicts ?: JsonNull)
                    put("qna", bundle.qna ?: JsonNull)
                    put("engagement", bundle.engagement ?: JsonNull)
                    put("note", bundle.note)
                    put(
                        "_client_note",
                        "Raw photo bytes are not included in this export. GlowUp AI user id: $userId.",
                    )
                }
            file.writeText(
                PrettyExportJson.encodeToString(
                    kotlinx.serialization.json.JsonElement
                        .serializer(),
                    json,
                ),
            )

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    /** A chooser [Intent] for the file at [uri]. Caller starts it via
     * `context.startActivity(shareIntent(...))`. */
    fun shareIntent(uri: Uri): Intent {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "My GlowUp AI data export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(send, "Save or share your data export")
    }
}
