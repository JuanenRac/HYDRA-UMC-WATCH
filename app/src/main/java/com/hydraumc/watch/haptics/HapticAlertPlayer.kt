// =============================================================================
// HYDRA-UMC-WATCH - Wear OS haptic-alert playback service
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Plays the already-tested severity waveforms through the watch's real
 * vibration hardware. Calling this is harmless on a device without a
 * vibrator: the request is ignored. The caller decides when an alert is
 * legitimate; this class never creates an E-STOP or sends a robot command.
 */
class HapticAlertPlayer(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(severity: AlertSeverity) {
        val target = vibrator ?: return
        if (!target.hasVibrator()) return
        val waveform = patternFor(severity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            target.vibrate(VibrationEffect.createWaveform(waveform, -1))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(waveform, -1)
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }
}
