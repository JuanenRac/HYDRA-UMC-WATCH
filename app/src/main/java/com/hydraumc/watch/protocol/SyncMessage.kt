// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/protocol/SyncMessage.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.common.SemanticVersion
import com.hydraumc.watch.haptics.AlertSeverity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val CURRENT_PROTOCOL_VERSION = 1
private const val MAX_ALERT_MESSAGE_LENGTH = 280
private const val MAX_VOICE_TRANSCRIPT_LENGTH = 500
private const val MAX_ASSISTANT_TEXT_LENGTH = 500
private const val MAX_STATUS_HEADLINE_LENGTH = 80
private const val MAX_STATUS_DETAIL_LENGTH = 280
private const val MAX_LOWERED_SWITCHES = 16
private const val MAX_SECURITY_COUNT = 100_000
private const val MAX_CONFIRMATION_VALIDITY_SECONDS = 300
private const val MAX_ROBOTS_RUNNING = 10_000
private val confirmationTokenPattern = Regex("^[A-Za-z0-9._~+/=-]{16,1024}$")
private val switchIdPattern = Regex("^[A-Za-z]{1,24}\\.[A-Za-z]{1,32}$")

/**
 * No message of this protocol comes near this size; refusing a longer one before
 * it is parsed keeps a deeply nested or huge document from exhausting the stack.
 */
private const val MAX_RAW_MESSAGE_LENGTH = 8 * 1024
private val requestIdPattern = Regex("^[A-Za-z0-9_-]{1,64}$")

/** Priority displayed as a colour/icon and optionally a haptic pattern. */
@Serializable
enum class WatchStatusLevel {
    NOMINAL,
    ATTENTION,
    WARNING,
    CRITICAL,
    OFFLINE,
}

/** Whether the operator is in the cell or has ended the presence on purpose. */
@Serializable
enum class PresenceState {
    PRESENT,
    LEFT,
}

/**
 * The message shapes carried over the "WebSocket Sync" arrow in README.md's
 * Wearable Sync Flow diagram, between HYDRA-UMC-SERVER and this watch app.
 * Defining and (de)serializing them correctly is real, testable work
 * independent of actually opening the socket - that transport wiring is
 * still future work and intentionally outside this protocol-only component.
 */
@Serializable
sealed class SyncMessage {
    /**
     * Watch -> cognitive gateway: already-recognized operator speech.
     * Raw audio never travels through this control protocol; it stays on the
     * watch/approved STT engine and only bounded text enters the AI flow.
     */
    @Serializable
    @SerialName("voice_turn")
    data class VoiceTurn(
        val requestId: String,
        val transcript: String,
        val locale: String,
    ) : SyncMessage()

    /**
     * Cognitive gateway -> watch: an AI/system answer that can be rendered
     * as text and spoken by the local Wear OS TTS engine.
     *
     * [errorCode] is the fix for this class's own real localization
     * gap. This class's own [text] field is free-form: for a real AI reply
     * it is already correctly localized upstream (Voice UI/the LLM answers
     * in the request's own [VoiceTurn.locale]) - but WatchVoiceRelayService
     * (HYDRA-UMC-ANDROID-CONTROL, the phone-side sender of this exact
     * message) also uses this SAME field for its own SYSTEM fallback text
     * (a connection timeout, no paired Server session, ...), which it
     * generates itself, in English, with no access to the watch's own
     * locale. [errorCode] separates those two real cases instead of
     * conflating them: a stable, NEVER-translated protocol identifier
     * (`errorCodeToStringRes()`'s own known set) that this watch, not the
     * phone, resolves to a real localized string using its own strings.xml
     * (already real 7-language resources - see `getString(R.string...)`
     * calls throughout MainActivity.kt). `null` (the default, and every
     * real AI reply) means "trust [text] as already-correct localized
     * content", matching this field's original, unchanged behavior. A
     * sender-known code this watch build doesn't yet recognize falls back
     * to [text] as well, rather than failing the whole message - a real
     * forward-compatibility concern given the phone and watch app are
     * updated independently.
     */
    @Serializable
    @SerialName("assistant_reply")
    data class AssistantReply(
        val requestId: String,
        val text: String,
        val level: WatchStatusLevel = WatchStatusLevel.ATTENTION,
        val speak: Boolean = true,
        val requiresConfirmation: Boolean = false,
        val errorCode: String? = null,
        /**
         * The one-use token that completes a reply with [requiresConfirmation] (see [VoiceConfirm]). It is held in the memory of
         * the screen only, for [confirmationValiditySeconds] at most, and never stored, notified or spoken.
         */
        val confirmationToken: String? = null,
        val confirmationValiditySeconds: Int? = null,
    ) : SyncMessage()

