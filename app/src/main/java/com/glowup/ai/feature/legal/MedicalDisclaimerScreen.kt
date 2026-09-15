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

/** Dedicated, always-available explanation of the app's non-medical scope. */
@Composable
fun MedicalDisclaimerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current

    Scaffold(
        modifier = modifier,
        topBar = { GlowTopBar(title = "Medical Disclaimer", onBack = onBack) },
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
                text = "Cosmetic tracking only",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
                modifier = Modifier.semantics { heading() },
            )
            DisclaimerNote(
                text = "GlowUp AI is not a medical device and does not provide medical advice, diagnosis, treatment, prevention, or emergency care.",
            )
            Text(
                text = "The app compares visible appearance over time—such as redness, texture, or tone—to help you organize personal skincare observations. A result is not a clinical measurement and should not be used to decide whether a symptom is safe.",
                style = MaterialTheme.typography.bodyLarge,
                color = glow.ink900,
            )
            LegalSafetySection(
                title = "Use results carefully",
                bullets = listOf(
                    "Lighting, camera angle, image quality, skin tone, and other factors can affect results.",
                    "AI-generated explanations and product information may be wrong, incomplete, or out of date.",
                    "Do not delay care or change prescribed treatment based on anything in the app.",
                    "Ask a licensed dermatologist or other qualified clinician about medical concerns.",
                ),
            )
            LegalSafetySection(
                title = "When to get help",
                body = "Contact a qualified healthcare professional for a new, worsening, painful, bleeding, infected, or otherwise concerning skin change. For an emergency, contact local emergency services.",
            )
            Spacer(modifier = Modifier.height(GlowSpacing.sm))
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "For medical questions or concerns, contact a qualified healthcare professional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.ink600,
                )
            }
        }
    }
}

@Composable
private fun LegalSafetySection(
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
