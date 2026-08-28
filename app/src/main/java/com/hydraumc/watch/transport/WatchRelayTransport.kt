// =============================================================================
// HYDRA-UMC-WATCH - Paired-phone voice relay transport
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// Uses the Wear OS Data Layer rather than a self-made Bluetooth socket. Google
// Play services accepts this traffic only between APKs with the same package
// name and signing certificate, then encrypts it over Bluetooth or its relay.
package com.hydraumc.watch.transport

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.hydraumc.watch.protocol.SyncMessage

object WatchRelayPaths {
    const val VOICE_TURN = "/hydra-umc/voice-turn/v1"
    const val STATUS_REQUEST = "/hydra-umc/system-status/v1"
    const val ASSISTANT_REPLY = "/hydra-umc/assistant-reply/v1"
    const val SYSTEM_STATUS = "/hydra-umc/system-status-reply/v1"
}

class WatchRelayTransport(context: Context) {
    private val appContext = context.applicationContext

    fun sendVoiceTurn(turn: SyncMessage.VoiceTurn, onResult: (Result<Unit>) -> Unit) {
        send(WatchRelayPaths.VOICE_TURN, turn.toJson(), onResult)
    }

    fun requestSystemStatus(onResult: (Result<Unit>) -> Unit) {
        // MessageClient messages are intentionally ephemeral. A stale health
        // card must never be delivered later and mistaken for current state.
        send(WatchRelayPaths.STATUS_REQUEST, "{}", onResult)
    }

    private fun send(path: String, payload: String, onResult: (Result<Unit>) -> Unit) {
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                val phone = nodes.firstOrNull()
                if (phone == null) {
                    onResult(Result.failure(IllegalStateException("No paired HYDRA-UMC phone is connected")))
                    return@addOnSuccessListener
                }
                Wearable.getMessageClient(appContext)
                    .sendMessage(phone.id, path, payload.encodeToByteArray())
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { error -> onResult(Result.failure(error)) }
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }
}
