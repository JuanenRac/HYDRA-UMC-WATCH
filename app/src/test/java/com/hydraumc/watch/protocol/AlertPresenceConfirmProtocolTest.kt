// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/AlertPresenceConfirmProtocolTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.complication.ComplicationSummary
import com.hydraumc.watch.haptics.AlertSeverity
import com.hydraumc.watch.transport.KeyValueStore
import com.hydraumc.watch.transport.LastRelayStore
import com.hydraumc.watch.transport.WatchRelayPaths
import com.hydraumc.watch.transport.WatchRelayRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AlertPresenceConfirmProtocolTest {
    private val token = "tok_ABCDEFGHIJKLMNOP.signature"

    private fun rejected(raw: String) {
        try {
            parseSyncMessage(raw)
            fail("should be rejected: $raw")
        } catch (_: SyncMessageParseException) {
        }
    }

    @Test
    fun `an alert on its own path is forwarded and only while the option is on`() {
        val raw = SyncMessage.Alert(AlertSeverity.CRITICAL, "Robot 2 stopped at an endstop").toJson()
        assertEquals(raw, WatchRelayRouting.decide(WatchRelayPaths.ALERT, raw.encodeToByteArray()))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ALERT, raw.encodeToByteArray(), alertsEnabled = false))
        // an alert is not accepted on the path of the assistant, nor a reply on the path of the alerts
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ASSISTANT_REPLY, raw.encodeToByteArray()))
        val reply = SyncMessage.AssistantReply(requestId = "r-1", text = "ok").toJson()
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.ALERT, reply.encodeToByteArray()))
    }

    @Test
    fun `an alert with a blank or huge text is refused`() {
        rejected("""{"type":"alert","severity":"INFO","message":"   "}""")
        rejected("""{"type":"alert","severity":"INFO","message":"${"x".repeat(281)}"}""")
    }

    @Test
    fun `a reply carries a confirmation token that is checked`() {
        val reply = SyncMessage.AssistantReply(
            requestId = "r-1", text = "Start?", requiresConfirmation = true,
            confirmationToken = token, confirmationValiditySeconds = 30,
        )
        assertEquals(reply, parseSyncMessage(reply.toJson()))
        rejected("""{"type":"assistant_reply","requestId":"r-1","text":"x","confirmationToken":"short"}""")
        rejected("""{"type":"assistant_reply","requestId":"r-1","text":"x","confirmationToken":"$token<script>"}""")
        rejected("""{"type":"assistant_reply","requestId":"r-1","text":"x","confirmationToken":"$token","confirmationValiditySeconds":0}""")
        rejected("""{"type":"assistant_reply","requestId":"r-1","text":"x","confirmationToken":"$token","confirmationValiditySeconds":9999}""")
    }

    @Test
    fun `the token is removed from whatever is kept and never from nothing`() {
        val reply = SyncMessage.AssistantReply(
            requestId = "r-1", text = "Start?", requiresConfirmation = true,
            confirmationToken = token, confirmationValiditySeconds = 30,
        )
        val stripped = WatchRelayRouting.withoutConfirmationToken(reply.toJson())
        assertFalse(stripped.contains(token))
        val back = parseSyncMessage(stripped) as SyncMessage.AssistantReply
        assertNull(back.confirmationToken)
        assertTrue(back.requiresConfirmation)
        val plain = SyncMessage.AssistantReply(requestId = "r-2", text = "hi").toJson()
        assertEquals(plain, WatchRelayRouting.withoutConfirmationToken(plain))
        assertEquals("not json", WatchRelayRouting.withoutConfirmationToken("not json"))
    }

    @Test
    fun `the store on the watch never writes the token`() {
        val texts = HashMap<String, String>()
        val store = object : KeyValueStore {
            override fun getString(key: String) = texts[key]
            override fun getLong(key: String): Long? = 1L
            override fun put(key: String, text: String, number: Long) {
                texts[key] = text
            }
            override fun remove(key: String) {
                texts.remove(key)
            }
        }
        val reply = SyncMessage.AssistantReply(requestId = "r-1", text = "Start?", requiresConfirmation = true, confirmationToken = token)
        assertTrue(LastRelayStore(store, now = { 2L }).save(reply.toJson()))
        assertTrue(texts.values.none { it.contains(token) })
    }

    @Test
    fun `presence heartbeat and voice confirm round trip and are checked`() {
        val beat = SyncMessage.PresenceHeartbeat(sequence = 7, intervalSeconds = 5, state = PresenceState.LEFT)
        assertEquals(beat, parseSyncMessage(beat.toJson()))
        rejected("""{"type":"presence_heartbeat","protocolVersion":1,"sequence":-1,"intervalSeconds":5}""")
        rejected("""{"type":"presence_heartbeat","protocolVersion":1,"sequence":1,"intervalSeconds":0}""")
        rejected("""{"type":"presence_heartbeat","protocolVersion":2,"sequence":1,"intervalSeconds":5}""")
        val confirm = SyncMessage.VoiceConfirm(requestId = "c-1", confirms = "r-1", confirmationToken = token)
        assertEquals(confirm, parseSyncMessage(confirm.toJson()))
        rejected("""{"type":"voice_confirm","requestId":"c 1","confirms":"r-1","confirmationToken":"$token"}""")
        rejected("""{"type":"voice_confirm","requestId":"c-1","confirms":"r-1","confirmationToken":"x"}""")
        // the watch only ever sends these two: the relay never forwards them to the screen
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.PRESENCE, beat.toJson().encodeToByteArray()))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.VOICE_CONFIRM, confirm.toJson().encodeToByteArray()))
    }

    @Test
    fun `a status may say how many robots run and whether an alarm is active`() {
        val status = SyncMessage.SystemStatus(headline = "Cell", detail = "ok", robotsRunning = 3, alarmActive = false)
        assertEquals(status, parseSyncMessage(status.toJson()))
        rejected("""{"type":"system_status","headline":"Cell","detail":"ok","robotsRunning":-1}""")
        rejected("""{"type":"system_status","headline":"Cell","detail":"ok","robotsRunning":99999}""")
        // an older phone that says nothing still parses
        val old = parseSyncMessage("""{"type":"system_status","headline":"Cell","detail":"ok"}""") as SyncMessage.SystemStatus
        assertNull(old.robotsRunning)
        assertNull(old.alarmActive)
    }

    @Test
    fun `the complication never invents a count and never shows an old one`() {
        val running = SyncMessage.SystemStatus(headline = "Cell", detail = "ok", robotsRunning = 3)
        assertEquals(ComplicationSummary.Running(3), ComplicationSummary.from(running, 1_000, 300_000))
        assertEquals("3", ComplicationSummary.from(running, 1_000, 300_000).shortText())
        assertEquals(ComplicationSummary.Outdated, ComplicationSummary.from(running, 300_000, 300_000))
        assertEquals("?", ComplicationSummary.Outdated.shortText())
        assertEquals(ComplicationSummary.Unknown, ComplicationSummary.from(null, null, 300_000))
        assertEquals(ComplicationSummary.Unknown, ComplicationSummary.from(running.copy(robotsRunning = null), 10, 300_000))
        assertEquals(ComplicationSummary.Running(0), ComplicationSummary.from(running.copy(robotsRunning = 0), 10, 300_000))
        val alarm = running.copy(alarmActive = true)
        assertEquals(ComplicationSummary.Alarm, ComplicationSummary.from(alarm, 10, 300_000))
        assertEquals("!", ComplicationSummary.Alarm.shortText())
        // an alarm that is old may have ended: outdated, not an alarm
        assertEquals(ComplicationSummary.Outdated, ComplicationSummary.from(alarm, 400_000, 300_000))
    }
}
