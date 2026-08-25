package com.glowup.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt's generated component roots here; every
 * `@Inject`/`@HiltViewModel`/module in `com.glowup.ai.di.*` and feature packages
 * hangs off this component.
 */
@HiltAndroidApp
class GlowUpApplication : Application()
