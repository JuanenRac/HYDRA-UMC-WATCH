<!-- =============================================================================
HYDRA-UMC-WATCH - Companion version-status protocol
Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
GPL-3.0 - see LICENSE
============================================================================= -->

# Android Control to Watch version status

This document defines the small status message that a future authenticated
Android Control-to-Watch transport will carry. It deliberately carries no APK,
download link, installation command or signing material: Wear OS installation
remains an explicit operator action through the documented ADB channel.

## Wire message

```json
{
  "type": "companion_version_status",
  "protocolVersion": 1,
  "appVersion": "0.2.9",
  "updateAvailable": true
}
```

- `protocolVersion` is the version of this message shape, currently `1`.
- `appVersion` is Android Control's installed stable semantic version.
- `updateAvailable` tells the watch that Android Control found a newer stable
  GitHub Release. It is informational only.

## Safety boundary

The phone app may display update status; it must not silently install an APK
onto the watch. The operator uses `update-from-github.bat` or
`update-from-github.sh` on a trusted computer, where the script compares the
installed Wear OS version and asks for confirmation before calling ADB.
