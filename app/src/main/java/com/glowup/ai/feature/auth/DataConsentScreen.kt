package com.glowup.ai.feature.auth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar

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
    modifier: Modifier = Modifier
) {
    var isSubmitting by remember { mutableStateOf(false) }

    DataConsentScreen(
        onConsent = { granted ->
            isSubmitting = true
            onConsent(granted)
        },
        onBack = onBack,
        isSubmitting = isSubmitting,
        modifier = modifier
    )
}

@Composable
private fun DataConsentScreen(
    onConsent: (Boolean) -> Unit,
    onBack: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        GlowTopBar(
            title = "Help Improve GlowupAI",
            onNavigateUp = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "Make our AI better for everyone",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "We'd like your help training future versions of our AI model. This is completely optional — the app works great either way!",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalGlowColors.current.textSecondary
            )

            // What we collect
            GlowCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = LocalGlowColors.current.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "What we collect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    BulletPoint("Your capture images (face photos)")
                    BulletPoint("Analysis results from our AI")
                    BulletPoint("Lighting conditions and image quality")
                    BulletPoint("Device info (camera model, OS)")
                    BulletPoint("Your feedback on accuracy")
                }
            }

            // Privacy guarantees
            GlowCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LocalGlowColors.current.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Your privacy is protected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    BulletPoint("All data is anonymized — no personal info")
                    BulletPoint("Your face gets a random ID hash")
                    BulletPoint("Data is automatically deleted after 1 year")
                    BulletPoint("You can opt-out anytime in settings")
                    BulletPoint("GDPR & CCPA compliant")
                }
            }

            // Benefits
            GlowCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = LocalGlowColors.current.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "How this helps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    BulletPoint("More accurate skin analysis for everyone")
                    BulletPoint("Better detection in diverse lighting conditions")
                    BulletPoint("Improved performance on different skin tones")
                    BulletPoint("Faster and more reliable predictions")
                }
            }

            // Info note
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = LocalGlowColors.current.surfaceSecondary
                )
            ) {
                Text(
                    text = "Note: This only affects training data collection. Your regular app usage, captures, and analysis history are never shared or used for training without this consent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGlowColors.current.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlowButton(
                    onClick = { onConsent(true) },
                    variant = GlowButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text("Yes, help improve the AI")
                }

                GlowButton(
                    onClick = { onConsent(false) },
                    variant = GlowButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text("No thanks, not now")
                }
            }

            // Privacy policy link
            TextButton(
                onClick = { /* Open privacy policy */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "View our Data Collection Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGlowColors.current.primary
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalGlowColors.current.textSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalGlowColors.current.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
