// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/CancellationGateTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellationGateTest {

    @Test
    fun `a value captured before any cancellation is current`() {
        val gate = CancellationGate()
        val captured = gate.current
        assertTrue(gate.isCurrent(captured))
    }

    // this project's own exact scenario: WatchRelayTransport.send captures
    // gate.current right before starting a Play Services listener that
    // stays in flight - if cancelPendingRetries() (gate.cancel()) runs
    // while that listener is still pending, the value it captured earlier
    // must be refused once the listener finally fires.
    @Test
    fun `a captured value is stale after cancel - the real in-flight-listener scenario`() {
        val gate = CancellationGate()
        val capturedBeforeCancel = gate.current
        gate.cancel()
        assertFalse(gate.isCurrent(capturedBeforeCancel))
    }

    @Test
    fun `a value captured AFTER cancel is current until the next cancel`() {
        val gate = CancellationGate()
        gate.cancel()
        val capturedAfterCancel = gate.current
        assertTrue(gate.isCurrent(capturedAfterCancel))
    }

    @Test
    fun `cancelling before AND after resolve - neither capture stays valid, matching cancelling before or after resolving capability slash sendMessage`() {
        val gate = CancellationGate()
        // "cancel before resolve": captured, then cancelled before the
        // listener fires.
        val capturedBeforeResolve = gate.current
        gate.cancel()
        assertFalse(gate.isCurrent(capturedBeforeResolve))

        // "cancel after resolve": a second real send captures fresh,
        // resolves successfully (isCurrent checked and found true at that
        // moment), then a LATER cancel must invalidate any further reuse
        // of that same stale capture (e.g. a retry scheduled from it).
        val capturedForSecondSend = gate.current
        assertTrue(gate.isCurrent(capturedForSecondSend)) // resolves while still valid
        gate.cancel()
        assertFalse(gate.isCurrent(capturedForSecondSend)) // a retry attempting to reuse it afterward is refused
    }

    @Test
    fun `multiple cancels in a row never resurrect an old capture`() {
        val gate = CancellationGate()
        val captured = gate.current
        gate.cancel()
        gate.cancel()
        gate.cancel()
        assertFalse(gate.isCurrent(captured))
    }
}
