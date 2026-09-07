// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/haptics/HapticPatterns.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.haptics

/**
 * Differentiated vibration patterns per [AlertSeverity] - the "Haptic
 * Alerts" Key Feature from README.md. Deliberately plain data (a
 * [LongArray] in the on/off-duration-pair format
 * `android.os.VibrationEffect.createWaveform(timings, repeat)` expects:
 * index 0 is the initial off-delay, then alternating on/off durations in
 * milliseconds) rather than something that calls into the real Vibrator
 * API - that keeps this function testable on a plain JVM, no watch
 * hardware or emulator required. Wiring `VibrationEffect.createWaveform`
 * and the actual `Vibrator` service call is a one-line integration left
 * for when the wearable sync flow itself lands.
 */
fun patternFor(severity: AlertSeverity): LongArray = when (severity) {
    // Three short, fast pulses - the most attention-grabbing pattern,
    // reserved for E-STOP-adjacent critical alerts.
    AlertSeverity.CRITICAL -> longArrayOf(0, 150, 100, 150, 100, 150)
    // Two medium pulses - noticeable but not alarming.
    AlertSeverity.WARNING -> longArrayOf(0, 250, 200, 250)
    // One short pulse - a glanceable nudge, not meant to interrupt.
    AlertSeverity.INFO -> longArrayOf(0, 100)
}
