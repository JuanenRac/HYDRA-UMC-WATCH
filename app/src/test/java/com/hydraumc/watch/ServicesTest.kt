// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/ServicesTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.app.Application
import android.app.Service
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.tiles.RequestBuilders
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import com.hydraumc.watch.protocol.toJson
import com.hydraumc.watch.tile.RobotsTileService
import com.hydraumc.watch.transport.LastRelayStore
import com.hydraumc.watch.transport.PresenceHeartbeatService
import com.hydraumc.watch.transport.SharedPrefsKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The presence service and the tile, run on the JVM with no watch: presence stays off unless the file of settings says so. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ServicesTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test
    fun thePresenceServiceDoesNothingWhileThePresenceIsOffInTheSettings() {
        val service = Robolectric.buildService(PresenceHeartbeatService::class.java).create().get()
        val result = service.onStartCommand(Intent(app, PresenceHeartbeatService::class.java), 0, 1)
        assertEquals(Service.START_NOT_STICKY, result)
        assertFalse(PresenceHeartbeatService.running)
        assertEquals(null, service.onBind(Intent()))
    }

    @Test
    fun theStopActionEndsThePresenceAndTheServiceDoesNotRestart() {
        val controller = Robolectric.buildService(PresenceHeartbeatService::class.java).create()
        val service = controller.get()
        val stop = Intent(app, PresenceHeartbeatService::class.java).setAction(PresenceHeartbeatService.ACTION_STOP)
        assertEquals(Service.START_NOT_STICKY, service.onStartCommand(stop, 0, 1))
        assertFalse(PresenceHeartbeatService.running)
        controller.destroy()
        assertFalse(PresenceHeartbeatService.running)
    }

    @Test
    fun stoppingFromTheScreenAsksTheServiceForTheStopAction() {
        PresenceHeartbeatService.stop(app)
        val started = shadowOf(app).nextStartedService
        assertNotNull(started)
        assertEquals(PresenceHeartbeatService.ACTION_STOP, started.action)
    }

    private fun tileTexts(): List<String> {
        val service = Robolectric.buildService(RobotsTileService::class.java).create().get()
        val tile = service.onTileRequest(RequestBuilders.TileRequest.Builder().build()).get()
        assertEquals("1", tile.resourcesVersion)
        assertEquals(60_000L, tile.freshnessIntervalMillis)
        val root = tile.tileTimeline!!.timelineEntries[0].layout!!.root as LayoutElementBuilders.Column
        return root.contents.map { (it as LayoutElementBuilders.Text).text!!.value }
    }

    @Test
    fun theTileSaysUnknownBeforeAnyStatusArrives() {
        val texts = tileTexts()
        assertEquals(3, texts.size)
        assertEquals(app.getString(R.string.complication_title), texts[0])
        assertEquals("-", texts[1])
        assertEquals(app.getString(R.string.complication_unknown), texts[2])
    }

    @Test
    fun theTileShowsTheRobotsRunningOfTheLastStatus() {
        val status = SyncMessage.SystemStatus("Cell", "ok", WatchStatusLevel.NOMINAL, robotsRunning = 3, alarmActive = false)
        LastRelayStore(SharedPrefsKeyValueStore(app)).save(status.toJson())
        val texts = tileTexts()
        assertEquals("3", texts[1])
        assertEquals(app.getString(R.string.complication_running, 3), texts[2])
    }

    @Test
    fun theTileShowsTheAlarmBeforeTheCount() {
        val status = SyncMessage.SystemStatus("Cell", "alarm", WatchStatusLevel.CRITICAL, robotsRunning = 2, alarmActive = true)
        LastRelayStore(SharedPrefsKeyValueStore(app)).save(status.toJson())
        val texts = tileTexts()
        assertEquals("!", texts[1])
        assertEquals(app.getString(R.string.complication_alarm), texts[2])
    }

    @Test
    fun theTileResourcesCarryTheirVersion() {
        val service = Robolectric.buildService(RobotsTileService::class.java).create().get()
        val resources = service.onTileResourcesRequest(RequestBuilders.ResourcesRequest.Builder().build()).get()
        assertEquals("1", resources.version)
    }
}
