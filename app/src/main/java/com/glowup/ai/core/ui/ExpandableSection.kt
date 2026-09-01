package com.glowup.ai.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowMotion
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.design.rememberReducedMotion

/**
 * Expandable section with smooth animation.
 * Collapses and expands with fade + vertical expand animation.
 *
 * Use for:
 * - FAQs
 * - Settings sections
 * - Detailed information panels
 * - Any collapsible content
 */
@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val glow = LocalGlowColors.current
    val reducedMotion = rememberReducedMotion()

    // Rotate chevron icon
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec =
            if (reducedMotion) {
                tween(0)
            } else {
                tween(durationMillis = 220, easing = GlowMotion.easing)
            },
        label = "chevronRotation",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Header (always visible)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { expanded = !expanded },
                        onClickLabel = if (expanded) "Collapse $title" else "Expand $title",
                    ).padding(GlowSpacing.md)
                    .semantics {
                        contentDescription = "$title, ${if (expanded) "expanded" else "collapsed"}"
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(GlowSpacing.sm))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
                modifier = Modifier.weight(1f),
            )

            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = glow.ink600,
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(rotation),
            )
        }

        // Content (animated)
        AnimatedVisibility(
            visible = expanded,
            enter =
                if (reducedMotion) {
                    fadeIn(animationSpec = tween(0))
                } else {
                    expandVertically(
                        animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
                    ) +
                        fadeIn(
                            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
                        )
                },
            exit =
                if (reducedMotion) {
                    fadeOut(animationSpec = tween(0))
                } else {
                    shrinkVertically(
                        animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing),
                    ) +
                        fadeOut(
                            animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing),
                        )
                },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlowSpacing.md, vertical = GlowSpacing.sm),
            ) {
                content()
            }
        }
    }
}

/**
 * Controlled expandable section where expanded state is managed externally.
 */
@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val glow = LocalGlowColors.current
    val reducedMotion = rememberReducedMotion()

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec =
            if (reducedMotion) {
                tween(0)
            } else {
                tween(durationMillis = 220, easing = GlowMotion.easing)
            },
        label = "chevronRotation",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { onExpandChange(!expanded) },
                        onClickLabel = if (expanded) "Collapse $title" else "Expand $title",
                    ).padding(GlowSpacing.md)
                    .semantics {
                        contentDescription = "$title, ${if (expanded) "expanded" else "collapsed"}"
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(GlowSpacing.sm))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
                modifier = Modifier.weight(1f),
            )

            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = glow.ink600,
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter =
                if (reducedMotion) {
                    fadeIn(animationSpec = tween(0))
                } else {
                    expandVertically(
                        animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
                    ) +
                        fadeIn(
                            animationSpec = tween(durationMillis = 220, easing = GlowMotion.easing),
                        )
                },
            exit =
                if (reducedMotion) {
                    fadeOut(animationSpec = tween(0))
                } else {
                    shrinkVertically(
                        animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing),
                    ) +
                        fadeOut(
                            animationSpec = tween(durationMillis = 180, easing = GlowMotion.easing),
                        )
                },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlowSpacing.md, vertical = GlowSpacing.sm),
            ) {
                content()
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ExpandableSectionPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpandableSection(
                title = "What is GlowUp AI?",
                initiallyExpanded = true,
            ) {
                Text(
                    "GlowUp AI is your personal skincare companion that tracks your progress, " +
                        "analyzes your routine, and provides personalized recommendations.",
                )
            }
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ExpandableSectionPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpandableSection(
                title = "What is GlowUp AI?",
                initiallyExpanded = false,
            ) {
                Text(
                    "GlowUp AI is your personal skincare companion that tracks your progress, " +
                        "analyzes your routine, and provides personalized recommendations.",
                )
            }
        }
    }
}
