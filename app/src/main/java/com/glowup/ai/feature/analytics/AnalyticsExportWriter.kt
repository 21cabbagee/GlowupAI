package com.glowup.ai.feature.analytics

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.glowup.ai.domain.model.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object AnalyticsExportWriter {
    suspend fun write(context: Context, history: List<HistoryItem>, pdf: Boolean): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(directory, "glowup-analytics-${UUID.randomUUID()}.${if (pdf) "pdf" else "csv"}")
        val rows = history.map { item -> listOf(
            item.capturedAt, item.rednessScore?.toString().orEmpty(), item.blemishCount?.toString().orEmpty(),
            item.darkspotArea?.toString().orEmpty(), item.textureScore?.toString().orEmpty(), item.confidence?.toString().orEmpty(),
        ) }
        if (pdf) {
            val document = PdfDocument()
            try {
                val paint = Paint().apply { textSize = 10f; typeface = Typeface.MONOSPACE }
                rows.chunked(32).ifEmpty { listOf(emptyList()) }.forEachIndexed { index, pageRows ->
                    val page = document.startPage(PdfDocument.PageInfo.Builder(842, 595, index + 1).create())
                    page.canvas.drawText("GlowUp AI capture history — page ${index + 1}", 32f, 35f, paint)
                    page.canvas.drawText("Cosmetic tracking only. Not a diagnosis. Blank values mean unavailable.", 32f, 55f, paint)
                    page.canvas.drawText("Captured at                        Redness   Blemishes   Dark spots   Texture   Confidence", 32f, 85f, paint)
                    pageRows.forEachIndexed { rowIndex, row ->
                        val line = row.mapIndexed { column, value -> value.take(if (column == 0) 32 else 10).padEnd(if (column == 0) 35 else 12) }.joinToString("")
                        page.canvas.drawText(line, 32f, 105f + rowIndex * 14f, paint)
                    }
                    document.finishPage(page)
                }
                file.outputStream().use(document::writeTo)
            } finally { document.close() }
        } else {
            file.bufferedWriter().use { writer ->
                writer.appendLine("captured_at,redness_score,blemish_count,darkspot_area,texture_score,confidence")
                rows.forEach { row -> writer.appendLine(row.joinToString(",") { csvCell(it) }) }
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    internal fun csvCell(value: String): String {
        val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}
