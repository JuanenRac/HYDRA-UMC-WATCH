// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/RelayListenerServiceTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.MessageEvent
import com.hydraumc.watch.haptics.AlertSeverity
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import com.hydraumc.watch.protocol.parseSyncMessage
import com.hydraumc.watch.protocol.toJson
import com.hydraumc.watch.transport.ACTION_WATCH_RELAY_MESSAGE
import com.hydraumc.watch.transport.EXTRA_WATCH_RELAY_MESSAGE
import com.hydraumc.watch.transport.LastRelayStore
import com.hydraumc.watch.transport.RelayInboxHolder
import com.hydraumc.watch.transport.SharedPrefsKeyValueStore
import com.hydraumc.watch.transport.WatchRelayListenerService
import com.hydraumc.watch.transport.WatchRelayPaths
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The listener service that receives what the phone sends, run on the JVM with no phone: a path or a payload it does not
 * serve is dropped, a status is kept and handed to a live screen, a message that finds no screen is held for the next one,
 * and the one-use confirmation token reaches the live screen and nothing that is kept.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RelayListenerServiceTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private fun event(path: String, data: String) = object : MessageEvent {
        override fun getRequestId(): Int = 1
        override fun getPath(): String = path
        override fun getData(): ByteArray = data.encodeToByteArray()
        override fun getSourceNodeId(): String = "phone"
    }

    private fun service() = Robolectric.buildService(WatchRelayListenerService::class.java).create().get()

    private fun broadcasts() = shadowOf(app).broadcastIntents.filter { it.action == ACTION_WATCH_RELAY_MESSAGE }

    @Before
    fun clean() {
        RelayInboxHolder.inbox.clear()
        RelayInboxHolder.inbox.screenAttached = false
        shadowOf(app).clearBroadcastIntents()
    }

    @After
    fun detach() {
        RelayInboxHolder.inbox.clear()
        RelayInboxHolder.inbox.screenAttached = false
    }

    private val status = SyncMessage.SystemStatus("Cell", "ok", WatchStatusLevel.NOMINAL, robotsRunning = 2, alarmActive = false)

    @Test
    fun aPathItDoesNotServeIsDropped() {
        service().onMessageReceived(event("/somewhere/else", status.toJson()))
        assertTrue(broadcasts().isEmpty())
        assertEquals(0, RelayInboxHolder.inbox.size())
    }

    @Test
    fun aPayloadThatIsNotAMessageIsDropped() {
        service().onMessageReceived(event(WatchRelayPaths.SYSTEM_STATUS, "not json at all"))
        assertTrue(broadcasts().isEmpty())
        assertEquals(0, RelayInboxHolder.inbox.size())
    }

    @Test
    fun aStatusIsKeptAndHandedToTheLiveScreen() {
        RelayInboxHolder.inbox.screenAttached = true
        service().onMessageReceived(event(WatchRelayPaths.SYSTEM_STATUS, status.toJson()))
        val sent = broadcasts()
        assertEquals(1, sent.size)
        assertEquals(status.toJson(), sent[0].getStringExtra(EXTRA_WATCH_RELAY_MESSAGE))
        assertEquals(app.packageName, sent[0].`package`)
        assertEquals(0, RelayInboxHolder.inbox.size())
        val kept = LastRelayStore(SharedPrefsKeyValueStore(app)).load()
        assertEquals(status, kept?.first)
    }

    @Test
    fun aStatusThatFindsNoScreenIsHeldForTheNextOneAndStillBroadcast() {
        service().onMessageReceived(event(WatchRelayPaths.SYSTEM_STATUS, status.toJson()))
        assertEquals(1, RelayInboxHolder.inbox.size())
        assertEquals(1, broadcasts().size)
        assertEquals(status.toJson(), RelayInboxHolder.inbox.drain().single().raw)
    }

    @Test
    fun theConfirmationTokenReachesTheLiveScreenAndNothingThatIsKept() {
        RelayInboxHolder.inbox.screenAttached = true
        val reply = SyncMessage.AssistantReply(
            requestId = "r1",
            text = "Stop the cell?",
            requiresConfirmation = true,
            confirmationToken = "one-use-token",
            confirmationValiditySeconds = 30,
        )
        service().onMessageReceived(event(WatchRelayPaths.ASSISTANT_REPLY, reply.toJson()))
        val live = broadcasts().single().getStringExtra(EXTRA_WATCH_RELAY_MESSAGE)!!
        assertEquals("one-use-token", (parseSyncMessage(live) as SyncMessage.AssistantReply).confirmationToken)
        val kept = LastRelayStore(SharedPrefsKeyValueStore(app)).load()?.first as SyncMessage.AssistantReply
        assertNull(kept.confirmationToken)
        assertNull(kept.confirmationValiditySeconds)
    }

    @Test
    fun anAlertIsHeldWhenThereIsNoScreenAndBroadcastWhenThereIsOne() {
        val alert = SyncMessage.Alert(AlertSeverity.CRITICAL, "E-STOP engaged")
        service().onMessageReceived(event(WatchRelayPaths.ALERT, alert.toJson()))
        assertEquals(1, RelayInboxHolder.inbox.size())
        assertTrue(broadcasts().isEmpty())

        RelayInboxHolder.inbox.clear()
        RelayInboxHolder.inbox.screenAttached = true
        service().onMessageReceived(event(WatchRelayPaths.ALERT, alert.toJson()))
        assertEquals(0, RelayInboxHolder.inbox.size())
        assertEquals(alert.toJson(), broadcasts().single().getStringExtra(EXTRA_WATCH_RELAY_MESSAGE))
    }

    @Test
    fun anAlertIsNotKeptAsTheLastStatus() {
        RelayInboxHolder.inbox.screenAttached = true
        val alert = SyncMessage.Alert(AlertSeverity.WARNING, "pressure low")
        service().onMessageReceived(event(WatchRelayPaths.ALERT, alert.toJson()))
        assertNull(LastRelayStore(SharedPrefsKeyValueStore(app)).load())
    }
}
