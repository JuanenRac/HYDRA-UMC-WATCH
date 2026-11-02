// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/PresenceHeartbeatService.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.hydraumc.watch.MainActivity
import com.hydraumc.watch.R
import com.hydraumc.watch.WatchConfig

/**
 * Sends the heartbeat of the presence of the operator while the operator has switched it on, for as long as the watch is
 * worn in the cell: it is a foreground service (the screen of a watch is destroyed as soon as the wrist is lowered) with a
 * notification that says so and a way to end it. Off unless `presence.enabled` is on in `watch.cfg`; the beat and the
 * numbering are [PresenceHeartbeatPolicy]'s, and what a lost heartbeat means is the decision of the server. Not tried on a
 * real watch.
 */
class PresenceHeartbeatService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var policy: PresenceHeartbeatPolicy? = null
    private var transport: WatchRelayTransport? = null

    private val tick = object : Runnable {
        override fun run() {
            val beat = policy?.nextDue(System.currentTimeMillis())
            if (beat != null) transport?.sendPresence(beat)
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = WatchConfig.load(this)
        if (!config.presenceEnabled || intent?.action == ACTION_STOP) {
            endPresence()
            return START_NOT_STICKY
        }
        if (policy == null) {
            policy = PresenceHeartbeatPolicy(enabled = true, intervalMs = config.presenceIntervalMs).also { it.start() }
            transport = WatchRelayTransport(applicationContext)
        }
        startAsForeground()
        running = true
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_NOT_STICKY
    }

    /** One last heartbeat that says the operator left on purpose, then the service ends. */
    private fun endPresence() {
        handler.removeCallbacks(tick)
        policy?.stop(System.currentTimeMillis())?.let { transport?.sendPresence(it) }
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager?.getNotificationChannel(CHANNEL_ID) == null) {
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_presence), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            3,
            Intent(this, PresenceHeartbeatService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.presence_notification_title))
            .setContentText(getString(R.string.presence_notification_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.presence_stop), stop)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        transport?.cancelPendingRetries()
        running = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.hydraumc.watch.PRESENCE_STOP"
        private const val CHANNEL_ID = "presence"
        private const val NOTIFICATION_ID = 4303
        private const val TICK_MS = 1_000L

        /** Whether the presence is on (read by the screen to label its button). */
        @Volatile
        var running: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, PresenceHeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, PresenceHeartbeatService::class.java).setAction(ACTION_STOP))
        }
    }
}
