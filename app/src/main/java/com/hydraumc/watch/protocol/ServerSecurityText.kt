// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/protocol/ServerSecurityText.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.R

/**
 * The string resource of the sentence for a switch of the server that is on its value that lowers the protection, or `null` for an id this
 * build does not know (a newer phone may list one: the screen shows the id as it is instead of dropping it). Pure: the screen resolves the
 * resource with its own language.
 */
fun securitySwitchStringRes(id: String): Int? = when (id) {
    "health.open" -> R.string.security_sw_health_open
    "connections.recordAddress" -> R.string.security_sw_connections_record_address
    "camera.open" -> R.string.security_sw_camera_open
    "websocket.allowQueryToken" -> R.string.security_sw_websocket_query_token
    "logging.redactSecrets" -> R.string.security_sw_logging_redaction
    "camera.credentialsViaEnv" -> R.string.security_sw_camera_credentials
    "install.newInstallTls" -> R.string.security_sw_install_tls
    else -> null
}

/** The level a status deserves on the wrist: attention as soon as one switch lowers the protection, nominal otherwise. */
fun securityLevel(status: SyncMessage.ServerSecurityStatus): WatchStatusLevel =
    if (status.loweredSwitches.isEmpty()) WatchStatusLevel.NOMINAL else WatchStatusLevel.ATTENTION
