// =============================================================================
// HYDRA-UMC-WATCH - Paired-phone relay message listener
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.parseSyncMessage

const val ACTION_WATCH_RELAY_MESSAGE = "com.hydraumc.watch.RELAY_MESSAGE"
const val EXTRA_WATCH_RELAY_MESSAGE = "message"

class WatchRelayListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path !in setOf(WatchRelayPaths.ASSISTANT_REPLY, WatchRelayPaths.SYSTEM_STATUS)) return
        val raw = event.data.decodeToString()
        val message = runCatching { parseSyncMessage(raw) }.getOrNull() ?: return
        if (message !is SyncMessage.AssistantReply && message !is SyncMessage.SystemStatus) return
        sendBroadcast(
            Intent(ACTION_WATCH_RELAY_MESSAGE)
                .setPackage(packageName)
                .putExtra(EXTRA_WATCH_RELAY_MESSAGE, raw),
        )
    }
}
