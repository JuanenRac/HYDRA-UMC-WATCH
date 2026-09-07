// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/protocol/SyncMessage.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.haptics.AlertSeverity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val CURRENT_PROTOCOL_VERSION = 1
private const val MAX_ALERT_MESSAGE_LENGTH = 280
private const val MAX_VOICE_TRANSCRIPT_LENGTH = 500
private const val MAX_ASSISTANT_TEXT_LENGTH = 500
private const val MAX_STATUS_HEADLINE_LENGTH = 80
private const val MAX_STATUS_DETAIL_LENGTH = 280
private val stableVersionPattern = Regex("^\\d+\\.\\d+\\.\\d+$")
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
     */
    @Serializable
    @SerialName("assistant_reply")
    data class AssistantReply(
        val requestId: String,
        val text: String,
        val level: WatchStatusLevel = WatchStatusLevel.ATTENTION,
        val speak: Boolean = true,
        val requiresConfirmation: Boolean = false,
    ) : SyncMessage()

    /** Server -> watch: compact, glanceable state card independent of chat. */
    @Serializable
    @SerialName("system_status")
    data class SystemStatus(
        val headline: String,
        val detail: String,
        val level: WatchStatusLevel,
        val speak: Boolean = false,
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
    val element = try {
        json.parseToJsonElement(raw)
    } catch (exc: Exception) {
        throw SyncMessageParseException("malformed JSON: ${exc.message}")
    }
    val obj = element as? JsonObject
        ?: throw SyncMessageParseException("expected a JSON object, got: $raw")
    val type = obj["type"]?.jsonPrimitive?.content
        ?: throw SyncMessageParseException("missing 'type' field: $raw")
    if (type !in setOf(
            "voice_turn",
            "assistant_reply",
            "system_status",
            "companion_version_status",
            "estop_command",
            "alert",
        )
    ) {
        throw SyncMessageParseException("unknown message type '$type'")
    }
    val decoded = try {
        json.decodeFromJsonElement(SyncMessage.serializer(), element)
    } catch (exc: Exception) {
        throw SyncMessageParseException("malformed '$type' message: ${exc.message}")
    }
    validateProtocolMessage(decoded)
    return decoded
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
            if (message.text.isBlank() || message.text.length > MAX_ASSISTANT_TEXT_LENGTH) {
                throw SyncMessageParseException("assistant text must contain 1-$MAX_ASSISTANT_TEXT_LENGTH characters")
            }
        }
        is SyncMessage.SystemStatus -> {
            if (message.headline.isBlank() || message.headline.length > MAX_STATUS_HEADLINE_LENGTH) {
                throw SyncMessageParseException("status headline must contain 1-$MAX_STATUS_HEADLINE_LENGTH characters")
            }
            if (message.detail.length > MAX_STATUS_DETAIL_LENGTH) {
                throw SyncMessageParseException("status detail must contain at most $MAX_STATUS_DETAIL_LENGTH characters")
            }
        }
        is SyncMessage.CompanionVersionStatus -> {
            if (message.protocolVersion != CURRENT_PROTOCOL_VERSION) {
                throw SyncMessageParseException(
                    "unsupported companion protocol version ${message.protocolVersion}",
                )
            }
            if (!stableVersionPattern.matches(message.appVersion)) {
                throw SyncMessageParseException("invalid companion app version '${message.appVersion}'")
            }
        }
        is SyncMessage.Alert -> {
            if (message.message.isBlank() || message.message.length > MAX_ALERT_MESSAGE_LENGTH) {
                throw SyncMessageParseException("alert message must contain 1-$MAX_ALERT_MESSAGE_LENGTH characters")
            }
        }
        SyncMessage.EStopCommand -> Unit
    }
}
