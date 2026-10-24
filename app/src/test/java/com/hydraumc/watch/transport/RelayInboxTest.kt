// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/RelayInboxTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayInboxTest {
    private var clock = 1_000_000L
    private fun inbox(maxHeld: Int = 8, maxAgeMs: Long = 60_000L) = RelayInbox(maxHeld, maxAgeMs) { clock }

    @Test fun `a message that arrives with no screen is held and handed over once`() {
        val box = inbox()
        assertTrue(box.holdIfNoScreen("one"))
        assertEquals(listOf("one"), box.drain().map { it.raw })
        assertEquals(emptyList<HeldRelayMessage>(), box.drain())
    }

    @Test fun `nothing is held while the screen exists`() {
        val box = inbox()
        box.screenAttached = true
        assertFalse(box.holdIfNoScreen("one"))
        assertEquals(0, box.size())
        box.screenAttached = false
        assertTrue(box.holdIfNoScreen("two"))
        assertEquals(1, box.size())
    }

    @Test fun `messages come back oldest first with the instant they arrived`() {
        val box = inbox()
        box.holdIfNoScreen("a")
        clock += 5_000
        box.holdIfNoScreen("b")
        val drained = box.drain()
        assertEquals(listOf("a", "b"), drained.map { it.raw })
        assertEquals(5_000L, drained[1].receivedAtMs - drained[0].receivedAtMs)
    }

    @Test fun `only the newest messages are kept`() {
        val box = inbox(maxHeld = 3)
        for (i in 1..7) box.holdIfNoScreen("m$i")
        assertEquals(listOf("m5", "m6", "m7"), box.drain().map { it.raw })
    }

    @Test fun `a message that waited too long is dropped, not shown as current`() {
        val box = inbox(maxAgeMs = 60_000L)
        box.holdIfNoScreen("old")
        clock += 61_000
        box.holdIfNoScreen("new")
        assertEquals(listOf("new"), box.drain().map { it.raw })
    }

    @Test fun `a message exactly at the age limit is dropped`() {
        val box = inbox(maxAgeMs = 60_000L)
        box.holdIfNoScreen("edge")
        clock += 60_000
        assertEquals(emptyList<HeldRelayMessage>(), box.drain())
    }

    @Test fun `clear forgets everything`() {
        val box = inbox()
        box.holdIfNoScreen("a")
        box.clear()
        assertEquals(0, box.size())
        assertEquals(emptyList<HeldRelayMessage>(), box.drain())
    }

    @Test fun `silly limits are refused`() {
        for (bad in listOf(0, -1)) {
            val failed = runCatching { RelayInbox(maxHeld = bad) }.isFailure
            assertTrue(failed)
        }
        assertTrue(runCatching { RelayInbox(maxAgeMs = 0) }.isFailure)
    }

    @Test fun `the shared inbox starts with no screen and nothing held`() {
        RelayInboxHolder.inbox.clear()
        assertFalse(RelayInboxHolder.inbox.screenAttached)
        assertEquals(0, RelayInboxHolder.inbox.size())
    }
}
