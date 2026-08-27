#!/usr/bin/env bash
# =============================================================================
# HYDRA-UMC-WATCH - Run Script
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
# GPL-3.0 - see LICENSE
# =============================================================================
# Installs the debug APK onto a connected Wear OS device/emulator (via
# Gradle's installDebug task, which calls adb install internally) and
# launches MainActivity. Run build.sh first, or just run this - Gradle
# builds installDebug's own dependencies (assembleDebug) automatically.
set -euo pipefail
cd "$(dirname "$0")"

./gradlew installDebug
adb shell am start -n com.hydraumc.watch/.MainActivity
