// =============================================================================
// HYDRA-UMC-WATCH - Main entry point activity: MainActivity.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// The watch screen: glanceable system status, tap-to-talk voice turns relayed
// through the paired phone, local text-to-speech and level-driven haptics. The
// wireless E-STOP and a direct WebSocket pairing with HYDRA-UMC-SERVER are
// still planned; nothing on this screen can command a robot.
package com.hydraumc.watch

import android.annotation.SuppressLint
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.hydraumc.watch.protocol.hapticSeverityFor
import com.hydraumc.watch.protocol.canOtaFinished
import com.hydraumc.watch.protocol.canOtaPhaseStringRes
import com.hydraumc.watch.protocol.parseSyncMessage
import com.hydraumc.watch.protocol.securitySwitchStringRes
import com.hydraumc.watch.transport.ACTION_WATCH_RELAY_MESSAGE
import com.hydraumc.watch.transport.EXTRA_WATCH_RELAY_MESSAGE
import com.hydraumc.watch.transport.AlertNotifier
import com.hydraumc.watch.transport.AlertPolicy
import com.hydraumc.watch.transport.ConfirmTapResult
import com.hydraumc.watch.transport.PresenceHeartbeatService
import com.hydraumc.watch.transport.TwoStepConfirmation
import com.hydraumc.watch.transport.CriticalActionGate
import com.hydraumc.watch.transport.LastKnownStateCache
import com.hydraumc.watch.transport.LastRelayStore
import com.hydraumc.watch.transport.RelayNotifier
import com.hydraumc.watch.transport.SharedPrefsKeyValueStore
import com.hydraumc.watch.transport.PendingVoiceRequest
import com.hydraumc.watch.transport.RelayInboxHolder
import com.hydraumc.watch.transport.WatchRelayTransport
import java.util.Locale

// A relayed status/reply older than this must not be shown as if it just
// arrived - real staleness handling (see LastKnownStateCache), the
// diagnosis notes' "no presentar una alerta caducada como orden
// vigente".
private const val RELAY_STATE_STALE_AFTER_MS = 5 * 60_000L
private const val STALE_CHECK_PERIOD_MS = 5_000L

/** How long the line of a flash cycle that ended stays on the screen, in milliseconds. */
private const val CANOTA_LINE_SHOWN_AFTER_END_MS = 120_000L

class MainActivity : ComponentActivity() {
    private var latestVoiceTranscript by mutableStateOf<String?>(null)
    private var voiceStatus by mutableStateOf<String?>(null)
    private var systemStatus by mutableStateOf<String?>(null)
    private var systemStatusStale by mutableStateOf(false)
    private var securityStatus by mutableStateOf<String?>(null)
    /** The line of the flash cycle the phone reports (phase and percent); null when there is none or it ended a while ago. */
    private var canOtaLine by mutableStateOf<String?>(null)
    private val canOtaClear = Runnable { canOtaLine = null }
    private var companionAvailable by mutableStateOf(false)
    // Low-power screen of the watch (the wrist is down): only the last state is drawn, in a quiet way.
    private var ambient by mutableStateOf(false)

