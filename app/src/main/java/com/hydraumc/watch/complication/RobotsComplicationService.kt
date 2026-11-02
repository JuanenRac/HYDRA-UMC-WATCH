// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/complication/RobotsComplicationService.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.hydraumc.watch.MainActivity
import com.hydraumc.watch.R
import com.hydraumc.watch.WatchConfig
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.transport.LastRelayStore
import com.hydraumc.watch.transport.SharedPrefsKeyValueStore

/**
 * Watch-face complication with the number of robots running, or "!" while an alarm is active. It reads the last status the
 * phone sent (kept by [LastRelayStore]); what it shows is [ComplicationSummary]'s: no invented count, and a state older
 * than `complication.stale_after_s` is shown as "?". Read only, it commands nothing. Not tried on a real watch.
 */
class RobotsComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return build(ComplicationSummary.Running(2), tap = null)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return NoDataComplicationData()
        val config = WatchConfig.load(this)
        if (!config.complicationEnabled) return NoDataComplicationData()
        val kept = runCatching { LastRelayStore(SharedPrefsKeyValueStore(applicationContext)).load() }.getOrNull()
        val status = kept?.first as? SyncMessage.SystemStatus
        val ageMs = kept?.let { System.currentTimeMillis() - it.second }
        val tap = PendingIntent.getActivity(
            this,
            4,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return build(ComplicationSummary.from(status, ageMs, config.complicationStaleAfterMs), tap)
    }

    private fun build(summary: ComplicationSummary, tap: PendingIntent?): ComplicationData {
        val description = when (summary) {
            ComplicationSummary.Unknown -> getString(R.string.complication_unknown)
            ComplicationSummary.Outdated -> getString(R.string.complication_outdated)
            ComplicationSummary.Alarm -> getString(R.string.complication_alarm)
            is ComplicationSummary.Running -> getString(R.string.complication_running, summary.count)
        }
        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(summary.shortText()).build(),
            PlainComplicationText.Builder(description).build(),
        )
            .setTitle(PlainComplicationText.Builder(getString(R.string.complication_title)).build())
            .setTapAction(tap)
            .build()
    }

    companion object {
        /** Asks the watch face to read the kept state again (called when a new status arrives). */
        fun requestUpdate(context: Context) {
            runCatching {
                ComplicationDataSourceUpdateRequester
                    .create(context, ComponentName(context, RobotsComplicationService::class.java))
                    .requestUpdateAll()
            }
        }
    }
}
