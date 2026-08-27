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

/**
 * The message shapes carried over the "WebSocket Sync" arrow in README.md's
 * Wearable Sync Flow diagram, between HYDRA-UMC-SERVER and this watch app.
 * Defining and (de)serializing them correctly is real, testable work
 * independent of actually opening the socket - that transport wiring is
 * still future work, tracked in SONNET/HYDRA-UMC-WATCH/mejoras_futuras.txt.
 */
@Serializable
sealed class SyncMessage {
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
    if (type != "estop_command" && type != "alert") {
        throw SyncMessageParseException("unknown message type '$type'")
    }
    return try {
        json.decodeFromJsonElement(SyncMessage.serializer(), element)
    } catch (exc: Exception) {
        throw SyncMessageParseException("malformed '$type' message: ${exc.message}")
    }
}
