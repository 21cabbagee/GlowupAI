package com.glowup.ai.core.design

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Canonical corner-radius scale, mirrored into [MaterialTheme.shapes][Shapes] by [GlowUpTheme]. */
object GlowShapes {
    val sm: CornerBasedShape = RoundedCornerShape(8.dp)
    val md: CornerBasedShape = RoundedCornerShape(16.dp)
    val lg: CornerBasedShape = RoundedCornerShape(24.dp)
    val xl: CornerBasedShape = RoundedCornerShape(32.dp)

    /** Fully rounded — pills, FABs, chips. Large enough to stay circular at any component height. */
    val pill: CornerBasedShape = RoundedCornerShape(percent = 50)
}

val GlowMaterialShapes =
    Shapes(
        extraSmall = GlowShapes.sm,
        small = GlowShapes.sm,
        medium = GlowShapes.md,
        large = GlowShapes.lg,
        extraLarge = GlowShapes.xl,
    )
