package com.glowup.ai.feature.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowTextField
import com.glowup.ai.feature.shell.GlowDestination
import kotlinx.coroutines.launch

private data class CarouselPage(val title: String, val body: String)

private val carouselPages = listOf(
    CarouselPage(
        title = "See real change, not guesses",
        body = "Guided photo captures track redness, texture, and tone over weeks — with the " +
            "same lighting checks every time.",
    ),
    CarouselPage(
        title = "Test your routine like an experiment",
        body = "Log what you use, then see which changes actually correlate with your results.",
    ),
    CarouselPage(
        title = "We'll need a few permissions",
        body = "• Camera: Take consistent photos to track your skin\n" +
            "• Photo access: See your progress with before/after comparisons\n" +
            "• Notifications: Optional reminders for your daily capture",
    ),
    CarouselPage(
        title = "Your photos, your choice",
        body = "Nothing is analyzed until you explicitly say yes on the next screen — and you " +
            "can change your mind at any time.",
    ),
)

private val skinTypeOptions = listOf(
    "normal" to "Normal",
    "dry" to "Dry",
    "oily" to "Oily",
    "combination" to "Combination",
    "sensitive" to "Sensitive",
)

private val goalOptions = listOf(
    "reduce_redness" to "Reduce redness",
    "even_tone" to "Even out tone",
    "improve_texture" to "Improve texture",
    "test_routine" to "Test a routine change",
    "general_tracking" to "Just track over time",
)

private val experienceOptions = listOf(
    "new_to_skincare" to "New to skincare",
    "some_experience" to "Some experience",
    "very_experienced" to "Very experienced",
)

/**
 * [GlowDestination.Onboarding]: a 3-card value-prop carousel with a working "Skip" (the previous
 * app had no skip affordance at all), followed by the profile form
 * (`PATCH /profile`: display name, skin type, goals, experience level, `onboarding_complete`).
 */
@Composable
fun OnboardingRoute(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()

    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        navController.navigate(destination) {
            popUpTo(GlowDestination.Onboarding) { inclusive = true }
            launchSingleTop = true
        }
        viewModel.consumeNavigationTarget()
    }

    OnboardingContent(
        uiState = uiState,
        form = form,
        onDisplayNameChange = viewModel::updateDisplayName,
        onSkinTypeSelected = viewModel::setSkinType,
        onGoalToggled = viewModel::toggleGoal,
        onExperienceSelected = viewModel::setExperienceLevel,
        onSubmit = viewModel::submit,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    form: OnboardingFormState,
    onDisplayNameChange: (String) -> Unit,
    onSkinTypeSelected: (String) -> Unit,
    onGoalToggled: (String) -> Unit,
    onExperienceSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
) {
    // Step 0 = value carousel, step 1 = profile form. Purely local UI state — the view model
    // does not need to know which sub-step is showing.
    var step by rememberSaveable { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState is OnboardingUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Loading your profile…",
                        modifier = Modifier.semantics { contentDescription = "Loading your profile" },
                    )
                }
                uiState is OnboardingUiState.Error -> Box(Modifier.fillMaxSize().padding(GlowSpacing.lg), contentAlignment = Alignment.Center) {
                    ErrorState(message = uiState.message, onRetry = onRetry)
                }
                step == 0 -> ValueCarousel(onDone = { step = 1 })
                else -> ProfileForm(
                    form = form,
                    saving = uiState is OnboardingUiState.Saving,
                    onDisplayNameChange = onDisplayNameChange,
                    onSkinTypeSelected = onSkinTypeSelected,
                    onGoalToggled = onGoalToggled,
                    onExperienceSelected = onExperienceSelected,
                    onSubmit = onSubmit,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ValueCarousel(onDone: () -> Unit) {
    val glow = LocalGlowColors.current
    val pagerState = rememberPagerState(pageCount = { carouselPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == carouselPages.lastIndex

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GlowSpacing.md),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDone) {
                Text("Skip", color = glow.ink600)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            val content = carouselPages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = GlowSpacing.xl, vertical = GlowSpacing.lg),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = glow.ink900,
                )
                Text(
                    text = content.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = glow.ink600,
                    modifier = Modifier.padding(top = GlowSpacing.md),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GlowSpacing.sm)
                .semantics { contentDescription = "Page ${pagerState.currentPage + 1} of ${carouselPages.size}" },
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(carouselPages.size) { index ->
                val active = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (active) glow.honey500 else glow.ink600.copy(alpha = 0.25f)),
                )
            }
        }

        GlowButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = GlowSpacing.lg, vertical = GlowSpacing.md),
            text = if (isLastPage) "Get started" else "Next",
            onClick = {
                if (isLastPage) {
                    onDone()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
        )
    }
}

@Composable
private fun ProfileForm(
    form: OnboardingFormState,
    saving: Boolean,
    onDisplayNameChange: (String) -> Unit,
    onSkinTypeSelected: (String) -> Unit,
    onGoalToggled: (String) -> Unit,
    onExperienceSelected: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val glow = LocalGlowColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GlowSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlowSpacing.lg),
    ) {
        Column {
            Text(
                text = "Tell us a bit about you",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = glow.ink900,
            )
            Text(
                text = "Every field here is optional and can be changed later in your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = glow.ink600,
                modifier = Modifier.padding(top = GlowSpacing.xs),
            )
        }

        GlowTextField(
            value = form.displayName,
            onValueChange = onDisplayNameChange,
            label = "Display name",
            supportingText = "Shown on your profile only.",
            enabled = !saving,
        )

        OptionSection(title = "Skin type") {
            skinTypeOptions.forEach { (value, label) ->
                SelectableChip(
                    label = label,
                    selected = form.skinType == value,
                    enabled = !saving,
                    onClick = { onSkinTypeSelected(value) },
                )
            }
        }

        OptionSection(title = "What are you hoping to track?") {
            goalOptions.forEach { (value, label) ->
                SelectableChip(
                    label = label,
                    selected = value in form.goals,
                    enabled = !saving,
                    onClick = { onGoalToggled(value) },
                )
            }
        }

        OptionSection(title = "Skincare experience") {
            experienceOptions.forEach { (value, label) ->
                SelectableChip(
                    label = label,
                    selected = form.experienceLevel == value,
                    enabled = !saving,
                    onClick = { onExperienceSelected(value) },
                )
            }
        }

        GlowButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            loading = saving,
            enabled = !saving,
            onClick = onSubmit,
        )
    }
}

@Composable
private fun OptionSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    val glow = LocalGlowColors.current
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink900,
            modifier = Modifier.padding(bottom = GlowSpacing.sm),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GlowSpacing.sm),
            content = content,
        )
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val glow = LocalGlowColors.current
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = if (selected) "$label, selected" else label },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = glow.honey500,
            selectedLabelColor = glow.ink900,
        ),
    )
}

