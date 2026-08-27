// =============================================================================
// HYDRA-UMC-WATCH - App module Gradle build: app/build.gradle.kts
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
}

// =============================================================================
// Ecosystem-wide auto version bump ("odometer" rule, base 10) - identical
// mechanism to sibling repo HYDRA-UMC-ANDROID-CONTROL/app/build.gradle.kts
// (copied rather than reinvented - same Gradle/Kotlin toolchain). Runs at
// Gradle CONFIGURATION time, which happens on every real build
// (assembleDebug, installDebug, compileDebugKotlin, ...), so
// version.properties is read, bumped and rewritten with the new values
// BEFORE those values are used for versionCode/versionName below - the APK
// produced by this exact invocation already carries the bumped number. CI sets
// HYDRA_UMC_CI=1 so verification tasks do not mutate version.properties.
//
// Rule: versionPatch +1; if it would go above 9 it resets to 0 and
// versionMinor +1 instead (example: 0.0.9 -> 0.1.0). versionCode is a
// separate simple monotonic counter, always +1, no carry - Android requires
// versionCode to strictly increase across every build that ever ships.
val versionPropsFile = file("version.properties")
val versionPropsText = versionPropsFile.readText()

fun readIntProp(text: String, key: String): Int {
    val match = Regex("(?m)^$key=(\\d+)\\s*$").find(text)
        ?: throw GradleException("version.properties: missing '$key=<number>' line")
    return match.groupValues[1].toInt()
}

fun replaceIntProp(text: String, key: String, value: Int): String =
    text.replace(Regex("(?m)^$key=\\d+\\s*$"), "$key=$value")

var appVersionMajor = readIntProp(versionPropsText, "versionMajor")
var appVersionMinor = readIntProp(versionPropsText, "versionMinor")
var appVersionPatch = readIntProp(versionPropsText, "versionPatch")
var appVersionCode = readIntProp(versionPropsText, "versionCode")

if (System.getenv("HYDRA_UMC_CI") != "1") {
    appVersionPatch += 1
    if (appVersionPatch > 9) {
        appVersionPatch = 0
        appVersionMinor += 1
    }
    appVersionCode += 1

    var newVersionPropsText = versionPropsText
    newVersionPropsText = replaceIntProp(newVersionPropsText, "versionMajor", appVersionMajor)
    newVersionPropsText = replaceIntProp(newVersionPropsText, "versionMinor", appVersionMinor)
    newVersionPropsText = replaceIntProp(newVersionPropsText, "versionPatch", appVersionPatch)
    newVersionPropsText = replaceIntProp(newVersionPropsText, "versionCode", appVersionCode)
    versionPropsFile.writeText(newVersionPropsText)
}

val appVersionName = "$appVersionMajor.$appVersionMinor.$appVersionPatch"

android {
    namespace = "com.hydraumc.watch"
    // 36 required by androidx.core 1.18.0 - same pin as HYDRA-UMC-ANDROID-CONTROL.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hydraumc.watch"
        // 30 = Wear OS 3 baseline (the platform androidx.wear.compose
        // targets today) - lower than the phone app's minSdk 24 on purpose,
        // this is a standalone watch app, not a companion that has to
        // support old Wear OS 2 hardware.
        minSdk = 30
        // 35 (Android 15 / Wear OS 5) for stable runtime behavior, same
        // pin as HYDRA-UMC-ANDROID-CONTROL.
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        // Needed so BuildConfig.VERSION_NAME reflects the auto-bumped
        // versionName above at runtime (MainActivity reads it for the
        // on-watch version label) - disabled by default on AGP 8+.
        buildConfig = true
    }

    buildTypes {
        release {
            // Minify stays off at this andamiaje (scaffolding) stage - the
            // app has a single screen and no third-party reflection-heavy
            // libraries yet that would need proguard-rules.pro tuning.
            // Revisit once the real safety-dashboard screens land.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // Use debug key for now for easy testing
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Wear OS UI toolkit - Wear's own MaterialTheme/Text/Scaffold/TimeText,
    // NOT androidx.compose.material3 (the handheld/tablet variant).
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    // Declares this app as Wear OS-aware to the Play/ADB tooling and
    // brings the AmbientModeSupport helpers used for always-on display.
    implementation(libs.androidx.wear)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
