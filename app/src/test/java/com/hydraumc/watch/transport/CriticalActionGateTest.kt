// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/CriticalActionGateTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CriticalActionGateTest {
    private var clock = 1_000L
    private val cache = LastKnownStateCache(staleAfterMs = 60_000L, now = { clock })

    private fun heartbeat() = cache.update(SyncMessage.SystemStatus(headline = "ok", detail = "ok", level = com.hydraumc.watch.protocol.WatchStatusLevel.NOMINAL))

    @Test
    fun `withheld until the companion has been heard from`() {
        assertEquals(ActionAvailability.NO_COMPANION_HEARTBEAT, CriticalActionGate.availability(cache))
        assertFalse(CriticalActionGate.isAvailable(cache))
    }

    @Test
    fun `offered right after a companion message`() {
        heartbeat()
        assertTrue(CriticalActionGate.isAvailable(cache))
    }

    @Test
    fun `withheld again once the last message is stale`() {
        heartbeat()
        clock += 60_000L
        assertEquals(ActionAvailability.COMPANION_HEARTBEAT_STALE, CriticalActionGate.availability(cache))
    }

    @Test
    fun `a new message restores it`() {
        heartbeat()
        clock += 120_000L
        heartbeat()
        assertTrue(CriticalActionGate.isAvailable(cache))
    }
}
