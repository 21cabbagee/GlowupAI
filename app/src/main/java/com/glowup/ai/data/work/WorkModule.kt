package com.glowup.ai.data.work

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkModule {

    /** Real Android notifications are permission-gated inside AndroidReminderNotifier. */
    @Binds
    @Singleton
    abstract fun bindReminderNotifier(impl: AndroidReminderNotifier): ReminderNotifier
}
