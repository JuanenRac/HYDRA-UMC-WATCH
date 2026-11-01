// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/RelayNotificationPolicyTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayNotificationPolicyTest {
    private val reply = SyncMessage.AssistantReply(requestId = "r-1", text = "ok")
    private fun status(level: WatchStatusLevel) = SyncMessage.SystemStatus(headline = "h", detail = "d", level = level)

    @Test
    fun `an answer with no screen is notified`() {
        assertTrue(RelayNotificationPolicy.shouldNotify(reply, screenAttached = false, permissionGranted = true))
    }

    @Test
    fun `a screen shows the message itself`() {
        assertFalse(RelayNotificationPolicy.shouldNotify(reply, screenAttached = true, permissionGranted = true))
    }

    @Test
    fun `nothing is shown without the permission`() {
        assertFalse(RelayNotificationPolicy.shouldNotify(reply, screenAttached = false, permissionGranted = false))
    }

    @Test
    fun `a routine status is not notified but a problem is`() {
        assertFalse(RelayNotificationPolicy.shouldNotify(status(WatchStatusLevel.NOMINAL), false, true))
        for (level in listOf(WatchStatusLevel.ATTENTION, WatchStatusLevel.WARNING, WatchStatusLevel.CRITICAL, WatchStatusLevel.OFFLINE)) {
            assertTrue(RelayNotificationPolicy.shouldNotify(status(level), false, true))
        }
    }

    @Test
    fun `other messages are never notified`() {
        val security = SyncMessage.ServerSecurityStatus(1, emptyList(), 0, 0, 0)
        assertFalse(RelayNotificationPolicy.shouldNotify(security, false, true))
    }
}
