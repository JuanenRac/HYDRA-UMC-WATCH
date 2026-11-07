// =============================================================================
// HYDRA-UMC WATCH - Instrumented checks of the installed app
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package com.hydraumc.watch

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that need a real Wear OS system and so run on an emulator (started by the manual workflow): that the installed app is the one
 * that was built, that it declares itself a watch app, that it asks for no more permissions than it needs and that its launcher
 * activity is the one the system starts.
 */
@RunWith(AndroidJUnit4::class)
class InstalledWatchAppInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun requested(): Set<String> {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        return info.requestedPermissions?.toSet() ?: emptySet()
    }

    @Test
    fun theWatchAppSharesTheApplicationIdOfThePhoneAppForTheDataLayer() {
        // the Data Layer accepts messages only between packages with an identical application ID
        assertEquals("com.hydraumc.control", context.packageName)
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertNotNull(info.versionName)
        assertTrue(info.versionName!!.isNotBlank())
    }

    @Test
    fun theAppDeclaresItselfAWatchApp() {
        assertTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
    }

    @Test
    fun theAppAsksOnlyForThePermissionsItNeeds() {
        val allowed = setOf(
            "android.permission.INTERNET",
            "android.permission.RECORD_AUDIO",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        )
        val extra = requested().filterNot { it in allowed || it.startsWith("${context.packageName}.") }
        assertTrue("permissions that are not in the list of the watch app: $extra", extra.isEmpty())
    }

    @Test
    fun theWatchAskedForNoLocationNoContactsAndNoStorage() {
        val permissions = requested()
        for (name in listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "READ_CONTACTS", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "CAMERA", "BODY_SENSORS")) {
            assertFalse("android.permission.$name", "android.permission.$name" in permissions)
        }
    }

    @Test
    fun theLauncherActivityIsTheMainActivityOfTheApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        assertNotNull(launch)
        assertEquals(Intent.ACTION_MAIN, launch!!.action)
        assertEquals("com.hydraumc.watch.MainActivity", launch.component?.className)
    }
}
