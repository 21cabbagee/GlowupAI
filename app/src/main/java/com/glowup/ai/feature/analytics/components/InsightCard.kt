package com.glowup.ai.feature.analytics.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.feature.analytics.AiInsight
import com.glowup.ai.feature.analytics.InsightType

@Composable
fun InsightCard(
    insight: AiInsight,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current
    var expanded by remember { mutableStateOf(false) }

    val (backgroundColor, iconColor, icon) = when (insight.type) {
        InsightType.IMPROVEMENT -> Triple(
            glow.success.copy(alpha = 0.15f),
            glow.success,
            Icons.Filled.TrendingUp
        )
        InsightType.CONCERN -> Triple(
            glow.danger.copy(alpha = 0.15f),
            glow.danger,
            Icons.Filled.Warning
        )
        InsightType.PATTERN -> Triple(
            glow.honey500.copy(alpha = 0.15f),
            glow.honey700,
            Icons.Filled.Psychology
        )
        InsightType.ACHIEVEMENT -> Triple(
            glow.success.copy(alpha = 0.15f),
            glow.success,
            Icons.Filled.EmojiEvents
        )
        InsightType.RECOMMENDATION -> Triple(
            Color(0xFF7C4DFF).copy(alpha = 0.15f),
            Color(0xFF7C4DFF),
            Icons.Filled.Lightbulb
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { expanded = !expanded }
            .padding(GlowSpacing.md)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900
                )

                insight.changePercent?.let { change ->
                    Text(
                        text = "${if (change > 0) "+" else ""}${String.format("%.1f", change)}%",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (change > 0) glow.success else glow.danger
                    )
                }
            }

            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = glow.ink600
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(GlowSpacing.sm))

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink900,
                modifier = Modifier.padding(start = 48.dp)
            )

            insight.recommendation?.let { recommendation ->
                Spacer(modifier = Modifier.height(GlowSpacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp)
                        .background(
                            glow.honey500.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(GlowSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = glow.honey700,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.ink900,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightsList(
    insights: List<AiInsight>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        insights.forEach { insight ->
            InsightCard(insight = insight)
        }
    }
}
