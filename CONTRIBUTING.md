# Contributing to HYDRA-UMC-WATCH 🦾

We welcome contributions to the wearable safety dashboard of the HYDRA-UMC ecosystem.

## Technology Stack
- **Android**: Kotlin, Jetpack Compose for Wear OS.
- **iOS**: Swift 6.0, SwiftUI for WatchOS.
- **Communication**: WebSocket (Real-time Sync), JWT Auth.
- **UI/UX**: Material 3 for Wear, Apple Design Guidelines.

## Guidelines
1. **Low-Latency Safety**: The wireless E-STOP trigger must always have top priority in the network stack to ensure sub-50ms latency.
2. **Haptic Consistency**: Use the standardized haptic vibration patterns defined in `assets/haptics.json` for critical alerts.
3. **Power Efficiency**: Wearable apps must be optimized for battery life, using efficient polling and background sync mechanisms.
4. **Cross-Platform**: Ensure that new features are implemented for both WearOS and WatchOS to maintain ecosystem parity.
