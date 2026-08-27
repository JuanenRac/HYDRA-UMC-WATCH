// =============================================================================
// HYDRA-UMC-WATCH - Main entry point activity: MainActivity.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// STARTING POINT ONLY - proves the Wear OS toolchain (Gradle/Kotlin/Jetpack
// Compose for Wear, same pattern as sibling repo HYDRA-UMC-ANDROID-CONTROL)
// actually builds and renders a real screen on a round/square watch face,
// not the safety dashboard/haptic E-STOP yet. That real work (WebSocket
// pairing with HYDRA-UMC-SERVER, wireless E-STOP, differentiated haptic
// alert patterns) is tracked in SONNET/HYDRA-UMC-WATCH/mejoras_futuras.txt.
package com.hydraumc.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HydraWatchApp()
        }
    }
}

/**
 * Root composable for the watch screen. Uses Wear Compose's own
 * [MaterialTheme]/[Scaffold]/[TimeText] (androidx.wear.compose.material),
 * not the handheld androidx.compose.material3 the phone app uses - Wear's
 * variant lays text out correctly on both round and square displays.
 */
@Composable
fun HydraWatchApp() {
    MaterialTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "HYDRA-UMC WATCH",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.status_placeholder),
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
