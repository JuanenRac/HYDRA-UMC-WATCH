// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/ServerSecurityStatusTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The read-only security status that the paired phone sends: what is accepted, what is refused, and that every switch has a sentence. */
class ServerSecurityStatusTest {
    private fun refused(raw: String) {
        try {
            parseSyncMessage(raw)
            fail("should have been refused: $raw")
        } catch (_: SyncMessageParseException) {
            // expected
        }
    }

    @Test
    fun `a status round-trips`() {
        val original = SyncMessage.ServerSecurityStatus(
            protocolVersion = 1,
            loweredSwitches = listOf("health.open", "camera.open"),
            lockedSwitches = 2,
            activeClients = 3,
            failedLogins = 1,
        )
        assertEquals(original, parseSyncMessage(original.toJson()))
    }

    @Test
    fun `the lists and counts have defaults, so a status with only the version is the all-protected one`() {
        val parsed = parseSyncMessage("""{"type":"server_security_status","protocolVersion":1}""") as SyncMessage.ServerSecurityStatus
        assertTrue(parsed.loweredSwitches.isEmpty())
        assertEquals(0, parsed.activeClients)
        assertEquals(WatchStatusLevel.NOMINAL, securityLevel(parsed))
    }

    @Test
    fun `one switch that lowers the protection makes it attention`() {
        val parsed = SyncMessage.ServerSecurityStatus(protocolVersion = 1, loweredSwitches = listOf("logging.redactSecrets"))
        assertEquals(WatchStatusLevel.ATTENTION, securityLevel(parsed))
    }

    @Test
    fun `an unknown version, a bad id, a repeated id, too many ids and counts out of range are refused`() {
        refused("""{"type":"server_security_status","protocolVersion":2}""")
        refused("""{"type":"server_security_status","protocolVersion":1,"loweredSwitches":["not a switch"]}""")
        refused("""{"type":"server_security_status","protocolVersion":1,"loweredSwitches":["health"]}""")
        refused("""{"type":"server_security_status","protocolVersion":1,"loweredSwitches":["health.open","health.open"]}""")
        refused("""{"type":"server_security_status","protocolVersion":1,"loweredSwitches":[${('a'..'q').joinToString(",") { "\"s.$it\"" }}]}""")
        refused("""{"type":"server_security_status","protocolVersion":1,"activeClients":-1}""")
        refused("""{"type":"server_security_status","protocolVersion":1,"failedLogins":100001}""")
        refused("""{"type":"server_security_status","protocolVersion":1.5}""")
    }

    @Test
    fun `an id a newer phone adds is accepted and has no sentence of its own`() {
        val parsed = parseSyncMessage("""{"type":"server_security_status","protocolVersion":1,"loweredSwitches":["newsection.newKey"]}""") as SyncMessage.ServerSecurityStatus
        assertEquals(listOf("newsection.newKey"), parsed.loweredSwitches)
        assertNull(securitySwitchStringRes("newsection.newKey"))
    }

    @Test
    fun `a message that is too long is refused before it is parsed`() {
        refused("""{"type":"server_security_status","protocolVersion":1,"pad":"${"x".repeat(9000)}"}""")
    }

    @Test
    fun `every switch the server can report as lowered has a sentence in every language`() {
        val ids = listOf(
            "health.open", "connections.recordAddress", "camera.open", "websocket.allowQueryToken", "logging.redactSecrets",
            "camera.credentialsViaEnv", "install.newInstallTls",
        )
        ids.forEach { assertNotNull(it, securitySwitchStringRes(it)) }
        assertEquals(ids.size, ids.map { securitySwitchStringRes(it) }.toSet().size)
        val res = File("src/main/res")
        val keys = Regex("""<string name="(security_[^"]+)"""")
        val base = keys.findAll(File(res, "values/strings.xml").readText()).map { it.groupValues[1] }.toSet()
        assertTrue(base.size >= 13)
        res.listFiles { f -> f.isDirectory && f.name.startsWith("values-") }!!.forEach { dir ->
            val found = keys.findAll(File(dir, "strings.xml").readText()).map { it.groupValues[1] }.toSet()
            assertEquals(dir.name, base, found)
        }
    }
}
