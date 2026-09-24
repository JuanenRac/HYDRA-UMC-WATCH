// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/CriticalActionGate.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

/** Why an action that reaches the robot system is, or is not, offered on the watch. */
enum class ActionAvailability {
    AVAILABLE,

    /** Nothing has been received from the phone companion yet. */
    NO_COMPANION_HEARTBEAT,

    /** The last message from the companion is too old to prove it is still there. */
    COMPANION_HEARTBEAT_STALE,
}

/**
 * Decides whether an action that goes through the phone companion may be
 * offered. The companion's own messages are the heartbeat: until one has
 * arrived, and again once the last one is stale, the action is withheld
 * instead of being offered and failing later. Actions that only ask the
 * companion for its state (a refresh) are never gated, since they are how
 * the heartbeat is obtained. Pure Kotlin, testable on a plain JVM.
 */
object CriticalActionGate {
    fun availability(cache: LastKnownStateCache): ActionAvailability = when {
        cache.current() == null -> ActionAvailability.NO_COMPANION_HEARTBEAT
        cache.isStale() -> ActionAvailability.COMPANION_HEARTBEAT_STALE
        else -> ActionAvailability.AVAILABLE
    }

    fun isAvailable(cache: LastKnownStateCache): Boolean =
        availability(cache) == ActionAvailability.AVAILABLE
}
