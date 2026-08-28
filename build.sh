#!/usr/bin/env bash
# HYDRA_UMC_SCRIPT_STANDARD_HEADER_BEGIN
# *****************************************************************************
# Project   : HYDRA-UMC-WATCH
# Script    : build.sh
# Purpose   : Incremental project build, verification and packaging workflow.
# Author    : JuanenRac (Electro Hobby 3D)
# Email     : electrohobby3d@gmail.com
# Copyright : (C) 2026 JuanenRac
# License   : GPL-3.0 - see LICENSE
# *****************************************************************************
# HYDRA_UMC_SCRIPT_STANDARD_HEADER_END
# HYDRA_UMC_SCRIPT_STANDARD_BANNER_BEGIN
printf '\n*******************************************************************************\n'
printf '%s\n' "* HYDRA-UMC-WATCH - build.sh"
printf '%s\n' "* Mode      : INCREMENTAL BUILD"
printf '%s\n' "* Author    : JuanenRac (Electro Hobby 3D)"
printf '%s\n' "* Email     : electrohobby3d@gmail.com"
printf '%s\n' "* Copyright : (C) 2026 JuanenRac"
printf '%s\n' "* License   : GPL-3.0 - see LICENSE"
printf '%s\n' "* ------------------------------------------------------------------------- *"
printf '%s\n' "* 1. Increment the project version and synchronise its manifest."
printf '%s\n' "* 2. Run this project's declared build, verification and packaging commands."
printf '%s\n' "* 3. Report the result and keep an interactive terminal open."
printf '%s\n' "*******************************************************************************"
printf '\n'
# HYDRA_UMC_SCRIPT_STANDARD_BANNER_END
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
# HYDRA_UMC_SCRIPT_STANDARD_VERSION_STEP
printf '%s\n' "[1/3] Incrementing project version and synchronising its manifest..."
# HYDRA_UMC_SCRIPT_STANDARD_VERSION_CAPTURE_BEFORE
HYDRA_UMC_VERSION_BEFORE="$(python3 -c 'import json, pathlib, sys; print(json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))["version"])' "$(dirname "$0")/hydra-umc.project.json")"
python3 bump_manifest_version.py || exit 1
# HYDRA_UMC_SCRIPT_STANDARD_VERSION_CAPTURE_AFTER
HYDRA_UMC_VERSION_AFTER="$(python3 -c 'import json, pathlib, sys; print(json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))["version"])' "$(dirname "$0")/hydra-umc.project.json")"
printf '\n*******************************************************************************\n'
printf '%s\n' '* VERSION INCREMENT COMPLETED'
printf '%s\n' "* v${HYDRA_UMC_VERSION_BEFORE:-unknown} -> v${HYDRA_UMC_VERSION_AFTER:-unknown}"
printf '%s\n' '* Project manifest has been synchronised by the project build flow.'
printf '%s\n' '*******************************************************************************'
printf '\n'
python3 bump_version_code.py || exit 1

echo ""
echo "-- Running the real unit test suite and compiling the debug APK --"
HYDRA_UMC_CI=1 ./gradlew testDebugUnitTest assembleDebug -PhydraUmcReadOnly=true

echo ""
echo "========================================"
echo " Build complete. APK: app/build/outputs/apk/debug/app-debug.apk"
echo " Run ./run.sh to install it on a connected Wear OS device/emulator."
echo "========================================"
