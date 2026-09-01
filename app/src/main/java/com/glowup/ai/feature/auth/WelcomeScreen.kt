package com.glowup.ai.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.glowup.ai.core.design.GlowSpacing
import com.glowup.ai.core.design.LocalGlowColors
import com.glowup.ai.core.ui.DisclaimerNote
import com.glowup.ai.core.ui.ErrorState
import com.glowup.ai.core.ui.GlowButton
import com.glowup.ai.core.ui.GlowButtonVariant
import com.glowup.ai.feature.shell.GlowDestination

/**
 * [GlowDestination.Welcome]. The identity choice screen: "Continue with Google" (Firebase
 * generic-OAuth flow, see [FirebaseAuthGateway.signInWithGoogle]) or "Continue with email", which
 * hands off to [GlowDestination.SignIn] for the email/password + create-account flow.
 */
@Composable
fun WelcomeRoute(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        navController.navigate(destination) {
            popUpTo(GlowDestination.Welcome) { inclusive = true }
            launchSingleTop = true
        }
        viewModel.consumeNavigationTarget()
    }

    WelcomeContent(
        uiState = uiState,
        onContinueWithGoogle = {
            context.findActivity()?.let(viewModel::signInWithGoogle)
        },
        onContinueWithEmail = { navController.navigate(GlowDestination.SignIn) },
    )
}

@Composable
private fun WelcomeContent(
    uiState: AuthUiState,
    onContinueWithGoogle: () -> Unit,
    onContinueWithEmail: () -> Unit,
) {
    val glow = LocalGlowColors.current
    val isBusy = uiState is AuthUiState.Authenticating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(glow.paper)
            .padding(GlowSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top spacing - 80dp
        Spacer(modifier = Modifier.height(80.dp))

        // Animated gradient orb illustration
        AnimatedGradientOrb()

        // Spacing after orb - 48dp
        Spacer(modifier = Modifier.height(48.dp))

        // Title - 32sp SemiBold
        Text(
            text = "Track your skin,\nwith evidence. ✨",
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = glow.ink900,
            lineHeight = 40.sp,
            letterSpacing = (-0.02).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Spacing - 16dp
        Spacer(modifier = Modifier.height(16.dp))

        // Body text - 16sp Regular
        Text(
            text = "Guided photo tracking, routine testing, and honest verdicts — never a diagnosis.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = glow.ink600,
            lineHeight = 24.sp,
            letterSpacing = 0.02.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Flexible spacer to push buttons to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Error state if present
        if (uiState is AuthUiState.Error) {
            ErrorState(
                modifier = Modifier.padding(bottom = GlowSpacing.md),
                message = uiState.message,
                retryLabel = "Try again",
                onRetry = onContinueWithGoogle,
            )
        }

        // Primary button - Continue with Google
        PremiumPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "✨ Continue with Google",
            onClick = onContinueWithGoogle,
            enabled = !isBusy,
            loading = isBusy,
        )

        // Spacing - 16dp (following 8-point grid)
        Spacer(modifier = Modifier.height(16.dp))

        // Secondary button - Continue with email
        PremiumSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue with email",
            onClick = onContinueWithEmail,
            enabled = !isBusy,
        )

        // Spacing - 48dp
        Spacer(modifier = Modifier.height(48.dp))

        // Improved disclaimer card
        EnhancedDisclaimerNote(
            text = "💚 GlowUp AI tracks cosmetic skin appearance over time. It is not a diagnosis and does not replace a dermatologist.",
        )

        // Spacing - 24dp
        Spacer(modifier = Modifier.height(24.dp))

        // Consent notice
        Text(
            text = "By continuing you agree we'll ask for your explicit consent before analyzing any photo.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = glow.ink600.copy(alpha = 0.8f),
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Bottom spacing - 24dp
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Animated gradient orb with subtle pulse animation - Cal.ai style
 */
@Composable
private fun AnimatedGradientOrb() {
    val glow = LocalGlowColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")

    // Pulse animation - scale between 0.95 and 1.05 over 2 seconds
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    // Opacity pulse for glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbGlow"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow layer
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .alpha(glowAlpha)
                .blur(32.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glow.honey400.copy(alpha = 0.8f),
                            glow.honey500.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main gradient orb
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glow.honey400,
                            glow.honey500,
                            glow.honey600
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Premium primary button with gradient background and elevation
 */
@Composable
private fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    GlowButton(
        modifier = modifier.height(56.dp),
        text = text,
        onClick = onClick,
        variant = GlowButtonVariant.Primary,
        enabled = enabled,
        loading = loading,
        contentDescription = text,
    )
}

/**
 * Premium secondary button with refined styling
 */
@Composable
private fun PremiumSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlowButton(
        modifier = modifier.height(56.dp),
        text = text,
        onClick = onClick,
        variant = GlowButtonVariant.Secondary,
        enabled = enabled,
        contentDescription = text,
    )
}

/**
 * Enhanced disclaimer note with improved styling and soft shadow
 */
@Composable
private fun EnhancedDisclaimerNote(text: String) {
    val glow = LocalGlowColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = glow.surfaceCard,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = glow.ink600,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Standard Compose recipe for recovering the hosting [Activity] from a (possibly wrapped)
 * [Context] — needed because Firebase's `startActivityForSignInWithProvider` requires one. */
internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
