#!/usr/bin/env python3
# =============================================================================
# HYDRA-UMC-WATCH - Paired phone relay contract verification
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
# GPL-3.0 - see LICENSE
# =============================================================================
"""Statically verify the Watch/Android Data Layer safety boundary.

It is intentionally independent of a physical watch. Pairing, radio and STT
still need hardware evidence, but a source drift must not silently change a
path, package ID or listener declaration before that phase.
"""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
ANDROID = ROOT.parent / "HYDRA-UMC-ANDROID-CONTROL"
APPLICATION_ID = 'applicationId = "com.hydraumc.control"'
PATHS = (
    "/hydra-umc/voice-turn/v1",
    "/hydra-umc/system-status/v1",
    "/hydra-umc/assistant-reply/v1",
    "/hydra-umc/system-status-reply/v1",
)


def fail(message: str) -> None:
    print(f"PAIRED_RELAY_CONTRACT=FAIL {message}", file=sys.stderr)
    raise SystemExit(1)


def read(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        fail(f"cannot read {path}: {exc}")


def require(text: str, fragment: str, description: str) -> None:
    if fragment not in text:
        fail(description)


def main() -> int:
    watch_gradle = read(ROOT / "app" / "build.gradle.kts")
    android_gradle = read(ANDROID / "app" / "build.gradle.kts")
    watch_manifest = read(ROOT / "app" / "src" / "main" / "AndroidManifest.xml")
    android_manifest = read(ANDROID / "app" / "src" / "main" / "AndroidManifest.xml")
    watch_transport = read(ROOT / "app" / "src" / "main" / "java" / "com" / "hydraumc" / "watch" / "transport" / "WatchRelayTransport.kt")
    android_service = read(ANDROID / "app" / "src" / "main" / "java" / "com" / "hydraumc" / "control" / "wear" / "WatchVoiceRelayService.kt")

    require(watch_gradle, APPLICATION_ID, "Watch applicationId must match Android Control")
    require(android_gradle, APPLICATION_ID, "Android Control applicationId changed")
    for text, owner in ((watch_gradle, "Watch"), (android_gradle, "Android Control")):
        require(text, "libs.play.services.wearable", f"{owner} lacks the official Wear Data Layer dependency")
    for text, owner in ((watch_manifest, "Watch"), (android_manifest, "Android Control")):
        require(text, "com.google.android.gms.wearable.MESSAGE_RECEIVED", f"{owner} lacks a Wear message listener")
        require(text, 'android:pathPrefix="/hydra-umc/"', f"{owner} listener path prefix drifted")
    for path in PATHS:
        require(watch_transport, path, f"Watch transport lacks {path}")
        require(android_service, path, f"Android relay lacks {path}")
    require(android_service, "authenticatedClient().postWatchVoiceTurn(turn)", "voice route must use Server relay")
    require(android_service, "authenticatedClient().getWatchSystemStatus()", "status route must use Server")
    if "postRobotCommand" in android_service or "sendAtomicCommand" in android_service:
        fail("Watch relay must not invoke robot control")

    print("PAIRED_RELAY_CONTRACT=PASS package=com.hydraumc.control paths=4 robot_commands=0")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
