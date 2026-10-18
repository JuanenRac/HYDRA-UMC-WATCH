// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/SyncMessageHostileTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.transport.WatchRelayPaths
import com.hydraumc.watch.transport.WatchRelayRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Messages from the phone that are wrong, odd or hostile. Whatever arrives, the
 * parser answers with a [SyncMessage] or with a [SyncMessageParseException]
 * and never with another kind of exception, so a listener that catches only
 * the parse exception is never taken down by a message.
 */
class SyncMessageHostileTest {

    private fun rejected(raw: String): SyncMessageParseException {
        try {
            parseSyncMessage(raw)
        } catch (e: SyncMessageParseException) {
            return e
        } catch (e: Throwable) {
            fail("`$raw` escaped as ${e.javaClass.simpleName}: ${e.message}")
        }
        fail("`$raw` was accepted")
        throw IllegalStateException()
    }

    private fun voice(requestId: String = "req-1", transcript: String = "stop", locale: String = "en-US") =
        """{"type":"voice_turn","requestId":"$requestId","transcript":"$transcript","locale":"$locale"}"""

    private fun reply(text: String, requestId: String = "req-1") =
        """{"type":"assistant_reply","requestId":"$requestId","text":"$text"}"""

    @Test
    fun `a type that is not a plain name is a parse error, whatever its shape`() {
        for (type in listOf("{}", "[]", """{"a":1}""", """["voice_turn"]""", "5", "true", "null", "1.5", "\"\"", "\"VOICE_TURN\"", "\"voice_turn \"")) {
            rejected("""{"type":$type,"requestId":"r"}""")
        }
    }

    @Test
    fun `the top level has to be an object`() {
        for (raw in listOf("", " ", "[]", "[{}]", "null", "5", "\"text\"", "true", "{", "}", "{\"type\":", "{\"type\":\"alert\"", "not json", "\u0000")) {
            rejected(raw)
        }
    }

    @Test
    fun `fields of the wrong type are a parse error`() {
        for (raw in listOf(
            """{"type":"voice_turn","requestId":5,"transcript":"x","locale":"en"}""",
            """{"type":"voice_turn","requestId":"r","transcript":["x"],"locale":"en"}""",
            """{"type":"voice_turn","requestId":"r","transcript":"x","locale":{"a":1}}""",
            """{"type":"assistant_reply","requestId":"r","text":null}""",
            """{"type":"assistant_reply","requestId":"r","text":"x","speak":"yes"}""",
            """{"type":"assistant_reply","requestId":"r","text":"x","requiresConfirmation":1}""",
            """{"type":"system_status","headline":5,"detail":"d"}""",
            """{"type":"companion_version_status","protocolVersion":1,"appVersion":1,"updateAvailable":true}""",
            """{"type":"alert","severity":"INFO","message":5}""",
            """{"type":"alert","severity":5,"message":"m"}""",
            """{"type":"alert","severity":"LOUD","message":"m"}""",
            """{"type":"alert","message":"m"}""",
        )) {
            rejected(raw)
        }
    }

    @Test
    fun `unknown extra fields are ignored`() {
        val parsed = parseSyncMessage("""{"type":"assistant_reply","requestId":"r1","text":"hello","future":{"a":[1,2]},"x":null}""")
        assertEquals(SyncMessage.AssistantReply(requestId = "r1", text = "hello"), parsed)
    }

    @Test
    fun `the request id is one to 64 letters, digits, underscores or hyphens`() {
        parseSyncMessage(voice(requestId = "a"))
        parseSyncMessage(voice(requestId = "a".repeat(64)))
        parseSyncMessage(voice(requestId = "A_b-9"))
        for (id in listOf("", "a".repeat(65), "a b", "a/b", "a.b", "a\\nb", "ñ", "a;b", "<b>", "a\\u0000")) {
            rejected(voice(requestId = id))
            rejected(reply("hello", requestId = id))
        }
    }

    @Test
    fun `the limits of every field are exact`() {
        parseSyncMessage(voice(transcript = "t".repeat(500)))
        rejected(voice(transcript = "t".repeat(501)))
        rejected(voice(transcript = ""))
        rejected(voice(transcript = "   "))

        parseSyncMessage(voice(locale = "en"))
        parseSyncMessage(voice(locale = "l".repeat(35)))
        rejected(voice(locale = "e"))
        rejected(voice(locale = "l".repeat(36)))

        parseSyncMessage(reply("t".repeat(500)))
        rejected(reply(""))
        rejected(reply("   "))

        parseSyncMessage("""{"type":"system_status","headline":"${"h".repeat(80)}","detail":"${"d".repeat(280)}"}""")
        rejected("""{"type":"system_status","headline":"${"h".repeat(81)}","detail":"d"}""")
        rejected("""{"type":"system_status","headline":"h","detail":"${"d".repeat(281)}"}""")
        rejected("""{"type":"system_status","headline":"","detail":"d"}""")
        parseSyncMessage("""{"type":"system_status","headline":"h","detail":""}""")

        parseSyncMessage("""{"type":"alert","severity":"INFO","message":"${"m".repeat(280)}"}""")
        rejected("""{"type":"alert","severity":"INFO","message":"${"m".repeat(281)}"}""")
        rejected("""{"type":"alert","severity":"INFO","message":""}""")
        rejected("""{"type":"alert","severity":"INFO","message":"  "}""")
    }

