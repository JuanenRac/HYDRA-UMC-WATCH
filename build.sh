#!/usr/bin/env bash
# =============================================================================
# HYDRA-UMC-WATCH - Build and Compile Script
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
# GPL-3.0 - see LICENSE
# =============================================================================
# Compiles the debug APK via the Gradle wrapper. Bumping the version
# happens inside app/build.gradle.kts itself (Gradle configuration time),
# so it runs automatically as part of this same command - no separate
# version-bump step needed, unlike the Node/Go/Python projects.
#
# Usage:
#   chmod +x build.sh gradlew   (one-time)
#   ./build.sh
set -euo pipefail
python3 "$(dirname "$0")/bump_manifest_version.py" || exit 1
cd "$(dirname "$0")"

echo "========================================"
echo " HYDRA-UMC-WATCH"
echo " Build and Compile Script - compiles the Wear OS debug APK"
echo " Author: JuanenRac (Electro Hobby 3D)"
echo " E-mail: electrohobby3d@gmail.com"
echo " License: GPL-3.0 - see LICENSE"
echo "========================================"
echo ""

./gradlew assembleDebug

echo ""
echo "========================================"
echo " Build complete. APK: app/build/outputs/apk/debug/app-debug.apk"
echo " Run ./run.sh to install it on a connected Wear OS device/emulator."
echo "========================================"
