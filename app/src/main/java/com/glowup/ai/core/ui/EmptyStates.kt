package com.glowup.ai.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Empty State Components
 * Friendly, actionable empty states instead of blank screens
 * Following best practices: explain why empty + clear next action
 */

/**
 * Generic empty state component
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Action button
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))

            GlowButton(
                onClick = onActionClick
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * No captures yet empty state
 */
@Composable
fun NoCapturesEmptyState(
    onTakeCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.CameraAlt,
        title = "No Captures Yet",
        description = "Take your first photo to start tracking your skincare journey. Consistent photos help you see real progress over time.",
        actionLabel = "Take First Capture",
        onActionClick = onTakeCaptureClick,
        modifier = modifier
    )
}

/**
 * No products empty state
 */
@Composable
fun NoProductsEmptyState(
    onAddProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.ShoppingBag,
        title = "No Products Yet",
        description = "Add products to your routine to track what works for your skin. You can scan products or add them manually.",
        actionLabel = "Add First Product",
        onActionClick = onAddProductClick,
        modifier = modifier
    )
}

/**
 * No experiments empty state
 */
@Composable
fun NoExperimentsEmptyState(
    onCreateExperimentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.Science,
        title = "No Experiments Yet",
        description = "Test products scientifically with A/B experiments. Track what actually works for YOUR skin with data, not guesswork.",
        actionLabel = "Create Experiment",
        onActionClick = onCreateExperimentClick,
        modifier = modifier
    )
}

/**
 * No routine events empty state
 */
@Composable
fun NoRoutineEmptyState(
    onLogEventClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.EventNote,
        title = "No Routine Events",
        description = "Log when you start, stop, or change products. This helps you understand what's working over time.",
        actionLabel = "Log First Event",
        onActionClick = onLogEventClick,
        modifier = modifier
    )
}

/**
 * No history/data empty state
 */
@Composable
fun NoHistoryEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.TrendingUp,
        title = "No History Yet",
        description = "Your progress charts will appear here once you have multiple captures. Keep capturing to see your trends!",
        modifier = modifier
    )
}

/**
 * No search results empty state
 */
@Composable
fun NoSearchResultsEmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.SearchOff,
        title = "No Results Found",
        description = "We couldn't find any matches for \"$query\". Try a different search term or browse all items.",
        modifier = modifier
    )
}

/**
 * Offline empty state
 */
@Composable
fun OfflineEmptyState(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.CloudOff,
        title = "You're Offline",
        description = "Some features require an internet connection. Don't worry - your captures are saved locally and will sync when you're back online.",
        actionLabel = "Retry",
        onActionClick = onRetryClick,
        modifier = modifier
    )
}

/**
 * Error empty state
 */
@Composable
fun ErrorEmptyState(
    error: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.ErrorOutline,
        title = "Something Went Wrong",
        description = error,
        actionLabel = "Try Again",
        onActionClick = onRetryClick,
        modifier = modifier
    )
}

/**
 * Premium locked empty state
 */
@Composable
fun PremiumLockedEmptyState(
    featureName: String,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.Lock,
        title = "Premium Feature",
        description = "$featureName is available with Premium. Upgrade to unlock unlimited history, experiments, and insights.",
        actionLabel = "Upgrade to Premium",
        onActionClick = onUpgradeClick,
        modifier = modifier
    )
}

/**
 * Consent required empty state
 */
@Composable
fun ConsentRequiredEmptyState(
    onReviewConsentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.Security,
        title = "Consent Required",
        description = "To use photo tracking features, we need your consent to process facial images. Your privacy is important to us.",
        actionLabel = "Review Consent",
        onActionClick = onReviewConsentClick,
        modifier = modifier
    )
}

/**
 * Baseline required empty state
 */
@Composable
fun BaselineRequiredEmptyState(
    onTakeBaselineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.Flag,
        title = "Baseline Needed",
        description = "Take your first baseline photo to establish your starting point. This helps us track your progress accurately.",
        actionLabel = "Take Baseline",
        onActionClick = onTakeBaselineClick,
        modifier = modifier
    )
}

/**
 * Coming soon empty state
 */
@Composable
fun ComingSoonEmptyState(
    featureName: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.Schedule,
        title = "Coming Soon",
        description = "$featureName is coming in a future update. We're working hard to bring you new features!",
        modifier = modifier
    )
}

/**
 * No achievements unlocked empty state
 */
@Composable
fun NoAchievementsEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.EmojiEvents,
        title = "Start Your Journey",
        description = "Unlock achievements by taking captures, building your routine, and running experiments. Your first achievement is just one capture away!",
        modifier = modifier
    )
}

/**
 * No Q&A threads empty state
 */
@Composable
fun NoQnaEmptyState(
    onAskQuestionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.QuestionAnswer,
        title = "No Questions Yet",
        description = "Have questions about your skin or routine? Ask our AI assistant for personalized insights based on your data.",
        actionLabel = "Ask a Question",
        onActionClick = onAskQuestionClick,
        modifier = modifier
    )
}

/**
 * No notifications empty state
 */
@Composable
fun NoNotificationsEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Filled.NotificationsNone,
        title = "All Caught Up",
        description = "You don't have any notifications right now. We'll notify you about important updates and reminders.",
        modifier = modifier
    )
}
