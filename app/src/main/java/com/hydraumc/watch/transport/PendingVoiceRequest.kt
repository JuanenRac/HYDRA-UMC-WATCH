// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/PendingVoiceRequest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

/**
 * Tracks the one voice request the operator is waiting on, so a late answer
 * to an older question is never shown (or spoken) as the answer to the newest
 * one, and so a request nobody answered times out visibly instead of staying
 * on "sending" forever. Pure and JVM-testable: the caller supplies the clock.
 */
class PendingVoiceRequest(private val timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
    private var requestId: String? = null
    private var startedAtMs: Long = 0L

    /** Starts waiting for [id]; any earlier pending request is forgotten. */
    fun begin(id: String, nowMs: Long) {
        requestId = id
        startedAtMs = nowMs
    }

    /**
     * True when a reply for [id] answers the request being waited on. The
     * request is cleared on a match, so a duplicate delivery is refused.
     */
    fun accept(id: String): Boolean {
        if (requestId != id) return false
        requestId = null
        return true
    }

    /** True when a request has been waiting longer than the timeout. */
    fun isExpired(nowMs: Long): Boolean = requestId != null && nowMs - startedAtMs >= timeoutMs

    /** Forgets the pending request (after reporting the timeout, or a failed send). */
    fun clear() {
        requestId = null
    }

    val isPending: Boolean get() = requestId != null

    companion object {
        // A little longer than the phone's own 15 s relay timeout.
        const val DEFAULT_TIMEOUT_MS = 20_000L
    }
}
