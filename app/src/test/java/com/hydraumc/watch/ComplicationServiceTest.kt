// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/ComplicationServiceTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.hydraumc.watch.complication.RobotsComplicationService
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import com.hydraumc.watch.protocol.toJson
import com.hydraumc.watch.transport.LastRelayStore
import com.hydraumc.watch.transport.SharedPrefsKeyValueStore
import kotlinx.coroutines.runBlocking
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The watch-face complication, run on the JVM with no watch: what it says before any status, with a status, with an alarm and for a type it does not serve. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComplicationServiceTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private fun service() = Robolectric.buildService(RobotsComplicationService::class.java).create().get()

    private fun request(type: ComplicationType) = ComplicationRequest(1, type, false)

    private fun shortText(service: RobotsComplicationService): ShortTextComplicationData {
        val data = runBlocking { service.onComplicationRequest(request(ComplicationType.SHORT_TEXT)) }
        return data as ShortTextComplicationData
    }

    private fun textOf(data: ShortTextComplicationData): String = data.text.getTextAt(app.resources, Instant.now()).toString()

    private fun descriptionOf(data: ShortTextComplicationData): String =
        data.contentDescription!!.getTextAt(app.resources, Instant.now()).toString()

    @Test
    fun thePreviewShowsTwoRobotsRunningAndOnlyForTheShortText() {
        val service = service()
        val preview = service.getPreviewData(ComplicationType.SHORT_TEXT) as ShortTextComplicationData
        assertEquals("2", textOf(preview))
        assertEquals(app.getString(R.string.complication_running, 2), descriptionOf(preview))
        assertNull(service.getPreviewData(ComplicationType.LONG_TEXT))
    }

    @Test
    fun aTypeItDoesNotServeGetsNoData() {
        val data = runBlocking { service().onComplicationRequest(request(ComplicationType.RANGED_VALUE)) }
        assertTrue(data is NoDataComplicationData)
    }

    @Test
    fun beforeAnyStatusTheComplicationSaysUnknown() {
        val data = shortText(service())
        assertEquals("-", textOf(data))
        assertEquals(app.getString(R.string.complication_unknown), descriptionOf(data))
        assertNotNull(data.tapAction)
    }

    @Test
    fun withAFreshStatusItShowsTheRobotsRunning() {
        val status = SyncMessage.SystemStatus("Cell", "ok", WatchStatusLevel.NOMINAL, robotsRunning = 4, alarmActive = false)
        LastRelayStore(SharedPrefsKeyValueStore(app)).save(status.toJson())
        val data = shortText(service())
        assertEquals("4", textOf(data))
        assertEquals(app.getString(R.string.complication_running, 4), descriptionOf(data))
    }

    @Test
    fun anActiveAlarmIsShownBeforeTheCount() {
        val status = SyncMessage.SystemStatus("Cell", "alarm", WatchStatusLevel.CRITICAL, robotsRunning = 4, alarmActive = true)
        LastRelayStore(SharedPrefsKeyValueStore(app)).save(status.toJson())
        val data = shortText(service())
        assertEquals("!", textOf(data))
        assertEquals(app.getString(R.string.complication_alarm), descriptionOf(data))
    }

    @Test
    fun askingTheWatchFaceToUpdateNeverThrowsWithoutAWatchFace() {
        RobotsComplicationService.requestUpdate(app)
    }
}
