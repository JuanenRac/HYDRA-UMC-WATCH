// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/AlertPolicy.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import com.hydraumc.watch.haptics.AlertSeverity

/** How an alert that the phone forwards is presented on the wrist. */
enum class AlertPresentation {
    /** Only on the screen, with the vibration. */
    SCREEN_ONLY,

    /** A notification that goes away when the operator opens it. */
    NOTIFICATION,

    /** A notification that stays until the operator opens it (a critical alert must not be lost). */
    PERSISTENT_NOTIFICATION,
}

/**
 * What the watch does with a forwarded alert. A critical alert is a persistent notification even when the screen exists
 * (the operator may lower the wrist a second later); a warning is a notification only when there is no screen; an info
 * alert is never a notification. A notification needs the yes of the operator on Wear OS 4 and later. An alert is marked
 * as outdated once it is older than the limit. Pure Kotlin, checked on a plain JVM.
 */
object AlertPolicy {
    fun presentation(
        severity: AlertSeverity,
        screenAttached: Boolean,
        permissionGranted: Boolean,
        criticalPersistent: Boolean,
    ): AlertPresentation = when {
        !permissionGranted -> AlertPresentation.SCREEN_ONLY
        severity == AlertSeverity.CRITICAL ->
            if (criticalPersistent) AlertPresentation.PERSISTENT_NOTIFICATION else AlertPresentation.NOTIFICATION
        severity == AlertSeverity.WARNING && !screenAttached -> AlertPresentation.NOTIFICATION
        else -> AlertPresentation.SCREEN_ONLY
    }

    fun isOutdated(receivedAtMs: Long, nowMs: Long, staleAfterMs: Long): Boolean =
        receivedAtMs > nowMs || nowMs - receivedAtMs >= staleAfterMs
}
