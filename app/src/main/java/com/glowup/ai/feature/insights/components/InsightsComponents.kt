package com.glowup.ai.feature.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.domain.model.Citation

/**
 * One assistant/user chat bubble for the Q&A thread. [isSafetyHandoff] renders the clinician
 * hand-off treatment (distinct danger-toned card with a hospital glyph) instead of a normal
 * bubble — this must never look like an ordinary answer inviting a follow-up question
 * (frontend-api-map.md trap #10 / ANDROID_PLAN.md non-negotiable constraint).
 */
@Composable
fun ChatBubble(
    modifier: Modifier = Modifier,
    isUser: Boolean,
    text: String,
    pending: Boolean = false,
    isSafetyHandoff: Boolean = false,
    isError: Boolean = false,
    citations: List<Citation> = emptyList(),
) {
    val glow = LocalGlowColors.current

    if (isError) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = glow.danger,
                modifier =
                    Modifier
                        .widthIn(max = 320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(glow.danger.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics { contentDescription = "Error: $text" },
            )
        }
        return
    }

    if (isSafetyHandoff) {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(glow.danger.copy(alpha = 0.10f))
                    .padding(GlowSpacing.md)
                    .semantics { contentDescription = "Dermatologist hand-off: $text" },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = null,
                    tint = glow.danger,
                )
                Text(
                    text = "Talk to a dermatologist",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = glow.danger,
                    modifier = Modifier.padding(start = GlowSpacing.sm),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink900,
                modifier = Modifier.padding(top = GlowSpacing.sm),
            )
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isUser) glow.honey500 else glow.surfaceCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .semantics {
                        contentDescription = (if (isUser) "You: " else "Assistant: ") +
                            if (pending) "thinking" else text
                    },
        ) {
            Text(
                text = if (pending) "Thinking…" else text,
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink900,
            )
            if (!pending && citations.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = GlowSpacing.xs)) {
                    citations.forEach { citation -> CitationRow(citation) }
                }
            }
        }
    }
}

/** One citation, rendered inline with its answer: `type`, `date`, `id`. */
@Composable
fun CitationRow(
    citation: Citation,
    modifier: Modifier = Modifier,
) {
    val glow = LocalGlowColors.current
    val label =
        buildString {
            append(citation.type.replace('_', ' ').replaceFirstChar { it.uppercase() })
            citation.date?.let { append(" · $it") }
            citation.id?.let { append(" · #${it.take(8)}") }
        }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = glow.ink600,
        modifier =
            modifier
                .padding(top = 2.dp)
                .semantics { contentDescription = "Source: $label" },
    )
}

/**
 * A user-authored annotation card. Deliberately styled with a neutral "person" glyph and no
 * verdict color — a label is a human note, never an automated classification, and must never be
 * visually confused with a model-generated metric or verdict chip.
 */
@Composable
fun LabelCard(
    modifier: Modifier = Modifier,
    labelType: String,
    value: String,
    photoId: String,
    notes: String?,
    createdAt: String?,
) {
    val glow = LocalGlowColors.current
    GlowCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .padding(end = GlowSpacing.sm)
                        .background(glow.ink600.copy(alpha = 0.12f), CircleShape)
                        .padding(6.dp),
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = glow.ink600)
            }
            Column {
                Text(
                    text = "User note · $labelType",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = glow.ink600,
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = glow.ink900)
            }
        }
        Text(
            text = "Capture $photoId" + (createdAt?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = GlowSpacing.xs),
        )
        if (!notes.isNullOrBlank()) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun InsightsComponentsPreviewLight() {
    GlowUpTheme(darkTheme = false) { PreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun InsightsComponentsPreviewDark() {
    GlowUpTheme(darkTheme = true) { PreviewContent() }
}

@Composable
private fun PreviewContent() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChatBubble(isUser = true, text = "Why did my redness change this week?")
        ChatBubble(
            isUser = false,
            text = "Redness dropped 12% since you started the new moisturizer.",
            citations = listOf(Citation(type = "capture", date = "2026-08-10", id = "cap_12345678")),
        )
        ChatBubble(isUser = false, text = "", pending = true)
        ChatBubble(
            isUser = false,
            text = "This reads as something a dermatologist should look at directly.",
            isSafetyHandoff = true,
        )
        LabelCard(
            labelType = "user_note",
            value = "New sunscreen started here",
            photoId = "cap_98765432",
            notes = "Felt drier than usual.",
            createdAt = "2026-08-20",
        )
    }
}
