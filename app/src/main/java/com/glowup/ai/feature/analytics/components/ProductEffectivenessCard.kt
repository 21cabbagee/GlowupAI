package com.glowup.ai.feature.analytics.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.feature.analytics.ProductScore

@Composable
fun ProductEffectivenessCard(
    productScore: ProductScore,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    // Animate the progress
    val animatedProgress by animateFloatAsState(
        targetValue = productScore.effectivenessScore.toFloat(),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "effectiveness"
    )

    val trendIcon = when (productScore.trend) {
        "improving" -> Icons.Filled.TrendingUp
        "declining" -> Icons.Filled.TrendingDown
        else -> Icons.Filled.TrendingFlat
    }

    val trendColor = when (productScore.trend) {
        "improving" -> glow.success
        "declining" -> glow.danger
        else -> glow.ink600
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(glow.surfaceCard, RoundedCornerShape(12.dp))
            .padding(GlowSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productScore.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900
                )

                Text(
                    text = "${productScore.dataPoints} captures tracked",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Circular progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = animatedProgress,
                    color = glow.honey700,
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = "${(productScore.effectivenessScore * 100).toInt()}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900
                )
            }
        }

        Spacer(modifier = Modifier.height(GlowSpacing.sm))

        // Metric and trend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    trendColor.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
                .padding(GlowSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = trendIcon,
                contentDescription = null,
                tint = trendColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = productScore.primaryMetric,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink900,
                modifier = Modifier.weight(1f)
            )

            productScore.changePercent?.let { change ->
                Text(
                    text = "${if (change > 0) "+" else ""}${String.format("%.1f", change)}%",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }
        }
    }
}

@Composable
private fun CircularProgressIndicator(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 8f,
) {
    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val radius = (canvasSize - strokeWidth) / 2

        // Background circle
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        // Progress arc
        val sweepAngle = 360f * progress.coerceIn(0f, 1f)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ProductEffectivenessList(
    products: List<ProductScore>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        LocalGlowColors.current.surfaceCard,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(GlowSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No product data available yet.\nStart tracking your routine to see effectiveness scores.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalGlowColors.current.ink600
                )
            }
        } else {
            products.forEach { product ->
                ProductEffectivenessCard(productScore = product)
            }
        }
    }
}
