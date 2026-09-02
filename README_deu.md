<p align="center">
  <img src="images/HYDRA_UMC_BANNER.svg" alt="HYDRA-UMC-WATCH banner" width="100%">
</p>

# ⌚ HYDRA-UMC-WATCH

<p align="center"><a href="README.md">🇺🇸 English</a> | <a href="README_spa.md">🇪🇸 Español</a> | <a href="README_fra.md">🇫🇷 Français</a> | <a href="README_ita.md">🇮🇹 Italiano</a> | 🇩🇪 <b>Deutsch</b> | <a href="README_zho.md">🇨🇳 简体中文</a> | <a href="README_jpn.md">🇯🇵 日本語</a></p>

### 🛡️ Tragbares Sicherheits-Dashboard & Haptisches Notfall-Alarmsystem

<p align="left">
  <img src="https://img.shields.io/badge/Licencia-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Platform-Wear%20OS-4285F4.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Feature-Wireless%20E--STOP-red.svg" alt="E-STOP">
</p>

---

## 1. 🛠️ TECHNISCHER ÜBERBLICK

**HYDRA-UMC-WATCH** ist die taktische Erweiterung für den Anlagenbediener. Sie liefert kritische, auf einen Blick erfassbare Informationen und Sicherheitskontrollen direkt am Handgelenk und stellt sicher, dass der Bediener stets die Kontrolle behält, selbst fernab der Haupt-HMI.

Gebaut als eigenständige Wear-OS-App (Kotlin + Jetpack Compose für Wear), die dieselbe Gradle/Kotlin-Toolchain wie das Schwester-Repository HYDRA-UMC-ANDROID-CONTROL wiederverwendet, statt eine neue einzuführen.

### Hauptmerkmale:
* ✅ **Echtes v0 - haptische Muster & Sync-Protokoll:** `haptics/HapticPatterns.kt` definiert ein echtes, eigenständiges Vibrationsmuster pro Alarmschweregrad (Kritisch/Warnung/Info); `protocol/SyncMessage.kt` definiert und (de)serialisiert die echten `EStopCommand`/`Alert`-Nachrichtenformen für den SERVER<->WATCH-Sync-Ablauf unten. Beides ist reines, testbares Kotlin - keine Uhr-Hardware, kein Emulator, kein offener WebSocket nötig, um es auszuführen oder zu testen.
* 🛑 **Kabelloser E-STOP** — dedizierter Notfallknopf mit unter 50ms Latenz über industrielles WLAN. *(die `EStopCommand`-Nachricht, die er senden würde, ist echt und getestet; der WebSocket-Transport und die physische Knopf-Verdrahtung bleiben geplant — benötigt Kopplung mit HYDRA-UMC-SERVER.)*
* 📳 **Haptische Alarme** — unterschiedliche Vibrationsmuster für verschiedene Alarmtypen (Kritisch, Warnung, Info), abgespielt über Androids echten `Vibrator`/`VibratorManager`-Dienst. *(implementiert - `haptics/HapticAlertPlayer.kt`; noch unverifiziert auf einem echten Wear-OS-Gerät, wie der Rest dieser App.)*
* 🎙️ **Sprache** — Tap-to-Talk: explizite `RECORD_AUDIO`-Berechtigungsanfrage, das System-Spracherkennungs-Intent (`RecognizerIntent`) für die Transkription, die Transkription wird als begrenzte `voice_turn`-Sync-Nachricht an HYDRA-UMC-ANDROID-CONTROL weitergeleitet, und eine lokale `TextToSpeech`-Antwort auf der Uhr. *(implementiert - `MainActivity.kt`; Sprache kann niemals direkt einen Roboter ansteuern, siehe Architektur unten.)*
* ⌚ **Statusübersicht auf einen Blick** — Echtzeit-Zusammenfassung der Flottenaktivität und des Missionsfortschritts. *(geplant - benötigt die echte WebSocket-Verbindung.)*
* 🔐 **Sichere Authentifizierung** — JWT-basierte Kopplung mit HYDRA-UMC-SERVER. *(geplant.)*
* ✅ **Eigenständige Wear-OS-Toolchain** — eine echte Gradle/Kotlin/Compose-for-Wear-App, die eine funktionierende Debug-APK baut. *(implementiert — siehe BUILD & AUSFÜHRUNG unten)*
* 🔁 **Relay-Wiederverbindungsrichtlinie** — `transport/RelayRetryPolicy.kt` ist eine echte, reine Exponential-Backoff-Richtlinie für einen fehlgeschlagenen Relay-Sendevorgang (z. B. noch kein gekoppeltes Telefon), begrenzt auf eine maximale Verzögerung. *(implementiert)*
* 🗂️ **Cache des zuletzt bekannten Zustands** — `transport/LastKnownStateCache.kt` verfolgt die echte Veralterung des zuletzt weitergeleiteten Status/Alarms, sodass ein alter niemals als aktuell angezeigt wird. *(implementiert)*

