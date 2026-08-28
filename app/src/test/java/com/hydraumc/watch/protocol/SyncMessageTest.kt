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
    fun `companion version status round-trips without permitting APK delivery`() {
        val original = SyncMessage.CompanionVersionStatus(
            protocolVersion = 1,
            appVersion = "0.2.9",
            updateAvailable = true,
        )

        val parsed = parseSyncMessage(original.toJson())

        assertEquals(original, parsed)
    }

    @Test
    fun `voice turn and assistant reply round-trip safely`() {
        val voice = SyncMessage.VoiceTurn(
            requestId = "watch-voice-001",
            transcript = "What is the status of robot 3?",
            locale = "en-US",
        )
        val reply = SyncMessage.AssistantReply(
            requestId = voice.requestId,
            text = "Robot 3 is online and idle.",
            level = WatchStatusLevel.NOMINAL,
            speak = true,
        )

        assertEquals(voice, parseSyncMessage(voice.toJson()))
        assertEquals(reply, parseSyncMessage(reply.toJson()))
    }

    @Test
    fun `critical system status keeps its visual and speech metadata`() {
        val status = SyncMessage.SystemStatus(
            headline = "Safety zone alert",
            detail = "Robot A4 entered a restricted zone.",
            level = WatchStatusLevel.CRITICAL,
            speak = true,
        )

        assertEquals(status, parseSyncMessage(status.toJson()))
    }

    @Test
    fun `oversized voice transcript is rejected before it reaches AI`() {
        val oversized = "x".repeat(501)
        try {
            parseSyncMessage(
                """{"type":"voice_turn","requestId":"watch-voice-001","transcript":"$oversized","locale":"en-US"}""",
            )
            fail("expected SyncMessageParseException")
        } catch (exc: SyncMessageParseException) {
            assertTrue(exc.message!!.contains("voice transcript"))
        }
    }

    @Test
    fun `unsupported companion protocol version is rejected`() {
        try {
            parseSyncMessage(
                """{"type":"companion_version_status","protocolVersion":2,"appVersion":"0.2.9","updateAvailable":false}""",
            )
            fail("expected SyncMessageParseException")
        } catch (exc: SyncMessageParseException) {
            assertTrue(exc.message!!.contains("unsupported companion protocol version"))
        }
    }

    @Test
    fun `invalid companion semantic version is rejected`() {
        try {
            parseSyncMessage(
                """{"type":"companion_version_status","protocolVersion":1,"appVersion":"v0.2.9-beta","updateAvailable":false}""",
            )
            fail("expected SyncMessageParseException")
        } catch (exc: SyncMessageParseException) {
            assertTrue(exc.message!!.contains("invalid companion app version"))
        }
    }

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
