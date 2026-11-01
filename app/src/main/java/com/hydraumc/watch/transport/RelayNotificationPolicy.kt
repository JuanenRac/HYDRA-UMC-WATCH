// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/RelayNotificationPolicy.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel

/**
 * When a message that arrives with no screen to show it is worth a notification. The answer to a question the operator
 * asked always is; a status only when it is not nominal, so a routine status every few minutes does not fill the
 * notification shade. Nothing is shown when there is a screen (it shows the message itself) or when the operator has not
 * allowed notifications. Pure Kotlin, so it is checked on a plain JVM.
 */
object RelayNotificationPolicy {
    fun shouldNotify(message: SyncMessage, screenAttached: Boolean, permissionGranted: Boolean): Boolean {
        if (screenAttached || !permissionGranted) return false
        return when (message) {
            is SyncMessage.AssistantReply -> true
            is SyncMessage.SystemStatus -> message.level != WatchStatusLevel.NOMINAL
            else -> false
        }
    }
}
