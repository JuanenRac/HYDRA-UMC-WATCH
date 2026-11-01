// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/RelayText.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.content.Context
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.errorCodeToDetailStringRes
import com.hydraumc.watch.protocol.errorCodeToStringRes

/**
 * The words of a message in the language of the watch. A known error code is this watch's own text (the phone that sent
 * it has no access to the language of the watch); any other message keeps the text it came with.
 */
object RelayText {
    /** The text of an assistant reply. */
    fun reply(context: Context, message: SyncMessage.AssistantReply): String =
        errorCodeToStringRes(message.errorCode)?.let { context.getString(it) } ?: message.text

    /** The headline of a system status. */
    fun headline(context: Context, message: SyncMessage.SystemStatus): String =
        errorCodeToStringRes(message.errorCode)?.let { context.getString(it) } ?: message.headline

    /** The detail of a system status. */
    fun detail(context: Context, message: SyncMessage.SystemStatus): String =
        errorCodeToDetailStringRes(message.errorCode)?.let { context.getString(it) } ?: message.detail

    /** The line a status is shown as on the screen, in a notification and in a tile: `headline: detail`. */
    fun statusLine(context: Context, message: SyncMessage.SystemStatus): String =
        "${headline(context, message)}: ${detail(context, message)}"

    /** The one line that stands for a reply or a status, or null for any other message. */
    fun summary(context: Context, message: SyncMessage): String? = when (message) {
        is SyncMessage.AssistantReply -> reply(context, message)
        is SyncMessage.SystemStatus -> statusLine(context, message)
        else -> null
    }
}
