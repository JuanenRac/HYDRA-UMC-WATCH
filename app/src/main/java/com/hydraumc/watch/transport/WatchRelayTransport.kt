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
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.Wearable
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.toJson

object WatchRelayPaths {
    const val VOICE_TURN = "/hydra-umc/voice-turn/v1"
    const val STATUS_REQUEST = "/hydra-umc/system-status/v1"
    const val ASSISTANT_REPLY = "/hydra-umc/assistant-reply/v1"
    const val SYSTEM_STATUS = "/hydra-umc/system-status-reply/v1"
}

class WatchRelayTransport(
    context: Context,
    // Real, JVM-testable-on-its-own retry policy (see RelayRetryPolicy.kt) -
    // this class only schedules the delays that policy computes; it doesn't
    // decide them itself, so the actual backoff math stays testable without
    // Android's Handler/Wearable APIs.
    private val retryPolicy: RelayRetryPolicy = RelayRetryPolicy(),
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private val appContext = context.applicationContext

    fun sendVoiceTurn(turn: SyncMessage.VoiceTurn, onResult: (Result<Unit>) -> Unit) {
        sendWithRetry(WatchRelayPaths.VOICE_TURN, turn.toJson(), attempt = 1, onResult)
    }

    fun requestSystemStatus(onResult: (Result<Unit>) -> Unit) {
        // The relayed REQUEST itself is fine to retry (it's idempotent - "what's
        // the current status" - unlike replaying a stale REPLY, which
        // LastKnownStateCache guards against on the receiving side instead).
        sendWithRetry(WatchRelayPaths.STATUS_REQUEST, "{}", attempt = 1, onResult)
    }

    /**
     * Real reconnection policy - the promotion audit's own requirement. A
     * send that fails because no phone node is connected yet (a real,
     * common transient state right after the watch reboots, or while
     * Bluetooth is momentarily out of range) is retried on the delay
     * [retryPolicy] computes, up to its own attempt cap, before finally
     * reporting failure to the caller.
     */
    private fun sendWithRetry(path: String, payload: String, attempt: Int, onResult: (Result<Unit>) -> Unit) {
        send(path, payload) { result ->
            if (result.isSuccess || !retryPolicy.shouldRetry(attempt)) {
                onResult(result)
                return@send
            }
            handler.postDelayed(
                { sendWithRetry(path, payload, attempt + 1, onResult) },
                retryPolicy.delayBeforeAttemptMs(attempt),
            )
        }
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
