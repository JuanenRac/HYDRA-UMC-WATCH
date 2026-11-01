// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/LastRelayStoreTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import com.hydraumc.watch.protocol.toJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

class LastRelayStoreTest {
    private val status = SyncMessage.SystemStatus(headline = "Cell A", detail = "idle", level = WatchStatusLevel.WARNING)
    private val reply = SyncMessage.AssistantReply(requestId = "r-1", text = "All good")

    @Test
    fun `a status is kept with the instant it arrived`() {
        var clock = 5_000L
        val store = LastRelayStore(MapStore(), now = { clock })
        assertTrue(store.save(status.toJson()))
        clock = 9_000L
        assertEquals(status to 5_000L, store.load())
    }

    @Test
    fun `the newest reply or status wins`() {
        var clock = 1L
        val store = LastRelayStore(MapStore(), now = { clock })
        store.save(status.toJson())
        clock = 2L
        store.save(reply.toJson())
        assertEquals(reply to 2L, store.load())
    }

    @Test
    fun `nothing else is kept`() {
        val store = LastRelayStore(MapStore())
        val security = SyncMessage.ServerSecurityStatus(
            protocolVersion = 1, loweredSwitches = emptyList(), lockedSwitches = 0, activeClients = 0, failedLogins = 0,
        )
        assertFalse(store.save(security.toJson()))
        assertFalse(store.save("not json at all"))
        assertNull(store.load())
    }

    @Test
    fun `a message older than the limit is dropped instead of shown`() {
        var clock = 0L
        val backing = MapStore()
        val store = LastRelayStore(backing, maxAgeMs = 1_000L, now = { clock })
        store.save(status.toJson())
        clock = 1_000L
        assertNull(store.load())
        assertTrue(backing.texts.isEmpty())
    }

    @Test
    fun `a time in the future is not trusted`() {
        val backing = MapStore()
        LastRelayStore(backing, now = { 10_000L }).save(status.toJson())
        assertNull(LastRelayStore(backing, now = { 5_000L }).load())
    }

    @Test
    fun `damaged text is cleared`() {
        val backing = MapStore()
        backing.put("last_relay_message", "{broken", 1L)
        assertNull(LastRelayStore(backing, now = { 2L }).load())
        assertTrue(backing.texts.isEmpty())
    }

    @Test
    fun `a restored state ages from when it arrived and is marked outdated`() {
        var clock = 1_000_000L
        val cache = LastKnownStateCache(staleAfterMs = 60_000L, now = { clock })
        cache.restore(status, receivedAtMs = 100_000L)
        assertEquals(status, cache.current()?.message)
        assertTrue(cache.isStale())
        assertFalse(CriticalActionGate.isAvailable(cache))
        // a time in the future is refused
        val other = LastKnownStateCache(staleAfterMs = 60_000L, now = { clock })
        other.restore(status, receivedAtMs = clock + 1)
        assertNull(other.current())
    }
}