    /**
     * Server -> watch: compact, glanceable state card independent of chat.
     * [errorCode] is the same mechanism as [AssistantReply]'s own -
     * see that field's header comment.
     */
    @Serializable
    @SerialName("system_status")
    data class SystemStatus(
        val headline: String,
        val detail: String,
        val level: WatchStatusLevel = WatchStatusLevel.ATTENTION,
        val speak: Boolean = false,
        val errorCode: String? = null,
        /** How many robots the phone reports as running, or null when it does not say (shown by the complication). */
        val robotsRunning: Int? = null,
        /** Whether an alarm is active on the plant, or null when the phone does not say. */
        val alarmActive: Boolean? = null,
    ) : SyncMessage()

    /**
     * Phone -> watch: reports the paired Android Control version and whether
     * it found a newer stable GitHub Release. It is status-only: a Wear OS
     * package is never downloaded or installed through this protocol.
     */
    @Serializable
    @SerialName("companion_version_status")
    data class CompanionVersionStatus(
        val protocolVersion: Int,
        val appVersion: String,
        val updateAvailable: Boolean,
    ) : SyncMessage()

    /**
     * Phone -> watch: what the server's security options and its connection history say, to be READ on the wrist. Nothing in it can change
     * anything: the watch has no way to write an option, and the sender is the paired phone, which reads them as an administrator.
     * [loweredSwitches] are the `section.key` ids of the options that are on their value that lowers the protection of the server (the
     * watch resolves each to a sentence in its own language, and shows an id it does not know as it is); [lockedSwitches] is how many
     * options the environment of the server fixes; [activeClients] and [failedLogins] come from the history of connections.
     */
    @Serializable
    @SerialName("server_security_status")
    data class ServerSecurityStatus(
        val protocolVersion: Int,
        val loweredSwitches: List<String> = emptyList(),
        val lockedSwitches: Int = 0,
        val activeClients: Int = 0,
        val failedLogins: Int = 0,
    ) : SyncMessage()

    /**
     * Watch -> phone: the operator wears the watch and is in the cell. Sent every few seconds while the presence option is on; the
     * phone passes it on and the server (not this watch) decides what a lost heartbeat means. [state] is LEFT when the operator
     * ended the presence on purpose, so a deliberate leave is told apart from a loss.
     */
    @Serializable
    @SerialName("presence_heartbeat")
    data class PresenceHeartbeat(
        val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
        val sequence: Long,
        val intervalSeconds: Int,
        val state: PresenceState = PresenceState.PRESENT,
    ) : SyncMessage()

    /**
     * Watch -> phone: the operator confirmed, with two deliberate taps on the watch, the order of the reply [confirms]. [requestId] is
     * new: the answer comes back as an assistant reply with it. The phone sends the confirmation to the server only if it has a session
     * and the reservation of the robot; the watch cannot make a robot move by itself.
     */
    @Serializable
    @SerialName("voice_confirm")
    data class VoiceConfirm(
        val requestId: String,
        val confirms: String,
        val confirmationToken: String,
    ) : SyncMessage()

    /** Watch -> server: the dedicated emergency button was pressed. */
    @Serializable
    @SerialName("estop_command")
    object EStopCommand : SyncMessage()

    /** Server -> watch: an alert to surface (and vibrate) on the wrist. */
    @Serializable
    @SerialName("alert")
    data class Alert(val severity: AlertSeverity, val message: String) : SyncMessage()
}

