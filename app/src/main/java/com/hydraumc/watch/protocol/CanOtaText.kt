// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/protocol/CanOtaText.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import androidx.annotation.StringRes
import com.hydraumc.watch.R

/** The text of a phase of a flash cycle in the language of the watch; null for a phase this build does not know (a parsed message never has one). */
@StringRes
fun canOtaPhaseStringRes(phase: String): Int? = when (phase) {
    "connecting" -> R.string.canota_phase_connecting
    "entering_bootloader" -> R.string.canota_phase_entering_bootloader
    "erasing_fram" -> R.string.canota_phase_erasing_fram
    "transferring" -> R.string.canota_phase_transferring
    "verifying" -> R.string.canota_phase_verifying
    "rebooting" -> R.string.canota_phase_rebooting
    "done" -> R.string.canota_phase_done
    "error" -> R.string.canota_phase_error
    else -> null
}

/** True once the cycle ended, well or badly. */
fun canOtaFinished(phase: String): Boolean = phase == "done" || phase == "error"
