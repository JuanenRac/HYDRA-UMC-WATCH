// =============================================================================
// HYDRA-UMC-WATCH - Root Gradle settings: settings.gradle.kts
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
// Same repository/module wiring as sibling repo HYDRA-UMC-ANDROID-CONTROL - reused rather
// than reinvented, since it's the same Gradle/Kotlin/Android toolchain.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "HYDRA-UMC WATCH APP"
include(":app")
