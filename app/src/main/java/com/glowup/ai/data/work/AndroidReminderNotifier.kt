package com.glowup.ai.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.glowup.ai.MainActivity
import com.glowup.ai.data.local.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Posts the local capture nudge after the server-driven reminder worker fires. */
class AndroidReminderNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReminderNotifier {
        override suspend fun notifyCaptureDue(settings: SessionStore.ReminderSettings) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            ensureChannel()
            val body =
                when {
                    settings.windowStart != null && settings.windowEnd != null -> {
                        "Your next capture window is open. A consistent photo keeps your trend useful."
                    }

                    else -> {
                        "A quick, consistent photo keeps your skin trend up to date."
                    }
                }
            val openApp =
                PendingIntent.getActivity(
                    context,
                    NOTIFICATION_ID,
                    Intent(context, MainActivity::class.java).apply {
                        action = ACTION_OPEN_CAPTURE
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(com.glowup.ai.R.drawable.ic_launcher_foreground)
                    .setContentTitle("Time for your GlowUp capture")
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(openApp)
                    .build()
            runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Capture reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Reminders based on your next server-provided capture window"
                },
            )
        }

        private companion object {
            const val CHANNEL_ID = "capture_reminders"
            const val NOTIFICATION_ID = 4101
            const val ACTION_OPEN_CAPTURE = "com.glowup.ai.action.OPEN_CAPTURE"
        }
    }
