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
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.toJson

object WatchRelayPaths {
    const val VOICE_TURN = "/hydra-umc/voice-turn/v1"
    const val STATUS_REQUEST = "/hydra-umc/system-status/v1"
    const val ASSISTANT_REPLY = "/hydra-umc/assistant-reply/v1"
    const val SYSTEM_STATUS = "/hydra-umc/system-status-reply/v1"
}

// WATCH-01 (P2):
// the real, Google-documented Wear OS capability name HYDRA-UMC-ANDROID-
// CONTROL declares in its own res/values/wear.xml
// (android_wear_capabilities) - the one real signal that a connected
// node is actually running the companion app, not just "some phone/
// watch happens to be connected right now". Two literal strings kept in
// sync BY HAND across repos (an Android string resource can't be shared
// across separate Gradle projects) - see that file's own header comment.
const val PHONE_COMPANION_CAPABILITY = "hydra_umc_phone_companion"

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
     * WATCH-01 (P2): a retry scheduled via [handler] used to have no way to be
     * cancelled at all - only a max-attempt COUNTER bounded it, so a
     * pending retry from before the screen closed still fired later
     * regardless, invoking [onResult] against a caller (MainActivity)
     * that may have already been destroyed/recreated. Call this from the
     * owning component's own lifecycle teardown (MainActivity.onDestroy)
     * to cancel every retry still pending for THIS transport instance -
     * [Handler.removeCallbacksAndMessages] only ever removes messages
     * this exact Handler instance itself posted, so it can never cancel
     * a different transport/screen's own pending work.
     */
    fun cancelPendingRetries() {
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Real reconnection policy - the diagnosis notes' own requirement. A
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
        // WATCH-01: connectedNodes (any Bluetooth/Wear-companion node at
        // all, including one with no HYDRA-UMC app whatsoever) replaced
        // with a real capability query - FILTER_REACHABLE only returns
        // nodes this exact companion app is actually installed AND
        // currently reachable on, so a second connected node without it
        // (another phone, a different watch) can never be picked.
        Wearable.getCapabilityClient(appContext)
            .getCapability(PHONE_COMPANION_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capabilityInfo ->
                val phone = pickCompanionNode(capabilityInfo.nodes)
                if (phone == null) {
                    onResult(Result.failure(IllegalStateException("No paired HYDRA-UMC phone companion is reachable")))
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

/**
 * WATCH-01: which reachable, capability-verified companion node to send
 * to when more than one qualifies (a real but rare case - paired to more
 * than one phone with the app installed). Prefers a NEARBY node (Play
 * services' own signal for "directly Bluetooth-connected", not relayed
 * through the cloud) - lower latency, and far more likely to actually be
 * the phone on the operator's wrist/person right now - falling back to
 * whichever verified node comes first if none report as nearby. A pure
 * function over Node (not the transport class itself) so this real
 * selection logic is unit-testable on the JVM without a Looper/Handler/
 * live Play services connection, same convention as RelayRetryPolicy.
 */
internal fun pickCompanionNode(nodes: Set<Node>): Node? =
    nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