---

## 2. 🔄 WEARABLE-SYNC-ABLAUF

```mermaid
flowchart LR
    SERVER["HYDRA-UMC-SERVER"] --> WS["WebSocket-Synchronisation"]
    WS --> WATCH["HYDRA-UMC-WATCH"]
    WATCH -- E-STOP-Befehl --> SERVER
    SERVER -- Kritischer Alarm --> WATCH
    WATCH -- Haptisches Feedback --> OPERATOR["Anlagenbediener"]
```

---

## 3. 🧱 ARCHITEKTUR & DESIGNENTSCHEIDUNGEN

* **Warum es eine eigenständige Wear-OS-App ist, kein Feature der Telefon-App.** Eine Uhr läuft als eigener unabhängiger Prozess auf ihrem eigenen Betriebssystem - sie kann nicht einfach ein UI-Modus von HYDRA-UMC-ANDROID-CONTROL sein, sie braucht ihr eigenes Manifest, ihren eigenen Build und ihre eigene (viel eingeschränktere) UI für Status auf einen Blick/schnellen E-STOP.
* **Warum `minSdk 30` (Wear OS 3), niedriger als der eigene minSdk der Telefon-App.** Dies zielt bewusst auf die aktuelle Wear-OS-3+-Hardware-Generation, nicht auf alte Wear-OS-2-Geräte - anders als HYDRA-UMC-ANDROID-CONTROL, das ältere Telefone unterstützt, hat eine Begleituhr-App eine deutlich engere realistische Hardware-Basis zu unterstützen.
* **Warum haptische Muster und das Sync-Protokoll vor der WebSocket-Verbindung kommen.** Die Vibrationswellenformen und die Nachrichtenformen zu definieren, auf die sich beide Seiten einigen müssen, ist echte, reine Kotlin-Arbeit - dafür braucht es weder einen offenen Socket noch einen gekoppelten Server noch eine physische Uhr zum Schreiben oder Testen. Diese Verbindung tatsächlich zu öffnen, ist der nächste Schritt.
* **Warum Wiederverbindungsrichtlinie und Cache des zuletzt bekannten Zustands eigene reine Module sind, nicht inline in `WatchRelayTransport`.** Beide sind echte, entkoppelte Kotlin-Klassen (`RelayRetryPolicy`, `LastKnownStateCache`), testbar auf einer einfachen JVM ohne Uhr, Emulator oder gekoppeltes Telefon - derselbe Standard, den `HapticPatterns.kt`/`SyncMessage.kt` bereits gesetzt haben. `WatchRelayTransport` selbst bleibt das schlanke, notwendigerweise Android-spezifische Stück, das tatsächlich einen erneuten Versuch plant oder eine empfangene Nachricht aufzeichnet, nicht der Ort, an dem die zugrunde liegende Richtlinienlogik lebt.
* **Warum ein veralteter zwischengespeicherter Zustand markiert, nicht verborgen wird.** Einen alten Status vollständig zurückzuhalten würde das Ziffernblatt der Uhr genau dann leer lassen, wenn die Telefonverbindung instabil ist - der eigentliche Risikomoment. Ihn klar markiert als "zuletzt bekannt - könnte veraltet sein" anzuzeigen, hält den Bediener informiert, ohne eine minutenalte Ablesung als aktuell durchgehen zu lassen.
* **Wie sich das ins restliche Ökosystem einfügt.** Bildet ein Paar mit HYDRA-UMC-ANDROID-CONTROL und HYDRA-UMC-IOS-CONTROL als Begleiter am Handgelenk, auf einen Blick - kein Ersatz für eines von beiden, sondern eine Oberfläche für schnellen Status/schnellen E-STOP.

---

## 📂 VERZEICHNISSTRUKTUR

