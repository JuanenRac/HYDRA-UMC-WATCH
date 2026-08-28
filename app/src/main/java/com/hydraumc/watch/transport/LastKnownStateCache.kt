// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/LastKnownStateCache.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage

/** One real relay message plus the real wall-clock instant it was received. */
data class CachedRelayState(
    val message: SyncMessage,
    val receivedAtMs: Long,
)

/**
 * Real cache of the most recently received [SyncMessage.AssistantReply] /
 * [SyncMessage.SystemStatus] / [SyncMessage.Alert], with real staleness
 * tracking - the promotion audit's own "no presentar una alerta caducada
 * como orden vigente". Pure Kotlin, no Android dependency, so it is
 * unit-testable on a plain JVM without a watch, like [SyncMessage.kt]'s own
 * protocol logic.
 */
class LastKnownStateCache(
    private val staleAfterMs: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init {
        require(staleAfterMs > 0) { "staleAfterMs must be positive" }
    }

    private var cached: CachedRelayState? = null

    /** Records a real, freshly received message as the new last-known state. */
    fun update(message: SyncMessage) {
        cached = CachedRelayState(message, now())
    }

    /** The current cached state, or null if nothing has ever been received. */
    fun current(): CachedRelayState? = cached

    /**
     * True once a cached state is old enough that it must not be shown as
     * current - the caller should either withhold it or clearly mark it as
     * last-known/possibly outdated, never render it as a live order (the
     * exact real risk with, for example, a CRITICAL [SyncMessage.Alert]
     * relayed minutes ago on a since-disconnected watch).
     */
    fun isStale(): Boolean {
        val state = cached ?: return false
        return now() - state.receivedAtMs >= staleAfterMs
    }

    /** How long ago (real ms) the cached state was received, or null if none exists. */
    fun ageMs(): Long? {
        val state = cached ?: return null
        return now() - state.receivedAtMs
    }

    fun clear() {
        cached = null
    }
}