class SyncMessageParseException(message: String) : Exception(message)

private val json = Json {
    ignoreUnknownKeys = true
    // an enum value a newer sender adds (a new status level) falls back to the
    // property's default instead of dropping the whole message
    coerceInputValues = true
    classDiscriminator = "type"
}

/** Serializes a [SyncMessage] to the wire format (a `type`-discriminated JSON object). */
fun SyncMessage.toJson(): String = json.encodeToString(SyncMessage.serializer(), this)

/**
 * Parses a wire-format JSON string into a [SyncMessage].
 *
 * Deliberately hand-checks the `type` discriminator itself rather than
 * trusting the polymorphic deserializer's own error message, so a
 * malformed/unknown message from a future protocol version fails with a
 * message this app's own log/UI can show verbatim instead of a raw
 * kotlinx.serialization stack trace.
 */
fun parseSyncMessage(raw: String): SyncMessage {
    if (raw.length > MAX_RAW_MESSAGE_LENGTH) {
        throw SyncMessageParseException("message too long (${raw.length} characters)")
    }
    val element = try {
        json.parseToJsonElement(raw)
    } catch (exc: Exception) {
        throw SyncMessageParseException("malformed JSON: ${exc.message}")
    }
    val obj = element as? JsonObject
        ?: throw SyncMessageParseException("expected a JSON object, got: $raw")
    val typeElement = obj["type"] ?: throw SyncMessageParseException("missing 'type' field: $raw")
    val type = (typeElement as? JsonPrimitive)?.content
        ?: throw SyncMessageParseException("'type' must be a text, got: $raw")
    if (type !in setOf(
            "voice_turn",
            "assistant_reply",
            "system_status",
            "companion_version_status",
            "server_security_status",
            "estop_command",
            "alert",
            "presence_heartbeat",
            "voice_confirm",
        )
    ) {
        throw SyncMessageParseException("unknown message type '$type'")
    }
    val decoded = try {
        json.decodeFromJsonElement(SyncMessage.serializer(), element)
    } catch (exc: Exception) {
        throw SyncMessageParseException("malformed '$type' message: ${exc.message}")
    }
    // validated before the cut: a text of blanks must not become "blanks…" and pass
    validateProtocolMessage(decoded)
    return truncateLongReply(decoded)
}

/**
 * The Server accepts assistant answers up to 600 characters while this watch
 * shows 500: a longer answer is cut with an ellipsis instead of being dropped
 * whole (an answer that vanishes leaves the operator on "sending" forever).
 */
private fun truncateLongReply(message: SyncMessage): SyncMessage {
    if (message is SyncMessage.AssistantReply && message.text.length > MAX_ASSISTANT_TEXT_LENGTH) {
        return message.copy(text = message.text.take(MAX_ASSISTANT_TEXT_LENGTH - 1) + "…")
    }
    return message
}

