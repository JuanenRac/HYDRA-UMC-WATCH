// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/RelayTextAndStoreTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.content.Context
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.transport.SharedPrefsKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The words of a relayed message in the language of the watch, and the preferences the last message is kept in. They need an Android
 * context (Robolectric), the rest of the transport is checked on a plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RelayTextAndStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun reply(code: String?) = SyncMessage.AssistantReply(requestId = "r1", text = "free text of the phone", errorCode = code)

    private fun status(code: String?) = SyncMessage.SystemStatus(headline = "phone headline", detail = "phone detail", errorCode = code)

    @Test
    fun `a known error code of a reply is the own text of the watch`() {
        assertEquals(context.getString(R.string.voice_error_connection_unavailable), RelayText.reply(context, reply("connection_unavailable")))
    }

    @Test
    fun `a reply with no code or an unknown code keeps the text it came with`() {
        assertEquals("free text of the phone", RelayText.reply(context, reply(null)))
        assertEquals("free text of the phone", RelayText.reply(context, reply("a_code_of_a_newer_phone")))
    }

    @Test
    fun `an offline status is told in the own words of the watch, headline and detail`() {
        val offline = status("offline")
        assertEquals(context.getString(R.string.status_error_offline_headline), RelayText.headline(context, offline))
        assertEquals(context.getString(R.string.status_error_offline_detail), RelayText.detail(context, offline))
        assertEquals(
            context.getString(R.string.status_error_offline_headline) + ": " + context.getString(R.string.status_error_offline_detail),
            RelayText.statusLine(context, offline),
        )
    }

    @Test
    fun `a status with no code or an unknown one keeps the headline and the detail of the phone`() {
        for (code in listOf(null, "something_new")) {
            val message = status(code)
            assertEquals("phone headline", RelayText.headline(context, message))
            assertEquals("phone detail", RelayText.detail(context, message))
            assertEquals("phone headline: phone detail", RelayText.statusLine(context, message))
        }
    }

    @Test
    fun `a code that only has a headline keeps the detail of the phone`() {
        // connection_unavailable has a text for the headline of a status but none for its detail
        val message = status("connection_unavailable")
        assertEquals(context.getString(R.string.voice_error_connection_unavailable), RelayText.headline(context, message))
        assertEquals("phone detail", RelayText.detail(context, message))
    }

    @Test
    fun `the summary is one line for a reply or a status and nothing for any other message`() {
        assertEquals("free text of the phone", RelayText.summary(context, reply(null)))
        assertEquals("phone headline: phone detail", RelayText.summary(context, status(null)))
        assertNull(RelayText.summary(context, SyncMessage.ServerSecurityStatus(protocolVersion = 1)))
    }

    @Test
    fun `the preferences keep the text with its instant and give both back`() {
        val store = SharedPrefsKeyValueStore(context, "test_relay_a")
        assertNull(store.getString("k"))
        assertNull(store.getLong("k"))
        store.put("k", "text", 1234L)
        assertEquals("text", store.getString("k"))
        assertEquals(1234L, store.getLong("k"))
        store.put("k", "newer", 99L)
        assertEquals("newer", store.getString("k"))
        assertEquals(99L, store.getLong("k"))
    }

    @Test
    fun `removing a key removes its text and its instant and leaves the others`() {
        val store = SharedPrefsKeyValueStore(context, "test_relay_b")
        store.put("one", "1", 1L)
        store.put("two", "2", 2L)
        store.remove("one")
        assertNull(store.getString("one"))
        assertNull(store.getLong("one"))
        assertEquals("2", store.getString("two"))
        store.remove("never_there")
    }

    @Test
    fun `what was stored is read again by a new store of the same name and not by another name`() {
        SharedPrefsKeyValueStore(context, "test_relay_c").put("k", "kept", 7L)
        val again = SharedPrefsKeyValueStore(context, "test_relay_c")
        assertEquals("kept", again.getString("k"))
        assertEquals(7L, again.getLong("k"))
        val other = SharedPrefsKeyValueStore(context, "test_relay_d")
        assertNull(other.getString("k"))
        assertNotEquals(again.getString("k"), other.getString("k"))
    }
}
