// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/WatchRelayRoutingTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import com.hydraumc.watch.protocol.toJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchRelayRoutingTest {

    @Test
    fun `a real assistant reply on its own path is forwarded verbatim`() {
        val reply = SyncMessage.AssistantReply(
            requestId = "watch-req-1",
            text = "Robot 3 is online and idle.",
            level = WatchStatusLevel.NOMINAL,
        )
        val raw = reply.toJson()

        val decision = WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, raw.encodeToByteArray())

        assertEquals(raw, decision)
    }

    @Test
    fun `a real system status on its own path is forwarded verbatim`() {
        val status = SyncMessage.SystemStatus(
            headline = "All systems nominal",
            detail = "4 robots online, 0 alerts.",
            level = WatchStatusLevel.NOMINAL,
        )
        val raw = status.toJson()

        val decision = WatchRelayRouting.decide(WatchRelayPaths.SYSTEM_STATUS, raw.encodeToByteArray())

        assertEquals(raw, decision)
    }

    @Test
    fun `an unrelated path is dropped even with an otherwise valid message`() {
        val reply = SyncMessage.AssistantReply(requestId = "watch-req-1", text = "hello")
        val decision = WatchRelayRouting.decide("/hydra-umc/not-a-real-relay-path", reply.toJson().encodeToByteArray())
        assertNull(decision)
    }

    @Test
    fun `malformed bytes are dropped, never crash the caller`() {
        val decision = WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, "{not json".encodeToByteArray())
        assertNull(decision)
    }

    @Test
    fun `a message that parses but fails real protocol validation is dropped`() {
        // A blank assistant text is syntactically valid JSON but fails
        // parseSyncMessage()'s own validateProtocolMessage() - the real
        // gate this routing decision must not bypass.
        val invalid = """{"type":"assistant_reply","requestId":"watch-req-1","text":""}"""
        val decision = WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, invalid.encodeToByteArray())
        assertNull(decision)
    }

    @Test
    fun `a real message of the wrong type on a relay path is dropped`() {
        // EStopCommand is a real, validly-parseable SyncMessage - just not
        // one of the two types (AssistantReply/SystemStatus) this relay is
        // meant to forward, even if it somehow arrived on one of these
        // two paths.
        val raw = SyncMessage.EStopCommand.toJson()
        val decision = WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, raw.encodeToByteArray())
        assertNull(decision)
    }
}
