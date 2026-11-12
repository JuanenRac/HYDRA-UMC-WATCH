// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/RelayNotifierTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.hydraumc.watch.protocol.SyncMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The notification of an answer that arrives while no screen of the watch is open: only when the operator allowed notifications. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RelayNotifierTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val manager get() = shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

    private fun reply() = SyncMessage.AssistantReply(requestId = "r1", text = "the cell is ready", errorCode = null)

    @Test
    fun `an answer is posted when no screen shows it and notifications are allowed`() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertTrue(RelayNotifier(context).notifyIfNeeded(reply(), screenAttached = false))

        val posted = manager.getNotification(RelayNotifier.NOTIFICATION_ID)
        assertNotNull(posted)
        assertEquals("the cell is ready", posted.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun `nothing is posted when a screen shows the answer`() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertFalse(RelayNotifier(context).notifyIfNeeded(reply(), screenAttached = true))
        assertNull(manager.getNotification(RelayNotifier.NOTIFICATION_ID))
    }

    @Test
    fun `nothing is posted without the permission and the permission is what the notifier reports`() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val notifier = RelayNotifier(context)
        assertFalse(notifier.permissionGranted())
        assertFalse(notifier.notifyIfNeeded(reply(), screenAttached = false))
        assertNull(manager.getNotification(RelayNotifier.NOTIFICATION_ID))
    }
}
