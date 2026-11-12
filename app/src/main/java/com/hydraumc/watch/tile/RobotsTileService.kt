// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/tile/RobotsTileService.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.hydraumc.watch.MainActivity
import com.hydraumc.watch.R
import com.hydraumc.watch.WatchConfig
import com.hydraumc.watch.complication.ComplicationSummary
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.transport.LastRelayStore
import com.hydraumc.watch.transport.SharedPrefsKeyValueStore

/**
 * Tile with the last state the phone sent (kept by [LastRelayStore]), worded exactly like the complication: the number of
 * robots running, "!" while an alarm is active, "?" once the state is older than `complication.stale_after_s`, "-" when
 * the phone has not said. It never invents a count, commands nothing, and a tap only opens the app. Off with
 * `tile.enabled = false`. Not tried on a real watch.
 */
class RobotsTileService : TileService() {
    public override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            completer.set(buildTile())
            "robots tile"
        }

    public override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { completer ->
            completer.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())
            "robots tile resources"
        }

    private fun buildTile(): TileBuilders.Tile {
        val config = WatchConfig.load(this)
        val summary = if (config.tileEnabled) currentSummary(config) else ComplicationSummary.Unknown
        val description = when (summary) {
            ComplicationSummary.Unknown -> getString(R.string.complication_unknown)
            ComplicationSummary.Outdated -> getString(R.string.complication_outdated)
            ComplicationSummary.Alarm -> getString(R.string.complication_alarm)
            is ComplicationSummary.Running -> getString(R.string.complication_running, summary.count)
        }
        val open = ModifiersBuilders.Clickable.Builder()
            .setId("open")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build(),
                    )
                    .build(),
            )
            .build()
        val column = LayoutElementBuilders.Column.Builder()
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(open).build())
            .addContent(LayoutElementBuilders.Text.Builder().setText(getString(R.string.complication_title)).build())
            .addContent(LayoutElementBuilders.Text.Builder().setText(summary.shortText()).build())
            .addContent(LayoutElementBuilders.Text.Builder().setText(description).build())
            .build()
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_MS)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(column))
            .build()
    }

    private fun currentSummary(config: WatchConfig): ComplicationSummary {
        val kept = runCatching { LastRelayStore(SharedPrefsKeyValueStore(applicationContext)).load() }.getOrNull()
        val status = kept?.first as? SyncMessage.SystemStatus
        val ageMs = kept?.let { System.currentTimeMillis() - it.second }
        return ComplicationSummary.from(status, ageMs, config.complicationStaleAfterMs)
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val FRESHNESS_MS = 60_000L

        /** Asks the watch to draw the tile again (called when a new status arrives). */
        fun requestUpdate(context: Context) {
            runCatching { TileService.getUpdater(context).requestUpdate(RobotsTileService::class.java) }
        }
    }
}
