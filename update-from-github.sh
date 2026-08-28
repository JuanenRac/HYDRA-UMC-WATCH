#!/usr/bin/env bash
# HYDRA_UMC_SCRIPT_STANDARD_HEADER_BEGIN
# *****************************************************************************
# Project   : HYDRA-UMC-WATCH
# Script    : update-from-github.sh
# Purpose   : Download and install a newer signed Wear OS APK through ADB.
# Author    : JuanenRac (Electro Hobby 3D)
# Email     : electrohobby3d@gmail.com
# Copyright : (C) 2026 JuanenRac
# License   : GPL-3.0 - see LICENSE
# *****************************************************************************
# HYDRA_UMC_SCRIPT_STANDARD_HEADER_END
set -euo pipefail

printf '\n*******************************************************************************\n'
printf '%s\n' '* HYDRA-UMC-WATCH - update-from-github.sh'
printf '%s\n' '* Mode      : GITHUB RELEASE > ADB WEAR OS UPDATE'
printf '%s\n' '* Author    : JuanenRac (Electro Hobby 3D)'
printf '%s\n' '* Email     : electrohobby3d@gmail.com'
printf '%s\n' '* Copyright : (C) 2026 JuanenRac'
printf '%s\n' '* License   : GPL-3.0 - see LICENSE'
printf '%s\n' '* ------------------------------------------------------------------------- *'
printf '%s\n' '* 1. Read the latest stable GitHub Release metadata over HTTPS.'
printf '%s\n' '* 2. Compare it with the selected Wear OS device through ADB.'
printf '%s\n' '* 3. Download the exact signed APK and ask before Android installs it.'
printf '%s\n' '*******************************************************************************'
printf '\n'

# This updater is read-only for the repository: it never changes versions,
# manifests, or CHANGELOG.md.
readonly HYDRA_UMC_REPOSITORY='JuanenRac/HYDRA-UMC-WATCH'
readonly HYDRA_UMC_PACKAGE='com.hydraumc.watch'
readonly HYDRA_UMC_ASSET='HYDRA-UMC-WATCH-release.apk'
readonly HYDRA_UMC_API="https://api.github.com/repos/${HYDRA_UMC_REPOSITORY}/releases/latest"
readonly HYDRA_UMC_TEMP_APK="${TMPDIR:-/tmp}/HYDRA-UMC-WATCH-update.apk"

pause_before_exit() {
    local status=$?
    rm -f "$HYDRA_UMC_TEMP_APK"
    if [[ -t 0 && -t 1 ]]; then
        printf '\nPress Enter to close this window...'
        read -r _
    fi
    return "$status"
}
trap pause_before_exit EXIT

# [1/5] Validate all local dependencies before contacting GitHub.
command -v adb >/dev/null || { echo '[X] adb was not found. Install Android Platform Tools first.'; exit 1; }
command -v curl >/dev/null || { echo '[X] curl was not found.'; exit 1; }
command -v python3 >/dev/null || { echo '[X] python3 was not found. It is required for safe version comparison.'; exit 1; }

# [2/5] An explicit ADB_SERIAL avoids accidentally updating a phone or emulator.
if [[ -n "${ADB_SERIAL:-}" ]]; then
    HYDRA_UMC_SERIAL="$ADB_SERIAL"
    [[ "$(adb -s "$HYDRA_UMC_SERIAL" get-state 2>/dev/null)" == 'device' ]] || { echo '[X] ADB_SERIAL is not an authorized online device.'; exit 1; }
