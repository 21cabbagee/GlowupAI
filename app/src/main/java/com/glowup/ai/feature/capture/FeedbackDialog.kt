package com.glowup.ai.feature.capture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField

/**
 * Feedback dialog shown after capture to collect user feedback on analysis accuracy.
 * Part of the data collection pipeline for model improvements.
 */
@Composable
fun FeedbackDialog(
    captureId: String,
    onDismiss: () -> Unit,
    onSubmit: (FeedbackData) -> Unit,
    modifier: Modifier = Modifier
) {
    var feedbackType by remember { mutableStateOf<FeedbackType?>(null) }
    var selectedIssues by remember { mutableStateOf(setOf<String>()) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlowCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title
                Text(
                    text = "Was this analysis accurate?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Your feedback helps us improve our AI model",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalGlowColors.current.textSecondary
                )

                // Thumbs up/down selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thumbs Up
                    GlowButton(
                        onClick = { feedbackType = FeedbackType.ACCURATE },
                        variant = if (feedbackType == FeedbackType.ACCURATE)
                            GlowButtonVariant.Primary
                        else
                            GlowButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Accurate",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accurate")
                    }

                    // Thumbs Down
                    GlowButton(
                        onClick = { feedbackType = FeedbackType.INACCURATE },
                        variant = if (feedbackType == FeedbackType.INACCURATE)
                            GlowButtonVariant.Primary
                        else
                            GlowButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = "Inaccurate",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inaccurate")
                    }
                }

                // Show issue selection if inaccurate
                if (feedbackType == FeedbackType.INACCURATE) {
                    Text(
                        text = "What seems wrong?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IssueOption(
                            issue = "blemishes_too_high",
                            label = "Blemish count too high",
                            selected = "blemishes_too_high" in selectedIssues,
                            onToggle = { selected ->
                                selectedIssues = if (selected) {
                                    selectedIssues + "blemishes_too_high"
                                } else {
                                    selectedIssues - "blemishes_too_high"
                                }
                            }
                        )

                        IssueOption(
                            issue = "blemishes_too_low",
                            label = "Blemish count too low",
                            selected = "blemishes_too_low" in selectedIssues,
                            onToggle = { selected ->
                                selectedIssues = if (selected) {
                                    selectedIssues + "blemishes_too_low"
                                } else {
                                    selectedIssues - "blemishes_too_low"
                                }
                            }
                        )

                        IssueOption(
                            issue = "redness_too_high",
                            label = "Redness score too high",
                            selected = "redness_too_high" in selectedIssues,
                            onToggle = { selected ->
                                selectedIssues = if (selected) {
                                    selectedIssues + "redness_too_high"
                                } else {
                                    selectedIssues - "redness_too_high"
                                }
                            }
                        )

                        IssueOption(
                            issue = "redness_too_low",
                            label = "Redness score too low",
                            selected = "redness_too_low" in selectedIssues,
                            onToggle = { selected ->
                                selectedIssues = if (selected) {
                                    selectedIssues + "redness_too_low"
                                } else {
                                    selectedIssues - "redness_too_low"
                                }
                            }
                        )

                        IssueOption(
                            issue = "texture_wrong",
                            label = "Texture analysis seems off",
                            selected = "texture_wrong" in selectedIssues,
                            onToggle = { selected ->
                                selectedIssues = if (selected) {
                                    selectedIssues + "texture_wrong"
                                } else {
                                    selectedIssues - "texture_wrong"
                                }
                            }
                        )

                        IssueOption(
                            issue = "darkspots_wrong",
                            label = "Dark spots detection incorrect",
                            selected = "darkspots_wrong" in selectedIssues,
                            onToggle = { selected ->
                                selectedIssues = if (selected) {
                                    selectedIssues + "darkspots_wrong"
                                } else {
                                    selectedIssues - "darkspots_wrong"
                                }
                            }
                        )
                    }

                    // Optional comment field
                    GlowTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Additional details (optional)") },
                        placeholder = { Text("Tell us more about what's wrong...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlowButton(
                        onClick = onDismiss,
                        variant = GlowButtonVariant.Tertiary,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Skip")
                    }

                    GlowButton(
                        onClick = {
                            feedbackType?.let { type ->
                                isSubmitting = true
                                onSubmit(
                                    FeedbackData(
                                        captureId = captureId,
                                        feedbackType = type,
                                        issues = if (type == FeedbackType.INACCURATE)
                                            selectedIssues.toList()
                                        else
                                            emptyList(),
                                        comment = comment.takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                        },
                        variant = GlowButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                        enabled = feedbackType != null && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueOption(
    issue: String,
    label: String,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onToggle
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

enum class FeedbackType {
    ACCURATE,
    INACCURATE
}

data class FeedbackData(
    val captureId: String,
    val feedbackType: FeedbackType,
    val issues: List<String>,
    val comment: String?
)
