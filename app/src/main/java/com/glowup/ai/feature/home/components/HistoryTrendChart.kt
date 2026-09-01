package com.glowup.ai.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.feature.home.chartableMetrics
import com.glowup.ai.feature.home.formatMetricValue
import com.glowup.ai.feature.home.label
import com.glowup.ai.feature.home.valueFor
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Ports the geometry from `backend/web/components/charts.tsx`'s `TrendChart`/`Sparkline` to
 * Compose `Canvas`: a padded domain (18% either side, or a synthetic band when the series is
 * flat), gridlines at lo/mid/hi, a baseline-capture ring marker, nearest-point hit testing on
 * tap/drag, and the "needs 2+ captures" fallback — plus an accessible table alternative for
 * TalkBack, since the web client has one and a `<Canvas>` alone has no text content at all.
 *
 * `model_version` and `noise_floor` are always shown alongside the chart (task 3.3 deliverable
 * #6 / frontend-api-map.md `GET /history`: "Show model version and noise floor wherever a verdict
 * or trend could be mistaken for medical certainty").
 */
@Composable
fun HistoryTrendSection(
    modifier: Modifier = Modifier,
    history: List<HistoryItem>,
    selectedMetric: PrimaryMetric,
    onMetricSelected: (PrimaryMetric) -> Unit,
    onCaptureAgain: () -> Unit,
    captureEnabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "History trend")

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chartableMetrics.forEach { metric ->
                GlowButton(
                    text = metric.label(),
                    onClick = { onMetricSelected(metric) },
                    variant = if (metric == selectedMetric) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                    contentDescription = "${metric.label()}${if (metric == selectedMetric) ", selected" else ""}",
                )
            }
        }

        val chartHistory = history.filter { it.valueFor(selectedMetric) != null }
        when {
            !captureEnabled -> {
                EmptyState(
                    modifier = Modifier.padding(top = 16.dp),
                    title = "Photo tracking is off",
                    body = "Re-enable facial-photo consent in Account → Data & Privacy to collect new readings.",
                    ctaLabel = "Capture unavailable",
                    onCtaClick = onCaptureAgain,
                    enabled = false,
                )
            }

            history.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.padding(top = 16.dp),
                    title = "No captures yet",
                    body = "Take your first guided capture to start a history.",
                    ctaLabel = "Capture now",
                    onCtaClick = onCaptureAgain,
                )
            }

            chartHistory.size < 2 -> {
                EmptyState(
                    modifier = Modifier.padding(top = 16.dp),
                    title = "Not enough ${selectedMetric.label().lowercase()} readings",
                    body = "Capture at least two comparable readings to see how this metric is changing.",
                    ctaLabel = "Capture again",
                    onCtaClick = onCaptureAgain,
                )
            }

            else -> {
                TrendChartCanvas(
                    modifier = Modifier.padding(top = 16.dp),
                    history = chartHistory,
                    metric = selectedMetric,
                )
                val latest = chartHistory.lastOrNull()
                val noiseFloor = latest?.noiseFloor?.get(selectedMetric.toWire())
                Text(
                    text =
                        buildString {
                            append(latest?.modelVersion?.let { "Model $it" } ?: "Model version unknown")
                            if (noiseFloor != null) append(" · noise floor ±${formatMetricValue(selectedMetric, noiseFloor)}")
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalGlowColors.current.ink600,
                    modifier = Modifier.padding(top = 8.dp),
                )
                DisclaimerNote(
                    modifier = Modifier.padding(top = 10.dp),
                    text = "Cosmetic tracking only — small changes may be capture noise, not a real change in your skin.",
                )
                HistoryAccessibleTable(
                    modifier = Modifier.padding(top = 16.dp),
                    history = history,
                )
            }
        }
    }
}

/** Nice-looking padded domain; a flat series still gets a visible band — ported 1:1 from
 * `charts.tsx`'s `domain()`. */
