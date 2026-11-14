// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/CanOtaProgressStatusTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.transport.WatchRelayPaths
import com.hydraumc.watch.transport.WatchRelayRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The live progress of a flash cycle that the phone pushes: parsed with its rules, routed on its own path only, and with a text for every phase. */
class CanOtaProgressStatusTest {
    private fun raw(phase: String, percent: String? = null, version: Int = 1): String =
        """{"type":"canota_progress_status","protocolVersion":$version,"phase":"$phase"${percent?.let { ",\"percent\":$it" } ?: ""}}"""

    @Test
    fun `a progress with a phase and a percent is read, and the percent may be missing`() {
        val full = parseSyncMessage(raw("transferring", "40")) as SyncMessage.CanOtaProgressStatus
        assertEquals("transferring", full.phase)
        assertEquals(40, full.percent)
        val bare = parseSyncMessage(raw("done")) as SyncMessage.CanOtaProgressStatus
        assertNull(bare.percent)
        assertEquals(bare, parseSyncMessage(bare.toJson()))
    }

    @Test
    fun `an unknown phase, another version and a percent outside 0 to 100 are refused`() {
        for (bad in listOf(raw("dancing"), raw("done", version = 2), raw("verifying", "101"), raw("verifying", "-1"))) {
            try {
                parseSyncMessage(bad)
                fail("accepted $bad")
            } catch (_: SyncMessageParseException) {
                // refused
            }
        }
    }

    @Test
    fun `it is accepted on its own path only`() {
        val bytes = raw("rebooting", "95").encodeToByteArray()
        assertNotNull(WatchRelayRouting.decide(WatchRelayPaths.CANOTA_PROGRESS, bytes))
        for (other in listOf(WatchRelayPaths.SYSTEM_STATUS, WatchRelayPaths.ASSISTANT_REPLY, WatchRelayPaths.SERVER_SECURITY, WatchRelayPaths.ALERT)) {
            assertNull("accepted on $other", WatchRelayRouting.decide(other, bytes))
        }
        // and nothing else is accepted on it
        val status = """{"type":"system_status","headline":"h","detail":"d"}""".encodeToByteArray()
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.CANOTA_PROGRESS, status))
    }

    @Test
    fun `every phase has a text and only done and error end the cycle`() {
        assertEquals(8, CANOTA_PHASES.size)
        for (phase in CANOTA_PHASES) {
            assertNotNull(phase, canOtaPhaseStringRes(phase))
            assertEquals(phase in listOf("done", "error"), canOtaFinished(phase))
        }
        assertNull(canOtaPhaseStringRes("dancing"))
        assertFalse(canOtaFinished("transferring"))
        assertTrue(canOtaFinished("error"))
    }
}
