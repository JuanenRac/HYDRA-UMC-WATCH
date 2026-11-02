// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/TwoStepConfirmationTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoStepConfirmationTest {
    private val token = "tok_ABCDEFGHIJKLMNOP.signature"
    private val reply = SyncMessage.AssistantReply(
        requestId = "watch-1",
        text = "Start mission alpha?",
        requiresConfirmation = true,
        confirmationToken = token,
        confirmationValiditySeconds = 30,
    )

    private fun on() = TwoStepConfirmation(enabled = true, secondTapWindowMs = 6_000, minGapMs = 500, maxValidityMs = 30_000)

    @Test
    fun `off by default nothing is offered and taps are ignored`() {
        val off = TwoStepConfirmation(enabled = false)
        assertFalse(off.offer(reply, 0))
        assertEquals(ConfirmTapResult.Ignored, off.tap(1_000, companionAvailable = true))
    }

    @Test
    fun `a reply without a token or without the flag is not offered`() {
        assertFalse(on().offer(reply.copy(confirmationToken = null), 0))
        assertFalse(on().offer(reply.copy(requiresConfirmation = false), 0))
    }

    @Test
    fun `two deliberate taps release the confirmation once`() {
        val c = on()
        assertTrue(c.offer(reply, 0))
        assertTrue(c.tap(1_000, true) is ConfirmTapResult.FirstTap)
        assertTrue(c.isArmed(2_000))
        val done = c.tap(2_000, true) as ConfirmTapResult.Confirmed
        assertEquals("watch-1", done.confirmation.confirms)
        assertEquals(token, done.confirmation.confirmationToken)
        // single use: the token is gone
        assertFalse(c.isOffered)
        assertEquals(ConfirmTapResult.Ignored, c.tap(3_000, true))
    }

    @Test
    fun `a hurried second touch is not a second tap`() {
        val c = on()
        c.offer(reply, 0)
        c.tap(1_000, true)
        assertEquals(ConfirmTapResult.TooFast, c.tap(1_200, true))
        assertTrue(c.tap(1_600, true) is ConfirmTapResult.Confirmed)
    }

    @Test
    fun `a second tap after the window starts over`() {
        val c = on()
        c.offer(reply, 0)
        c.tap(1_000, true)
        assertTrue(c.tap(8_000, true) is ConfirmTapResult.FirstTap)
        assertFalse(c.isArmed(15_000))
    }

    @Test
    fun `the validity runs out and the token is forgotten`() {
        val c = on()
        c.offer(reply, 0)
        assertEquals(ConfirmTapResult.Expired, c.tap(30_000, true))
        assertFalse(c.isOffered)
    }

    @Test
    fun `the configured limit caps a longer validity from the sender`() {
        val c = TwoStepConfirmation(enabled = true, maxValidityMs = 10_000)
        c.offer(reply.copy(confirmationValiditySeconds = 120), 0)
        assertEquals(10_000, c.remainingMs(0))
        assertEquals(0, c.remainingMs(10_000))
        // a shorter validity from the sender wins
        c.offer(reply.copy(confirmationValiditySeconds = 4), 0)
        assertEquals(4_000, c.remainingMs(0))
    }

    @Test
    fun `without the phone companion nothing is released`() {
        val c = on()
        c.offer(reply, 0)
        assertEquals(ConfirmTapResult.CompanionMissing, c.tap(1_000, companionAvailable = false))
        assertTrue(c.isOffered)
    }

    @Test
    fun `a new offer replaces the old one and clear forgets it`() {
        val c = on()
        c.offer(reply, 0)
        c.tap(1_000, true)
        c.offer(reply.copy(requestId = "watch-2"), 5_000)
        assertFalse(c.isArmed(5_100))
        val first = c.tap(5_200, true)
        assertTrue(first is ConfirmTapResult.FirstTap)
        c.clear()
        assertNull((c.tap(6_000, true) as? ConfirmTapResult.Confirmed))
    }
}