    @Test
    fun `a reply over the limit is cut to exactly the limit with an ellipsis`() {
        val parsed = parseSyncMessage(reply("t".repeat(501))) as SyncMessage.AssistantReply
        assertEquals(500, parsed.text.length)
        assertTrue(parsed.text.endsWith("…"))
        val long = parseSyncMessage(reply("t".repeat(5_000))) as SyncMessage.AssistantReply
        assertEquals(500, long.text.length)
    }

    @Test
    fun `a text that is only blanks after the cut is still refused when it started blank`() {
        rejected(reply(" ".repeat(600)))
    }

    @Test
    fun `the companion version has to be the current protocol and a stable x y z`() {
        fun status(protocol: Int, version: String) =
            """{"type":"companion_version_status","protocolVersion":$protocol,"appVersion":"$version","updateAvailable":false}"""
        parseSyncMessage(status(1, "0.0.0"))
        parseSyncMessage(status(1, "10.20.300"))
        for (version in listOf("", "1.0", "1.0.0.1", "v1.0.0", "1.0.0-beta", "1.0.0+5", "a.b.c", "1.0.0 ", " 1.0.0", "1..0", "1.0.x", "١.٠.٠")) {
            rejected(status(1, version))
        }
        for (protocol in listOf(0, 2, -1, 1_000_000)) {
            rejected(status(protocol, "1.0.0"))
        }
    }

    @Test
    fun `an unknown status level falls back to attention, a known one is kept`() {
        val unknown = parseSyncMessage("""{"type":"system_status","headline":"h","detail":"d","level":"PANIC"}""") as SyncMessage.SystemStatus
        assertEquals(WatchStatusLevel.ATTENTION, unknown.level)
        for (level in WatchStatusLevel.values()) {
            val status = parseSyncMessage("""{"type":"system_status","headline":"h","detail":"d","level":"${level.name}"}""") as SyncMessage.SystemStatus
            assertEquals(level, status.level)
        }
    }

    @Test
    fun `an estop command carries nothing and survives extra fields`() {
        assertEquals(SyncMessage.EStopCommand, parseSyncMessage("""{"type":"estop_command"}"""))
        assertEquals(SyncMessage.EStopCommand, parseSyncMessage("""{"type":"estop_command","x":1}"""))
    }

    @Test
    fun `quotes, newlines and unicode in text survive a round trip`() {
        val original = SyncMessage.AssistantReply(
            requestId = "r-1",
            text = "He said \"stop\"\nthen left \\ ñ 日本語 🚀",
        )
        assertEquals(original, parseSyncMessage(original.toJson()))
    }

    @Test
    fun `an error code that this build does not know is kept and does not stop the message`() {
        val parsed = parseSyncMessage("""{"type":"assistant_reply","requestId":"r","text":"t","errorCode":"from_the_future"}""") as SyncMessage.AssistantReply
        assertEquals("from_the_future", parsed.errorCode)
        assertNull(errorCodeToStringRes("from_the_future"))
        assertNull(errorCodeToStringRes(null))
        assertNull(errorCodeToDetailStringRes("from_the_future"))
        assertNull(errorCodeToDetailStringRes(null))
    }

    @Test
    fun `the relay forwards only replies and statuses that parse, on their own paths`() {
        val goodReply = reply("hello")
        val goodStatus = """{"type":"system_status","headline":"h","detail":"d"}"""
        assertEquals(goodReply, WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, goodReply.toByteArray()))
        assertEquals(goodStatus, WatchRelayRouting.decide(WatchRelayPaths.SYSTEM_STATUS, goodStatus.toByteArray()))
        // another path, or a message of another type on a known path, or junk
        assertNull(WatchRelayRouting.decide("/other", goodReply.toByteArray()))
        assertNull(WatchRelayRouting.decide("", goodReply.toByteArray()))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, """{"type":"estop_command"}""".toByteArray()))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.SYSTEM_STATUS, """{"type":"alert","severity":"INFO","message":"m"}""".toByteArray()))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, ByteArray(0)))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, byteArrayOf(0xff.toByte(), 0xfe.toByte(), 0x00)))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, """{"type":{},"requestId":"r"}""".toByteArray()))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, ByteArray(100_000) { 'a'.code.toByte() }))
    }

    @Test
    fun `a deeply nested document is refused without taking the app down`() {
        val deep = "[".repeat(5_000) + "]".repeat(5_000)
        val raw = """{"type":"assistant_reply","requestId":"r","text":"t","extra":$deep}"""
        try {
            val parsed = parseSyncMessage(raw)
            assertNotNull(parsed)
        } catch (e: SyncMessageParseException) {
            // refused cleanly is fine too
        }
    }
}
