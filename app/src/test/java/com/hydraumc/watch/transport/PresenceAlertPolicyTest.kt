// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/PresenceAlertPolicyTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.haptics.AlertSeverity
import com.hydraumc.watch.protocol.PresenceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceAlertPolicyTest {
    @Test
    fun `presence is silent when the option is off`() {
        val p = PresenceHeartbeatPolicy(enabled = false, intervalMs = 5_000)
        p.start()
        assertFalse(p.isRunning)
        assertNull(p.nextDue(0))
        assertNull(p.stop(1))
    }

    @Test
    fun `heartbeats come at the interval with rising numbers`() {
        val p = PresenceHeartbeatPolicy(enabled = true, intervalMs = 5_000)
        assertNull(p.nextDue(0)) // not started
        p.start()
        val first = p.nextDue(0)!!
        assertEquals(0L, first.sequence)
        assertEquals(5, first.intervalSeconds)
        assertEquals(PresenceState.PRESENT, first.state)
        assertNull(p.nextDue(4_999))
        assertEquals(1L, p.nextDue(5_000)!!.sequence)
        assertEquals(2L, p.nextDue(10_000)!!.sequence)
    }

    @Test
    fun `ending on purpose sends one last heartbeat that says left`() {
        val p = PresenceHeartbeatPolicy(enabled = true, intervalMs = 5_000)
        p.start()
        p.nextDue(0)
        val last = p.stop(2_000)!!
        assertEquals(PresenceState.LEFT, last.state)
        assertEquals(1L, last.sequence)
        assertNull(p.nextDue(20_000))
        assertNull(p.stop(21_000))
    }

    @Test
    fun `a clock that goes back does not stop the heartbeat for good`() {
        val p = PresenceHeartbeatPolicy(enabled = true, intervalMs = 5_000)
        p.start()
        p.nextDue(100_000)
        assertNotNull(p.nextDue(50_000))
    }

    @Test
    fun `a critical alert stays as a notification and a warning only without a screen`() {
        assertEquals(AlertPresentation.PERSISTENT_NOTIFICATION, AlertPolicy.presentation(AlertSeverity.CRITICAL, true, true, true))
        assertEquals(AlertPresentation.NOTIFICATION, AlertPolicy.presentation(AlertSeverity.CRITICAL, true, true, false))
        assertEquals(AlertPresentation.SCREEN_ONLY, AlertPolicy.presentation(AlertSeverity.WARNING, true, true, true))
        assertEquals(AlertPresentation.NOTIFICATION, AlertPolicy.presentation(AlertSeverity.WARNING, false, true, true))
        assertEquals(AlertPresentation.SCREEN_ONLY, AlertPolicy.presentation(AlertSeverity.INFO, false, true, true))
    }

    @Test
    fun `without the permission an alert stays on the screen`() {
        assertEquals(AlertPresentation.SCREEN_ONLY, AlertPolicy.presentation(AlertSeverity.CRITICAL, false, false, true))
    }

    @Test
    fun `an old alert is outdated and one from the future too`() {
        assertFalse(AlertPolicy.isOutdated(1_000, 2_000, 300_000))
        assertTrue(AlertPolicy.isOutdated(0, 300_000, 300_000))
        assertTrue(AlertPolicy.isOutdated(9_000, 1_000, 300_000))
    }
}