Eigenständige Wear-OS-App — ohne eigene Hardware, Firmware oder Betriebssystem (läuft auf handelsüblicher Uhren-Hardware); diese Ordner werden gemäß der Repository-Strukturpolitik ausgelassen.

```text
HYDRA-UMC-WATCH/
├── app/
│   ├── build.gradle.kts       # App-Modul-Konfiguration (liest version.properties, schreibt es nie)
│   ├── version.properties     # versionMajor/Minor/Patch/Code (nur von build.sh/.bat erhöht)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/hydraumc/watch/
│       │       ├── MainActivity.kt         # Compose-for-Wear-Einstiegspunkt
│       │       ├── haptics/                # Vibrationsmuster pro Schweregrad
│       │       ├── protocol/               # SERVER<->WATCH-Sync-Nachrichten-Codec
│       │       └── transport/              # Data-Layer-Relay, Retry-Richtlinie, Cache des zuletzt bekannten Zustands
│       └── test/java/com/hydraumc/watch/   # Echte JUnit-Tests (haptics, protocol, transport)
├── gradle/
│   ├── libs.versions.toml     # Abhängigkeits-Versionskatalog
│   └── wrapper/                # Gradle-Wrapper (fixiert auf 9.7.0)
├── build.gradle.kts           # Gradle-Root-Build
├── settings.gradle.kts        # Modul-Verdrahtung
├── gradlew / gradlew.bat      # Gradle-Wrapper-Launcher
├── docs/                      # Dokumentation und Sicherheitsprotokolle
├── build/                     # Reserviert (Gradles eigenes app/build/ ist von git ignoriert)
├── images/                    # Medien und Diagramme
├── bump_manifest_version.py   # Erhöht major/minor/patch + das Manifest, gemeinsam
├── bump_version_code.py       # Erhöht den eigenen versionCode-Zähler von Android
├── build.sh / build.bat       # Echter Build: erhöht Version, führt Tests aus, assembleDebug
├── run.sh / run.bat           # Echte Ausführung: gradlew installDebug + adb-Start
└── src/                       # Reserviert (der Code dieses Projekts liegt unter app/src/)
```

---

## 4. ⚙️ BUILD & AUSFÜHRUNG

Erfordert JDK 21, das Android SDK (`local.properties` → `sdk.dir`, von git ignoriert — auf die eigene SDK-Installation verweisen) und ein Wear-OS-Gerät oder einen Emulator für `run`.

```bash
# Linux/macOS
chmod +x gradlew   # einmalig
./build.sh
./run.sh            # benötigt ein verbundenes Wear-OS-Gerät/Emulator

# Windows
build.bat
run.bat
```

`build` erhöht die Version (`bump_manifest_version.py` für major/minor/patch im Gleichschritt mit `hydra-umc.project.json`, `bump_version_code.py` für Androids eigenen `versionCode`), führt die echte JUnit-Test-Suite aus (`haptics`, `protocol`) und kompiliert die Debug-APK nach `app/build/outputs/apk/debug/app-debug.apk` - alles in einem einzigen Gradle-Aufruf mit `-PhydraUmcReadOnly=true`, damit der versionslesende Code in `app/build.gradle.kts` sie niemals *auch* erhöht (dasselbe Flag verwenden `build-test.sh`/`.bat` für ihre separate, nicht mutierende, reine Compile-CI-Prüfung). `run` installiert die APK über `gradlew installDebug` und startet `MainActivity` mit `adb`.

Echtes Beispiel - die beiden Versions-Erhöhungsskripte laufen auch eigenständig, nützlich um zu prüfen, was ein Build tun würde, ohne Gradle auszulösen:

```bash
python3 bump_manifest_version.py   # z.B. "HYDRA-UMC version: v0.1.2 -> v0.1.3"
python3 bump_version_code.py       # z.B. "versionCode: 12 -> 13"
```

---

## ✅ Aktueller Status & Nächste Schritte

**Heute real:** lokale Alarmwiedergabe über den Android-Dienst `Vibrator`, explizite Mikrofonberechtigung und System-Spracherkennung, sichtbares Transkript und lokale Sprachausgabe sowie getestete typisierte Nachrichten für `voice_turn`, `assistant_reply`, `system_status`, `EStopCommand`, `Alert` und den Versionsstatus des Begleitgeräts. Der offizielle Wear-OS-Data-Layer-Transport leitet begrenzte Sprachanfragen und Statuskarten über HYDRA-UMC-ANDROID-CONTROL an den authentifizierten Server und Voice UI weiter.

