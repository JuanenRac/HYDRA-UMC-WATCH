// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/RelayRetryPolicyTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayRetryPolicyTest {

    @Test
    fun `first retry delay equals the real base delay`() {
        val policy = RelayRetryPolicy(maxAttempts = 5, baseDelayMs = 500, maxDelayMs = 8_000)
        assertEquals(500L, policy.delayBeforeAttemptMs(1))
    }

    @Test
    fun `delay doubles each attempt - real exponential backoff`() {
        val policy = RelayRetryPolicy(maxAttempts = 5, baseDelayMs = 500, maxDelayMs = 8_000)
        assertEquals(500L, policy.delayBeforeAttemptMs(1))
        assertEquals(1_000L, policy.delayBeforeAttemptMs(2))
        assertEquals(2_000L, policy.delayBeforeAttemptMs(3))
        assertEquals(4_000L, policy.delayBeforeAttemptMs(4))
    }

    @Test
    fun `delay is capped at maxDelayMs - a real bounded backoff, not unbounded growth`() {
        val policy = RelayRetryPolicy(maxAttempts = 10, baseDelayMs = 500, maxDelayMs = 8_000)
        assertEquals(8_000L, policy.delayBeforeAttemptMs(5))
        assertEquals(8_000L, policy.delayBeforeAttemptMs(20))
    }

    @Test
    fun `shouldRetry is true while under maxAttempts, false once reached`() {
        val policy = RelayRetryPolicy(maxAttempts = 3, baseDelayMs = 500, maxDelayMs = 8_000)
        assertTrue(policy.shouldRetry(1))
        assertTrue(policy.shouldRetry(2))
        assertFalse(policy.shouldRetry(3))
    }

    @Test
    fun `a single-attempt policy never retries`() {
        val policy = RelayRetryPolicy(maxAttempts = 1, baseDelayMs = 500, maxDelayMs = 8_000)
        assertFalse(policy.shouldRetry(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a non-positive maxAttempts`() {
        RelayRetryPolicy(maxAttempts = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a maxDelayMs smaller than baseDelayMs`() {
        RelayRetryPolicy(baseDelayMs = 1_000, maxDelayMs = 500)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an attempt number below 1`() {
        RelayRetryPolicy().delayBeforeAttemptMs(0)
    }
}
