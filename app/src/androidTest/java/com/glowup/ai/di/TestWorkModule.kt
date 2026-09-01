package com.glowup.ai.di

import com.glowup.ai.data.work.ReminderNotifier
import com.glowup.ai.data.work.WorkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module that replaces WorkModule for instrumented tests.
 *
 * Provides a fake ReminderNotifier that doesn't send actual notifications.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [WorkModule::class]
)
object TestWorkModule {
    @Provides
    @Singleton
    fun provideReminderNotifier(): ReminderNotifier = object : ReminderNotifier {
        override suspend fun scheduleReminder(userId: String, timeMillis: Long) {
            // No-op for tests
        }

        override suspend fun cancelReminder(userId: String) {
            // No-op for tests
        }

        override suspend fun showImmediateNotification(title: String, message: String) {
            // No-op for tests
        }
    }
}
