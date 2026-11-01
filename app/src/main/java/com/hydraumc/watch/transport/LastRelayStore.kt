// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/LastRelayStore.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.parseSyncMessage

/** The little storage the last message lives in (the preferences of the app on the watch, a map in the tests). */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getLong(key: String): Long?
    fun put(key: String, text: String, number: Long)
    fun remove(key: String)
}

/**
 * Keeps the last assistant reply or system status that reached the watch, with the instant it arrived, so a screen that
 * is created later (the wrist was lowered and the activity destroyed), a tile or a complication can show it. What is read
 * back carries its original time: it is shown as the last known state and marked outdated by the same rule as a live
 * one, never as if it had just arrived. A message older than [maxAgeMs] is dropped instead. Pure Kotlin, so it is checked
 * on a plain JVM.
 */
class LastRelayStore(
    private val store: KeyValueStore,
    private val maxAgeMs: Long = 24 * 60 * 60_000L,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init {
        require(maxAgeMs > 0) { "maxAgeMs must be positive" }
    }

    /** Remembers [raw] when it is a reply or a status; any other message is not kept. Returns whether it was kept. */
    fun save(raw: String): Boolean {
        val message = runCatching { parseSyncMessage(raw) }.getOrNull() ?: return false
        if (message !is SyncMessage.AssistantReply && message !is SyncMessage.SystemStatus) return false
        store.put(KEY_RAW, raw, now())
        return true
    }

    /** The last message kept and when it arrived, or null when there is none, it is too old or it no longer parses. */
    fun load(): Pair<SyncMessage, Long>? {
        val raw = store.getString(KEY_RAW) ?: return null
        val at = store.getLong(KEY_RAW) ?: return null
        if (now() - at >= maxAgeMs || at > now()) {
            clear()
            return null
        }
        val message = runCatching { parseSyncMessage(raw) }.getOrNull()
        if (message == null) {
            clear()
            return null
        }
        return message to at
    }

    fun clear() = store.remove(KEY_RAW)

    private companion object {
        const val KEY_RAW = "last_relay_message"
    }
}
