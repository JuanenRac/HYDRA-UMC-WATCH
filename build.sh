#!/usr/bin/env bash
# =============================================================================
# HYDRA-UMC-WATCH - Build and Compile Script
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
# GPL-3.0 - see LICENSE
# =============================================================================
# Compiles the debug APK via the Gradle wrapper. This script is the single
# source of the real version bump: it increments version.properties AND
# hydra-umc.project.json together via bump_manifest_version.py, plus
# versionCode via bump_version_code.py, THEN runs Gradle with
# -PhydraUmcReadOnly=true so app/build.gradle.kts's own version-bump logic
# stays inert for this build - that logic still exists and still runs for
# tools/build_test.py's compile-only CI check (which intentionally must
# not touch version.properties/the manifest/CHANGELOG.md), but a real
# build must never bump the version from two places at once.
#
# Usage:
#   chmod +x build.sh gradlew   (one-time)
#   ./build.sh
set -euo pipefail
cd "$(dirname "$0")"

trap '[ -t 0 ] && read -r -p "Press Enter to close..." _' EXIT

echo "========================================"
echo " HYDRA-UMC-WATCH"
echo " Build and Compile Script - compiles the Wear OS debug APK"
echo " Author: JuanenRac (Electro Hobby 3D)"
echo " E-mail: electrohobby3d@gmail.com"
echo " License: GPL-3.0 - see LICENSE"
echo "========================================"
echo ""

echo "-- Bumping the version (major.minor.patch + versionCode) --"
python3 bump_manifest_version.py || exit 1
python3 bump_version_code.py || exit 1

echo ""
echo "-- Running the real unit test suite and compiling the debug APK --"
HYDRA_UMC_CI=1 ./gradlew testDebugUnitTest assembleDebug -PhydraUmcReadOnly=true

echo ""
echo "========================================"
echo " Build complete. APK: app/build/outputs/apk/debug/app-debug.apk"
echo " Run ./run.sh to install it on a connected Wear OS device/emulator."
echo "========================================"
