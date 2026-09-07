// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/LastKnownStateCacheTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.haptics.AlertSeverity
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun statusMessage(headline: String) =
    SyncMessage.SystemStatus(headline = headline, detail = "detail", level = WatchStatusLevel.NOMINAL)

class LastKnownStateCacheTest {

    @Test
    fun `starts with no cached state at all`() {
        val cache = LastKnownStateCache(staleAfterMs = 60_000, now = { 0L })
        assertNull(cache.current())
        assertFalse(cache.isStale())
        assertNull(cache.ageMs())
    }

    @Test
    fun `update records the real message and the real receipt time`() {
        var clock = 1_000L
        val cache = LastKnownStateCache(staleAfterMs = 60_000, now = { clock })
        val message = statusMessage("Swarm nominal")

        cache.update(message)

        assertEquals(message, cache.current()?.message)
        assertEquals(1_000L, cache.current()?.receivedAtMs)
    }

    @Test
    fun `is not stale immediately after a real update`() {
        var clock = 0L
        val cache = LastKnownStateCache(staleAfterMs = 60_000, now = { clock })
        cache.update(statusMessage("ok"))
        assertFalse(cache.isStale())
        assertEquals(0L, cache.ageMs())
    }

    @Test
    fun `becomes stale once real elapsed time reaches staleAfterMs`() {
        var clock = 0L
        val cache = LastKnownStateCache(staleAfterMs = 60_000, now = { clock })
        cache.update(statusMessage("ok"))

        clock = 59_999L
        assertFalse("just under the threshold must not be stale yet", cache.isStale())

        clock = 60_000L
        assertTrue("at the threshold must be stale - never presented as current", cache.isStale())
    }

    @Test
    fun `a real subsequent update replaces the previous cached state and its age`() {
        var clock = 0L
        val cache = LastKnownStateCache(staleAfterMs = 60_000, now = { clock })
        cache.update(statusMessage("first"))

        clock = 30_000L
        cache.update(SyncMessage.Alert(severity = AlertSeverity.CRITICAL, message = "E-STOP engaged"))

        assertEquals(SyncMessage.Alert(AlertSeverity.CRITICAL, "E-STOP engaged"), cache.current()?.message)
        assertEquals(30_000L, cache.current()?.receivedAtMs)
        assertEquals(0L, cache.ageMs())
        assertFalse(cache.isStale())
    }

    @Test
    fun `clear real removes the cached state, reverting isStale to false`() {
        var clock = 0L
        val cache = LastKnownStateCache(staleAfterMs = 60_000, now = { clock })
        cache.update(statusMessage("ok"))
        clock = 120_000L
        assertTrue(cache.isStale())

        cache.clear()

        assertNull(cache.current())
        assertFalse(cache.isStale())
        assertNull(cache.ageMs())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a non-positive staleAfterMs`() {
        LastKnownStateCache(staleAfterMs = 0)
    }
}
