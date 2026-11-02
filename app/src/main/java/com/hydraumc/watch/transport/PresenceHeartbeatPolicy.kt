// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/PresenceHeartbeatPolicy.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.PresenceState
import com.hydraumc.watch.protocol.SyncMessage

/**
 * Builds the heartbeats of the presence of the operator (dead-man). It only says WHEN to send and WHAT: the numbers rise
 * by one for every heartbeat so the server can see a gap, and ending the presence on purpose sends one last heartbeat with
 * the state LEFT, so a deliberate leave is not taken for a loss. What a lost heartbeat means (slow down, pause) is decided
 * by the server, never by this watch. Off unless [enabled]. Pure Kotlin, the caller supplies the clock.
 */
class PresenceHeartbeatPolicy(
    private val enabled: Boolean,
    private val intervalMs: Long,
) {
    init {
        require(intervalMs in 1_000L..60_000L) { "the interval must be from 1 to 60 seconds" }
    }

    private var sequence = 0L
    private var lastSentAtMs: Long? = null
    private var running = false

    val isRunning: Boolean get() = running

    /** The presence starts: the first heartbeat is due at once. */
    fun start() {
        if (!enabled) return
        running = true
        lastSentAtMs = null
    }

    /** The next heartbeat when one is due at [nowMs], else null. Nothing is produced while the presence is off or stopped. */
    fun nextDue(nowMs: Long): SyncMessage.PresenceHeartbeat? {
        if (!enabled || !running) return null
        val last = lastSentAtMs
        if (last != null && nowMs >= last && nowMs - last < intervalMs) return null
        return build(PresenceState.PRESENT, nowMs)
    }

    /** The presence ends on purpose: one last heartbeat that says so, or null when it was not running. */
    fun stop(nowMs: Long): SyncMessage.PresenceHeartbeat? {
        if (!enabled || !running) return null
        running = false
        return build(PresenceState.LEFT, nowMs)
    }

    private fun build(state: PresenceState, nowMs: Long): SyncMessage.PresenceHeartbeat {
        lastSentAtMs = nowMs
        return SyncMessage.PresenceHeartbeat(
            sequence = sequence++,
            intervalSeconds = (intervalMs / 1000L).toInt(),
            state = state,
        )
    }
}
