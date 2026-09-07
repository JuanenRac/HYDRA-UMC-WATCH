// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/transport/PickCompanionNodeTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// WATCH-01 (found in an ecosystem-wide software-improvements audit, P2):
// pickCompanionNode() is the real fix for connectedNodes.firstOrNull()
// picking any connected node with no proof it actually runs the companion
// app - WatchRelayTransport.send() now only ever calls this with nodes
// CapabilityClient already verified as running HYDRA-UMC-ANDROID-CONTROL,
// and this pure function decides which ONE of those (if more than one) to
// use. A hand-written fake, not a mocking library - Node is a plain
// interface with no Android framework dependency, so this runs on a
// plain JVM like every other test in this file's own package.
package com.hydraumc.watch.transport

import com.google.android.gms.wearable.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeNode(private val id: String, private val nearby: Boolean = false) : Node {
    override fun getId(): String = id
    override fun getDisplayName(): String = id
    override fun isNearby(): Boolean = nearby
}

class PickCompanionNodeTest {

    @Test
    fun `no capability-verified nodes at all returns null`() {
        assertNull(pickCompanionNode(emptySet()))
    }

    @Test
    fun `a single verified node is picked even when not nearby`() {
        val phone = FakeNode("phone-1", nearby = false)
        assertEquals(phone, pickCompanionNode(setOf(phone)))
    }

    @Test
    fun `a nearby node is preferred over a cloud-relayed one`() {
        // The real scenario this closes: two connected nodes, only one
        // directly Bluetooth-reachable - picking the wrong one adds real
        // relay latency for no reason.
        val relayed = FakeNode("phone-cloud", nearby = false)
        val nearby = FakeNode("phone-local", nearby = true)
        assertEquals(nearby, pickCompanionNode(setOf(relayed, nearby)))
    }

    @Test
    fun `falls back to any verified node when none report as nearby`() {
        val a = FakeNode("phone-a", nearby = false)
        val b = FakeNode("phone-b", nearby = false)
        val picked = pickCompanionNode(setOf(a, b))
        assertEquals(true, picked === a || picked === b)
    }
}
