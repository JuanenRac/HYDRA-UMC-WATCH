// =============================================================================
// HYDRA-UMC Kotlin app common - src/test/kotlin/com/hydraumc/common/ReleaseFeedJsonTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package com.hydraumc.common

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The release feed as GitHub sends it: the JSON is read into the same trust gate, and a field that is JSON null reads as missing. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReleaseFeedJsonTest {
    private val asset = "APP-release.apk"

    private fun feed(tag: String, url: String = "https://example.invalid/app.apk", extras: String = "") =
        """{"tag_name":"$tag","name":"Release $tag","body":" notes ","assets":[{"name":"$asset","browser_download_url":"$url"}]$extras}"""

    @Test
    fun `a newer stable release with the exact HTTPS asset is offered with its notes`() {
        val result = ReleaseMetadataParser.parseLatestStable(feed("v0.4.5"), "0.4.4", asset)
        val update = (result as UpdateCheckResult.Available).update
        assertEquals(SemanticVersion(0, 4, 5), update.version)
        assertEquals("Release v0.4.5", update.releaseName)
        assertEquals("notes", update.notes)
        assertEquals("https://example.invalid/app.apk", update.assetUrl)
        assertEquals(null, update.sha256)
    }

    @Test
    fun `the digest the feed publishes for the asset reaches the update`() {
        val hex = "d".repeat(64)
        val json = """{"tag_name":"v0.4.5","assets":[{"name":"$asset","browser_download_url":"https://example.invalid/a.apk","digest":"sha256:$hex"}]}"""
        val update = (ReleaseMetadataParser.parseLatestStable(json, "0.4.4", asset) as UpdateCheckResult.Available).update
        assertEquals(hex, update.sha256)
    }

    @Test
    fun `a release whose name is JSON null takes the fallback name and a null body reads as empty`() {
        val json = """{"tag_name":"v0.4.5","name":null,"body":null,"assets":[{"name":"$asset","browser_download_url":"https://example.invalid/a.apk"}]}"""
        val update = (ReleaseMetadataParser.parseLatestStable(json, "0.4.4", asset, "APP") as UpdateCheckResult.Available).update
        assertEquals("APP v0.4.5", update.releaseName)
        assertEquals("", update.notes)
    }

    @Test
    fun `a draft, a prerelease and an older release offer nothing`() {
        assertEquals(UpdateCheckResult.UpToDate, ReleaseMetadataParser.parseLatestStable(feed("v0.4.5", extras = ",\"draft\":true"), "0.4.4", asset))
        assertEquals(UpdateCheckResult.UpToDate, ReleaseMetadataParser.parseLatestStable(feed("v0.4.5", extras = ",\"prerelease\":true"), "0.4.4", asset))
        assertEquals(UpdateCheckResult.UpToDate, ReleaseMetadataParser.parseLatestStable(feed("v0.4.3"), "0.4.4", asset))
    }

    @Test
    fun `a tag that is not stable, a missing asset list and a plain HTTP URL fail`() {
        assertTrue(ReleaseMetadataParser.parseLatestStable(feed("v0.4.5-beta"), "0.4.4", asset) is UpdateCheckResult.Failed)
        assertTrue(ReleaseMetadataParser.parseLatestStable("""{"tag_name":"v0.4.5"}""", "0.4.4", asset) is UpdateCheckResult.Failed)
        assertTrue(ReleaseMetadataParser.parseLatestStable(feed("v0.4.5", "http://example.invalid/a.apk"), "0.4.4", asset) is UpdateCheckResult.Failed)
        assertTrue(ReleaseMetadataParser.parseLatestStable(feed("v0.4.5"), "not-a-version", asset) is UpdateCheckResult.Failed)
    }

    @Test
    fun `an asset entry that is not an object is skipped and the right one is still found`() {
        val json = """{"tag_name":"v0.4.5","assets":[7,{"name":"other.zip","browser_download_url":"https://example.invalid/o.zip"},{"name":"$asset","browser_download_url":"https://example.invalid/a.apk"}]}"""
        assertTrue(ReleaseMetadataParser.parseLatestStable(json, "0.4.4", asset) is UpdateCheckResult.Available)
    }

    @Test
    fun `optStr gives the fallback for a missing key and for JSON null and the text otherwise`() {
        val json = JSONObject("""{"a":"text","b":null,"c":5}""")
        assertEquals("text", json.optStr("a"))
        assertEquals("fallback", json.optStr("b", "fallback"))
        assertEquals("", json.optStr("missing"))
        assertEquals("x", json.optStr("missing", "x"))
        assertEquals("5", json.optStr("c"))
    }
}
