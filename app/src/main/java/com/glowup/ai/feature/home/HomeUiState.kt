package com.glowup.ai.feature.home

import com.glowup.ai.domain.model.CheckInRoutineState
import com.glowup.ai.domain.model.CheckInSkinFeel
import com.glowup.ai.domain.model.Dashboard
import com.glowup.ai.domain.model.HistoryItem
import com.glowup.ai.domain.model.PrimaryMetric
import com.glowup.ai.domain.model.Streak
import com.glowup.ai.domain.model.UserAchievement

/**
 * `feature/home`'s screen-level state. `GET /dashboard` is the single initial snapshot (task
 * 3.3); `GET /history` is fetched alongside it but is allowed to fail/lag independently, so
 * [HomeUiState.Content] tracks its own staleness/error rather than failing the whole screen when
 * only the history call has trouble (the dashboard response embeds a `history[]` array too, used
 * as the fallback source in that case — see [HomeViewModel]).
 *
 * There is deliberately no top-level `Empty`/`Locked` variant here: those are section-local
 * concerns (verdicts, experiments, the history chart) driven by `profile.entitlement`/`features`
 * first per frontend-api-map.md trap #5, not a single screen-wide flag. A screen with genuinely
 * nothing to show yet still reaches [Content] — its sections render their own [core.ui.EmptyState]
 * naming the next action.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Error(
        val message: String,
    ) : HomeUiState

    data class Content(
        val dashboard: Dashboard,
        /** True when [dashboard] is a cached copy shown while a background refresh is in
         * flight or failed — see [dashboardRefreshError]. */
        val dashboardStale: Boolean,
        val dashboardRefreshError: String?,
        val history: List<HistoryItem>,
        val historyStale: Boolean,
        val historyError: String?,
        /** True only while an explicit, user-triggered refresh is in flight (trap #7: never a
         * polling indicator). */
        val isRefreshing: Boolean,
        val selectedMetric: PrimaryMetric,
        val checkInSheetVisible: Boolean,
        val checkInSubmitting: Boolean,
        val checkInError: String?,
        /** Calculated streak from capture history using loss aversion psychology */
        val streak: Streak,
        /** All achievements with progress and unlock status */
        val achievements: List<UserAchievement> = emptyList(),
        /** Achievement to show in celebration dialog, null if none */
        val celebrationAchievement: UserAchievement? = null,
    ) : HomeUiState
}

/** The four metrics a capture's history can be charted on — reuses the domain enum that already
 * knows the backend's exact field spellings ([PrimaryMetric.toWire]) rather than inventing a new
 * one for the chart. */
val chartableMetrics: List<PrimaryMetric> =
    listOf(
        PrimaryMetric.REDNESS_SCORE,
        PrimaryMetric.BLEMISH_COUNT,
        PrimaryMetric.DARKSPOT_AREA,
        PrimaryMetric.TEXTURE_SCORE,
    )

fun PrimaryMetric.label(): String =
    when (this) {
        PrimaryMetric.REDNESS_SCORE -> "Redness"
        PrimaryMetric.BLEMISH_COUNT -> "Blemishes"
        PrimaryMetric.DARKSPOT_AREA -> "Dark spots"
        PrimaryMetric.TEXTURE_SCORE -> "Texture"
        PrimaryMetric.UNKNOWN -> "Metric"
    }

/** Whether a rising value is an improvement for this metric — texture is the only "more is
 * better" measurement of the four; the rest read as worse the higher they go. */
fun PrimaryMetric.higherIsBetter(): Boolean = this == PrimaryMetric.TEXTURE_SCORE

fun HistoryItem.valueFor(metric: PrimaryMetric): Double? =
    when (metric) {
        PrimaryMetric.REDNESS_SCORE -> rednessScore
        PrimaryMetric.BLEMISH_COUNT -> blemishCount
        PrimaryMetric.DARKSPOT_AREA -> darkspotArea
        PrimaryMetric.TEXTURE_SCORE -> textureScore
        PrimaryMetric.UNKNOWN -> null
    }

fun formatMetricValue(
    metric: PrimaryMetric,
    value: Double,
): String =
    when (metric) {
        PrimaryMetric.BLEMISH_COUNT -> value.toInt().toString()
        else -> String.format("%.2f", value)
    }

/** Human copy for a `GET /check-ins` routine_state / skin_feel value — kept here rather than in
 * `domain/model` since it is presentation-only. */
fun CheckInRoutineState.displayLabel(): String =
    when (this) {
        CheckInRoutineState.STEADY -> "Routine steady"
        CheckInRoutineState.CHANGED -> "Routine changed"
        CheckInRoutineState.MISSED -> "Missed routine"
        CheckInRoutineState.NOT_SURE -> "Not sure"
        CheckInRoutineState.UNKNOWN -> "Unknown"
    }

fun CheckInSkinFeel.displayLabel(): String =
    when (this) {
        CheckInSkinFeel.BETTER -> "Feels better"
        CheckInSkinFeel.SAME -> "Feels the same"
        CheckInSkinFeel.WORSE -> "Feels worse"
        CheckInSkinFeel.NOT_SURE -> "Not sure"
        CheckInSkinFeel.UNKNOWN -> "Unknown"
    }
