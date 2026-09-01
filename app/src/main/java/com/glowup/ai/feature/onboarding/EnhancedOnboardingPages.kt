package com.glowup.ai.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowCard

/**
 * Individual page composables for the Enhanced Onboarding flow.
 * Each page represents a single screen in the onboarding pager.
 */

// ================================================================================================
// Welcome & Value Proposition
// ================================================================================================

@Composable
fun WelcomeScreen() {
    val glow = LocalGlowColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GlowSpacing.xl)
            .padding(bottom = 180.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(glow.honey500.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = glow.honey600,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(GlowSpacing.xl))

        Text(
            text = "Welcome to\nGlowUp AI",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(GlowSpacing.md))

        Text(
            text = "Track your skin with evidence",
            style = MaterialTheme.typography.titleLarge,
            color = glow.honey700,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(GlowSpacing.xl))

        ValuePropItem(
            icon = Icons.Filled.CameraAlt,
            title = "Consistent photo tracking",
            description = "Guided captures with same lighting every time"
        )

        ValuePropItem(
            icon = Icons.Filled.LocalFireDepartment,
            title = "Build your streak",
            description = "Daily check-ins keep you accountable"
        )

        ValuePropItem(
            icon = Icons.Filled.Science,
            title = "Test what works",
            description = "Experiment with your routine scientifically"
        )

        ValuePropItem(
            icon = Icons.Filled.Timeline,
            title = "See real changes",
            description = "Track redness, texture, and tone over time"
        )
    }
}

// ================================================================================================
// Tutorial Screens
// ================================================================================================

@Composable
fun TutorialStreaksScreen() {
    TutorialScreenTemplate(
        icon = Icons.Filled.LocalFireDepartment,
        title = "Build your streak",
        subtitle = "Consistency is key to tracking change",
        contentSections = listOf(
            TutorialSection(
                "Daily check-ins",
                "Take a photo or log how your skin feels each day to maintain your streak"
            ),
            TutorialSection(
                "Freeze days",
                "Premium users get freeze days to protect their streak when life gets busy"
            ),
            TutorialSection(
                "Track your progress",
                "See your capture calendar fill up as you build the habit"
            ),
        )
    )
}

@Composable
fun TutorialPhotosScreen() {
    TutorialScreenTemplate(
        icon = Icons.Filled.CameraAlt,
        title = "How to take good photos",
        subtitle = "Consistent conditions = accurate tracking",
        contentSections = listOf(
            TutorialSection(
                "Same lighting",
                "Natural daylight near a window works best. Avoid direct sun or harsh shadows"
            ),
            TutorialSection(
                "Clean face",
                "No makeup, filters, or products. Capture your baseline skin"
            ),
            TutorialSection(
                "Same position",
                "Use the guide overlay to match your face position each time"
            ),
            TutorialSection(
                "Same time of day",
                "Morning captures show your skin most consistently"
            ),
        )
    )
}

@Composable
fun TutorialMetricsScreen() {
    TutorialScreenTemplate(
        icon = Icons.Filled.Timeline,
        title = "Understanding your metrics",
        subtitle = "What we measure and why",
        contentSections = listOf(
            TutorialSection(
                "Redness Score",
                "Tracks inflammatory redness, not natural pigmentation"
            ),
            TutorialSection(
                "Texture Score",
                "Measures smoothness and visible pores or bumps"
            ),
            TutorialSection(
                "Tone Evenness",
                "Tracks overall uniformity and dark spots"
            ),
            TutorialSection(
                "Trends over time",
                "Weekly comparisons show real change better than day-to-day"
            ),
        )
    )
}

@Composable
fun TutorialExperimentsScreen() {
    TutorialScreenTemplate(
        icon = Icons.Filled.Science,
        title = "Test your routine",
        subtitle = "Scientific approach to skincare",
        contentSections = listOf(
            TutorialSection(
                "One change at a time",
                "Change only one product to see what really works"
            ),
            TutorialSection(
                "Give it time",
                "Most changes need 4-6 weeks to show results"
            ),
            TutorialSection(
                "Track everything",
                "Log products, sleep, stress, and diet for complete context"
            ),
            TutorialSection(
                "Get verdicts",
                "We'll analyze correlations and give honest assessments"
            ),
        )
    )
}

// ================================================================================================
// Permission Screens
// ================================================================================================

@Composable
fun CameraPermissionScreen() {
    PermissionScreenTemplate(
        icon = Icons.Filled.CameraAlt,
        title = "Camera access",
        subtitle = "Take consistent tracking photos",
        description = "GlowUp needs camera access to capture your skin photos. " +
            "Photos are stored securely on your device and only sent to our " +
            "servers when you explicitly request analysis.",
        reasonItems = listOf(
            "Capture guided selfies",
            "Use overlay guides for consistency",
            "Store photos locally until you consent"
        )
    )
}

@Composable
fun NotificationPermissionScreen() {
    PermissionScreenTemplate(
        icon = Icons.Filled.Notifications,
        title = "Stay on track",
        subtitle = "Gentle reminders for your routine",
        description = "Optional reminders help you maintain your streak and " +
            "remember to take photos at the same time each day. You can " +
            "customize or disable these anytime.",
        reasonItems = listOf(
            "Daily capture reminders",
            "Streak protection alerts",
            "Weekly progress updates"
        )
    )
}

// ================================================================================================
// Setup Screens
// ================================================================================================

@Composable
fun BaselinePhotoScreen() {
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
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = glow.honey600,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = "Ready for your first photo?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "This will be your baseline",
            style = MaterialTheme.typography.titleMedium,
            color = glow.honey700,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        GlowCard {
            Column(
                modifier = Modifier.padding(GlowSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
            ) {
                Text(
                    text = "Before you capture:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink900,
                )
                ChecklistItem("Remove makeup and wash your face")
                ChecklistItem("Find natural light near a window")
                ChecklistItem("Avoid direct sunlight or harsh shadows")
                ChecklistItem("Pull back hair from your face")
            }
        }

        GlowCard {
            Column(
                modifier = Modifier.padding(GlowSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
            ) {
                Text(
                    text = "💡 Pro tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink900,
                )
                Text(
                    text = "Try to take photos at the same time each day — " +
                        "morning light is most consistent!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink600,
                )
            }
        }
    }
}

@Composable
fun RoutineSetupScreen() {
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
                imageVector = Icons.Filled.Science,
                contentDescription = null,
                tint = glow.honey600,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = "Add your routine",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Optional, but recommended",
            style = MaterialTheme.typography.titleMedium,
            color = glow.honey700,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Track what products you're using so we can help you " +
                "understand which changes actually make a difference.",
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
                    text = "What you can track:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink900,
                )
                ChecklistItem("Cleansers, serums, and moisturizers")
                ChecklistItem("Sunscreen and treatments")
                ChecklistItem("When you started using each product")
                ChecklistItem("How often you use them")
            }
        }

        GlowCard {
            Column(
                modifier = Modifier.padding(GlowSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.xs)
            ) {
                Text(
                    text = "You can skip this",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink900,
                )
                Text(
                    text = "Add products anytime from your routine tab. " +
                        "Start simple and build your tracking over time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.ink600,
                )
            }
        }
    }
}
