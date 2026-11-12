// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/AlertNotifierTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.hydraumc.watch.R
import com.hydraumc.watch.WatchConfig
import com.hydraumc.watch.haptics.AlertSeverity
import com.hydraumc.watch.protocol.SyncMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The notification of an alert the phone forwarded: posted only as the policy decides, a critical one stays until it is opened. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlertNotifierTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val manager get() = shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

    private fun notifier(persistent: Boolean) = AlertNotifier(context, WatchConfig(alertsCriticalPersistent = persistent))

    private fun alert(severity: AlertSeverity) = SyncMessage.Alert(severity, "pressure low on line 2")

    @Test
    fun `a critical alert stays in the shade until it is opened when the watch is set so`() {
        val presented = notifier(persistent = true).present(alert(AlertSeverity.CRITICAL), screenAttached = true, permissionGranted = true)

        assertEquals(AlertPresentation.PERSISTENT_NOTIFICATION, presented)
        val posted = manager.getNotification(AlertNotifier.NOTIFICATION_ID)
        assertNotNull(posted)
        assertTrue(posted.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("pressure low on line 2", posted.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(context.getString(R.string.alert_critical), posted.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }

    @Test
    fun `a critical alert can be dismissed when the watch is not set to keep it`() {
        val presented = notifier(persistent = false).present(alert(AlertSeverity.CRITICAL), screenAttached = true, permissionGranted = true)

        assertEquals(AlertPresentation.NOTIFICATION, presented)
        assertEquals(0, manager.getNotification(AlertNotifier.NOTIFICATION_ID).flags and Notification.FLAG_ONGOING_EVENT)
    }

    @Test
    fun `a warning is a notification only when no screen shows it`() {
        assertEquals(AlertPresentation.SCREEN_ONLY, notifier(true).present(alert(AlertSeverity.WARNING), screenAttached = true, permissionGranted = true))
        assertNull(manager.getNotification(AlertNotifier.NOTIFICATION_ID))
        assertEquals(AlertPresentation.NOTIFICATION, notifier(true).present(alert(AlertSeverity.WARNING), screenAttached = false, permissionGranted = true))
        assertEquals(
            context.getString(R.string.alert_warning),
            manager.getNotification(AlertNotifier.NOTIFICATION_ID).extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
    }

    @Test
    fun `an info alert and an alert with no permission post nothing`() {
        assertEquals(AlertPresentation.SCREEN_ONLY, notifier(true).present(alert(AlertSeverity.INFO), screenAttached = false, permissionGranted = true))
        assertEquals(AlertPresentation.SCREEN_ONLY, notifier(true).present(alert(AlertSeverity.CRITICAL), screenAttached = false, permissionGranted = false))
        assertNull(manager.getNotification(AlertNotifier.NOTIFICATION_ID))
    }

    @Test
    fun `the channel of the alerts is created once and is of high importance`() {
        notifier(true).present(alert(AlertSeverity.CRITICAL), screenAttached = true, permissionGranted = true)
        notifier(true).present(alert(AlertSeverity.CRITICAL), screenAttached = true, permissionGranted = true)

        val channel = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).getNotificationChannel(AlertNotifier.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }
}
