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

## [Unreleased]

- **CI compilation** - imported the existing `SyncMessage.toJson` extension
  into `WatchRelayTransport`, so the Data Layer voice-turn payload compiles
  correctly on a clean GitHub Actions runner.

- **Paired phone relay** - `WatchRelayTransport` and
  `WatchRelayListenerService` now use the official Wear OS Data Layer for
  bounded voice turns and system-status cards. Android Control owns the
  encrypted Server session; no Server JWT or Voice UI token reaches Watch.
- **Release-pairing protection** - Watch and Android Control deliberately use
  the same application ID and require the same signing certificate, as
  enforced by Data Layer. Added an ignored release-key configuration template.

- **Voice-ready Watch surface** - explicit microphone permission and system
  speech recognition are available from **Speak to HYDRA-UMC**. Recognised
  text is sent through the paired authenticated gateway; replies/status are
  displayed and can be spoken locally. Voice still cannot actuate a robot.
- **AI/status contract** - `voice_turn`, `assistant_reply` and
  `system_status` now carry bounded text, correlation IDs, status levels and
  confirmation metadata. `docs/VOICE_AI_PROTOCOL.md` defines the safe route
  through HYDRA-UMC-VOICE-UI and the Cognitive Node.
- **Real on-watch haptics** - `HapticAlertPlayer.kt` now sends the existing,
  tested severity waveforms to Wear OS's actual `Vibrator` service. The home
  screen includes a deliberately informational **Test haptic alert** button
  for provisioning checks; it has no server connection and cannot issue an
  E-STOP or any robot command.
- Added the matching Android resources in English, Japanese and Chinese.

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

## [0.1.6]

- **UI localization gap closed** - `app/src/main/res/values-ja` and
  `values-zh` were each missing 5 of the 17 real string keys (`voice_sending`,
  `voice_phone_unavailable`, `status_refresh`, `status_loading`,
  `status_phone_unavailable`), added later to the English base without a
  matching translation pass; both now translate all 17. `values-es`,
  `values-fr`, `values-de`, and `values-it` did not exist at all - the app
  silently fell back to English on those locales despite the rest of the
  HYDRA-UMC ecosystem shipping all 7 languages. Added full real translations
  for all four, bringing HYDRA-UMC-WATCH to the same 7-language coverage as
  every other UI in the ecosystem.

## [0.1.5] - Made Gradle read-only for versioning; build.bat/build.sh are the sole release flow

- **`app/build.gradle.kts`** - previously bumped `version.properties` at
  Gradle CONFIGURATION time on *any* real task (`assembleDebug`,
  `installDebug`, `compileDebugKotlin`, ...), guarded only by
  `-PhydraUmcReadOnly=true`/`HYDRA_UMC_CI=1`. A plain dev build run
  without that flag silently advanced the native version with no
  matching manifest/CHANGELOG update - the exact version-mirror drift
  class this ecosystem's convention exists to prevent. Gradle now only
  *reads* `version.properties`; it never writes it.
- **`build.bat`/`build.sh`** - now the sole source of a real version
  bump: `bump_manifest_version.py` (native version + manifest) and the
  new `bump_version_code.py` (the separate, always-monotonic Android
  `versionCode`) run first, then Gradle runs with
  `-PhydraUmcReadOnly=true` - now purely informational, since Gradle
  itself no longer has bump logic to suppress, but kept so a build
  invoked with the old flag still behaves as expected.
- `CI_VALIDATION=PASS`.

## [0.1.4] - Real relay reconnection policy and last-known-state cache

- **`transport/RelayRetryPolicy.kt`** (new) - a real, pure exponential-backoff
  policy (`delayBeforeAttemptMs()`/`shouldRetry()`) for a relay send, capped
  at a real maximum delay rather than growing unbounded. No Android
  dependency - testable on a plain JVM, same standard as `SyncMessage.kt`/
  `HapticPatterns.kt`.
- **`transport/LastKnownStateCache.kt`** (new) - a real cache of the most
  recently received `AssistantReply`/`SystemStatus`/`Alert`, with real
  staleness tracking (`isStale()`/`ageMs()`) so an old cached state is never
  presented as a current one - the exact risk called out for a relayed
  `Alert` on a since-disconnected watch.
- **`WatchRelayTransport`** now retries a failed send (most commonly: no
  paired phone node connected yet) using `RelayRetryPolicy`'s own delays via
  a real `android.os.Handler`, instead of failing on the first attempt.
- **`MainActivity`** now records every real relayed status/reply into a
  `LastKnownStateCache` (5-minute staleness window) and shows a real
  "last known - may be outdated" indicator once it goes stale, rather than
  leaving an old status looking indistinguishable from a fresh one.
- New `status_stale` string resource, added to `values/`, `values-ja/` and
  `values-zh/`.
- 15 new JUnit tests (`RelayRetryPolicyTest.kt`: 8, `LastKnownStateCacheTest.kt`: 7)
  = 33 total, all passing (`./gradlew testDebugUnitTest`), wired into
  `build.sh`/`build.bat` alongside the existing suite. `assembleDebug` also
  verified real end to end as part of the same build.

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
