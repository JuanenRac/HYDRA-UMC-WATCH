// =============================================================================
// HYDRA-UMC-WATCH - Paired-phone relay message listener
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.hydraumc.watch.WatchConfig
import com.hydraumc.watch.complication.RobotsComplicationService
import com.hydraumc.watch.haptics.HapticAlertPlayer
import com.hydraumc.watch.protocol.SyncMessage
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
        val config = WatchConfig.load(this)
        val decided = WatchRelayRouting.decide(event.path, event.data, alertsEnabled = config.alertsEnabled) ?: return
        // The one-use confirmation token lives only in the broadcast to a live screen: what is stored or held has none.
        val raw = WatchRelayRouting.withoutConfirmationToken(decided)
        val message0 = runCatching { parseSyncMessage(raw) }.getOrNull()
        if (message0 is SyncMessage.Alert) {
            handleAlert(message0, raw)
            return
        }
        // The last reply or status is kept on the watch, so a screen created later, a tile or a complication can show it.
        runCatching { LastRelayStore(SharedPrefsKeyValueStore(this)).save(raw) }
        if (message0 is SyncMessage.SystemStatus) RobotsComplicationService.requestUpdate(this)
        // With the screen gone (the wrist was lowered and the activity destroyed) the broadcast below has no receiver:
        // the message is held for the next time the screen is created (see RelayInbox) and shown as a notification.
        val held = RelayInboxHolder.inbox.holdIfNoScreen(raw)
        if (held) {
            runCatching {
                val message = parseSyncMessage(raw)
                RelayNotifier(this).notifyIfNeeded(message, screenAttached = RelayInboxHolder.inbox.screenAttached)
            }
        }
        // a live screen gets the original (with the token, if any); everything kept above had it removed
        sendBroadcast(
            Intent(ACTION_WATCH_RELAY_MESSAGE)
                .setPackage(packageName)
                .putExtra(EXTRA_WATCH_RELAY_MESSAGE, if (RelayInboxHolder.inbox.screenAttached) decided else raw),
        )
    }

    /** An alert the phone pushed: a vibration by severity (the screen does it itself when it exists) and the notification. */
    private fun handleAlert(alert: SyncMessage.Alert, raw: String) {
        val screen = RelayInboxHolder.inbox.screenAttached
        val notifier = RelayNotifier(this)
        AlertNotifier(this).present(alert, screenAttached = screen, permissionGranted = notifier.permissionGranted())
        if (!screen) {
            runCatching { HapticAlertPlayer(applicationContext).play(alert.severity) }
            RelayInboxHolder.inbox.holdIfNoScreen(raw)
            return
        }
        sendBroadcast(
            Intent(ACTION_WATCH_RELAY_MESSAGE)
                .setPackage(packageName)
                .putExtra(EXTRA_WATCH_RELAY_MESSAGE, raw),
        )
    }
}
