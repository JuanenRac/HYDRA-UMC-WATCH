// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/CancellationGate.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// WatchRelayTransport.cancelPendingRetries used to call only
// Handler.removeCallbacksAndMessages(null), which cancels a Runnable already
// sitting in that Handler's own queue but can never touch a Play Services
// getCapability()/sendMessage() call already dispatched and awaiting its own
// network/IPC round trip - that listener's own success/failure callback fired
// regardless, and on a retryable failure would schedule a brand-new retry
// AFTER cancellation already ran, as if it had never happened. This is the
// real generation-token mechanism that closes that gap - pulled out as its
// own pure class (no Android dependency) so it's directly unit-testable on
// the JVM, same convention as RelayRetryPolicy/pickCompanionNode in this
// same package.
package com.hydraumc.watch.transport

/**
 * A simple generation-token gate. The owner captures [current] right before
 * starting real async work (a Play Services listener, in
 * WatchRelayTransport's own case) and later checks [isCurrent] with that
 * captured value once the work's own callback actually fires - if [cancel]
 * ran in between, the captured value is stale and the callback should be a
 * silent no-op instead of invoking whatever it was about to do.
 */
class CancellationGate {
    @Volatile
    var current: Int = 0
        private set

    /** Invalidates every value captured before this call. */
    fun cancel() {
        current++
    }

    /** True only if no [cancel] has run since [captured] was read. */
    fun isCurrent(captured: Int): Boolean = captured == current
}
