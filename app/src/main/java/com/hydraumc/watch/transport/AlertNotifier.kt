// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/AlertNotifier.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hydraumc.watch.MainActivity
import com.hydraumc.watch.R
import com.hydraumc.watch.WatchConfig
import com.hydraumc.watch.haptics.AlertSeverity
import com.hydraumc.watch.protocol.SyncMessage

/** Shows an alert the phone forwarded as a notification, as [AlertPolicy] decides (a critical one stays until opened). */
class AlertNotifier(private val context: Context, private val config: WatchConfig = WatchConfig.load(context)) {
    /** Posts [alert] when the policy asks for a notification; returns the way it was presented. */
    fun present(alert: SyncMessage.Alert, screenAttached: Boolean, permissionGranted: Boolean): AlertPresentation {
        val presentation = AlertPolicy.presentation(alert.severity, screenAttached, permissionGranted, config.alertsCriticalPersistent)
        if (presentation == AlertPresentation.SCREEN_ONLY) return presentation
        ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val label = context.getString(
            when (alert.severity) {
                AlertSeverity.CRITICAL -> R.string.alert_critical
                AlertSeverity.WARNING -> R.string.alert_warning
                AlertSeverity.INFO -> R.string.alert_info
            },
        )
        val persistent = presentation == AlertPresentation.PERSISTENT_NOTIFICATION
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(label)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setOngoing(persistent)
            .setAutoCancel(!persistent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            presentation
        } catch (_: SecurityException) {
            AlertPresentation.SCREEN_ONLY
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_alerts), NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "relay_alerts"
        const val NOTIFICATION_ID = 4302
    }
}