    /**
     * Follows the ambient mode of the watch. The library that provides it is part of the watch system image, not of the
     * app, so on a device without it (or in a test on a plain JVM) the screen simply never enters ambient mode.
     */
    private fun attachAmbientMode() {
        try {
            val observer = AmbientLifecycleObserver(
                this,
                object : AmbientLifecycleObserver.AmbientLifecycleCallback {
                    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                        ambient = true
                    }

                    override fun onExitAmbient() {
                        ambient = false
                    }
                },
            )
            lifecycle.addObserver(observer)
        } catch (_: Throwable) {
            ambient = false
        }
    }
    private val config by lazy { WatchConfig.load(applicationContext) }
    // The last alert the phone pushed, and whether it is old enough to be taken for news no longer.
    private var alertText by mutableStateOf<String?>(null)
    private var alertOutdated by mutableStateOf(false)
    private var alertReceivedAtMs = 0L
    // Two-step confirmation of a spoken order (off unless confirm.enabled): the token lives only in this object.
    private val twoStep by lazy {
        TwoStepConfirmation(
            enabled = config.confirmEnabled,
            secondTapWindowMs = config.confirmSecondTapWindowMs,
            minGapMs = config.confirmMinGapMs,
            maxValidityMs = config.confirmMaxValidityMs,
        )
    }
    private var confirmOffered by mutableStateOf(false)
    private var confirmArmed by mutableStateOf(false)
    private var presenceOn by mutableStateOf(false)
    private val lastRelayStore by lazy { LastRelayStore(SharedPrefsKeyValueStore(applicationContext)) }
    private var textToSpeech: TextToSpeech? = null
    @Volatile private var textToSpeechReady = false
    private var pendingSpeech: String? = null
    private val pendingVoiceRequest = PendingVoiceRequest()
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
            companionAvailable = CriticalActionGate.isAvailable(lastKnownStateCache)
            if (alertText != null) alertOutdated = AlertPolicy.isOutdated(alertReceivedAtMs, System.currentTimeMillis(), config.alertsStaleAfterMs)
            if (confirmOffered && twoStep.remainingMs(System.currentTimeMillis()) == 0L) {
                // the validity ran out: the token is forgotten and the order has to be spoken again
                twoStep.clear()
                confirmOffered = false
                confirmArmed = false
                voiceStatus = getString(R.string.confirm_expired)
                hapticAlertPlayer.play(AlertSeverity.WARNING)
            }
            confirmArmed = confirmOffered && twoStep.isArmed(System.currentTimeMillis())
            presenceOn = PresenceHeartbeatService.running
            if (pendingVoiceRequest.isExpired(System.currentTimeMillis())) {
                // nobody answered: say so instead of staying on "sending"
                pendingVoiceRequest.clear()
                voiceStatus = getString(R.string.voice_phone_unavailable)
                hapticAlertPlayer.play(AlertSeverity.WARNING)
            }
            staleCheckHandler.postDelayed(this, STALE_CHECK_PERIOD_MS)
        }
    }

    private val relayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val raw = intent.getStringExtra(EXTRA_WATCH_RELAY_MESSAGE) ?: return
            handleRelayMessage(raw, held = false)
        }
    }

    /**
     * Shows one relay message. [held] is true for a message the listener service kept while this screen did not exist
     * (see RelayInbox): it is shown, but not spoken and without a haptic - it is not news any more - and an answer is
     * shown even though the question it answers was asked before this screen was created.
     */
    private fun handleRelayMessage(raw: String, held: Boolean, receivedAtMs: Long = System.currentTimeMillis()) {
            when (val message = runCatching { parseSyncMessage(raw) }.getOrNull()) {
                is SyncMessage.AssistantReply -> {
                    // a late answer to an older question is not shown as the
                    // answer to the latest one
                    if (!held && !pendingVoiceRequest.accept(message.requestId)) return
                    lastKnownStateCache.update(message)
                    systemStatusStale = false
                    companionAvailable = true
                    // A known errorCode is this watch's OWN system-error text (the phone has no access to the language of
                    // the watch); a real AI reply keeps its own text, already localized upstream.
                    val resolvedText = RelayText.reply(this, message)
                    voiceStatus = resolvedText
                    if (held) return
                    // a reply that asks for confirmation and brings a token is offered for the two taps (never a held one)
                    confirmOffered = twoStep.offer(message, System.currentTimeMillis())
                    confirmArmed = false
                    if (message.requiresConfirmation && !confirmOffered && config.confirmEnabled) {
                        voiceStatus = resolvedText + "\n" + getString(R.string.confirm_repeat_order)
                    }
                    if (message.speak) speak(resolvedText)
                    // A real, distinct haptic per outcome: an errorCode
                    // (e.g. "connection_unavailable") is a real degraded
                    // condition, not an ordinary reply - WARNING's two-pulse
                    // pattern instead of INFO's single glanceable one.
                    hapticAlertPlayer.play(hapticSeverityFor(message.level, message.errorCode))
                }
                is SyncMessage.SystemStatus -> {
                    lastKnownStateCache.update(message)
                    systemStatusStale = false
                    companionAvailable = true
                    val headline = RelayText.headline(this, message)
                    val detail = RelayText.detail(this, message)
                    systemStatus = "$headline: $detail"
                    if (held) return
                    if (message.speak) speak("$headline. $detail")
                    hapticAlertPlayer.play(hapticSeverityFor(message.level, message.errorCode))
                }
                is SyncMessage.Alert -> {
                    alertText = getString(
                        R.string.alert_line,
                        getString(
                            when (message.severity) {
                                AlertSeverity.CRITICAL -> R.string.alert_critical
                                AlertSeverity.WARNING -> R.string.alert_warning
                                AlertSeverity.INFO -> R.string.alert_info
                            },
                        ),
                        message.message,
                    )
                    alertReceivedAtMs = receivedAtMs
                    alertOutdated = AlertPolicy.isOutdated(receivedAtMs, System.currentTimeMillis(), config.alertsStaleAfterMs)
                    // a held alert is shown, but it is not news any more: no vibration (the service played it when it arrived)
                    if (!held && !alertOutdated) hapticAlertPlayer.play(message.severity)
                }
                is SyncMessage.CanOtaProgressStatus -> {
                    // read only and silent: a line on the screen, no speech, no vibration; held messages never reach here (the service does not hold it)
                    val phaseText = canOtaPhaseStringRes(message.phase)?.let { getString(it) } ?: message.phase
                    canOtaLine = message.percent?.let { getString(R.string.canota_line_percent, phaseText, it) } ?: getString(R.string.canota_line, phaseText)
                    window.decorView.removeCallbacks(canOtaClear)
                    if (canOtaFinished(message.phase)) window.decorView.postDelayed(canOtaClear, CANOTA_LINE_SHOWN_AFTER_END_MS)
                }
                is SyncMessage.ServerSecurityStatus -> {
                    // read only: it is shown, never acted on, and it does not speak or vibrate (it is a glance, not an alert)
                    securityStatus = securityStatusText(message)
                }
                else -> Unit
            }
    }

    /** A tap on the confirm button: the first arms it, the second (not too soon, not too late) sends the confirmation once. */
    private fun onConfirmTap() {
        val now = System.currentTimeMillis()
        when (val result = twoStep.tap(now, CriticalActionGate.isAvailable(lastKnownStateCache))) {
            is ConfirmTapResult.Ignored -> Unit
            is ConfirmTapResult.Expired -> {
                confirmOffered = false
                confirmArmed = false
                voiceStatus = getString(R.string.confirm_expired)
                hapticAlertPlayer.play(AlertSeverity.WARNING)
            }
            is ConfirmTapResult.CompanionMissing -> voiceStatus = getString(R.string.companion_required)
            is ConfirmTapResult.FirstTap -> {
                confirmArmed = true
                voiceStatus = getString(R.string.confirm_tap_again, (result.windowMs / 1000L).toInt())
                hapticAlertPlayer.play(AlertSeverity.INFO)
            }
            is ConfirmTapResult.TooFast -> Unit
            is ConfirmTapResult.Confirmed -> {
                confirmOffered = false
                confirmArmed = false
                voiceStatus = getString(R.string.confirm_sent)
                pendingVoiceRequest.begin(result.confirmation.requestId, now)
                hapticAlertPlayer.play(AlertSeverity.WARNING)
                relayTransport.sendVoiceConfirm(result.confirmation) { sent ->
                    if (sent.isFailure) runOnUiThread {
                        pendingVoiceRequest.clear()
                        voiceStatus = getString(R.string.confirm_not_sent)
                        hapticAlertPlayer.play(AlertSeverity.WARNING)
                    }
                }
            }
        }
    }

    private fun onDeclineConfirmation() {
        twoStep.clear()
        confirmOffered = false
        confirmArmed = false
    }

    private fun onTogglePresence() {
        if (!config.presenceEnabled) return
        val turningOff = PresenceHeartbeatService.running
        if (turningOff) PresenceHeartbeatService.stop(this) else PresenceHeartbeatService.start(this)
        presenceOn = !turningOff
    }

    /** The lines of a security status in the language of the watch: what lowers the protection, then the counts. */
    private fun securityStatusText(status: SyncMessage.ServerSecurityStatus): String {
        val lines = mutableListOf(getString(R.string.security_title))
        if (status.loweredSwitches.isEmpty()) {
            lines += getString(R.string.security_all_protected)
        } else {
            lines += getString(R.string.security_lowered_header)
            status.loweredSwitches.forEach { id -> lines += "- " + (securitySwitchStringRes(id)?.let { getString(it) } ?: id) }
        }
        if (status.lockedSwitches > 0) lines += getString(R.string.security_locked, status.lockedSwitches)
        lines += getString(R.string.security_clients, status.activeClients, status.failedLogins)
        lines += getString(R.string.security_read_only)
        return lines.joinToString("\n")
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
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
            val requestId = "watch-${System.currentTimeMillis()}"
            pendingVoiceRequest.begin(requestId, System.currentTimeMillis())
            val turn = SyncMessage.VoiceTurn(
                requestId = requestId,
                transcript = transcript,
                locale = Locale.getDefault().toLanguageTag(),
            )
            relayTransport.sendVoiceTurn(turn) { result ->
                if (result.isFailure) runOnUiThread {
                    pendingVoiceRequest.clear()
                    voiceStatus = getString(R.string.voice_phone_unavailable)
                    // Connection lost, detected locally (no reachable
                    // paired-phone node) rather than reported by the phone -
                    // same real WARNING pattern as an errorCode'd reply above.
                    hapticAlertPlayer.play(AlertSeverity.WARNING)
                }
            }
        }
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
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
        window.decorView.filterTouchesWhenObscured = WatchConfig.load(this).filterObscuredTouches
        attachAmbientMode()
        // The notification of a message that arrived while this screen did not exist has done its job.
        NotificationManagerCompat.from(this).cancel(RelayNotifier.NOTIFICATION_ID)
        NotificationManagerCompat.from(this).cancel(AlertNotifier.NOTIFICATION_ID)
        presenceOn = PresenceHeartbeatService.running
        askForNotificationsOnce()
        restoreLastKnownState()
        // From here on the screen receives the broadcasts itself; what arrived while it did not exist is shown now.
        RelayInboxHolder.inbox.screenAttached = true
        ContextCompat.registerReceiver(
            this,
            relayReceiver,
            IntentFilter(ACTION_WATCH_RELAY_MESSAGE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        textToSpeech = TextToSpeech(this) { status ->
            textToSpeechReady = status == TextToSpeech.SUCCESS
            if (textToSpeechReady) runOnUiThread { pendingSpeech?.let { speak(it) } }
        }
        RelayInboxHolder.inbox.drain().forEach { handleRelayMessage(it.raw, held = true, receivedAtMs = it.receivedAtMs) }
        setContent {
            HydraWatchApp(
                latestVoiceTranscript = latestVoiceTranscript,
                voiceStatus = voiceStatus,
                systemStatus = systemStatus,
                systemStatusStale = systemStatusStale,
                securityStatus = securityStatus,
                canOtaLine = canOtaLine,
                companionAvailable = companionAvailable,
                ambient = ambient,
                alertText = alertText,
                alertOutdated = alertOutdated,
                confirmOffered = confirmOffered,
                confirmArmed = confirmArmed,
                presenceAvailable = config.presenceEnabled,
                presenceOn = presenceOn,
                onConfirmTap = ::onConfirmTap,
                onDeclineConfirmation = ::onDeclineConfirmation,
                onTogglePresence = ::onTogglePresence,
                onStartVoiceRecognition = ::startVoiceRecognition,
                onRefreshSystemStatus = ::refreshSystemStatus,
                onRefreshServerSecurity = ::refreshServerSecurity,
                onTestHaptic = { hapticAlertPlayer.play(AlertSeverity.INFO) },
            )
        }
    }

    /** Shows the last reply or status kept on the watch, marked as outdated by the same rule as a live one. */
    private fun restoreLastKnownState() {
        val (message, receivedAt) = runCatching { lastRelayStore.load() }.getOrNull() ?: return
        lastKnownStateCache.restore(message, receivedAt)
        when (message) {
            is SyncMessage.AssistantReply -> voiceStatus = RelayText.reply(this, message)
            is SyncMessage.SystemStatus -> systemStatus = RelayText.statusLine(this, message)
            else -> Unit
        }
        systemStatusStale = lastKnownStateCache.isStale()
        companionAvailable = CriticalActionGate.isAvailable(lastKnownStateCache)
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** On Wear OS 4 and later a notification needs the operator's yes; it is asked once, the first time the app opens. */
    private fun askForNotificationsOnce() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("asked_notifications", false)) return
        prefs.edit().putBoolean("asked_notifications", true).apply()
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
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

    private fun refreshServerSecurity() {
        securityStatus = getString(R.string.security_loading)
        relayTransport.requestServerSecurity { result ->
            if (result.isFailure) runOnUiThread {
                securityStatus = getString(R.string.status_phone_unavailable)
                hapticAlertPlayer.play(AlertSeverity.WARNING)
            }
        }
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
            pendingSpeech = null
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hydra-umc-watch-status")
        } else {
            // the engine is still starting: say it as soon as it is ready
            pendingSpeech = text
        }
    }

    // the freshness check only runs while the screen is visible
    override fun onStart() {
        super.onStart()
        staleCheckHandler.post(staleCheckRunnable)
    }

    override fun onStop() {
        staleCheckHandler.removeCallbacks(staleCheckRunnable)
        super.onStop()
    }

    override fun onDestroy() {
        // the one-use token never outlives the screen
        twoStep.clear()
        RelayInboxHolder.inbox.screenAttached = false
        unregisterReceiver(relayReceiver)
        staleCheckHandler.removeCallbacks(staleCheckRunnable)
        // a deferred retry (sendVoiceTurn/requestSystemStatus
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
    securityStatus: String? = null,
    canOtaLine: String? = null,
    companionAvailable: Boolean = true,
    ambient: Boolean = false,
    alertText: String? = null,
    alertOutdated: Boolean = false,
    confirmOffered: Boolean = false,
    confirmArmed: Boolean = false,
    presenceAvailable: Boolean = false,
    presenceOn: Boolean = false,
    onConfirmTap: () -> Unit = {},
    onDeclineConfirmation: () -> Unit = {},
    onTogglePresence: () -> Unit = {},
    onStartVoiceRecognition: () -> Unit = {},
    onRefreshSystemStatus: () -> Unit = {},
    onRefreshServerSecurity: () -> Unit = {},
    onTestHaptic: () -> Unit = {},
) {
    MaterialTheme {
        if (ambient) {
            AmbientScreen(systemStatus = systemStatus, systemStatusStale = systemStatusStale, voiceStatus = voiceStatus)
            return@MaterialTheme
        }
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
                alertText?.let { text ->
                    item {
                        // An alert the phone pushed; once it is old it is marked, never shown as a current order.
                        Text(text = text, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
                    }
                    if (alertOutdated) {
                        item {
                            Text(text = stringResource(R.string.status_stale), style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
                        }
                    }
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
                canOtaLine?.let { text ->
                    item {
                        // The flash cycle of a board the phone reports: read only
                        Text(text = text, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center)
                    }
                }
                securityStatus?.let { text ->
                    item {
                        // What the server's security options and its connection history say: read only
                        Text(text = text, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center)
                    }
                }
                item {
                    Button(onClick = onStartVoiceRecognition, enabled = companionAvailable) {
                        Text(text = stringResource(R.string.voice_start))
                    }
                }
                if (!companionAvailable) {
                    item {
                        // Voice requests go through the phone companion:
                        // withheld until it has been heard from (see
                        // CriticalActionGate). Refresh stays available.
                        Text(
                            text = stringResource(R.string.companion_required),
                            style = MaterialTheme.typography.caption2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item {
                    Button(onClick = onRefreshSystemStatus) {
                        Text(text = stringResource(R.string.status_refresh))
                    }
                }
                item {
                    Button(onClick = onRefreshServerSecurity) {
                        Text(text = stringResource(R.string.security_refresh))
                    }
                }
                voiceStatus?.let { status ->
                    item {
                        Text(text = status, style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
                    }
                }
                if (confirmOffered) {
                    item {
                        // Two deliberate taps confirm the order; nothing moves unless the phone has a session and the reservation.
                        Button(onClick = onConfirmTap) {
                            Text(text = stringResource(if (confirmArmed) R.string.confirm_second_tap else R.string.confirm_first_tap))
                        }
                    }
                    item {
                        Button(onClick = onDeclineConfirmation) {
                            Text(text = stringResource(R.string.confirm_cancel))
                        }
                    }
                }
                if (presenceAvailable) {
                    item {
                        Button(onClick = onTogglePresence) {
                            Text(text = stringResource(if (presenceOn) R.string.presence_stop else R.string.presence_start))
                        }
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
                    Button(onClick = onTestHaptic) {
                        Text(text = stringResource(R.string.test_haptic_alert))
                    }
                }
            }
        }
    }
}

/**
 * The screen while the watch is in ambient (low-power) mode: no buttons and no animation, only the last state, whether it
 * is outdated and the time. The full screen comes back when the wrist is raised.
 */
@Composable
fun AmbientScreen(systemStatus: String?, systemStatusStale: Boolean, voiceStatus: String?) {
    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "HYDRA-UMC", style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center)
            Text(
                text = systemStatus ?: voiceStatus ?: stringResource(R.string.status_placeholder),
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
            if (systemStatus != null && systemStatusStale) {
                Text(text = stringResource(R.string.status_stale), style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
            }
        }
    }
}
