// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/MainActivityTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import com.hydraumc.watch.protocol.SyncMessage
import com.hydraumc.watch.protocol.WatchStatusLevel
import com.hydraumc.watch.protocol.toJson
import com.hydraumc.watch.transport.ACTION_WATCH_RELAY_MESSAGE
import com.hydraumc.watch.transport.EXTRA_WATCH_RELAY_MESSAGE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun relay(message: SyncMessage) {
        val context = rule.activity.applicationContext
        val intent = Intent(ACTION_WATCH_RELAY_MESSAGE)
            .setPackage(context.packageName)
            .putExtra(EXTRA_WATCH_RELAY_MESSAGE, message.toJson())
        context.sendBroadcast(intent)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        rule.waitForIdle()
    }

    @Test
    fun theScreenStartsWithItsTitleAndNoStatus() {
        rule.onNodeWithText("HYDRA-UMC WATCH").assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.status_placeholder)).assertIsDisplayed()
    }

    @Test
    fun aStatusFromThePhoneIsShownAsHeadlineAndDetail() {
        relay(SyncMessage.SystemStatus(headline = "Cell A", detail = "all robots idle", level = WatchStatusLevel.NOMINAL))
        rule.onNodeWithText("Cell A: all robots idle").assertIsDisplayed()
    }

    @Test
    fun aKnownErrorCodeIsShownInTheLanguageOfTheWatchNotTheTextOfThePhone() {
        relay(
            SyncMessage.SystemStatus(
                headline = "untranslated headline",
                detail = "untranslated detail",
                level = WatchStatusLevel.WARNING,
                errorCode = "offline",
            ),
        )
        val headline = rule.activity.getString(com.hydraumc.watch.protocol.errorCodeToStringRes("offline")!!)
        rule.onNodeWithText(headline, substring = true).assertIsDisplayed()
    }

    @Test
    fun anAnswerNobodyAskedForIsNotShown() {
        relay(SyncMessage.AssistantReply(requestId = "never-asked", text = "unsolicited answer"))
        rule.onNodeWithText("unsolicited answer").assertDoesNotExist()
    }

    @Test
    fun voiceStaysDisabledUntilThePhoneHasBeenHeardFromAndRefreshStaysAvailable() {
        relay(SyncMessage.SystemStatus(headline = "Hello", detail = "from the phone"))
        rule.onNodeWithText(rule.activity.getString(R.string.voice_start)).assertIsEnabled()
        rule.onNodeWithText(rule.activity.getString(R.string.status_refresh)).assertIsEnabled()
    }

    @Test
    fun withoutThePhoneVoiceIsWithheld() {
        // the screen starts before any message: the gate decides from the last known state
        val voice = rule.onNodeWithText(rule.activity.getString(R.string.voice_start))
        voice.assertIsNotEnabled()
        rule.onNodeWithText(rule.activity.getString(R.string.status_refresh)).assertIsEnabled()
    }
}