/** Rejects messages that are syntactically valid but unsafe or incompatible. */
private fun validateProtocolMessage(message: SyncMessage) {
    when (message) {
        is SyncMessage.VoiceTurn -> {
            if (!requestIdPattern.matches(message.requestId)) {
                throw SyncMessageParseException("invalid voice request ID")
            }
            if (message.transcript.isBlank() || message.transcript.length > MAX_VOICE_TRANSCRIPT_LENGTH) {
                throw SyncMessageParseException("voice transcript must contain 1-$MAX_VOICE_TRANSCRIPT_LENGTH characters")
            }
            if (message.locale.length !in 2..35) {
                throw SyncMessageParseException("voice locale must contain 2-35 characters")
            }
        }
        is SyncMessage.AssistantReply -> {
            if (!requestIdPattern.matches(message.requestId)) {
                throw SyncMessageParseException("invalid assistant request ID")
            }
            // a text over the limit is cut afterwards (truncateLongReply), not refused
            if (message.text.isBlank()) {
                throw SyncMessageParseException("assistant text must contain 1-$MAX_ASSISTANT_TEXT_LENGTH characters")
            }
            if (message.confirmationToken != null && !confirmationTokenPattern.matches(message.confirmationToken)) {
                throw SyncMessageParseException("invalid confirmation token")
            }
            if (message.confirmationValiditySeconds != null && message.confirmationValiditySeconds !in 1..MAX_CONFIRMATION_VALIDITY_SECONDS) {
                throw SyncMessageParseException("confirmation validity must be from 1 to $MAX_CONFIRMATION_VALIDITY_SECONDS seconds")
            }
        }
        is SyncMessage.SystemStatus -> {
            if (message.headline.isBlank() || message.headline.length > MAX_STATUS_HEADLINE_LENGTH) {
                throw SyncMessageParseException("status headline must contain 1-$MAX_STATUS_HEADLINE_LENGTH characters")
            }
            if (message.detail.length > MAX_STATUS_DETAIL_LENGTH) {
                throw SyncMessageParseException("status detail must contain at most $MAX_STATUS_DETAIL_LENGTH characters")
            }
            if (message.robotsRunning != null && message.robotsRunning !in 0..MAX_ROBOTS_RUNNING) {
                throw SyncMessageParseException("robots running must be from 0 to $MAX_ROBOTS_RUNNING")
            }
        }
        is SyncMessage.CompanionVersionStatus -> {
            if (message.protocolVersion != CURRENT_PROTOCOL_VERSION) {
                throw SyncMessageParseException(
                    "unsupported companion protocol version ${message.protocolVersion}",
                )
            }
            // The phone app's own criterion (shared module): 3 or 4 numeric components, no pre-release label.
            // On the wire the version is bare, so a "v" prefix is not accepted here.
            if (message.appVersion != message.appVersion.trim() || message.appVersion.startsWith("v") || SemanticVersion.parseStable(message.appVersion) == null) {
                throw SyncMessageParseException("invalid companion app version '${message.appVersion}'")
            }
        }
        is SyncMessage.ServerSecurityStatus -> {
            if (message.protocolVersion != CURRENT_PROTOCOL_VERSION) {
                throw SyncMessageParseException("unsupported security status protocol version ${message.protocolVersion}")
            }
            if (message.loweredSwitches.size > MAX_LOWERED_SWITCHES || message.loweredSwitches.toSet().size != message.loweredSwitches.size) {
                throw SyncMessageParseException("security status lists at most $MAX_LOWERED_SWITCHES different switches")
            }
            if (message.loweredSwitches.any { !switchIdPattern.matches(it) }) {
                throw SyncMessageParseException("security status has a switch id that is not section.key")
            }
            if (listOf(message.lockedSwitches, message.activeClients, message.failedLogins).any { it !in 0..MAX_SECURITY_COUNT }) {
                throw SyncMessageParseException("security status counts must be from 0 to $MAX_SECURITY_COUNT")
            }
        }
        is SyncMessage.Alert -> {
            if (message.message.isBlank() || message.message.length > MAX_ALERT_MESSAGE_LENGTH) {
                throw SyncMessageParseException("alert message must contain 1-$MAX_ALERT_MESSAGE_LENGTH characters")
            }
        }
        is SyncMessage.PresenceHeartbeat -> {
            if (message.protocolVersion != CURRENT_PROTOCOL_VERSION) {
                throw SyncMessageParseException("unsupported presence protocol version ${message.protocolVersion}")
            }
            if (message.sequence < 0 || message.intervalSeconds !in 1..60) {
                throw SyncMessageParseException("presence needs a sequence from 0 and an interval from 1 to 60 seconds")
            }
        }
        is SyncMessage.VoiceConfirm -> {
            if (!requestIdPattern.matches(message.requestId) || !requestIdPattern.matches(message.confirms)) {
                throw SyncMessageParseException("invalid voice confirmation request ID")
            }
            if (!confirmationTokenPattern.matches(message.confirmationToken)) {
                throw SyncMessageParseException("invalid confirmation token")
            }
        }
        SyncMessage.EStopCommand -> Unit
    }
}
