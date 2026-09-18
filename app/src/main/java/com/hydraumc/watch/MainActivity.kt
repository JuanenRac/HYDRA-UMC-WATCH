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
// alert patterns) is planned with the production integration.
package com.hydraumc.watch

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.hydraumc.watch.haptics.AlertSeverity
import com.hydraumc.watch.haptics.HapticAlertPlayer
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.errorCodeToDetailStringRes
import com.hydraumc.watch.protocol.errorCodeToStringRes
import com.hydraumc.watch.protocol.parseSyncMessage
import com.hydraumc.watch.transport.ACTION_WATCH_RELAY_MESSAGE
import com.hydraumc.watch.transport.EXTRA_WATCH_RELAY_MESSAGE
import com.hydraumc.watch.transport.LastKnownStateCache
import com.hydraumc.watch.transport.WatchRelayTransport
import java.util.Locale

// A relayed status/reply older than this must not be shown as if it just
// arrived - real staleness handling (see LastKnownStateCache), the
// diagnosis notes' "no presentar una alerta caducada como orden
// vigente".
private const val RELAY_STATE_STALE_AFTER_MS = 5 * 60_000L

class MainActivity : ComponentActivity() {
    private var latestVoiceTranscript by mutableStateOf<String?>(null)
    private var voiceStatus by mutableStateOf<String?>(null)
    private var systemStatus by mutableStateOf<String?>(null)
    private var systemStatusStale by mutableStateOf(false)
    private var textToSpeech: TextToSpeech? = null
    private var textToSpeechReady = false
    private lateinit var relayTransport: WatchRelayTransport
    // Real Vibrator-backed player (see HapticAlertPlayer's own header) -
    // created here, not inside the composable below, so the non-Compose
    // relayReceiver/onResult callbacks can play a pattern directly on every
    // real event this app already surfaces: an AssistantReply/SystemStatus
    // actually arriving (INFO, or WARNING when it carries a real errorCode
    // such as "connection_unavailable"/"offline"), and a relay send that
    // fails because no paired phone is currently reachable (WARNING - the
    // same "connection lost" condition, just detected locally instead of
    // reported by the phone).
    private val hapticAlertPlayer by lazy { HapticAlertPlayer(applicationContext) }
    private val lastKnownStateCache = LastKnownStateCache(staleAfterMs = RELAY_STATE_STALE_AFTER_MS)
    private val staleCheckHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val staleCheckRunnable = object : Runnable {
        override fun run() {
            systemStatusStale = lastKnownStateCache.isStale()
            staleCheckHandler.postDelayed(this, 30_000L)
        }
    }

