// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/WatchConfig.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.content.Context

/**
 * The settings of the watch app, read from `assets/watch.cfg` (key = value, `#` starts a comment). Every number is kept
 * inside its range and a missing or invalid line keeps the default, so a broken file can never switch a safety option
 * on: the options that touch safety (presence, confirmation) are off by default. Pure Kotlin apart from [load].
 */
data class WatchConfig(
    val alertsEnabled: Boolean = true,
    val alertsStaleAfterMs: Long = 300_000L,
    val alertsCriticalPersistent: Boolean = true,
    val presenceEnabled: Boolean = false,
    val presenceIntervalMs: Long = 5_000L,
    val confirmEnabled: Boolean = false,
    val confirmSecondTapWindowMs: Long = 6_000L,
    val confirmMinGapMs: Long = 500L,
    val confirmMaxValidityMs: Long = 30_000L,
    val complicationEnabled: Boolean = true,
    val complicationStaleAfterMs: Long = 300_000L,
) {
    companion object {
        const val FILE_NAME = "watch.cfg"

        /** Reads the settings in [text]; anything not understood keeps its default. */
        fun parse(text: String): WatchConfig {
            val values = HashMap<String, String>()
            for (line in text.lineSequence()) {
                val clean = line.substringBefore('#').trim()
                val at = clean.indexOf('=')
                if (at <= 0) continue
                values[clean.substring(0, at).trim()] = clean.substring(at + 1).trim()
            }
            val d = WatchConfig()
            fun flag(key: String, default: Boolean): Boolean = when (values[key]?.lowercase()) {
                "true" -> true
                "false" -> false
                else -> default
            }
            fun number(key: String, default: Long, range: LongRange, unitMs: Long): Long {
                val v = values[key]?.toLongOrNull() ?: return default
                return if (v in range) v * unitMs else default
            }
            return WatchConfig(
                alertsEnabled = flag("alerts.enabled", d.alertsEnabled),
                alertsStaleAfterMs = number("alerts.stale_after_s", d.alertsStaleAfterMs, 10L..3600L, 1000L),
                alertsCriticalPersistent = flag("alerts.critical_persistent", d.alertsCriticalPersistent),
                presenceEnabled = flag("presence.enabled", d.presenceEnabled),
                presenceIntervalMs = number("presence.interval_s", d.presenceIntervalMs, 1L..60L, 1000L),
                confirmEnabled = flag("confirm.enabled", d.confirmEnabled),
                confirmSecondTapWindowMs = number("confirm.second_tap_window_s", d.confirmSecondTapWindowMs, 2L..30L, 1000L),
                confirmMinGapMs = number("confirm.min_gap_ms", d.confirmMinGapMs, 200L..2000L, 1L),
                confirmMaxValidityMs = number("confirm.max_validity_s", d.confirmMaxValidityMs, 5L..120L, 1000L),
                complicationEnabled = flag("complication.enabled", d.complicationEnabled),
                complicationStaleAfterMs = number("complication.stale_after_s", d.complicationStaleAfterMs, 30L..3600L, 1000L),
            )
        }

        /** The settings of the installed app; the defaults when the file cannot be read. */
        fun load(context: Context): WatchConfig = runCatching {
            parse(context.applicationContext.assets.open(FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() })
        }.getOrDefault(WatchConfig())
    }
}
