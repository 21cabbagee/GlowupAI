package com.glowup.ai.feature.capture.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors

/**
 * The oval framing guide over the live camera preview: a darkened surround with an oval cutout so
 * the user can align their face before the shutter, plus a stroke that turns from neutral to
 * "good" honey once [isFramingGood] is true. This is a passive guide only — it never marks
 * anything accepted; the server remains the sole authority on quality (trap #6).
 */
@Composable
fun OvalFramingGuide(
    modifier: Modifier = Modifier,
    isFramingGood: Boolean,
) {
    val glow = LocalGlowColors.current
    val scrimColor = Color.Black.copy(alpha = 0.55f)
    val strokeColor = if (isFramingGood) glow.honey500 else Color.White.copy(alpha = 0.85f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val ovalWidth = size.width * 0.72f
        val ovalHeight = size.height * 0.46f
        val center = Offset(size.width / 2f, size.height * 0.42f)
        val ovalTopLeft = Offset(center.x - ovalWidth / 2f, center.y - ovalHeight / 2f)

        val ovalPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(ovalTopLeft, Size(ovalWidth, ovalHeight)))
        }

        // Darken everything outside the oval.
        clipPath(path = ovalPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
            drawRect(color = scrimColor, size = size)
        }

        drawPath(
            path = ovalPath,
            color = strokeColor,
            style = Stroke(
                width = 4.dp.toPx(),
                pathEffect = if (isFramingGood) null else PathEffect.dashPathEffect(floatArrayOf(18f, 14f)),
            ),
        )
    }
}
