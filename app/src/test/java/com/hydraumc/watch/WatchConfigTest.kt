// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/WatchConfigTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchConfigTest {
    @Test
    fun `the file that ships keeps every safety option off`() {
        val c = WatchConfig.parse(File("src/main/assets/watch.cfg").readText(Charsets.UTF_8))
        assertFalse(c.presenceEnabled)
        assertFalse(c.confirmEnabled)
        assertEquals(WatchConfig(), c)
    }

    @Test
    fun `a missing or broken file keeps the conservative defaults`() {
        for (text in listOf("", "garbage", "presence.enabled = maybe\nconfirm.enabled = 1", "= true")) {
            val c = WatchConfig.parse(text)
            assertFalse(c.presenceEnabled)
            assertFalse(c.confirmEnabled)
        }
    }

    @Test
    fun `values are read and comments are ignored`() {
        val c = WatchConfig.parse(
            """
            # comment
            presence.enabled = true   # on
            presence.interval_s = 10
            confirm.enabled = true
            confirm.second_tap_window_s = 8
            confirm.min_gap_ms = 700
            alerts.stale_after_s = 120
            """.trimIndent(),
        )
        assertTrue(c.presenceEnabled)
        assertEquals(10_000L, c.presenceIntervalMs)
        assertTrue(c.confirmEnabled)
        assertEquals(8_000L, c.confirmSecondTapWindowMs)
        assertEquals(700L, c.confirmMinGapMs)
        assertEquals(120_000L, c.alertsStaleAfterMs)
    }

    @Test
    fun `numbers outside their range keep the default`() {
        val c = WatchConfig.parse("presence.interval_s = 0\nconfirm.min_gap_ms = 5\nconfirm.max_validity_s = 9999\nalerts.stale_after_s = abc")
        assertEquals(WatchConfig().presenceIntervalMs, c.presenceIntervalMs)
        assertEquals(WatchConfig().confirmMinGapMs, c.confirmMinGapMs)
        assertEquals(WatchConfig().confirmMaxValidityMs, c.confirmMaxValidityMs)
        assertEquals(WatchConfig().alertsStaleAfterMs, c.alertsStaleAfterMs)
    }
}
