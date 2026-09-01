package com.glowup.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glowup.ai.core.design.GlowUpTheme
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.data.repository.SessionRepository
import com.glowup.ai.data.telemetry.Telemetry
import com.glowup.ai.feature.account.ThemePreference
import com.glowup.ai.feature.shell.GlowDestination
import com.glowup.ai.feature.shell.GlowUpApp
import com.glowup.ai.feature.shell.destinationFromIntent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * App entry point. Deliberately thin: this activity hosts the Compose tree and nothing else.
 * All screens live under feature packages and are wired by the app shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionStore: SessionStore

    @Inject lateinit var sessionRepository: SessionRepository

    @Inject lateinit var telemetry: Telemetry

    private var pendingDestination by mutableStateOf<GlowDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingDestination =
            if (savedInstanceState?.getBoolean(STATE_OPEN_CAPTURE) == true) {
                GlowDestination.Capture
            } else {
                destinationFromIntent(intent)
            }

        setContent {
            val themePreference by sessionStore.themePreferenceFlow
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM.storageValue)
            val darkTheme =
                when (ThemePreference.fromStorage(themePreference)) {
                    ThemePreference.LIGHT -> false
                    ThemePreference.DARK -> true
                    ThemePreference.SYSTEM -> isSystemInDarkTheme()
                }
            GlowUpTheme(darkTheme = darkTheme) {
                GlowUpApp(
                    sessionStore = sessionStore,
                    sessionRepository = sessionRepository,
                    telemetry = telemetry,
                    pendingDestination = pendingDestination,
                    onPendingDestinationConsumed = { consumed ->
                        if (pendingDestination == consumed) {
                            pendingDestination = null
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = destinationFromIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_OPEN_CAPTURE, pendingDestination == GlowDestination.Capture)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_OPEN_CAPTURE = "open_capture_requested"
    }
}
