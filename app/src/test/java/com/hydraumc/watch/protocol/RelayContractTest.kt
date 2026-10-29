// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/RelayContractTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * docs/watch_relay_contract.json is shared with the phone app (an identical copy lives in its repository).
 * Every valid message of the file must be accepted by this parser and every invalid one refused.
 */
class RelayContractTest {
    private val contract: JsonObject = Json.parseToJsonElement(File("../docs/watch_relay_contract.json").readText()).jsonObject

    /** A value written as {"repeat": "x", "count": n} becomes a text of n characters. */
    private fun expand(value: JsonElement): JsonElement {
        if (value is JsonObject && value.containsKey("repeat")) {
            return JsonPrimitive(value.getValue("repeat").jsonPrimitive.content.repeat(value.getValue("count").jsonPrimitive.int))
        }
        return value
    }

    private fun wire(vector: JsonObject): String =
        JsonObject(vector.filterKeys { it != "why" }.mapValues { expand(it.value) }).toString()

    @Test
    fun `the contract names the protocol this watch speaks`() {
        assertEquals(1, contract.getValue("protocolVersion").jsonPrimitive.int)
        val levels = contract.getValue("levels").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(WatchStatusLevel.entries.map { it.name }, levels)
    }

    @Test
    fun `every valid message of the contract is accepted`() {
        for (vector in contract.getValue("valid").jsonArray) {
            val raw = wire(vector.jsonObject)
            try {
                parseSyncMessage(raw)
            } catch (exc: SyncMessageParseException) {
                fail("refused a valid message $raw: ${exc.message}")
            }
        }
    }

    @Test
    fun `every invalid message of the contract is refused`() {
        for (vector in contract.getValue("invalid").jsonArray) {
            val why = vector.jsonObject.getValue("why").jsonPrimitive.content
            val raw = wire(vector.jsonObject)
            try {
                parseSyncMessage(raw)
                fail("accepted an invalid message ($why)")
            } catch (_: SyncMessageParseException) {
                // refused, as the contract says
            }
        }
    }

    @Test
    fun `the error codes of the contract are the ones the watch has texts for`() {
        val codes = (contract.getValue("errorCodes") as JsonArray).map { it.jsonPrimitive.content }
        assertTrue(codes.isNotEmpty())
        for (code in codes) assertTrue(code, errorCodeToStringRes(code) != null)
    }
}
