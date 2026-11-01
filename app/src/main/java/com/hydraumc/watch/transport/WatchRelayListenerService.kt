// =============================================================================
// HYDRA-UMC-WATCH - Paired-phone relay message listener
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.hydraumc.watch.protocol.parseSyncMessage

const val ACTION_WATCH_RELAY_MESSAGE = "com.hydraumc.watch.RELAY_MESSAGE"
const val EXTRA_WATCH_RELAY_MESSAGE = "message"

class WatchRelayListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        // Real routing decision (path filter -> parse -> message-type
        // filter) lives in WatchRelayRouting.decide() - pure, unit-tested
        // on a plain JVM - this class is now only the thin, untestable-
        // by-necessity Android glue that hands it real MessageEvent bytes
        // and broadcasts whatever it decides to keep.
        val raw = WatchRelayRouting.decide(event.path, event.data) ?: return
        // The last reply or status is kept on the watch, so a screen created later, a tile or a complication can show it.
        runCatching { LastRelayStore(SharedPrefsKeyValueStore(this)).save(raw) }
        // With the screen gone (the wrist was lowered and the activity destroyed) the broadcast below has no receiver:
        // the message is held for the next time the screen is created (see RelayInbox) and shown as a notification.
        val held = RelayInboxHolder.inbox.holdIfNoScreen(raw)
        if (held) {
            runCatching {
                val message = parseSyncMessage(raw)
                RelayNotifier(this).notifyIfNeeded(message, screenAttached = RelayInboxHolder.inbox.screenAttached)
            }
        }
        sendBroadcast(
            Intent(ACTION_WATCH_RELAY_MESSAGE)
                .setPackage(packageName)
                .putExtra(EXTRA_WATCH_RELAY_MESSAGE, raw),
        )
    }
}
