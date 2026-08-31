package com.glowup.ai.feature.analytics.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.feature.analytics.MetricPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun LineChart(
    points: List<MetricPoint>,
    modifier: Modifier = Modifier,
    label: String = "",
    color: Color = LocalGlowColors.current.honey700,
    showGrid: Boolean = true,
    animated: Boolean = true,
) {
    val glow = LocalGlowColors.current

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(glow.surfaceCard, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600
            )
        }
        return
    }

    var selectedPoint by remember { mutableStateOf<MetricPoint?>(null) }

    // Animation progress
    val animationProgress = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "chart")
        var targetProgress by remember { mutableStateOf(0f) }

        LaunchedEffect(Unit) {
            targetProgress = 1f
        }

        animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(1000, easing = EaseOutCubic),
            label = "progress"
        ).value
    } else {
        1f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(glow.surfaceCard, RoundedCornerShape(12.dp))
            .padding(GlowSpacing.md)
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
                modifier = Modifier.padding(bottom = GlowSpacing.sm)
            )
        }

        // Selected point info
        selectedPoint?.let { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glow.honey300.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(GlowSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = point.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink900
                )
                Text(
                    text = String.format("%.2f", point.value),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900
                )
            }
            Spacer(modifier = Modifier.height(GlowSpacing.sm))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Find nearest point
                        val chartWidth = size.width.toFloat()
                        val spacing = chartWidth / (points.size - 1).coerceAtLeast(1)
                        val index = (offset.x / spacing).toInt().coerceIn(0, points.lastIndex)
                        selectedPoint = points.getOrNull(index)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val padding = 40f
            val chartHeight = height - padding * 2
            val chartWidth = width - padding * 2

            // Calculate min/max for scaling
            val minValue = points.minOfOrNull { it.value } ?: 0.0
            val maxValue = points.maxOfOrNull { it.value } ?: 1.0
            val valueRange = (maxValue - minValue).coerceAtLeast(0.01)

            // Draw grid
            if (showGrid) {
                val gridColor = Color.Gray.copy(alpha = 0.2f)
                for (i in 0..4) {
                    val y = padding + chartHeight * i / 4
                    drawLine(
                        color = gridColor,
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }

            // Calculate points positions
            val pointPositions = points.mapIndexed { index, point ->
                val x = padding + (chartWidth * index / (points.size - 1).coerceAtLeast(1))
                val normalizedValue = ((point.value - minValue) / valueRange).toFloat()
                val y = padding + chartHeight * (1 - normalizedValue)
                Offset(x, y)
            }

            // Draw line with animation
            if (pointPositions.size > 1) {
                val path = Path().apply {
                    val visiblePoints = (pointPositions.size * animationProgress).toInt().coerceAtLeast(2)
                    moveTo(pointPositions[0].x, pointPositions[0].y)

                    for (i in 1 until visiblePoints) {
                        lineTo(pointPositions[i].x, pointPositions[i].y)
                    }
                }

                // Draw gradient fill
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.05f)
                    ),
                    startY = padding,
                    endY = height - padding
                )

                val fillPath = Path().apply {
                    addPath(path)
                    val lastVisibleIndex = (pointPositions.size * animationProgress).toInt().coerceIn(1, pointPositions.lastIndex)
                    lineTo(pointPositions[lastVisibleIndex].x, height - padding)
                    lineTo(pointPositions[0].x, height - padding)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = gradient
                )

                // Draw line
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw points
            val visiblePoints = (pointPositions.size * animationProgress).toInt()
            pointPositions.take(visiblePoints).forEachIndexed { index, position ->
                val isSelected = points[index] == selectedPoint
                val radius = if (isSelected) 8f else 5f

                drawCircle(
                    color = Color.White,
                    radius = radius + 2f,
                    center = position
                )
                drawCircle(
                    color = color,
                    radius = radius,
                    center = position
                )
            }

            // Draw axis labels
            val textPaint = android.graphics.Paint().apply {
                textSize = 24f
                this.color = android.graphics.Color.GRAY
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // Y-axis labels
            for (i in 0..4) {
                val value = minValue + (valueRange * (4 - i) / 4)
                val y = padding + chartHeight * i / 4
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f", value),
                    padding / 2,
                    y + 8f,
                    textPaint
                )
            }

            // X-axis labels (show first, middle, last date)
            if (points.size > 2) {
                val formatter = DateTimeFormatter.ofPattern("MMM dd")
                listOf(0, points.size / 2, points.size - 1).forEach { index ->
                    val point = points[index]
                    val position = pointPositions[index]
                    drawContext.canvas.nativeCanvas.drawText(
                        point.date.format(formatter),
                        position.x,
                        height - padding / 3,
                        textPaint
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonChart(
    beforePoints: List<MetricPoint>,
    afterPoints: List<MetricPoint>,
    modifier: Modifier = Modifier,
    label: String = "Before vs After",
) {
    val glow = LocalGlowColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(glow.surfaceCard, RoundedCornerShape(12.dp))
            .padding(GlowSpacing.md)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            modifier = Modifier.padding(bottom = GlowSpacing.sm)
        )

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md)
        ) {
            LegendItem(color = glow.honey700, label = "Before")
            LegendItem(color = glow.success, label = "After")
        }

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        Box {
            if (beforePoints.isNotEmpty()) {
                LineChart(
                    points = beforePoints,
                    color = glow.honey700,
                    showGrid = true,
                    animated = false
                )
            }
            if (afterPoints.isNotEmpty()) {
                LineChart(
                    points = afterPoints,
                    color = glow.success,
                    showGrid = false,
                    animated = false
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalGlowColors.current.ink600
        )
    }
}
