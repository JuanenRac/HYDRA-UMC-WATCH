// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/WatchRelayRouting.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.parseSyncMessage

/**
 * Real, pure routing decision for one incoming relay message - found in an
 * ecosystem-wide software-improvements audit: [WatchRelayListenerService]
 * (the one Android-framework-coupled class under `transport/`) used to
 * inline this decision (path filter -> parse -> message-type filter)
 * directly, unlike [RelayRetryPolicy]/[LastKnownStateCache], which this
 * project's own README explains were deliberately extracted into pure,
 * JVM-testable classes for exactly this reason. Extracting the routing
 * decision the same way closes the one untested piece of message handling.
 */
object WatchRelayRouting {
    /**
     * Returns the raw message string to broadcast locally, or `null` if
     * this message should be dropped: a path this relay was never meant
     * to forward, a payload that fails to parse under the real protocol
     * contract ([parseSyncMessage]'s own validation), or a real
     * [SyncMessage] type other than [SyncMessage.AssistantReply]/
     * [SyncMessage.SystemStatus] - the only two this relay ever forwards
     * to the rest of the app, even if a future protocol version starts
     * routing other message types to these same two paths.
     */
    fun decide(path: String, rawBytes: ByteArray): String? {
        if (path !in setOf(WatchRelayPaths.ASSISTANT_REPLY, WatchRelayPaths.SYSTEM_STATUS)) return null
        val raw = rawBytes.decodeToString()
        val message = runCatching { parseSyncMessage(raw) }.getOrNull() ?: return null
        if (message !is SyncMessage.AssistantReply && message !is SyncMessage.SystemStatus) return null
        return raw
    }
}
