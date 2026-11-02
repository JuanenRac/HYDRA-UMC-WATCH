# Pushed alerts, operator presence and two-step confirmation

The watch side of three features that need the paired phone (HYDRA-UMC-ANDROID-CONTROL) and, for the last two, the server. The
watch checks every message below with the same parser as the rest of the protocol (`protocol/SyncMessage.kt`); the settings are in
`app/src/main/assets/watch.cfg`. Presence and confirmation are **off by default**.

## 1. Alerts pushed to the wrist (phone -> watch)

* Path `/hydra-umc/alert/v1`, body `{"type":"alert","severity":"CRITICAL|WARNING|INFO","message":"<1-280 characters>"}`.
* An alert is accepted on that path only (not on the assistant path) and only while `alerts.enabled` is on.
* The watch vibrates by severity, shows the alert marked as outdated once it is older than `alerts.stale_after_s`, and posts a
  notification: a critical one stays until opened (`alerts.critical_persistent`), a warning only when no screen exists, an info
  alert never.
* Still to do on the phone: forward the events of the server (robot stopped at an endstop, safety zone, loss of the server
  connection) as this message.

## 2. Presence of the operator (watch -> phone)

* Path `/hydra-umc/presence/v1`, body `{"type":"presence_heartbeat","protocolVersion":1,"sequence":<n>,"intervalSeconds":<1-60>,"state":"PRESENT|LEFT"}`.
* Sent every `presence.interval_s` seconds while the operator has switched the presence on (a foreground service with a
  notification and a stop button); `sequence` rises by one per heartbeat, so a gap can be seen. Ending on purpose sends one last
  heartbeat with `LEFT`, so a deliberate leave is told apart from a loss. Heartbeats are never retried.
* The watch never decides what a lost heartbeat means. Still to do: the phone relays it to the server, and the server decides
  (slow down, pause, nothing) behind its own switch, which needs a formal safety decision first.

## 3. Two-step confirmation of a spoken order

* The assistant reply may carry `confirmationToken` (16-1024 characters of `A-Za-z0-9._~+/=-`) and `confirmationValiditySeconds`
  (1-300) next to `requiresConfirmation`. The token is held only in the memory of the live screen, for at most
  `confirm.max_validity_s`, and is never stored, notified or spoken.
* With `confirm.enabled`, the screen shows a button: the first tap arms it, a second tap not sooner than `confirm.min_gap_ms` and not
  later than `confirm.second_tap_window_s` sends once, never retried, on `/hydra-umc/voice-confirm/v1`:
  `{"type":"voice_confirm","requestId":"<new id>","confirms":"<id of the reply>","confirmationToken":"<token>"}`.
* The answer is expected as an assistant reply with the new `requestId`. The watch cannot move a robot: the phone must have a
  session and the reservation of the robot before it sends the confirmation to the server. Still to do: the relay in the phone and
  the `POST /v1/voice/confirm` round trip in the server.

## 4. Complication

`complication/RobotsComplicationService.kt` reads the last status kept on the watch and shows `robotsRunning` as a number, `!`
while `alarmActive` is true and `?` once the status is older than `complication.stale_after_s`. The phone still has to put the two
optional fields `robotsRunning` (0-10000) and `alarmActive` in the status it sends; until then the complication shows `-`.
