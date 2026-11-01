// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/transport/SharedPrefsKeyValueStore.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.transport

import android.content.Context

/** [KeyValueStore] on the private preferences of the app: the text under the key, its time under `<key>.at`. */
class SharedPrefsKeyValueStore(context: Context, name: String = "last_relay") : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getLong(key: String): Long? = if (prefs.contains("$key.at")) prefs.getLong("$key.at", 0L) else null

    override fun put(key: String, text: String, number: Long) {
        prefs.edit().putString(key, text).putLong("$key.at", number).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).remove("$key.at").apply()
    }
}
