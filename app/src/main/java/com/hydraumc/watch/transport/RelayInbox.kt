// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/RelayInbox.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

/** A relay message that arrived while there was no screen to show it, and the instant it did. */
data class HeldRelayMessage(val raw: String, val receivedAtMs: Long)

/**
 * Keeps the relay messages that arrive while the screen of the watch does not exist. On Wear OS the activity is
 * destroyed as soon as the wrist is lowered and the screen goes off; the listener service lives on, but the local
 * broadcast it sends has no receiver then, so an answer to a question asked a moment earlier was lost. The service
 * holds the message here; the screen takes it when it is created again.
 *
 * Bounded (the newest [maxHeld] only), and a message older than [maxAgeMs] is dropped instead of being shown as if
 * it were current: a held alert must not turn into an order minutes later. Pure Kotlin, so it is checked on a plain
 * JVM; the instance the service and the screen share is [RelayInboxHolder.inbox].
 */
class RelayInbox(
    private val maxHeld: Int = 8,
    private val maxAgeMs: Long = 10 * 60_000L,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init {
        require(maxHeld > 0) { "maxHeld must be positive" }
        require(maxAgeMs > 0) { "maxAgeMs must be positive" }
    }

    private val held = ArrayDeque<HeldRelayMessage>()

    /** True from the moment the screen is created until it is destroyed: nothing needs holding meanwhile. */
    @Volatile
    var screenAttached: Boolean = false

    /** Holds a message when there is no screen; does nothing when there is one. Returns whether it was held. */
    @Synchronized
    fun holdIfNoScreen(raw: String): Boolean {
        if (screenAttached) return false
        held.addLast(HeldRelayMessage(raw, now()))
        while (held.size > maxHeld) held.removeFirst()
        return true
    }

    /** Takes every held message that is still fresh, oldest first, and empties the inbox. */
    @Synchronized
    fun drain(): List<HeldRelayMessage> {
        val limit = now() - maxAgeMs
        val fresh = held.filter { it.receivedAtMs > limit }
        held.clear()
        return fresh
    }

    @Synchronized
    fun size(): Int = held.size

    @Synchronized
    fun clear() = held.clear()
}

/** The inbox the listener service and the screen share inside the one process of the app. */
object RelayInboxHolder {
    val inbox = RelayInbox()
}
