// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/protocol/ErrorCodes.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// the real, stable set of system-error identifiers WatchVoiceRelayService
// (HYDRA-UMC-ANDROID-CONTROL, the phone-side sender of AssistantReply/
// SystemStatus) may send in errorCode - see AssistantReply's own header
// comment for why this exists at all. These identifiers themselves are
// PROTOCOL, never translated (matching every other wire-level string this
// ecosystem already treats that way) - only the *resolved* string this
// function returns is ever shown to a person or spoken by TTS.
package com.hydraumc.watch.protocol

import androidx.annotation.StringRes
import com.hydraumc.watch.R

/**
 * Maps a real, known [errorCode] to this watch's own localized string
 * resource - a pure function (no Context/Resources needed), so it's
 * directly unit-testable on the JVM without Robolectric, same convention
 * as RelayRetryPolicy/pickCompanionNode. Returns null for `null` or any
 * code this build doesn't recognize (an older watch build talking to a
 * newer phone, or a genuinely unknown value) - the caller falls back to
 * the message's own free-form text/headline/detail in that case, never a
 * crash or a raw, unlocalized code shown to the operator.
 */
@StringRes
fun errorCodeToStringRes(errorCode: String?): Int? = when (errorCode) {
    "connection_unavailable" -> R.string.voice_error_connection_unavailable
    "offline" -> R.string.status_error_offline_headline
    else -> null
}

/**
 * The [errorCode] counterpart for [SyncMessage.SystemStatus]'s own
 * `detail` field - kept separate from [errorCodeToStringRes] since a
 * SystemStatus message localizes two real strings (headline + detail)
 * from the SAME code, not one.
 */
@StringRes
fun errorCodeToDetailStringRes(errorCode: String?): Int? = when (errorCode) {
    "offline" -> R.string.status_error_offline_detail
    else -> null
}
