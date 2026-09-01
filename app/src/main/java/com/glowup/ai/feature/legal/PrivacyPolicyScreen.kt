package com.glowup.ai.feature.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowTopBar

/**
 * Privacy Policy screen.
 *
 * Displays comprehensive privacy policy covering data collection, user rights,
 * third-party services, and contact information. GDPR and CCPA compliant.
 */
@Composable
fun PrivacyPolicyRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrivacyPolicyScreen(
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowColors = LocalGlowColors.current

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        GlowTopBar(
            title = "Privacy Policy",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GlowSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg)
        ) {
            // Effective date
            Text(
                text = "Effective Date: September 1, 2026",
                style = MaterialTheme.typography.bodyMedium,
                color = glowColors.ink600
            )

            // Introduction
            Text(
                text = "At GlowupAI, we are committed to protecting your privacy and ensuring transparency in how we collect, use, and protect your personal information. This Privacy Policy explains our practices in detail.",
                style = MaterialTheme.typography.bodyLarge,
                color = glowColors.ink900
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 1: Information We Collect
            PolicySection(
                title = "1. Information We Collect",
                content = listOf(
                    PolicySubsection(
                        subtitle = "1.1 Information You Provide",
                        items = listOf(
                            "Account information: Name, email address, and authentication credentials",
                            "Profile information: Skin type, concerns, and skincare goals",
                            "Communication: Messages, feedback, and support requests"
                        )
                    ),
                    PolicySubsection(
                        subtitle = "1.2 Information We Collect Automatically",
                        items = listOf(
                            "Photos: Face photos you capture for skin analysis",
                            "Device information: Device model, operating system version, unique device identifiers",
                            "Camera information: Camera specifications and sensor data",
                            "Usage data: App interactions, feature usage, session duration",
                            "Performance data: App crashes, errors, and diagnostic information",
                            "Analysis results: AI-generated skin analysis, insights, and recommendations"
                        )
                    ),
                    PolicySubsection(
                        subtitle = "1.3 Location Information",
                        items = listOf(
                            "Approximate location (city/country level) to provide region-specific skincare recommendations",
                            "We do not collect precise GPS coordinates"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 2: How We Use Your Information
            PolicySection(
                title = "2. How We Use Your Information",
                content = listOf(
                    PolicySubsection(
                        subtitle = "We use your information to:",
                        items = listOf(
                            "Provide and improve our skin analysis services",
                            "Generate personalized skincare insights and recommendations",
                            "Analyze trends and patterns to improve AI model accuracy",
                            "Communicate with you about updates, features, and support",
                            "Ensure app security and prevent fraud or abuse",
                            "Comply with legal obligations and enforce our terms",
                            "Conduct research and development (with anonymized data only)"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 3: Model Training (Optional)
            PolicySection(
                title = "3. Model Training Data Collection (Optional)",
                content = listOf(
                    PolicySubsection(
                        subtitle = "If you opt-in to help improve our AI:",
                        items = listOf(
                            "Your captured photos and analysis results may be used for model training",
                            "All training data is anonymized before use - we assign a random ID hash that cannot be linked back to you",
                            "No personal information (name, email, profile data) is included in training datasets",
                            "Training data is stored securely and automatically deleted after 1 year",
                            "You can opt-out at any time in Settings > Data & Privacy",
                            "Opting out will not affect your app experience or analysis quality"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 4: Third-Party Services
            PolicySection(
                title = "4. Third-Party Services",
                content = listOf(
                    PolicySubsection(
                        subtitle = "We use the following third-party services:",
                        items = listOf(
                            "Firebase Authentication: For secure sign-in (Google, email/password)",
                            "Firebase Cloud Storage: For secure photo storage",
                            "Firebase Analytics: For anonymous usage analytics",
                            "Firebase Crashlytics: For crash reporting and diagnostics",
                            "Machine Learning APIs: For AI-powered skin analysis (photos processed in secure cloud environment)",
                            "Payment processors: For subscription and payment processing (we do not store payment card details)"
                        )
                    ),
                    PolicySubsection(
                        subtitle = "Data shared with third parties:",
                        items = listOf(
                            "Each service receives only the minimum data necessary to function",
                            "Third parties are contractually required to protect your data",
                            "We do not sell your personal information to third parties"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 5: Data Security
            PolicySection(
                title = "5. Data Security",
                content = listOf(
                    PolicySubsection(
                        subtitle = "We protect your data through:",
                        items = listOf(
                            "End-to-end encryption for photos in transit and at rest",
                            "Industry-standard security protocols (TLS 1.3, AES-256)",
                            "Secure authentication via Firebase",
                            "Regular security audits and vulnerability assessments",
                            "Access controls and employee training",
                            "Automated monitoring for suspicious activity"
                        )
                    ),
                    PolicySubsection(
                        subtitle = "While we implement strong security measures, no system is 100% secure. We cannot guarantee absolute security but commit to notifying you promptly of any data breaches as required by law."
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 6: Your Rights
            PolicySection(
                title = "6. Your Privacy Rights",
                content = listOf(
                    PolicySubsection(
                        subtitle = "Under GDPR (EU) and CCPA (California), you have the right to:",
                        items = listOf(
                            "Access: Request a copy of all personal data we hold about you",
                            "Rectification: Correct inaccurate or incomplete information",
                            "Erasure: Request deletion of your account and all associated data",
                            "Data portability: Receive your data in a machine-readable format",
                            "Restriction: Limit how we process your data",
                            "Objection: Opt-out of certain data processing activities",
                            "Withdraw consent: Remove consent for model training data collection at any time"
                        )
                    ),
                    PolicySubsection(
                        subtitle = "To exercise your rights:",
                        items = listOf(
                            "In-app: Go to Settings > Data & Privacy",
                            "Email us at: privacy@glowupai.com",
                            "We will respond within 30 days of your request"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 7: Data Retention
            PolicySection(
                title = "7. Data Retention",
                content = listOf(
                    PolicySubsection(
                        items = listOf(
                            "Account data: Retained while your account is active",
                            "Photos and analysis: Retained until you delete them or close your account",
                            "Model training data (if opted-in): Automatically deleted after 1 year",
                            "Usage analytics: Anonymized and retained for up to 2 years",
                            "Deleted account data: Permanently removed within 30 days, except where legal retention is required"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 8: Children's Privacy
            PolicySection(
                title = "8. Children's Privacy",
                content = listOf(
                    PolicySubsection(
                        items = listOf(
                            "GlowupAI is not intended for children under 13 years of age",
                            "We do not knowingly collect personal information from children under 13",
                            "If you believe we have inadvertently collected data from a child under 13, contact us immediately at privacy@glowupai.com"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 9: International Transfers
            PolicySection(
                title = "9. International Data Transfers",
                content = listOf(
                    PolicySubsection(
                        items = listOf(
                            "Your data may be transferred to and processed in countries outside your residence",
                            "We ensure appropriate safeguards are in place (standard contractual clauses, adequacy decisions)",
                            "Data transfers comply with GDPR, CCPA, and other applicable privacy laws"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 10: Changes to This Policy
            PolicySection(
                title = "10. Changes to This Policy",
                content = listOf(
                    PolicySubsection(
                        items = listOf(
                            "We may update this Privacy Policy periodically to reflect changes in our practices or legal requirements",
                            "Material changes will be notified via email or prominent in-app notice",
                            "Continued use of the app after changes constitutes acceptance of the updated policy",
                            "Previous versions are archived and available upon request"
                        )
                    )
                )
            )

            Divider(color = glowColors.ink600.copy(alpha = 0.1f))

            // Section 11: Contact Us
            PolicySection(
                title = "11. Contact Us",
                content = listOf(
                    PolicySubsection(
                        subtitle = "Questions about this Privacy Policy or our data practices?",
                        items = listOf(
                            "Email: privacy@glowupai.com",
                            "Support: support@glowupai.com",
                            "Mailing address: GlowupAI Inc., 123 Tech Street, San Francisco, CA 94105, USA",
                            "Data Protection Officer: dpo@glowupai.com"
                        )
                    )
                )
            )

            Spacer(modifier = Modifier.height(GlowSpacing.xl))

            // Footer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = glowColors.surfaceCard
                )
            ) {
                Text(
                    text = "By using GlowupAI, you acknowledge that you have read and understood this Privacy Policy and agree to our data practices as described.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glowColors.ink600,
                    modifier = Modifier.padding(GlowSpacing.md)
                )
            }
        }
    }
}

/**
 * Represents a section of the privacy policy
 */
private data class PolicySubsection(
    val subtitle: String? = null,
    val items: List<String> = emptyList()
)

/**
 * Renders a policy section with title, optional subtitle, and bullet points
 */
@Composable
private fun PolicySection(
    title: String,
    content: List<PolicySubsection>,
    modifier: Modifier = Modifier
) {
    val glowColors = LocalGlowColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.md)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glowColors.ink900,
            modifier = Modifier.semantics { heading() }
        )

        content.forEach { subsection ->
            Column(
                verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
            ) {
                subsection.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = glowColors.ink900
                    )
                }

                subsection.items.forEach { item ->
                    BulletPoint(text = item)
                }
            }
        }
    }
}

/**
 * Bullet point with proper indentation
 */
@Composable
private fun BulletPoint(
    text: String,
    modifier: Modifier = Modifier
) {
    val glowColors = LocalGlowColors.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = glowColors.ink600,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = glowColors.ink600,
            modifier = Modifier.weight(1f)
        )
    }
}
