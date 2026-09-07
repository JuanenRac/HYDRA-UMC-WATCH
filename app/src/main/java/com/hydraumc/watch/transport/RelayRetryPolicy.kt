// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/RelayRetryPolicy.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

/**
 * Real, pure exponential-backoff retry policy for a relay send - the
 * promotion audit's own "reconexion" requirement, extracted from any
 * Android transport so it is unit-testable on a plain JVM, without an
 * emulator or watch. [WatchRelayTransport] is the Android-dependent piece
 * that actually schedules a retry using this policy's own numbers.
 */
class RelayRetryPolicy(
    val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 500,
    private val maxDelayMs: Long = 8_000,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
        require(baseDelayMs > 0) { "baseDelayMs must be positive" }
        require(maxDelayMs >= baseDelayMs) { "maxDelayMs must be >= baseDelayMs" }
    }

    /**
     * Real delay before retry attempt number `attempt` (1-based: the delay
     * before the SECOND real send attempt). Doubles each attempt, capped at
     * [maxDelayMs] - a real, bounded backoff, not an unbounded exponential
     * growth that could leave a real alert unretried for minutes on a flaky
     * Bluetooth link.
     */
    fun delayBeforeAttemptMs(attempt: Int): Long {
        require(attempt >= 1) { "attempt must be at least 1" }
        val shift = (attempt - 1).coerceAtMost(20) // guards against Long overflow on a runaway attempt count
        val doubled = baseDelayMs shl shift
        return doubled.coerceIn(baseDelayMs, maxDelayMs)
    }

    /** Whether a real send that failed on `attempt` should be retried at all. */
    fun shouldRetry(attempt: Int): Boolean = attempt < maxAttempts
}
