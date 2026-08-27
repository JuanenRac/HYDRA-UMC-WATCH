# Changelog

All notable work on **HYDRA-UMC-WATCH** is summarized here, newest first.
This public changelog records release-relevant work rather than a
session-by-session diary.

## Versioning scheme

`app/version.properties`'s `versionMajor`/`versionMinor`/`versionPatch`
bump automatically on every real build - `build.sh`/`build.bat` are the
single source of that bump (`bump_manifest_version.py`, keeping
`hydra-umc.project.json` in lockstep in the same call), then invoke Gradle
with `-PhydraUmcReadOnly=true` so `app/build.gradle.kts` only *reads*
`version.properties`, never writes it. That flag is also how
`tools/build_test.py`'s compile-only CI check runs Gradle, so the same
mechanism keeps a plain `./gradlew assembleDebug` from ever touching the
version, the manifest, or `CHANGELOG.md` on its own. Follows the same
base-10 "odometer" rule used across the ecosystem rather than
semantic-versioning judgment calls:

- `versionPatch` +1 on every build
- when `versionPatch` would exceed 9, it resets to 0 and `versionMinor` +1 instead (e.g. `0.0.9` -> `0.1.0`, never `0.0.10`)
- the same carry cascades into `versionMajor` if `versionMinor` would exceed 9

`versionCode` is a separate, simple monotonic counter (+1 always, no
carry, bumped by `bump_version_code.py`) - Android requires it to
strictly increase across every build that ever ships.

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

## [0.1.2] - Real v0: haptic alert patterns and the sync-message protocol

- **`app/src/main/java/com/hydraumc/watch/haptics/AlertSeverity.kt`** /
  **`HapticPatterns.kt`** - the "Haptic Alerts" Key Feature: a real,
  differentiated vibration waveform per severity (`CRITICAL`/`WARNING`/`INFO`),
  in the on/off-duration-pair format `VibrationEffect.createWaveform()`
  expects. Plain data, no `Vibrator` API call - testable on a plain JVM,
  no watch hardware needed.
- **`app/src/main/java/com/hydraumc/watch/protocol/SyncMessage.kt`** - the
  message shapes carried over the "WebSocket Sync" arrow in the Wearable
  Sync Flow diagram: a `kotlinx.serialization`-backed sealed class
  (`EStopCommand`, `Alert`) with real JSON encode/decode and explicit,
  readable errors for malformed/unknown messages - independent of
  actually opening the socket.
- 12 real JUnit tests (`HapticPatternsTest.kt`, `SyncMessageTest.kt`),
  wired into `build.sh`/`build.bat` via `./gradlew testDebugUnitTest`.
- Added the `kotlinx-serialization-json` dependency and Kotlin
  serialization Gradle plugin (small, justified addition for the real
  protocol codec above).
- **Fixed a real version-drift bug**: the version bump used to live in two
  places at once - `app/build.gradle.kts` bumped `version.properties`
  itself at Gradle configuration time (so *any* Gradle invocation, not
  just a real build, silently advanced it), while `build.sh`/`build.bat`
  also called `bump_manifest_version.py` separately. Between the two,
  `hydra-umc.project.json` had drifted 2 real builds behind
  `version.properties` with no way to catch up automatically. Fixed by
  making `build.sh`/`build.bat` the single source of the real bump
  (`bump_manifest_version.py` for `versionMajor`/`Minor`/`Patch` in
  lockstep with the manifest, `bump_version_code.py` for `versionCode`),
  then invoking Gradle with `-PhydraUmcReadOnly=true` so its own
  version-bump code path - which still exists, for
  `tools/build_test.py`'s compile-only CI check - stays inert during a
  real build.
- Added the no-autoclose-on-double-click behavior common to the rest of
  the ecosystem's scripts.

Still out of scope: the real WebSocket transport (opening the connection
to HYDRA-UMC-SERVER, pairing/auth), wiring the vibration patterns above
into the actual `Vibrator` service call, and the glanceable status screen
itself - all need a running server and/or a real watch/emulator to
exercise end to end.

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
  HYDRA-UMC-SERVER.
