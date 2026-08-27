// =============================================================================
// HYDRA-UMC-WATCH - Root Gradle build: build.gradle.kts
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// The wrapper (gradlew/gradlew.bat/gradle/wrapper/*) is pinned to Gradle
// 9.7.0, which AGP 9.3.1 (below) requires - identical pin to sibling repo
// HYDRA-UMC-ANDROID-CONTROL so both projects share one known-good
// toolchain across the ecosystem.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
