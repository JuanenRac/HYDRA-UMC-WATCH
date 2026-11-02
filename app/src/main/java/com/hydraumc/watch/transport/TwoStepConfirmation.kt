// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/TwoStepConfirmation.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage

/** What a tap on the confirm button did. */
sealed class ConfirmTapResult {
    /** The option is off, or nothing is waiting for a confirmation. */
    object Ignored : ConfirmTapResult()

    /** The validity of the confirmation ran out: it is forgotten and the order has to be spoken again. */
    object Expired : ConfirmTapResult()

    /** The phone companion has not been heard from, so nothing is sent. */
    object CompanionMissing : ConfirmTapResult()

    /** First tap taken: a second one inside the window confirms. [windowMs] is how long it has. */
    data class FirstTap(val windowMs: Long) : ConfirmTapResult()

    /** A second tap that came too soon after the first: ignored, the first still stands. */
    object TooFast : ConfirmTapResult()

    /** The second tap: the confirmation is released once; the caller sends it. */
    data class Confirmed(val confirmation: SyncMessage.VoiceConfirm) : ConfirmTapResult()
}

/**
 * The two deliberate taps that confirm a spoken order from the wrist. A reply that asks for confirmation and carries a
 * token is offered; the first tap arms it, a second tap (not sooner than [minGapMs], not later than [secondTapWindowMs])
 * releases it exactly once. The token lives only in this object, never in storage, and is dropped when the confirmation
 * is sent, cancelled or expires, whatever the sender says its validity is ([maxValidityMs] caps it). Nothing here moves
 * a robot: the phone sends the confirmation on only if it has a session and the reservation of the robot. Pure Kotlin,
 * the caller supplies the clock.
 */
class TwoStepConfirmation(
    private val enabled: Boolean,
    private val secondTapWindowMs: Long = 6_000L,
    private val minGapMs: Long = 500L,
    private val maxValidityMs: Long = 30_000L,
    private val newRequestId: (Long) -> String = { "watch-confirm-$it" },
) {
    private var requestId: String? = null
    private var token: String? = null
    private var expiresAtMs = 0L
    private var firstTapAtMs: Long? = null

    /** Offers [reply] for confirmation. Returns whether it can be confirmed on the watch. */
    fun offer(reply: SyncMessage.AssistantReply, nowMs: Long): Boolean {
        clear()
        if (!enabled || !reply.requiresConfirmation) return false
        val offeredToken = reply.confirmationToken ?: return false
        val senderMs = (reply.confirmationValiditySeconds ?: Int.MAX_VALUE).toLong() * 1000L
        requestId = reply.requestId
        token = offeredToken
        expiresAtMs = nowMs + minOf(senderMs, maxValidityMs)
        return true
    }

    val isOffered: Boolean get() = token != null

    /** Milliseconds left of the validity, or 0 when nothing is offered. */
    fun remainingMs(nowMs: Long): Long = if (token == null) 0L else (expiresAtMs - nowMs).coerceAtLeast(0L)

    /** True between the first and the second tap. */
    fun isArmed(nowMs: Long): Boolean {
        val first = firstTapAtMs ?: return false
        return token != null && nowMs - first < secondTapWindowMs
    }

    fun tap(nowMs: Long, companionAvailable: Boolean): ConfirmTapResult {
        val offered = token ?: return ConfirmTapResult.Ignored
        if (!enabled) return ConfirmTapResult.Ignored
        if (nowMs >= expiresAtMs) {
            clear()
            return ConfirmTapResult.Expired
        }
        if (!companionAvailable) return ConfirmTapResult.CompanionMissing
        val first = firstTapAtMs
        if (first == null || nowMs - first >= secondTapWindowMs) {
            firstTapAtMs = nowMs
            return ConfirmTapResult.FirstTap(secondTapWindowMs)
        }
        if (nowMs - first < minGapMs) return ConfirmTapResult.TooFast
        val confirmation = SyncMessage.VoiceConfirm(
            requestId = newRequestId(nowMs),
            confirms = requireNotNull(requestId),
            confirmationToken = offered,
        )
        clear()
        return ConfirmTapResult.Confirmed(confirmation)
    }

    /** The operator declined, or the screen goes away: the token is forgotten. */
    fun clear() {
        requestId = null
        token = null
        expiresAtMs = 0L
        firstTapAtMs = null
    }
}