**Integrationsgrenze:** die gekoppelte Android-App bewahrt das verschlüsselte Server-JWT auf; Server bewahrt das Voice-UI-Token auf. Data Layer verlangt denselben Paketnamen und dasselbe Signaturzertifikat in beiden APKs. Sprache kann nie einen Roboter direkt betätigen; eine bewegungsbezogene Antwort muss eine Bestätigung verlangen, während der physische E-STOP unabhängig bleibt.

**Noch offen:** Validierung von Kopplung, Funktransport, Mikrofon/Lautsprecher und Ende-zu-Ende-Status auf einem echten Wear-OS-Gerät; drahtloser E-STOP und CM5-Livetelemetrie bleiben getrennte hardwareabhängige Arbeit.

---

## 🔗 Verwandte Projekte

Dieses Projekt ist Teil eines größeren Robotik-Ökosystems desselben Autors (JuanenRac / Electro Hobby 3D), das Firmware, Steuerungssoftware, KI-Knoten und Flotten-Tools umfasst. Gut zu wissen, denn eine Anfrage könnte tatsächlich eines dieser Projekte betreffen statt dieses Repository.

### Direkte Beziehung

- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — die App, mit der dieses Wearable gekoppelt wird.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — die App, mit der dieses Wearable gekoppelt wird.

### Restliches Ökosystem

