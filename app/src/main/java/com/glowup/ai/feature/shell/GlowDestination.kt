package com.glowup.ai.feature.shell

import kotlinx.serialization.Serializable

/**
 * The full, closed set of navigable destinations in the app.
 *
 * THIS IS THE PHASE 3 CONCURRENCY CONTRACT. Every destination any feature
 * package will ever navigate to is declared here, once, up front. Feature
 * agents must not add new destinations to this file or to [GlowNavGraph] —
 * if a screen needs a new destination, that is a cross-cutting change and
 * must go through the owner of the `feature/shell` package, not be added unilaterally
 * inside a single feature package.
 *
 * Every entry is `@Serializable` so it can be used directly as a type-safe
 * Navigation Compose route (`NavGraphBuilder.composable<T>`), and every
 * argument-bearing entry carries only primitive, directly-serializable data
 * (ids as `String`) — never domain models — so feature graphs stay decoupled
 * from each other's model types.
 */
@Serializable
sealed interface GlowDestination {

    // ---- feature/auth -----------------------------------------------------
    @Serializable data object Splash : GlowDestination
    @Serializable data object Welcome : GlowDestination
    @Serializable data object SignIn : GlowDestination

    // ---- feature/onboarding ------------------------------------------------
    @Serializable data object Onboarding : GlowDestination
    @Serializable data object Consent : GlowDestination

    // ---- feature/home -------------------------------------------------------
    @Serializable data object Home : GlowDestination

    // ---- feature/capture ----------------------------------------------------
    @Serializable data object Capture : GlowDestination
    @Serializable data class CaptureResult(val captureId: String) : GlowDestination

    // ---- feature/routine ------------------------------------------------------
    @Serializable data object Routine : GlowDestination
    @Serializable data class ProductDetail(val productId: String) : GlowDestination
    @Serializable data object ShelfScan : GlowDestination
    @Serializable data object Experiments : GlowDestination
    @Serializable data class ExperimentDetail(val experimentId: String) : GlowDestination

    // ---- feature/insights -----------------------------------------------------
    @Serializable data object Insights : GlowDestination
    @Serializable data class QnaThread(val threadId: String? = null) : GlowDestination
    @Serializable data object ContextLog : GlowDestination
    @Serializable data object RootCause : GlowDestination
    @Serializable data object BudgetOptimizer : GlowDestination
    @Serializable data object DermExport : GlowDestination

    // ---- feature/discover -----------------------------------------------------
    // Reachable from Home and from the product picker (feature/routine); NOT a bottom tab.
    @Serializable data object Discover : GlowDestination

    // ---- feature/account --------------------------------------------------------
    @Serializable data object Account : GlowDestination
    @Serializable data object Paywall : GlowDestination
    @Serializable data object Settings : GlowDestination
    @Serializable data object DataAndPrivacy : GlowDestination

    companion object {
        /** Destinations that render full-screen, chromeless — no bottom bar. */
        val fullScreenRoutes: Set<kotlin.reflect.KClass<out GlowDestination>> = setOf(
            Splash::class,
            Welcome::class,
            SignIn::class,
            Onboarding::class,
            Consent::class,
            Capture::class,
            CaptureResult::class,
        )
    }
}
