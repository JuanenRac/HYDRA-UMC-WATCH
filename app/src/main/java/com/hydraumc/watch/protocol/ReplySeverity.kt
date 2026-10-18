// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/protocol/ReplySeverity.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.haptics.AlertSeverity

/**
 * The haptic severity a relayed message deserves: its [WatchStatusLevel]
 * decides (a critical Server temperature must not feel like an ordinary
 * reply), and a known degraded condition ([errorCode]) is at least a WARNING.
 */
fun hapticSeverityFor(level: WatchStatusLevel, errorCode: String?): AlertSeverity {
    val fromLevel = when (level) {
        WatchStatusLevel.CRITICAL -> AlertSeverity.CRITICAL
        WatchStatusLevel.WARNING, WatchStatusLevel.OFFLINE -> AlertSeverity.WARNING
        WatchStatusLevel.ATTENTION, WatchStatusLevel.NOMINAL -> AlertSeverity.INFO
    }
    return if (errorCode != null && fromLevel == AlertSeverity.INFO) AlertSeverity.WARNING else fromLevel
}