**HYDRA-UMC-Plattform** — die Multi-Roboter-Mikrofabrikzelle
- **[HYDRA-UMC](https://github.com/JuanenRac/HYDRA-UMC)** — das CM5 + STM32H745-Motherboard, das bis zu 8 Roboterarme orchestriert.
- **[HYDRA-UMC-SERVER](https://github.com/JuanenRac/HYDRA-UMC-SERVER)** — das Express/WebSocket-Backend, mit dem jeder Steuerungsclient spricht.
- **[HYDRA-UMC-STUDIO](https://github.com/JuanenRac/HYDRA-UMC-STUDIO)** — webbasiertes Steuerungs-Dashboard, Multi-Roboter-3D-Visualisierung.
- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — Android-Steuerungs-App über Wi-Fi/Bluetooth.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — iOS/iPadOS-Steuerungs-App, gebaut in Flutter.
- **[HYDRA-UMC-SUITE](https://github.com/JuanenRac/HYDRA-UMC-SUITE)** — Desktop-Schwarm-Kommandozentrale (Python/PySide6).
- **[HYDRA-UMC-EDITOR-URDF](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-URDF)** — Desktop-URDF-Modelleditor für den Roboterkatalog.
- **[HYDRA-UMC-DSI](https://github.com/JuanenRac/HYDRA-UMC-DSI)** — native Touch-UI für den eingebauten DSI-Touchscreen.

**URTC-Plattform** — der Werkzeugkopf-Controller, den jeder HYDRA-UMC-Roboterarm trägt
- **[URTC](https://github.com/JuanenRac/URTC)** — CAN-Bus-Werkzeugkopf-Controller, 25 Werkzeugprofile.
- **[URTC-FLASHER](https://github.com/JuanenRac/URTC-FLASHER)** — Desktop-Tool für CAN-OTA + SWD/JTAG-Flashing.
- **[URTC-TESTER](https://github.com/JuanenRac/URTC-TESTER)** — Desktop-Tool für Live-CAN-Bus-Diagnose.
- **[URTC-WEB-STUDIO](https://github.com/JuanenRac/URTC-WEB-STUDIO)** — browserbasierte Alternative über die Web-Serial-API.

**🎥 Vision-KI-Knoten (Hailo-8)**
- [HYDRA-UMC-VISION-NODE](https://github.com/JuanenRac/HYDRA-UMC-VISION-NODE)
- [HYDRA-UMC-VISION-STREAMER](https://github.com/JuanenRac/HYDRA-UMC-VISION-STREAMER)
- [HYDRA-UMC-DETECTION-HEF](https://github.com/JuanenRac/HYDRA-UMC-DETECTION-HEF)
- [HYDRA-UMC-SAFETY-ZONES](https://github.com/JuanenRac/HYDRA-UMC-SAFETY-ZONES)
- [HYDRA-UMC-VISUAL-SERVOING-API](https://github.com/JuanenRac/HYDRA-UMC-VISUAL-SERVOING-API)

**🧠 Kognitiver KI-Knoten (Hailo-10)**
- [HYDRA-UMC-COGNITIVE-NODE](https://github.com/JuanenRac/HYDRA-UMC-COGNITIVE-NODE)
- [HYDRA-UMC-VLA-ENGINE](https://github.com/JuanenRac/HYDRA-UMC-VLA-ENGINE)
- [HYDRA-UMC-VOICE-UI](https://github.com/JuanenRac/HYDRA-UMC-VOICE-UI)
- [HYDRA-UMC-SEMANTIC-PLANNER](https://github.com/JuanenRac/HYDRA-UMC-SEMANTIC-PLANNER)
- [HYDRA-UMC-DOCS-QA](https://github.com/JuanenRac/HYDRA-UMC-DOCS-QA)

**🐝 Orchestrierung & Schwarm**
- [HYDRA-UMC-ORCHESTRATOR](https://github.com/JuanenRac/HYDRA-UMC-ORCHESTRATOR)
- [HYDRA-UMC-SWARM-SYNC](https://github.com/JuanenRac/HYDRA-UMC-SWARM-SYNC)
- [HYDRA-UMC-PATH-PLANNER-3D](https://github.com/JuanenRac/HYDRA-UMC-PATH-PLANNER-3D)
- [HYDRA-UMC-JOB-DISPATCHER](https://github.com/JuanenRac/HYDRA-UMC-JOB-DISPATCHER)
- [HYDRA-UMC-NODE-HEALING](https://github.com/JuanenRac/HYDRA-UMC-NODE-HEALING)

**🎮 Digitaler Zwilling & Simulation**
- [HYDRA-UMC-TWIN](https://github.com/JuanenRac/HYDRA-UMC-TWIN)
- [HYDRA-UMC-PHYSICS-REPLICA](https://github.com/JuanenRac/HYDRA-UMC-PHYSICS-REPLICA)
- [HYDRA-UMC-HIL-BRIDGE](https://github.com/JuanenRac/HYDRA-UMC-HIL-BRIDGE)
- [HYDRA-UMC-SYNTHETIC-DATA-GEN](https://github.com/JuanenRac/HYDRA-UMC-SYNTHETIC-DATA-GEN)

**📊 Daten & Analytik**
- [HYDRA-UMC-DATALAKE](https://github.com/JuanenRac/HYDRA-UMC-DATALAKE)
- [HYDRA-UMC-TELEMETRY-COLLECTOR](https://github.com/JuanenRac/HYDRA-UMC-TELEMETRY-COLLECTOR)
- [HYDRA-UMC-ANOMALY-DETECTOR](https://github.com/JuanenRac/HYDRA-UMC-ANOMALY-DETECTOR)
- [HYDRA-UMC-PRODUCTION-REPORTS](https://github.com/JuanenRac/HYDRA-UMC-PRODUCTION-REPORTS)

**🏭 Industrielles Gateway**
- [HYDRA-UMC-GATEWAY-INDUSTRIAL](https://github.com/JuanenRac/HYDRA-UMC-GATEWAY-INDUSTRIAL)
- [HYDRA-UMC-OPCUA-SERVER](https://github.com/JuanenRac/HYDRA-UMC-OPCUA-SERVER)
- [HYDRA-UMC-MQTT-BROKER](https://github.com/JuanenRac/HYDRA-UMC-MQTT-BROKER)
- [HYDRA-UMC-MTCONNECT-ADAPTER](https://github.com/JuanenRac/HYDRA-UMC-MTCONNECT-ADAPTER)

**🛠️ Ergänzende Werkzeuge**
- [URTC-SMART-RACK](https://github.com/JuanenRac/URTC-SMART-RACK)
- [URTC-VISION-TOOL](https://github.com/JuanenRac/URTC-VISION-TOOL)
- [HYDRA-UMC-TOOL-CLI](https://github.com/JuanenRac/HYDRA-UMC-TOOL-CLI)
- [HYDRA-UMC-DASHBOARD-AI](https://github.com/JuanenRac/HYDRA-UMC-DASHBOARD-AI)


## 👤 AUTOR
**JuanenRac** (Electro Hobby 3D)
📧 electrohobby3d@gmail.com
📺 [youtube.com/@electrohobby3d](https://youtube.com/@electrohobby3d)

## 📜 LIZENZ
GPL-3.0 - Siehe LICENSE für Details.