else
    mapfile -t HYDRA_UMC_DEVICES < <(adb devices | awk '$2 == "device" { print $1 }')
    [[ ${#HYDRA_UMC_DEVICES[@]} -eq 1 ]] || {
        echo '[X] Connect and authorize exactly one Wear OS device, or set ADB_SERIAL.'
        adb devices
        exit 1
    }
    HYDRA_UMC_SERIAL="${HYDRA_UMC_DEVICES[0]}"
fi
printf '[OK] Wear OS ADB target: %s\n' "$HYDRA_UMC_SERIAL"

# [3/5] Fetch JSON and accept only a stable semver tag with the exact asset.
HYDRA_UMC_RELEASE_JSON="$(curl --fail --location --retry 3 --silent --show-error \
    -H 'Accept: application/vnd.github+json' \
    -H 'User-Agent: HYDRA-UMC-WATCH-updater' \
    "$HYDRA_UMC_API")"
readarray -t HYDRA_UMC_RELEASE < <(python3 -c '
import json, re, sys
r=json.load(sys.stdin)
if r.get("draft") or r.get("prerelease"): raise SystemExit(1)
tag=r.get("tag_name", "")
if not re.fullmatch(r"v?\d+\.\d+\.\d+", tag): raise SystemExit(1)
asset=next((a for a in r.get("assets", []) if a.get("name") == "HYDRA-UMC-WATCH-release.apk"), None)
if not asset or not asset.get("browser_download_url", "").startswith("https://"): raise SystemExit(1)
print(tag)
print(asset["browser_download_url"])
' <<<"$HYDRA_UMC_RELEASE_JSON") || { echo "[X] No valid stable GitHub Release with $HYDRA_UMC_ASSET was found."; exit 1; }
HYDRA_UMC_REMOTE_TAG="${HYDRA_UMC_RELEASE[0]}"
HYDRA_UMC_DOWNLOAD_URL="${HYDRA_UMC_RELEASE[1]}"

HYDRA_UMC_INSTALLED_VERSION="$(adb -s "$HYDRA_UMC_SERIAL" shell dumpsys package "$HYDRA_UMC_PACKAGE" | sed -n 's/^versionName=//p' | tr -d '\r' | head -n 1)"
[[ -n "$HYDRA_UMC_INSTALLED_VERSION" ]] || {
    echo "[X] $HYDRA_UMC_PACKAGE is not installed on the selected device."
    echo '    Install an initial trusted APK with run.sh or adb install first.'
    exit 1
}
if ! python3 -c 'import re,sys; p=lambda v: tuple(map(int,re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)",v.strip()).groups())) if re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)",v.strip()) else None; old,new=p(sys.argv[1]),p(sys.argv[2]); raise SystemExit(0 if old and new and new>old else 1)' "$HYDRA_UMC_INSTALLED_VERSION" "$HYDRA_UMC_REMOTE_TAG"; then
    printf '[OK] Watch is already current or the release tag is invalid.\n     Installed: v%s  Latest: %s\n' "$HYDRA_UMC_INSTALLED_VERSION" "$HYDRA_UMC_REMOTE_TAG"
    exit 0
fi

printf '\n[4/5] Update available: v%s -> %s\n' "$HYDRA_UMC_INSTALLED_VERSION" "$HYDRA_UMC_REMOTE_TAG"
if [[ -t 0 ]]; then
    read -r -p 'Download and install this signed APK on the watch? [y/N]: ' HYDRA_UMC_CONFIRM
    [[ "$HYDRA_UMC_CONFIRM" =~ ^[Yy]$ ]] || { echo '[INFO] Update cancelled by operator. No APK was downloaded.'; exit 0; }
else
    echo '[X] Refusing unattended update: run interactively so an operator can confirm.'
    exit 1
fi

# [5/5] ADB delegates signature/package validation to Android on the watch.
curl --fail --location --retry 3 --silent --show-error --output "$HYDRA_UMC_TEMP_APK" "$HYDRA_UMC_DOWNLOAD_URL"
adb -s "$HYDRA_UMC_SERIAL" install -r "$HYDRA_UMC_TEMP_APK"
adb -s "$HYDRA_UMC_SERIAL" shell am start -n "$HYDRA_UMC_PACKAGE/.MainActivity" >/dev/null 2>&1 || true
printf '[SUCCESS] HYDRA-UMC-WATCH updated to %s.\n' "$HYDRA_UMC_REMOTE_TAG"
