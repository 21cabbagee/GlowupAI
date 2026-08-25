package com.glowup.ai.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Renders every design-system token and the full type scale, in both themes, so a reviewer can
 * eyeball the whole system without hunting through feature screens. Not shipped UI.
 */

private data class Swatch(val name: String, val color: Color, val onColor: Color, val ratioNote: String)

@Composable
private fun SwatchRow(swatch: Swatch) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(swatch.color, RoundedCornerShape(GlowSpacing.sm))
            .padding(horizontal = GlowSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = swatch.name, color = swatch.onColor, style = MaterialTheme.typography.labelLarge)
        Text(text = swatch.ratioNote, color = swatch.onColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = GlowSpacing.lg, bottom = GlowSpacing.sm),
    )
}

@Composable
private fun DesignSystemGallery() {
    val colors = LocalGlowColors.current
    val scheme = MaterialTheme.colorScheme

    val paletteSwatches = listOf(
        Swatch("honey-300", colors.honey300, colors.ink900, "surface only"),
        Swatch("honey-400", colors.honey400, colors.ink900, "surface only"),
        Swatch("honey-500 (primary)", colors.honey500, colors.ink900, "11.35:1"),
        Swatch("honey-600", colors.honey600, colors.ink900, "8.98:1"),
        Swatch("honey-700 (clay, text-safe on light)", colors.honey700, colors.ink900, "4.93:1"),
    )

    val verdictSwatches = listOf(
        Swatch("keep", colors.verdictColor("keep"), colors.onVerdictColor("keep"), "11.35:1"),
        Swatch("likely_useful", colors.verdictColor("likely_useful"), colors.onVerdictColor("likely_useful"), "4.8-4.88:1"),
        Swatch("evidence_unclear", colors.verdictColor("evidence_unclear"), colors.onVerdictColor("evidence_unclear"), "4.93:1"),
        Swatch("investigate", colors.verdictColor("investigate"), colors.onVerdictColor("investigate"), "4.90:1"),
        Swatch("locked / unknown", colors.verdictColor("locked"), colors.onVerdictColor("locked"), "7.87:1"),
    )

    val schemeSwatches = listOf(
        Swatch("primary / onPrimary", scheme.primary, scheme.onPrimary, "M3 role"),
        Swatch("primaryContainer / onPrimaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer, "M3 role"),
        Swatch("secondary / onSecondary", scheme.secondary, scheme.onSecondary, "M3 role"),
        Swatch("tertiary / onTertiary", scheme.tertiary, scheme.onTertiary, "M3 role"),
        Swatch("error / onError", scheme.error, scheme.onError, "M3 role"),
        Swatch("background / onBackground", scheme.background, scheme.onBackground, "M3 role"),
        Swatch("surface / onSurface", scheme.surface, scheme.onSurface, "M3 role"),
        Swatch("surfaceVariant / onSurfaceVariant", scheme.surfaceVariant, scheme.onSurfaceVariant, "M3 role"),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .padding(GlowSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs),
    ) {
        item {
            Text(
                text = "GlowUp Design System",
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onBackground,
            )
            Text(
                text = "Honey brand tokens, verdict semantics, M3 color roles, spacing, shapes, motion, and the full type scale.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink600,
                modifier = Modifier.padding(bottom = GlowSpacing.sm),
            )
        }

        item { SectionLabel("Honey palette (surfaces only — never text, except honey-700 on light)") }
        items(paletteSwatches) { SwatchRow(it) }

        item { SectionLabel("Verdict semantics") }
        items(verdictSwatches) { SwatchRow(it) }

        item { SectionLabel("Material 3 color roles") }
        items(schemeSwatches) { SwatchRow(it) }

        item { SectionLabel("Spacing scale") }
        item { SpacingRow() }

        item { SectionLabel("Shape scale") }
        item { ShapeRow() }

        item { SectionLabel("Type scale") }
        item { TypeScaleColumn() }
    }
}

@Composable
private fun SpacingRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        listOf(
            "xs" to GlowSpacing.xs,
            "sm" to GlowSpacing.sm,
            "md" to GlowSpacing.md,
            "lg" to GlowSpacing.lg,
            "xl" to GlowSpacing.xl,
            "xxl" to GlowSpacing.xxl,
        ).forEach { (label, size) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(size)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun ShapeRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)) {
        val shapes = listOf(
            "sm" to GlowShapes.sm,
            "md" to GlowShapes.md,
            "lg" to GlowShapes.lg,
            "xl" to GlowShapes.xl,
            "pill" to GlowShapes.pill,
        )
        shapes.forEach { (label, shape) ->
            ShapeSample(label, shape)
        }
    }
}

@Composable
private fun RowScope.ShapeSample(label: String, shape: androidx.compose.foundation.shape.CornerBasedShape) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, shape),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun TypeScaleColumn() {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)) {
        listOf(
            "Display Large" to GlowTypography.displayLarge,
            "Display Medium" to GlowTypography.displayMedium,
            "Display Small" to GlowTypography.displaySmall,
            "Headline Large" to GlowTypography.headlineLarge,
            "Headline Medium" to GlowTypography.headlineMedium,
            "Headline Small" to GlowTypography.headlineSmall,
            "Title Large" to GlowTypography.titleLarge,
            "Title Medium" to GlowTypography.titleMedium,
            "Title Small" to GlowTypography.titleSmall,
            "Body Large — the quick amber fox" to GlowTypography.bodyLarge,
            "Body Medium — the quick amber fox" to GlowTypography.bodyMedium,
            "Body Small — the quick amber fox" to GlowTypography.bodySmall,
            "LABEL LARGE" to GlowTypography.labelLarge,
            "LABEL MEDIUM" to GlowTypography.labelMedium,
            "LABEL SMALL" to GlowTypography.labelSmall,
        ).forEach { (label, style) ->
            Text(text = label, style = style, color = scheme.onBackground)
        }
    }
}

@Preview(name = "Design System — Light", showBackground = true, widthDp = 412, heightDp = 3400)
@Composable
private fun DesignSystemPreviewLight() {
    GlowUpTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DesignSystemGallery()
        }
    }
}

@Preview(name = "Design System — Dark", showBackground = true, widthDp = 412, heightDp = 3400)
@Composable
private fun DesignSystemPreviewDark() {
    GlowUpTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DesignSystemGallery()
        }
    }
}