    private val relayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val raw = intent.getStringExtra(EXTRA_WATCH_RELAY_MESSAGE) ?: return
            when (val message = runCatching { parseSyncMessage(raw) }.getOrNull()) {
                is SyncMessage.AssistantReply -> {
                    lastKnownStateCache.update(message)
                    systemStatusStale = false
                    // H062: a real, known errorCode means this is this
                    // watch's OWN system-error text (never translated
                    // upstream, since the phone that generated it has no
                    // access to this watch's own locale) - resolved to a
                    // real localized string from this app's own 7-language
                    // strings.xml instead of message.text's own untranslated
                    // English fallback. A real AI reply (errorCode == null,
                    // or an unrecognized code from a newer phone build)
                    // keeps using message.text exactly as before - it is
                    // already correctly localized upstream.
                    val resolvedText = errorCodeToStringRes(message.errorCode)?.let { getString(it) } ?: message.text
                    voiceStatus = resolvedText
                    if (message.speak) speak(resolvedText)
                    // A real, distinct haptic per outcome: an errorCode
                    // (e.g. "connection_unavailable") is a real degraded
                    // condition, not an ordinary reply - WARNING's two-pulse
                    // pattern instead of INFO's single glanceable one.
                    hapticAlertPlayer.play(if (message.errorCode != null) AlertSeverity.WARNING else AlertSeverity.INFO)
                }
                is SyncMessage.SystemStatus -> {
                    lastKnownStateCache.update(message)
                    systemStatusStale = false
                    // H062: same mechanism as AssistantReply above, applied
                    // to this message's own headline+detail pair.
                    val headline = errorCodeToStringRes(message.errorCode)?.let { getString(it) } ?: message.headline
                    val detail = errorCodeToDetailStringRes(message.errorCode)?.let { getString(it) } ?: message.detail
                    systemStatus = "$headline: $detail"
                    if (message.speak) speak("$headline. $detail")
                    hapticAlertPlayer.play(if (message.errorCode != null) AlertSeverity.WARNING else AlertSeverity.INFO)
                }
                else -> Unit
            }
        }
    }

    private val recognizeSpeech = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val transcript = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (transcript.isNotEmpty()) {
            latestVoiceTranscript = transcript
            voiceStatus = getString(R.string.voice_sending)
            val turn = SyncMessage.VoiceTurn(
                requestId = "watch-${System.currentTimeMillis()}",
                transcript = transcript,
                locale = Locale.getDefault().toLanguageTag(),
            )
            relayTransport.sendVoiceTurn(turn) { result ->
                if (result.isFailure) runOnUiThread {
                    voiceStatus = getString(R.string.voice_phone_unavailable)
                    // Connection lost, detected locally (no reachable
                    // paired-phone node) rather than reported by the phone -
                    // same real WARNING pattern as an errorCode'd reply above.
                    hapticAlertPlayer.play(AlertSeverity.WARNING)
                }
            }
        }
    }

    private val requestMicrophonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchSpeechRecognition()
        } else {
            voiceStatus = getString(R.string.voice_permission_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        relayTransport = WatchRelayTransport(applicationContext)
        ContextCompat.registerReceiver(
            this,
            relayReceiver,
            IntentFilter(ACTION_WATCH_RELAY_MESSAGE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        textToSpeech = TextToSpeech(this) { status ->
            textToSpeechReady = status == TextToSpeech.SUCCESS
        }
        staleCheckHandler.post(staleCheckRunnable)
        setContent {
            HydraWatchApp(
                latestVoiceTranscript = latestVoiceTranscript,
                voiceStatus = voiceStatus,
                systemStatus = systemStatus,
                systemStatusStale = systemStatusStale,
                onStartVoiceRecognition = ::startVoiceRecognition,
                onRefreshSystemStatus = ::refreshSystemStatus,
            )
        }
    }

    private fun startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        launchSpeechRecognition()
    }

    private fun launchSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        if (intent.resolveActivity(packageManager) == null) {
            voiceStatus = getString(R.string.voice_unavailable)
            return
        }
        voiceStatus = getString(R.string.voice_listening)
        recognizeSpeech.launch(intent)
    }

    private fun refreshSystemStatus() {
        systemStatus = getString(R.string.status_loading)
        relayTransport.requestSystemStatus { result ->
            if (result.isFailure) runOnUiThread {
                systemStatus = getString(R.string.status_phone_unavailable)
                hapticAlertPlayer.play(AlertSeverity.WARNING)
            }
        }
    }

    private fun speak(text: String) {
        if (textToSpeechReady) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hydra-umc-watch-status")
        }
    }

    override fun onDestroy() {
        unregisterReceiver(relayReceiver)
        staleCheckHandler.removeCallbacks(staleCheckRunnable)
        // WATCH-01: a deferred retry (sendVoiceTurn/requestSystemStatus
        // scheduled itself again on a delay) used to keep firing after
        // this screen was gone, with only an attempt counter - never a
        // real lifecycle - bounding it.
        relayTransport.cancelPendingRetries()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}

/**
 * Root composable for the watch screen. Uses Wear Compose's own
 * [MaterialTheme]/[Scaffold]/[TimeText] (androidx.wear.compose.material),
 * not the handheld androidx.compose.material3 the phone app uses - Wear's
 * variant lays text out correctly on both round and square displays.
 */
@Composable
fun HydraWatchApp(
    latestVoiceTranscript: String? = null,
    voiceStatus: String? = null,
    systemStatus: String? = null,
    systemStatusStale: Boolean = false,
    onStartVoiceRecognition: () -> Unit = {},
    onRefreshSystemStatus: () -> Unit = {},
) {
    val context = LocalContext.current
    val hapticAlertPlayer = remember(context) { HapticAlertPlayer(context.applicationContext) }
    MaterialTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Text(
                        text = "HYDRA-UMC WATCH",
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center,
                    )
                }
                item {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center,
                    )
                }
                item {
                    Text(
                        text = systemStatus ?: stringResource(R.string.status_placeholder),
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                    )
                }
                if (systemStatus != null && systemStatusStale) {
                    item {
                        // A real last-known reading old enough that it must
                        // not be mistaken for a fresh one - see
                        // LastKnownStateCache.isStale().
                        Text(
                            text = stringResource(R.string.status_stale),
                            style = MaterialTheme.typography.caption2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item {
                    Button(onClick = onStartVoiceRecognition) {
                        Text(text = stringResource(R.string.voice_start))
                    }
                }
                item {
                    Button(onClick = onRefreshSystemStatus) {
                        Text(text = stringResource(R.string.status_refresh))
                    }
                }
                voiceStatus?.let { status ->
                    item {
                        Text(text = status, style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
                    }
                }
                latestVoiceTranscript?.let { transcript ->
                    item {
                        Text(
                            text = stringResource(R.string.voice_last_request, transcript),
                            style = MaterialTheme.typography.caption2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item {
                    // Informational local waveform only: this neither
                    // connects to a server nor sends an E-STOP/robot command.
                    Button(onClick = { hapticAlertPlayer.play(AlertSeverity.INFO) }) {
                        Text(text = stringResource(R.string.test_haptic_alert))
                    }
                }
            }
        }
    }
}
