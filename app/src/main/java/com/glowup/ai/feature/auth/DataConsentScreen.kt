package com.glowup.ai.feature.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowEasing
import com.glowup.ai.core.ui.GlowTopBar
import com.glowup.ai.core.ui.isReducedMotionEnabled
import kotlinx.coroutines.delay

/**
 * Data collection consent screen.
 *
 * Explains data collection for model training and gets explicit user consent.
 * Part of GDPR/CCPA compliance and ethical AI practices.
 */
@Composable
fun DataConsentRoute(
    onConsent: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSubmitting by remember { mutableStateOf(false) }

    DataConsentScreen(
        onConsent = { granted ->
            isSubmitting = true
            onConsent(granted)
        },
        onBack = onBack,
        onPrivacyPolicy = onPrivacyPolicy,
        isSubmitting = isSubmitting,
        modifier = modifier,
    )
}

@Composable
private fun DataConsentScreen(
    onConsent: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = isReducedMotionEnabled()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        GlowTopBar(
            title = "Help Improve GlowupAI",
            onBack = onBack,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(GlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
        ) {
            // Header
            Text(
                text = "Make our AI better for everyone",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "We'd like your help training future versions of our AI model. This is completely optional — the app works great either way!",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalGlowColors.current.ink600,
            )

            // What we collect
            AnimatedCard(delay = 0, reducedMotion = reducedMotion) {
                GlowCard {
                    Column(
                        modifier = Modifier.padding(GlowSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Data collection information",
                                tint = LocalGlowColors.current.honey600,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "What we collect",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        BulletPoint("Your capture images (face photos)")
                        BulletPoint("Analysis results from our AI")
                        BulletPoint("Lighting conditions and image quality")
                        BulletPoint("Device info (camera model, OS)")
                        BulletPoint("Your feedback on accuracy")
                    }
                }
            }

            // Privacy guarantees
            AnimatedCard(delay = 100, reducedMotion = reducedMotion) {
                GlowCard {
                    Column(
                        modifier = Modifier.padding(GlowSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy protection",
                                tint = LocalGlowColors.current.honey600,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "Your privacy is protected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        BulletPoint("All data is anonymized — no personal info")
                        BulletPoint("Your face gets a random ID hash")
                        BulletPoint("Data is automatically deleted after 1 year")
                        BulletPoint("You can opt-out anytime in settings")
                        BulletPoint("GDPR & CCPA compliant")
                    }
                }
            }

            // Benefits
            AnimatedCard(delay = 200, reducedMotion = reducedMotion) {
                GlowCard {
                    Column(
                        modifier = Modifier.padding(GlowSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Benefits information",
                                tint = LocalGlowColors.current.honey600,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "How this helps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        BulletPoint("More accurate skin analysis for everyone")
                        BulletPoint("Better detection in diverse lighting conditions")
                        BulletPoint("Improved performance on different skin tones")
                        BulletPoint("Faster and more reliable predictions")
                    }
                }
            }

            // Info note
            AnimatedCard(delay = 300, reducedMotion = reducedMotion) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = LocalGlowColors.current.surfaceCard,
                        ),
                ) {
                    Text(
                        text = "Note: This only affects training data collection. Your regular app usage, captures, and analysis history are never shared or used for training without this consent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGlowColors.current.ink600,
                        modifier = Modifier.padding(GlowSpacing.md),
                    )
                }
            }

            Spacer(modifier = Modifier.height(GlowSpacing.sm))

            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.md),
            ) {
                GlowButton(
                    text = "Yes, help improve the AI",
                    onClick = { onConsent(true) },
                    variant = GlowButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    loading = isSubmitting,
                )

                GlowButton(
                    text = "No thanks, not now",
                    onClick = { onConsent(false) },
                    variant = GlowButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                )
            }

            // Privacy policy link
            TextButton(
                onClick = onPrivacyPolicy,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "View our Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGlowColors.current.honey600,
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.md),
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalGlowColors.current.ink600,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalGlowColors.current.ink600,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Animated card wrapper with staggered delay
 */
@Composable
private fun AnimatedCard(
    delay: Int,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay(delay.toLong())
        }
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 400, easing = GlowEasing),
        label = "cardAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 400, easing = GlowEasing),
        label = "cardScale",
    )

    Box(
        modifier =
            Modifier
                .alpha(alpha)
                .scale(scale),
    ) {
        content()
    }
}
