package com.glowup.ai.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTopBar

/** Terms governing use of the GlowUp AI service. */
@Composable
fun TermsOfServiceRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    Scaffold(
        modifier = modifier,
        topBar = { GlowTopBar(title = "Terms of Service", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(GlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
        ) {
            Text(
                text = "GlowUp AI Terms of Service",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = glow.honey700,
            )
            Text(
                text = "Effective date: September 7, 2026",
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
            )
            Text(
                text = "By creating an account or using GlowUp AI, you agree to these Terms of Service. They describe the rules for using the service and the limits that apply to its cosmetic-tracking features.",
                style = MaterialTheme.typography.bodyLarge,
                color = glow.ink900,
            )

            TermsSection(
                title = "1. The service",
                body = "GlowUp AI provides tools for tracking changes in cosmetic skin appearance, recording routine events, and organizing personal observations. Features may change, be unavailable, or contain errors.",
            )
            TermsSection(
                title = "2. Your account",
                bullets = listOf(
                    "Provide information that is reasonably accurate and keep access to your account secure.",
                    "Use only your own photos or photos for which you have permission.",
                    "Do not use the service to identify another person, harass someone, or upload unlawful content.",
                    "You are responsible for activity performed through your account.",
                ),
            )
            TermsSection(
                title = "3. Photos and personal data",
                bullets = listOf(
                    "Photo capture is locked until you give explicit facial-photo consent.",
                    "You keep ownership of photos and content you submit. We use them to provide the service and as described in the Privacy Policy.",
                    "Optional processing is controlled by separate consent where the app presents that choice; declining optional consent does not remove core account access.",
                    "You can export available account data or request account deletion from Data & Privacy.",
                ),
            )
            TermsSection(
                title = "4. Subscriptions",
                body = "If paid features are offered, purchases, renewals, refunds, and cancellations are handled through the applicable app store or payment provider. Cancelling a plan does not by itself delete your account or history.",
            )

            DisclaimerNote(
                text = "GlowUp AI is cosmetic appearance tracking only. It is not a medical device, diagnosis, treatment, or substitute for a licensed dermatologist.",
            )

            TermsSection(
                title = "5. Safety and no medical advice",
                bullets = listOf(
                    "AI outputs, measurements, product information, and recommendations are informational and may be incomplete or incorrect.",
                    "Do not use GlowUp AI to diagnose, treat, prevent, or rule out a medical condition.",
                    "For a concerning, changing, painful, or urgent symptom, contact a qualified healthcare professional. For emergencies, use local emergency services.",
                ),
            )
            TermsSection(
                title = "6. Availability and responsibility",
                body = "To the extent permitted by applicable law, you use the service at your own risk. Keep your own copies of important information. Nothing in these terms limits rights that cannot legally be limited.",
            )
            TermsSection(
                title = "7. Changes and contact",
                bullets = listOf(
                    "We may update these terms and will show material changes in the app or through another reasonable notice.",
                    "Questions about these terms: support@glowup.ai",
                ),
            )

            Spacer(modifier = Modifier.height(GlowSpacing.sm))
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "For questions about these Terms of Service, contact support@glowup.ai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                )
            }
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    body: String? = null,
    bullets: List<String> = emptyList(),
) {
    val glow = LocalGlowColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
            modifier = Modifier.semantics { heading() },
        )
        body?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
            )
        }
        bullets.forEach { bullet ->
            Text(
                text = "•  $bullet",
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
            )
        }
    }
}
