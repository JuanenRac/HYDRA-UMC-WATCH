// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/ReplySeverityTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.haptics.AlertSeverity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplySeverityTest {
    @Test
    fun `the level decides the haptic severity`() {
        assertEquals(AlertSeverity.CRITICAL, hapticSeverityFor(WatchStatusLevel.CRITICAL, null))
        assertEquals(AlertSeverity.WARNING, hapticSeverityFor(WatchStatusLevel.WARNING, null))
        assertEquals(AlertSeverity.WARNING, hapticSeverityFor(WatchStatusLevel.OFFLINE, null))
        assertEquals(AlertSeverity.INFO, hapticSeverityFor(WatchStatusLevel.NOMINAL, null))
        assertEquals(AlertSeverity.INFO, hapticSeverityFor(WatchStatusLevel.ATTENTION, null))
    }

    @Test
    fun `a known error code is at least a warning but never lowers a critical level`() {
        assertEquals(AlertSeverity.WARNING, hapticSeverityFor(WatchStatusLevel.ATTENTION, "offline"))
        assertEquals(AlertSeverity.CRITICAL, hapticSeverityFor(WatchStatusLevel.CRITICAL, "offline"))
    }
}
