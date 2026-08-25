package com.glowup.ai.feature.routine.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.domain.model.ConfoundCheck
import com.glowup.ai.domain.model.RoutineAction

/**
 * The `start`/`stop`/`change` logging form. There is deliberately no fourth option and no
 * "applied today" tick box — `POST /api/routine-events` only accepts these three actions
 * (frontend-api-map.md trap #1 / ANDROID_PLAN.md §3 bug #1), and [RoutineAction] enforces this at
 * the type level.
 */
@Composable
fun RoutineEventSheet(
    modifier: Modifier = Modifier,
    productName: String,
    initialAction: RoutineAction,
    confoundCheck: ConfoundCheck?,
    confoundCheckLoading: Boolean,
    onDismissConfound: () -> Unit,
    onActionChange: (RoutineAction) -> Unit = {},
    submitting: Boolean,
    errorText: String?,
    onSubmit: (action: RoutineAction, slot: String, dose: String?, frequency: String?, notes: String?) -> Unit,
) {
    val glow = LocalGlowColors.current
    var action by remember { mutableStateOf(initialAction) }
    var slot by remember { mutableStateOf("unspecified") }
    var dose by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { onActionChange(initialAction) }

    Column(modifier = modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = productName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = glow.ink900,
        )
        Text(
            text = "Log a routine change — not a daily \"I used this\" tick.",
            style = MaterialTheme.typography.bodySmall,
            color = glow.ink600,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                RoutineAction.START to "Start",
                RoutineAction.CHANGE to "Change",
                RoutineAction.STOP to "Stop",
            ).forEach { (candidate, label) ->
                GlowButton(
                    modifier = Modifier.weight(1f),
                    text = label,
                    variant = if (action == candidate) GlowButtonVariant.Primary else GlowButtonVariant.Secondary,
                    enabled = !submitting,
                    onClick = {
                        action = candidate
                        onActionChange(candidate)
                    },
                )
            }
        }

        if (confoundCheckLoading) {
            Text(
                text = "Checking for overlapping routine changes…",
                style = MaterialTheme.typography.bodySmall,
                color = glow.ink600,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        if (confoundCheck != null) {
            ConfoundWarningBanner(
                modifier = Modifier.padding(bottom = 12.dp),
                confound = confoundCheck,
                onDismiss = onDismissConfound,
            )
        }

        GlowTextField(
            value = slot,
            onValueChange = { slot = it },
            label = "Slot",
            supportingText = "e.g. am, pm, unspecified",
            enabled = !submitting,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = dose,
            onValueChange = { dose = it },
            label = "Dose (optional)",
            enabled = !submitting,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = frequency,
            onValueChange = { frequency = it },
            label = "Frequency (optional)",
            placeholder = "e.g. nightly, twice a week",
            enabled = !submitting,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        GlowTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes (optional)",
            singleLine = false,
            enabled = !submitting,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = glow.danger,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        GlowButton(
            text = "Log ${action.name.lowercase()}",
            loading = submitting,
            enabled = !submitting,
            onClick = {
                onSubmit(
                    action,
                    slot.trim().ifBlank { "unspecified" },
                    dose.trim().ifBlank { null },
                    frequency.trim().ifBlank { null },
                    notes.trim().ifBlank { null },
                )
            },
        )
    }
}
