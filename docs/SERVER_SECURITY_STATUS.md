<!-- =============================================================================
HYDRA-UMC-WATCH - The read-only security status of the server
Copyright (C) JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
GPL-3.0-or-later - see LICENSE
============================================================================= -->

# The security status of the server, on the wrist

The watch can show what the options of security of the server and its history of connections say. It is **read only**: the watch has no message that
writes an option, and the sender is the paired phone, which reads the server as an administrator. Nothing in this message can change anything.

## The message

Path of the Data Layer: `/hydra-umc/server-security/v1`. Phone to watch, JSON with the type `server_security_status`:

```json
{"type": "server_security_status", "protocolVersion": 1, "loweredSwitches": ["health.open", "camera.open"], "lockedSwitches": 2, "activeClients": 3, "failedLogins": 1}
```

| Field | Meaning |
|---|---|
| `protocolVersion` | `1`. Any other value is refused. |
| `loweredSwitches` | The `section.key` ids of the options that are on their value that lowers the protection of the server (`health.open`, `connections.recordAddress`, `camera.open`, `websocket.allowQueryToken`, `logging.redactSecrets`, `camera.credentialsViaEnv`, `install.newInstallTls`). At most 16, no repeats, each `letters.letters`. An id this build does not know is shown as it is. Empty: every option is protected. |
| `lockedSwitches` | How many options the environment of the server fixes (0 to 100000). |
| `activeClients` | Clients with an active session in the history of connections (0 to 100000). |
| `failedLogins` | Failed sign-ins in the history (0 to 100000). |

A message longer than 8 KiB, or that breaks any rule above, is dropped. It is accepted only on its own path, and the paths of the assistant and of the
status do not accept it. The watch shows it as lines of text in its own language (it neither speaks nor vibrates: it is a glance, not an alert).

## What is still open

The phone side (HYDRA-UMC-ANDROID-CONTROL) has to read `GET /api/admin/server-config` and `GET /api/connections/clients` as an administrator and send this
message; this repository defines and checks the watch side only. The vectors of this message are not yet in `watch_relay_contract.json`, which both
repositories keep identical, so that the phone repository can add them at the same time as its sender.
