# Contributing to HYDRA-UMC-WATCH 🦾

We welcome contributions to the wearable safety dashboard of the HYDRA-UMC ecosystem.

## Technology Stack
- **Platform**: Kotlin + Jetpack Compose for Wear OS (`androidx.wear.compose.material`), JDK 21, minSdk 30. This is a standalone Wear OS app only - there is no WatchOS/Swift counterpart in this repository.
- **Communication**: the official Wear OS Data Layer (`Wearable.getMessageClient()`/`getNodeClient()`), relaying bounded `kotlinx.serialization`-backed messages (`protocol/SyncMessage.kt`) to the paired HYDRA-UMC-ANDROID-CONTROL app, which holds the real Server JWT. Not a direct WebSocket or JWT session on the Watch itself - see the README's Architecture section for why.
- **UI/UX**: Wear Compose's own `MaterialTheme`/`Scaffold`/`TimeText`, laid out for both round and square watch faces.

## Guidelines
1. **Low-Latency Safety**: once the wireless E-STOP trigger is wired to a real transport, it must always have top priority in the network stack to keep sub-50ms latency.
2. **Haptic Consistency**: use the standardized vibration waveforms defined in `haptics/HapticPatterns.kt` (real Kotlin data, not a JSON asset) for critical alerts.
3. **Power Efficiency**: wearable code must be optimized for battery life - prefer the existing bounded retry/staleness classes (`RelayRetryPolicy`, `LastKnownStateCache`) over ad hoc polling loops.
4. **Ecosystem Parity**: keep this app's real message shapes (`SyncMessage.kt`) in sync with what HYDRA-UMC-ANDROID-CONTROL's own relay service actually sends/expects - `tools/verify_paired_relay_contract.py` checks the Data Layer paths and package/signing contract between the two repos.
