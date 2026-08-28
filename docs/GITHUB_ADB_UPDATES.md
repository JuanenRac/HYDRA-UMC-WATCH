<!-- =============================================================================
     HYDRA-UMC-WATCH - GitHub and ADB update-channel operation guide
     Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
     GPL-3.0 - see LICENSE
     ============================================================================= -->

# HYDRA-UMC-WATCH updates without Google Play or MDM

This project supports two non-Play distribution paths. Neither path changes
the repository, the version manifest, or the changelog while updating a
device.

## 1. Recommended: GitHub Releases and ADB

This is the repeatable path for development and private hardware. Enable
Developer options and Wireless debugging on the watch, pair it with the PC,
then run one of these scripts from the project root:

```text
update-from-github.bat
./update-from-github.sh
```

On Windows, the batch script also finds `adb.exe` through `ANDROID_SDK_ROOT`,
`ANDROID_HOME`, or Android Studio's standard SDK location when Platform Tools
is not in `PATH`.

The updater refuses ambiguous ADB selections. If more than one device is
connected, identify the watch and set `ADB_SERIAL` first:

```text
adb devices
set ADB_SERIAL=WATCH_SERIAL          # Windows cmd
export ADB_SERIAL=WATCH_SERIAL       # Linux / WSL
```

It then reads `releases/latest` from GitHub through HTTPS, requires a stable
`vMAJOR.MINOR.PATCH` tag, checks the installed version, asks for explicit
operator approval, downloads the APK, and invokes `adb install -r`.

The GitHub Release must contain an APK with exactly this name:

```text
HYDRA-UMC-WATCH-release.apk
```

### Preparing a private GitHub artifact

Run the normal incremental build once so the project version, manifest and
changelog advance together. Then create the release APK in read-only mode and
rename a copy for the GitHub Release asset:

```text
build.bat
set HYDRA_UMC_CI=1
gradlew.bat assembleRelease -PhydraUmcReadOnly=true
copy app\build\outputs\apk\release\app-release.apk HYDRA-UMC-WATCH-release.apk
```

Publish that file in a non-draft, non-prerelease GitHub Release whose tag
matches the generated app version, for example `v0.1.3`. The updater refuses
tags that are not plain stable semantic versions.

Android on the watch remains the authority for package identity and signing
certificate validation. An APK signed with a different key is rejected.

## 2. Manual GitHub installation on the watch

Download the same release APK manually and open it with a package installer on
the watch. This option is intentionally documented as best-effort only: some
Wear OS manufacturers do not expose a file installer or restrict unknown
sources. It is less repeatable than ADB and should not be the normal workflow.

## Mobile-phone role

`HYDRA-UMC-ANDROID-CONTROL` can notify an operator that a watch release exists
once the mobile-to-watch protocol reports the installed version. It cannot
silently install an APK on another Android device. The Wear OS Data Layer is
for application data, not package deployment; installation remains an ADB,
Play, MDM, or on-device user action.

## Signing before wider distribution

The current Gradle release configuration uses the debug signing key and is
appropriate only for local development. Before sending APKs beyond your own
test hardware, configure a protected, backed-up release keystore. All future
updates must preserve the same signing certificate.
