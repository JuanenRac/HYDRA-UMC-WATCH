// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/protocol/ErrorCodesTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.protocol

import com.hydraumc.watch.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorCodesTest {

    @Test
    fun `connection_unavailable resolves to its real string resource`() {
        assertEquals(R.string.voice_error_connection_unavailable, errorCodeToStringRes("connection_unavailable"))
    }

    @Test
    fun `offline resolves to its real headline and detail string resources`() {
        assertEquals(R.string.status_error_offline_headline, errorCodeToStringRes("offline"))
        assertEquals(R.string.status_error_offline_detail, errorCodeToDetailStringRes("offline"))
    }

    @Test
    fun `null errorCode resolves to null - callers fall back to the message's own text`() {
        assertNull(errorCodeToStringRes(null))
        assertNull(errorCodeToDetailStringRes(null))
    }

    // H062's own forward-compatibility requirement: a code this watch build
    // doesn't recognize (e.g. sent by a newer phone build) must fall back
    // to the message's own free-form text, never crash or show a raw code.
    @Test
    fun `an unrecognized errorCode resolves to null, not a crash`() {
        assertNull(errorCodeToStringRes("some_future_code_this_build_does_not_know"))
        assertNull(errorCodeToDetailStringRes("some_future_code_this_build_does_not_know"))
    }

    @Test
    fun `connection_unavailable has no detail resource - it is a single-string reply, not a headline plus detail card`() {
        assertNull(errorCodeToDetailStringRes("connection_unavailable"))
    }
}
