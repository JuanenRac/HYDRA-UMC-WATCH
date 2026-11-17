// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/ReleaseNoticeTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.WatchConfig
import com.hydraumc.watch.transport.KeyValueStore
import com.hydraumc.watch.transport.ReleaseNoticeStore
import com.hydraumc.watch.transport.WatchRelayPaths
import com.hydraumc.watch.transport.WatchRelayRouting
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The notice of a new release that the phone pushes: parsed with its rules, taken only when asked for, routed on its own path, and kept apart. */
class ReleaseNoticeTest {
    private fun raw(version: String, protocol: Int = 1): String = """{"type":"release_notice","protocolVersion":$protocol,"version":"$version"}"""

    private class MapStore : KeyValueStore {
        val texts = HashMap<String, String>()
        val numbers = HashMap<String, Long>()
        override fun getString(key: String) = texts[key]
        override fun getLong(key: String) = numbers[key]
        override fun put(key: String, text: String, number: Long) {
            texts[key] = text
            numbers[key] = number
        }
        override fun remove(key: String) {
            texts.remove(key)
            numbers.remove(key)
        }
    }

    @Test
    fun `a version of two to four groups of digits is read and round trips`() {
        for (v in listOf("0.5", "0.5.2", "0.8.0.1", "10.20.30.40")) {
            val m = parseSyncMessage(raw(v)) as SyncMessage.ReleaseNotice
            assertEquals(v, m.version)
            assertEquals(m, parseSyncMessage(m.toJson()))
        }
    }

    @Test
    fun `another version, text, one group, five groups, a link and a long number are refused`() {
        for (bad in listOf(raw("0.5.2", protocol = 2), raw("latest"), raw("5"), raw("1.2.3.4.5"), raw("0.5.2 http://x"), raw("12345.1"), raw("0.5.2-rc1"), raw(""))) {
            try {
                parseSyncMessage(bad)
                fail("accepted $bad")
            } catch (_: SyncMessageParseException) {
                // refused
            }
        }
    }

    @Test
    fun `it is taken while the option is on, which is on by default, and refused when it is switched off`() {
        val bytes = raw("0.5.2").encodeToByteArray()
        assertNotNull("on by default", WatchRelayRouting.decide(WatchRelayPaths.RELEASE_NOTICE, bytes))
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.RELEASE_NOTICE, bytes, releaseNoticeEnabled = false))
        assertNotNull(WatchRelayRouting.decide(WatchRelayPaths.RELEASE_NOTICE, bytes, releaseNoticeEnabled = true))
        // the alerts option does not switch it off
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.RELEASE_NOTICE, bytes, alertsEnabled = true, releaseNoticeEnabled = false))
    }

    @Test
    fun `it is accepted on its own path only and nothing else is accepted on it`() {
        val bytes = raw("0.5.2").encodeToByteArray()
        for (other in listOf(WatchRelayPaths.SYSTEM_STATUS, WatchRelayPaths.ASSISTANT_REPLY, WatchRelayPaths.SERVER_SECURITY, WatchRelayPaths.ALERT, WatchRelayPaths.CANOTA_PROGRESS)) {
            assertNull("accepted on $other", WatchRelayRouting.decide(other, bytes, releaseNoticeEnabled = true))
        }
        val status = """{"type":"system_status","headline":"h","detail":"d"}""".encodeToByteArray()
        assertNull(WatchRelayRouting.decide(WatchRelayPaths.RELEASE_NOTICE, status, releaseNoticeEnabled = true))
    }

    @Test
    fun `the store keeps the newest version, drops an old notice and never goes back`() {
        var clock = 1_000L
        val store = ReleaseNoticeStore(MapStore(), maxAgeMs = 10_000L, now = { clock })
        assertNull(store.load())
        assertTrue(store.save("0.5.2"))
        assertEquals("0.5.2", store.load())
        assertFalse("an older version does not replace a newer one", store.save("0.5.1"))
        assertEquals("0.5.2", store.load())
        clock += 6_000L
        assertFalse("the same version does not renew the instant", store.save("0.5.2"))
        clock += 4_000L
        assertNull("so the notice still expires three days after it first arrived", store.load())
        assertTrue(store.save("0.5.2"))
        assertTrue(store.save("0.8.0.1"))
        assertEquals("0.8.0.1", store.load())
        clock += 10_000L
        assertNull("too old", store.load())
        assertNull("and dropped", store.load())
    }

    @Test
    fun `versions are compared group by group`() {
        assertTrue(ReleaseNoticeStore.compare("0.10.0", "0.9.9") > 0)
        assertTrue(ReleaseNoticeStore.compare("0.8.0.1", "0.8.0") > 0)
        assertEquals(0, ReleaseNoticeStore.compare("0.8", "0.8.0.0"))
        assertTrue(ReleaseNoticeStore.compare("0.5.2", "0.5.10") < 0)
    }

    @Test
    fun `the notice option is on in the file that ships and a false value switches it off`() {
        assertTrue(WatchConfig().releaseNoticeEnabled)
        assertTrue(WatchConfig.parse(File("src/main/assets/watch.cfg").readText(Charsets.UTF_8)).releaseNoticeEnabled)
        assertTrue(WatchConfig.parse("updates.notice_enabled = maybe").releaseNoticeEnabled)
        assertTrue(WatchConfig.parse("").releaseNoticeEnabled)
        assertFalse(WatchConfig.parse("updates.notice_enabled = false").releaseNoticeEnabled)
    }
}