private fun paddedDomain(values: List<Double>): Pair<Double, Double> {
    val lo = values.min()
    val hi = values.max()
    if (lo == hi) {
        val pad = if (abs(lo) * 0.1 > 0.0) abs(lo) * 0.1 else 0.5
        return (lo - pad) to (hi + pad)
    }
    val pad = (hi - lo) * 0.18
    return (lo - pad) to (hi + pad)
}

private fun parseEpochMillis(iso: String): Long =
    runCatching {
        val formats = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")
        for (pattern in formats) {
            val parsed =
                runCatching {
                    SimpleDateFormat(pattern, Locale.US).parse(iso.take(pattern.length.coerceAtMost(iso.length)))
                }.getOrNull()
            if (parsed != null) return parsed.time
        }
        0L
    }.getOrDefault(0L)

private fun formatShortDate(iso: String): String =
    runCatching {
        val date = java.util.Date(parseEpochMillis(iso))
        SimpleDateFormat("MMM d", Locale.US).format(date)
    }.getOrDefault(iso.take(10))

@Composable
private fun TrendChartCanvas(
    modifier: Modifier = Modifier,
    history: List<HistoryItem>,
    metric: PrimaryMetric,
) {
    val glow = LocalGlowColors.current
    var selectedIndex by remember(metric, history) { mutableStateOf<Int?>(null) }

    val values = history.map { it.valueFor(metric) ?: 0.0 }
    val (lo, hi) = paddedDomain(values)
    val ticks = listOf(lo, lo + (hi - lo) / 2, hi)

    val active = selectedIndex?.let { history.getOrNull(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .semantics {
                        contentDescription = "${metric.label()} over ${history.size} captures. " +
                            "A table with the exact values follows this chart."
                    }.pointerInput(history, metric) {
                        fun nearestIndex(x: Float): Int {
                            val innerW = size.width - MARGIN_LEFT_PX - MARGIN_RIGHT_PX
                            var best = 0
                            var bestDist = Float.MAX_VALUE
                            history.indices.forEach { i ->
                                val px = xFor(i, history.size, MARGIN_LEFT_PX, innerW)
                                val dist = abs(px - x)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    best = i
                                }
                            }
                            return best
                        }
                        detectTapGestures(onPress = { offset -> selectedIndex = nearestIndex(offset.x) })
                    }.pointerInput(history, metric) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val innerW = size.width - MARGIN_LEFT_PX - MARGIN_RIGHT_PX
                                var best = 0
                                var bestDist = Float.MAX_VALUE
                                history.indices.forEach { i ->
                                    val px = xFor(i, history.size, MARGIN_LEFT_PX, innerW)
                                    val dist = abs(px - change.position.x)
                                    if (dist < bestDist) {
                                        bestDist = dist
                                        best = i
                                    }
                                }
                                selectedIndex = best
                            },
                        )
                    },
        ) {
            val innerW = size.width - MARGIN_LEFT_PX - MARGIN_RIGHT_PX
            val innerH = size.height - MARGIN_TOP_PX - MARGIN_BOTTOM_PX

            fun yFor(value: Double): Float = (MARGIN_TOP_PX + innerH - ((value - lo) / (hi - lo) * innerH)).toFloat()

            // Gridlines at lo / mid / hi.
            ticks.forEach { tick ->
                val y = yFor(tick)
                drawLine(
                    color = glow.chartGrid,
                    start = Offset(MARGIN_LEFT_PX, y),
                    end = Offset(size.width - MARGIN_RIGHT_PX, y),
                    strokeWidth = 1f,
                )
            }

            val points = history.indices.map { i -> Offset(xFor(i, history.size, MARGIN_LEFT_PX, innerW), yFor(values[i])) }

            // Area fill.
            val fillPath =
                androidx.compose.ui.graphics.Path().apply {
                    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                    lineTo(points.last().x, size.height - MARGIN_BOTTOM_PX)
                    lineTo(points.first().x, size.height - MARGIN_BOTTOM_PX)
                    close()
                }
            drawPath(fillPath, color = glow.chartFill)

            // Line.
            val linePath =
                androidx.compose.ui.graphics.Path().apply {
                    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                }
            drawPath(linePath, color = glow.chartLine, style = Stroke(width = 5f, cap = StrokeCap.Round))

            // Baseline capture ring markers.
            history.forEachIndexed { i, item ->
                if (item.isBaseline) {
                    drawCircle(
                        color = glow.paper,
                        radius = 9f,
                        center = points[i],
                    )
                    drawCircle(
                        color = glow.chartLine,
                        radius = 9f,
                        center = points[i],
                        style = Stroke(width = 4f),
                    )
                }
            }

            // Crosshair for the tapped/dragged point.
            selectedIndex?.let { index ->
                val point = points.getOrNull(index) ?: return@let
                drawLine(
                    color = glow.chartLine,
                    start = Offset(point.x, MARGIN_TOP_PX),
                    end = Offset(point.x, size.height - MARGIN_BOTTOM_PX),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
                drawCircle(color = glow.chartLine, radius = 8f, center = point)
            }
        }

        Text(
            text =
                if (active != null) {
                    "${formatShortDate(active.capturedAt)} · ${formatMetricValue(metric, active.valueFor(metric) ?: 0.0)}" +
                        " · ${active.confidenceLabel ?: "directional comparison"}"
                } else {
                    "Tap or drag the chart for the capture behind a point."
                },
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private const val MARGIN_LEFT_PX = 48f
private const val MARGIN_RIGHT_PX = 16f
private const val MARGIN_TOP_PX = 12f
private const val MARGIN_BOTTOM_PX = 12f

private fun xFor(
    index: Int,
    count: Int,
    marginLeft: Float,
    innerW: Float,
): Float = if (count <= 1) marginLeft + innerW / 2f else marginLeft + (index.toFloat() / (count - 1)) * innerW

/**
 * Accessible table alternative to the canvas chart — the web client keeps one alongside its SVG
 * chart specifically for screen readers (`backend/web/components/charts.tsx`'s `HistoryTable`);
 * a `Canvas` has no text content at all, so this is the only way a TalkBack user gets the values.
 */
@Composable
fun HistoryAccessibleTable(
    modifier: Modifier = Modifier,
    history: List<HistoryItem>,
) {
    val glow = LocalGlowColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    contentDescription = "Table header: date, redness, blemishes, dark spots, texture, reading quality"
                },
        ) {
            TableCell("Date", weight = 1.2f, header = true)
            TableCell("Redness", weight = 1f, header = true)
            TableCell("Blemish", weight = 1f, header = true)
            TableCell("Spots", weight = 1f, header = true)
            TableCell("Texture", weight = 1f, header = true)
            TableCell("Quality", weight = 1.2f, header = true)
        }
        history.asReversed().forEach { item ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription =
                                buildString {
                                    append(formatShortDate(item.capturedAt))
                                    if (item.isBaseline) append(", baseline")
                                    append(", redness ${item.rednessScore?.let { String.format("%.3f", it) } ?: "unknown"}")
                                    append(", blemish count ${item.blemishCount?.toInt() ?: "unknown"}")
                                    append(", dark spot area ${item.darkspotArea?.let { String.format("%.3f", it) } ?: "unknown"}")
                                    append(", texture ${item.textureScore?.let { String.format("%.3f", it) } ?: "unknown"}")
                                    append(", reading quality ${item.confidenceLabel ?: "directional"}")
                                }
                        },
            ) {
                TableCell(
                    text = formatShortDate(item.capturedAt) + if (item.isBaseline) " (base)" else "",
                    weight = 1.2f,
                )
                TableCell(item.rednessScore?.let { String.format("%.3f", it) } ?: "—", weight = 1f)
                TableCell(item.blemishCount?.toInt()?.toString() ?: "—", weight = 1f)
                TableCell(item.darkspotArea?.let { String.format("%.3f", it) } ?: "—", weight = 1f)
                TableCell(item.textureScore?.let { String.format("%.3f", it) } ?: "—", weight = 1f)
                TableCell(item.confidenceLabel ?: "directional", weight = 1.2f)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCell(
    text: String,
    weight: Float,
    header: Boolean = false,
) {
    val glow = LocalGlowColors.current
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        color = if (header) glow.ink600 else glow.ink900,
    )
}
