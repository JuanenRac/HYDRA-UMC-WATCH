# Changelog

All notable work on **HYDRA-UMC-WATCH** is summarized here, newest first.
Full session-by-session detail (including dates) lives in a private,
unpublished internal log - this file is public, so it intentionally omits
calendar dates.

## Versioning scheme

`app/version.properties`'s `versionMajor`/`versionMinor`/`versionPatch`
bump automatically on every real Gradle build (the bump logic lives at the
top of `app/build.gradle.kts` itself, so it runs at Gradle *configuration*
time - meaning `assembleDebug`, `installDebug`, or any other real build
task triggers it, same mechanism as sibling repo
HYDRA-UMC-ANDROID-CONTROL). It follows the same base-10 "odometer" rule
used across the ecosystem rather than semantic-versioning judgment calls:

- `versionPatch` +1 on every build
- when `versionPatch` would exceed 9, it resets to 0 and `versionMinor` +1 instead (e.g. `0.0.9` -> `0.1.0`, never `0.0.10`)
- the same carry cascades into `versionMajor` if `versionMinor` would exceed 9

`versionCode` is a separate, simple monotonic counter (+1 always, no
carry) - Android requires it to strictly increase across every build that
ever ships.

---

## [Unreleased] - Chinese and Japanese added to app resources

- New `app/src/main/res/values-zh/strings.xml` and
  `app/src/main/res/values-ja/strings.xml` - full translation of all 3
  string resources (`app_name`, `tagline`, `status_placeholder`). No code
  changes needed - Android resolves the new locale folders automatically
  once they exist, same as sibling repo HYDRA-UMC-ANDROID-CONTROL.
  Verified with a real Python XML parse: 3/3 keys present in `values/`,
  `values-zh/`, and `values-ja/` alike, zero gaps.
- New `README_zho.md` / `README_jpn.md` documentation translations, plus
  the 5 existing README files' language selectors updated to link them.

---

## [0.0.5]

- Build version synchronized with `hydra-umc.project.json` and the repository-native version source.

## [0.0.0] - Initial scaffolding

- **Standalone Wear OS Gradle project** (`settings.gradle.kts`,
  `build.gradle.kts`, `gradle/libs.versions.toml`) - same Gradle 9.7.0 /
  Kotlin 2.2.10 / AGP 9.3.1 toolchain pin as sibling repo
  HYDRA-UMC-ANDROID-CONTROL, reused rather than reinvented.
- **`app/build.gradle.kts`** - real app module: `applicationId
  com.hydraumc.watch`, `minSdk 30` (Wear OS 3 baseline), `compileSdk 36`,
  `targetSdk 35`, the same odometer-style version-bump block as
  ANDROID-CONTROL adapted to this module.
- **`MainActivity.kt`** - a real Jetpack Compose for Wear OS screen
  (`androidx.wear.compose.material` - Wear's own `MaterialTheme` /
  `Scaffold` / `TimeText`, distinct from the handheld
  `androidx.compose.material3` the phone app uses) rendering the app name,
  live `BuildConfig.VERSION_NAME`, and a pairing-status placeholder.
- **`AndroidManifest.xml`** - declares `android.hardware.type.watch` and
  `com.google.android.wearable.standalone` so the app runs on-wrist
  without requiring a paired phone companion to launch.
- **`build.sh` / `build.bat`** - `gradlew assembleDebug`.
- **`run.sh` / `run.bat`** - `gradlew installDebug` + `adb shell am start`
  against a connected device/emulator.
- The real safety-dashboard features described in the README (wireless
  E-STOP, differentiated haptics, live swarm status, JWT pairing) are the
  next milestone - they need a real WebSocket connection to
  HYDRA-UMC-SERVER, tracked in
  `SONNET/HYDRA-UMC-WATCH/mejoras_futuras.txt`.
