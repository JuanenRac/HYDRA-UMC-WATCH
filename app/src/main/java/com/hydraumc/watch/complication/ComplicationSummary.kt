// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/complication/ComplicationSummary.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.complication

import com.hydraumc.watch.protocol.SyncMessage

/** What the watch-face complication shows. */
sealed class ComplicationSummary {
    /** Nothing is known: no status yet, or the phone does not say how many robots run. */
    object Unknown : ComplicationSummary()

    /** The last state is too old to be shown as a number. */
    object Outdated : ComplicationSummary()

    /** An alarm is active. */
    object Alarm : ComplicationSummary()

    /** [count] robots are running (0 is a valid, quiet answer). */
    data class Running(val count: Int) : ComplicationSummary()

    /** The short text on the face: the count, "!" for an alarm, "?" when outdated, "-" when unknown. */
    fun shortText(): String = when (this) {
        Unknown -> "-"
        Outdated -> "?"
        Alarm -> "!"
        is Running -> count.toString()
    }

    companion object {
        /**
         * Reads the last status the phone sent. The numbers are the words of the phone: when it does not say how many robots
         * are running, the complication does not invent a count. A state older than [staleAfterMs] is "outdated", never a
         * number or an alarm that may have ended.
         */
        fun from(status: SyncMessage.SystemStatus?, ageMs: Long?, staleAfterMs: Long): ComplicationSummary {
            if (status == null || ageMs == null) return Unknown
            if (ageMs < 0 || ageMs >= staleAfterMs) return Outdated
            if (status.alarmActive == true) return Alarm
            val running = status.robotsRunning ?: return Unknown
            return Running(running)
        }
    }
}
