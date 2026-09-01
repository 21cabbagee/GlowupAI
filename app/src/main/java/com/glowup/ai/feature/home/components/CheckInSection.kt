package com.glowup.ai.feature.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.EmptyState
import com.glowup.ai.core.ui.GlowBottomSheet
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowCard
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.core.ui.SectionHeader
import com.glowup.ai.domain.model.CheckIn
import com.glowup.ai.domain.model.CheckInRoutineState
import com.glowup.ai.domain.model.CheckInSkinFeel
import com.glowup.ai.feature.home.displayLabel

/**
 * `POST /check-ins` entry point plus a summary of the dashboard's embedded `check_ins[]` — never
 * a separate `GET /check-ins` round trip on Home (the dashboard snapshot already carries it).
 */
@Composable
fun CheckInSection(
    modifier: Modifier = Modifier,
    checkIns: List<CheckIn>,
    onCheckInClick: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Daily check-in")
        if (checkIns.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(top = 12.dp),
                title = "No check-ins yet",
                body = "A quick daily note on your routine and how your skin feels helps explain what the numbers show.",
                ctaLabel = "Check in now",
                onCtaClick = onCheckInClick,
            )
        } else {
            GlowCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                checkIns.take(3).forEachIndexed { index, checkIn ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = glow.ink600.copy(alpha = 0.12f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = checkIn.routineState.displayLabel(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glow.ink900,
                        )
                        Text(
                            text = checkIn.skinFeel.displayLabel(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = glow.ink600,
                        )
                    }
                    if (!checkIn.note.isNullOrBlank()) {
                        Text(
                            text = checkIn.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = glow.ink600,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                GlowButton(
                    modifier = Modifier.padding(top = 12.dp),
                    text = "Check in today",
                    onClick = onCheckInClick,
                    variant = GlowButtonVariant.Secondary,
                )
            }
        }
    }
}

/**
 * `POST /check-ins` sheet. All three fields have a `not_sure`/`missed`-style neutral default so
 * skipping a question never silently records a false positive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInSheet(
    onDismiss: () -> Unit,
    submitting: Boolean,
    errorMessage: String?,
    onSubmit: (CheckInRoutineState, CheckInSkinFeel, String?) -> Unit,
) {
    var routineState by remember { mutableStateOf(CheckInRoutineState.STEADY) }
    var skinFeel by remember { mutableStateOf(CheckInSkinFeel.NOT_SURE) }
    var note by remember { mutableStateOf("") }
    val glow = LocalGlowColors.current

    GlowBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "How's your skin today?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )

            Text(
                text = "Routine",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink600,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            ChoiceRow(
                options =
                    listOf(
                        CheckInRoutineState.STEADY,
                        CheckInRoutineState.CHANGED,
                        CheckInRoutineState.MISSED,
                        CheckInRoutineState.NOT_SURE,
                    ),
                selected = routineState,
                labelOf = { it.compactLabel() },
                onSelected = { routineState = it },
            )

            Text(
                text = "Skin feel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = glow.ink600,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            ChoiceRow(
                options =
                    listOf(
                        CheckInSkinFeel.BETTER,
                        CheckInSkinFeel.SAME,
                        CheckInSkinFeel.WORSE,
                        CheckInSkinFeel.NOT_SURE,
                    ),
                selected = skinFeel,
                labelOf = { it.compactLabel() },
                onSelected = { skinFeel = it },
            )

            GlowTextField(
                modifier = Modifier.padding(top = 16.dp),
                value = note,
                onValueChange = { note = it },
                label = "Note (optional)",
                placeholder = "Anything worth remembering today?",
                singleLine = false,
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = glow.danger,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            GlowButton(
                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp).fillMaxWidth(),
                text = "Save check-in",
                onClick = { onSubmit(routineState, skinFeel, note) },
                loading = submitting,
            )
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            GlowButton(
                text = labelOf(option),
                onClick = { onSelected(option) },
                variant = if (isSelected) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                contentDescription = "${labelOf(option)}${if (isSelected) ", selected" else ""}",
            )
        }
    }
}

private fun CheckInRoutineState.compactLabel(): String =
    when (this) {
        CheckInRoutineState.STEADY -> "Steady"
        CheckInRoutineState.CHANGED -> "Changed"
        CheckInRoutineState.MISSED -> "Missed"
        CheckInRoutineState.NOT_SURE -> "Not sure"
        CheckInRoutineState.UNKNOWN -> "Unknown"
    }

private fun CheckInSkinFeel.compactLabel(): String =
    when (this) {
        CheckInSkinFeel.BETTER -> "Better"
        CheckInSkinFeel.SAME -> "Same"
        CheckInSkinFeel.WORSE -> "Worse"
        CheckInSkinFeel.NOT_SURE -> "Not sure"
        CheckInSkinFeel.UNKNOWN -> "Unknown"
    }
