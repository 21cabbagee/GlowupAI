package com.glowup.ai.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowCard

/**
 * Reusable components and templates for the Enhanced Onboarding flow.
 */

// ================================================================================================
// Data Models
// ================================================================================================

data class TutorialSection(val title: String, val description: String)

// ================================================================================================
// Reusable Components
// ================================================================================================

@Composable
fun ValuePropItem(
    icon: ImageVector,
    title: String,
    description: String,
) {
    val glow = LocalGlowColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GlowSpacing.sm),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(glow.honey500.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glow.honey700,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(GlowSpacing.md))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
            )
        }
    }
}

@Composable
fun TutorialSectionItem(title: String, description: String) {
    val glow = LocalGlowColors.current
    GlowCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(GlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink900,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
            )
        }
    }
}

@Composable
fun ChecklistItem(text: String) {
    val glow = LocalGlowColors.current
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = glow.success,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = glow.ink600,
        )
    }
}

// ================================================================================================
// Template Components
// ================================================================================================

@Composable
fun TutorialScreenTemplate(
    icon: ImageVector,
    title: String,
    subtitle: String,
    contentSections: List<TutorialSection>,
) {
    val glow = LocalGlowColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GlowSpacing.xl)
            .padding(bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
    ) {
        Spacer(modifier = Modifier.height(GlowSpacing.xl))

        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(glow.honey500.copy(alpha = 0.15f))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glow.honey600,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = glow.ink600,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        // Content sections
        contentSections.forEach { section ->
            TutorialSectionItem(
                title = section.title,
                description = section.description
            )
        }
    }
}

@Composable
fun PermissionScreenTemplate(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    reasonItems: List<String>,
) {
    val glow = LocalGlowColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GlowSpacing.xl)
            .padding(bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
    ) {
        Spacer(modifier = Modifier.height(GlowSpacing.xl))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(glow.honey500.copy(alpha = 0.15f))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glow.honey600,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = glow.honey700,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = glow.ink600,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        GlowCard {
            Column(
                modifier = Modifier.padding(GlowSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
            ) {
                Text(
                    text = "We'll use this to:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink900,
                )
                reasonItems.forEach { reason ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glow.honey700,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glow.ink600,
                        )
                    }
                }
            }
        }
    }
}
