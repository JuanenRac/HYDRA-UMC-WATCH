// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/PendingVoiceRequestTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingVoiceRequestTest {
    @Test
    fun `only the reply to the latest request is accepted, once`() {
        val pending = PendingVoiceRequest(timeoutMs = 1_000L)
        pending.begin("watch-1", nowMs = 0L)
        pending.begin("watch-2", nowMs = 10L)

        assertFalse(pending.accept("watch-1"))
        assertTrue(pending.accept("watch-2"))
        assertFalse(pending.accept("watch-2"))
        assertFalse(pending.isPending)
    }

    @Test
    fun `a request nobody answered expires and can be cleared`() {
        val pending = PendingVoiceRequest(timeoutMs = 1_000L)
        assertFalse(pending.isExpired(5_000L))

        pending.begin("watch-1", nowMs = 0L)
        assertFalse(pending.isExpired(999L))
        assertTrue(pending.isExpired(1_000L))

        pending.clear()
        assertFalse(pending.isExpired(9_000L))
    }
}
