// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/haptics/HapticPatternsTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.haptics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPatternsTest {

    @Test
    fun `every severity has a distinct pattern`() {
        val critical = patternFor(AlertSeverity.CRITICAL)
        val warning = patternFor(AlertSeverity.WARNING)
        val info = patternFor(AlertSeverity.INFO)

        assertNotEquals(critical.toList(), warning.toList())
        assertNotEquals(critical.toList(), info.toList())
        assertNotEquals(warning.toList(), info.toList())
    }

    @Test
    fun `every pattern starts with zero initial delay`() {
        for (severity in AlertSeverity.entries) {
            assertEquals("severity=$severity", 0L, patternFor(severity)[0])
        }
    }

    @Test
    fun `every pattern has an even length (paired on-off durations)`() {
        for (severity in AlertSeverity.entries) {
            assertEquals("severity=$severity", 0, patternFor(severity).size % 2)
        }
    }

    @Test
    fun `critical is the most urgent pattern - more pulses than warning or info`() {
        fun pulseCount(pattern: LongArray) = pattern.size / 2
        assertTrue(pulseCount(patternFor(AlertSeverity.CRITICAL)) > pulseCount(patternFor(AlertSeverity.WARNING)))
        assertTrue(pulseCount(patternFor(AlertSeverity.WARNING)) > pulseCount(patternFor(AlertSeverity.INFO)))
    }

    @Test
    fun `no duration is negative`() {
        for (severity in AlertSeverity.entries) {
            for (duration in patternFor(severity)) {
                assertTrue("severity=$severity duration=$duration", duration >= 0)
            }
        }
    }
}
