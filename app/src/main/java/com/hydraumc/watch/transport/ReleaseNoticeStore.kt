// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/ReleaseNoticeStore.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

/**
 * Keeps the number of the newest release the phone told the watch about, with the instant it arrived, so a screen created later can show the line.
 * It lives apart from [LastRelayStore] (a notice must not replace the last reply or status). A notice older than [maxAgeMs] is dropped, and an
 * older version never replaces a newer one. Pure Kotlin, checked on a plain JVM.
 */
class ReleaseNoticeStore(
    private val store: KeyValueStore,
    private val maxAgeMs: Long = 3 * 24 * 60 * 60_000L,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init {
        require(maxAgeMs > 0) { "maxAgeMs must be positive" }
    }

    /** Remembers [version] unless a newer or the same one is already kept (the same version does not renew the instant, so the notice still expires). Returns whether it was kept. */
    fun save(version: String): Boolean {
        val kept = load()
        if (kept != null && compare(version, kept) <= 0) return false
        store.put(KEY, version, now())
        return true
    }

    /** The version to show, or null when there is none or it is too old. */
    fun load(): String? {
        val version = store.getString(KEY) ?: return null
        val at = store.getLong(KEY) ?: return null
        if (now() - at >= maxAgeMs || at > now()) {
            clear()
            return null
        }
        return version
    }

    fun clear() = store.remove(KEY)

    companion object {
        const val STORE_NAME = "release_notice"
        private const val KEY = "version"

        /** Compares two numbers of release group by group (a missing group counts as 0). */
        fun compare(a: String, b: String): Int {
            val x = a.split('.').map { it.toIntOrNull() ?: 0 }
            val y = b.split('.').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(x.size, y.size)) {
                val c = (x.getOrElse(i) { 0 }).compareTo(y.getOrElse(i) { 0 })
                if (c != 0) return c
            }
            return 0
        }
    }
}
