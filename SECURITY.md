# Security Policy 🔒 (HYDRA-UMC-WATCH)

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.x.x  | ✅ Yes             |

## Reporting a Vulnerability

**CRITICAL: Do not report safety-critical vulnerabilities through public GitHub issues.**

In a wearable safety device, a security flaw can lead to unauthorized E-STOP triggers or masking of real emergencies. This project's real security surface today is the paired Wear OS Data Layer relay (`WatchRelayTransport`/`WatchRelayListenerService`, `protocol/SyncMessage.kt`) - it depends entirely on Android enforcing a matching package name and signing certificate between this app and HYDRA-UMC-ANDROID-CONTROL, and on `SyncMessage`'s own bounded validation (request-ID pattern, transcript/text length caps) rejecting a malformed or oversized message. WebSocket pairing and JWT auth are relevant to the planned direct Server connection, not to the real transport today. If you discover a vulnerability affecting the **Data Layer package/signature boundary**, **message validation in `SyncMessage.kt`**, a future **WebSocket pairing**, **JWT hijacking**, or **wireless E-STOP bypass**:

1. **Email**: Send a detailed report to `electrohobby3d@gmail.com`.
2. **Impact**: Describe if the bug allows an attacker to remotely stop the factory from a watch, intercept safety alerts, or spoof operator proximity data.
3. **Response**: Initial acknowledgment within 48 hours.

We follow a coordinated disclosure policy to ensure hardware safety before public release.
