// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/SyncMessageTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.haptics.AlertSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SyncMessageTest {

    @Test
    fun `estop command round-trips through JSON`() {
        val json = SyncMessage.EStopCommand.toJson()
        assertTrue(json.contains("estop_command"))

        val parsed = parseSyncMessage(json)
        assertEquals(SyncMessage.EStopCommand, parsed)
    }

    @Test
    fun `alert round-trips through JSON with severity and message intact`() {
        val original = SyncMessage.Alert(AlertSeverity.CRITICAL, "Collision risk on arm 3")
        val json = original.toJson()

        val parsed = parseSyncMessage(json)
        assertEquals(original, parsed)
        assertTrue(parsed is SyncMessage.Alert)
        assertEquals(AlertSeverity.CRITICAL, (parsed as SyncMessage.Alert).severity)
        assertEquals("Collision risk on arm 3", parsed.message)
    }

    @Test
    fun `all three severities parse correctly`() {
        for (severity in AlertSeverity.entries) {
            val original = SyncMessage.Alert(severity, "test")
            val parsed = parseSyncMessage(original.toJson())
            assertEquals(original, parsed)
        }
    }

    @Test
    fun `malformed json is rejected with a clear error`() {
        try {
            parseSyncMessage("{not json")
            fail("expected SyncMessageParseException")
        } catch (exc: SyncMessageParseException) {
            assertTrue(exc.message!!.contains("malformed JSON"))
        }
    }

    @Test
    fun `missing type field is rejected`() {
        try {
            parseSyncMessage("""{"severity":"CRITICAL","message":"hi"}""")
            fail("expected SyncMessageParseException")
        } catch (exc: SyncMessageParseException) {
            assertTrue(exc.message!!.contains("missing 'type'"))
        }
    }

    @Test
    fun `unknown type is rejected`() {
        try {
            parseSyncMessage("""{"type":"future_message_kind"}""")
            fail("expected SyncMessageParseException")
        } catch (exc: SyncMessageParseException) {
            assertTrue(exc.message!!.contains("unknown message type"))
        }
    }

    @Test
    fun `non-object json is rejected`() {
        try {
            parseSyncMessage("""["not", "an", "object"]""")
            fail("expected SyncMessageParseException")
        } catch (exc: SyncMessageParseException) {
            assertTrue(exc.message!!.contains("expected a JSON object"))
        }
    }
}
