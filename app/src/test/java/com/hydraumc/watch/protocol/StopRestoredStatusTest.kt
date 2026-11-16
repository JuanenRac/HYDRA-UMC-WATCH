// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/StopRestoredStatusTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.transport.WatchRelayPaths
import com.hydraumc.watch.transport.WatchRelayRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/** The stop-restored notice that the phone pushes: parsed with its rules and routed on its own path only. */
class StopRestoredStatusTest {
    private fun raw(restored: String, version: Int = 1): String =
        """{"type":"stop_restored_status","protocolVersion":$version,"restored":$restored}"""

    @Test
    fun `a notice with the flag true or false is read`() {
        val on = parseSyncMessage(raw("true")) as SyncMessage.StopRestoredStatus
        assertEquals(true, on.restored)
        val off = parseSyncMessage(raw("false")) as SyncMessage.StopRestoredStatus
        assertEquals(false, off.restored)
        assertEquals(on, parseSyncMessage(on.toJson()))
    }

    @Test
    fun `another version and a flag that is not true or false are refused`() {
        for (bad in listOf(raw("true", version = 2), raw("\"yes\""), raw("1"), """{"type":"stop_restored_status","protocolVersion":1}""")) {
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
        val bytes = raw("true").encodeToByteArray()
        assertNotNull(WatchRelayRouting.decide(WatchRelayPaths.STOP_RESTORED, bytes))
        for (other in listOf(WatchRelayPaths.SYSTEM_STATUS, WatchRelayPaths.ASSISTANT_REPLY, WatchRelayPaths.SERVER_SECURITY, WatchRelayPaths.ALERT, WatchRelayPaths.CANOTA_PROGRESS)) {
            assertNull("accepted on $other", WatchRelayRouting.decide(other, bytes))
        }
        val status = """{"type":"system_status","headline":"h","detail":"d"}""".encodeToByteArray()
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.STOP_RESTORED, status))
    }
}
