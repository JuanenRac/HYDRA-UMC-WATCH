<p align="center">
  <img src="images/HYDRA_UMC_BANNER.svg" alt="HYDRA-UMC-WATCH banner" width="100%">
</p>

# ⌚ HYDRA-UMC-WATCH

<p align="center"><a href="README.md">🇺🇸 English</a> | <a href="README_spa.md">🇪🇸 Español</a> | <a href="README_fra.md">🇫🇷 Français</a> | 🇮🇹 <b>Italiano</b> | <a href="README_deu.md">🇩🇪 Deutsch</a> | <a href="README_zho.md">🇨🇳 简体中文</a> | <a href="README_jpn.md">🇯🇵 日本語</a></p>

### 🛡️ Dashboard di Sicurezza Indossabile e Sistema di Allarme Aptico d'Emergenza

<p align="left">
  <img src="https://img.shields.io/badge/Licencia-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Platform-Wear%20OS-4285F4.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Feature-Wireless%20E--STOP-red.svg" alt="E-STOP">
</p>

---

## 1. 🛠️ PANORAMICA TECNICA

**HYDRA-UMC-WATCH** è l'estensione tattica per l'operatore di impianto. Fornisce informazioni critiche a colpo d'occhio e controlli di sicurezza direttamente al polso, garantendo che l'operatore mantenga sempre il controllo, anche lontano dall'HMI principale.

Costruita come app Wear OS autonoma (Kotlin + Jetpack Compose per Wear), riutilizzando la stessa toolchain Gradle/Kotlin del repository gemello HYDRA-UMC-ANDROID-CONTROL invece di introdurne una nuova.

### Caratteristiche Principali:
* ✅ **Reale v0 - pattern aptici e protocollo di sincronizzazione:** `haptics/HapticPatterns.kt` definisce un pattern di vibrazione reale e distinto per severità di avviso (Critico/Avviso/Info); `protocol/SyncMessage.kt` definisce e (de)serializza le forme reali dei messaggi `EStopCommand`/`Alert` del flusso di sincronizzazione SERVER<->WATCH sotto. Entrambi sono Kotlin puro, testabile - non serve hardware da orologio, emulatore, né un WebSocket aperto per eseguirli o testarli.
* 🛑 **E-STOP Wireless** — pulsante di emergenza dedicato con latenza inferiore a 50ms su Wi-Fi industriale. *(il messaggio `EStopCommand` che invierebbe è reale e testato; il trasporto WebSocket e il collegamento del pulsante fisico restano pianificati — richiede l'accoppiamento con HYDRA-UMC-SERVER.)*
* 📳 **Avvisi Aptici** — pattern di vibrazione differenziati per i vari tipi di avviso (Critico, Avviso, Info). *(i pattern stessi sono reali - vedi sopra; collegarli alla vera chiamata del servizio `Vibrator` resta pianificato.)*
* ⌚ **Stato a Colpo d'Occhio** — riepilogo in tempo reale dell'attivita della flotta e dell'avanzamento delle missioni. *(pianificato - richiede la vera connessione WebSocket.)*
* 🔐 **Autenticazione Sicura** — accoppiamento basato su JWT con HYDRA-UMC-SERVER. *(pianificato.)*
* ✅ **Toolchain Wear OS autonoma** — una vera app Gradle/Kotlin/Compose per Wear che compila un APK di debug funzionante. *(implementato — vedi COMPILAZIONE ED ESECUZIONE sotto)*

---

## 2. 🔄 FLUSSO DI SINCRONIZZAZIONE DEL WEARABLE

```mermaid
flowchart LR
    SERVER["HYDRA-UMC-SERVER"] --> WS["Sincronizzazione WebSocket"]
    WS --> WATCH["HYDRA-UMC-WATCH"]
    WATCH -- Comando E-STOP --> SERVER
    SERVER -- Avviso Critico --> WATCH
    WATCH -- Feedback Aptico --> OPERATOR["Operatore di Impianto"]
```

---

## 3. 🧱 ARCHITETTURA E DECISIONI DI PROGETTAZIONE

* **Perché è un'app Wear OS autonoma, non una funzionalità dell'app telefono.** Un orologio esegue il proprio processo indipendente sul proprio sistema operativo - non può essere semplicemente una modalità UI di HYDRA-UMC-ANDROID-CONTROL, ha bisogno del proprio manifest, della propria build, e della propria UI (molto più limitata) per stato a colpo d'occhio/E-STOP rapido.
* **Perché `minSdk 30` (Wear OS 3), inferiore al minSdk proprio dell'app telefono.** Questo mira deliberatamente all'attuale generazione hardware Wear OS 3+, non ai vecchi dispositivi Wear OS 2 - a differenza di HYDRA-UMC-ANDROID-CONTROL, che supporta telefoni più vecchi, un'app orologio companion ha una base hardware realistica da supportare molto più ristretta.
* **Perché i pattern aptici e il protocollo di sincronizzazione arrivano prima della connessione WebSocket.** Definire le forme d'onda di vibrazione e le forme dei messaggi su cui entrambe le parti devono concordare è vero lavoro in Kotlin puro - non serve un socket aperto, un server accoppiato o un orologio fisico per scriverlo o testarlo. Aprire davvero quella connessione è il prossimo passo.
* **Come si inserisce nel resto dell'ecosistema.** Si accoppia con HYDRA-UMC-ANDROID-CONTROL e HYDRA-UMC-IOS-CONTROL come companion al polso, a colpo d'occhio - non un sostituto di nessuno dei due, una superficie di stato rapido/E-STOP rapido.

---

## 📂 STRUTTURA DELLE DIRECTORY

App Wear OS autonoma — senza hardware, firmware o sistema operativo propri (gira su hardware da orologio disponibile in commercio); tali cartelle sono omesse secondo la politica della struttura del repository.

```text
HYDRA-UMC-WATCH/
├── app/
│   ├── build.gradle.kts       # Config del modulo app (legge version.properties, non lo scrive mai)
│   ├── version.properties     # versionMajor/Minor/Patch/Code (incrementato solo da build.sh/.bat)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/hydraumc/watch/
│       │       ├── MainActivity.kt         # Punto di ingresso Compose per Wear
│       │       ├── haptics/                # Pattern di vibrazione per severità
│       │       └── protocol/               # Codec dei messaggi di sincronizzazione SERVER<->WATCH
│       └── test/java/com/hydraumc/watch/   # Test JUnit reali (haptics, protocol)
├── gradle/
│   ├── libs.versions.toml     # Catalogo delle versioni delle dipendenze
│   └── wrapper/                # Gradle wrapper (fissato a 9.7.0)
├── build.gradle.kts           # Build radice di Gradle
├── settings.gradle.kts        # Cablaggio dei moduli
├── gradlew / gradlew.bat      # Launcher del Gradle wrapper
├── docs/                      # Documentazione e protocolli di sicurezza
├── build/                     # Riservato (l'app/build/ di Gradle stesso è ignorato da git)
├── images/                    # Media e diagrammi
├── scripts/                   # Script di utilità
├── bump_manifest_version.py   # Incrementa major/minor/patch + il manifest, insieme
├── bump_version_code.py       # Incrementa il contatore versionCode proprio di Android
├── build.sh / build.bat       # Build reale: incrementa la versione, esegue i test, assembleDebug
├── run.sh / run.bat           # Esecuzione reale: gradlew installDebug + avvio adb
└── src/                       # Riservato (il codice di questo progetto vive in app/src/)
```

---

## 4. ⚙️ COMPILAZIONE ED ESECUZIONE

Richiede JDK 21, l'Android SDK (`local.properties` → `sdk.dir`, ignorato da git — puntalo alla tua installazione locale dell'SDK) e un dispositivo o emulatore Wear OS per `run`.

```bash
# Linux/macOS
chmod +x gradlew   # una tantum
./build.sh
./run.sh            # richiede un dispositivo/emulatore Wear OS connesso

# Windows
build.bat
run.bat
```

`build` incrementa la versione (`bump_manifest_version.py` per major/minor/patch in sincronia con `hydra-umc.project.json`, `bump_version_code.py` per il `versionCode` proprio di Android), esegue la vera suite di test JUnit (`haptics`, `protocol`), e compila l'APK di debug in `app/build/outputs/apk/debug/app-debug.apk` - tutto in un'unica invocazione Gradle eseguita con `-PhydraUmcReadOnly=true` così che il codice di `app/build.gradle.kts` che legge la versione non la incrementi mai *anche* lui (la stessa bandiera usata da `build-test.sh`/`.bat` per la loro verifica CI separata, solo-compilazione, senza mutazioni). `run` installa l'APK tramite `gradlew installDebug` e avvia `MainActivity` con `adb`.

Esempio reale - i due script di incremento versione funzionano anche in modo autonomo, utile per ispezionare cosa farà una build senza attivare Gradle:

```bash
python3 bump_manifest_version.py   # es. "HYDRA-UMC version: v0.1.2 -> v0.1.3"
python3 bump_version_code.py       # es. "versionCode: 12 -> 13"
```

---

## ✅ Stato Attuale e Prossimi Passi

**Reale oggi:** pattern di vibrazione aptica differenziati per severità di avviso (`haptics/`), e un codec reale e testato di messaggi di sincronizzazione `EStopCommand`/`Alert` (`protocol/`) - le forme di messaggio che la futura connessione WebSocket a HYDRA-UMC-SERVER trasporterà - più la toolchain autonoma Gradle/Kotlin/Compose-per-Wear e 12 test JUnit.

**Ancora da fare:** il vero trasporto WebSocket in sé (apertura della connessione, accoppiamento/autenticazione), il collegamento dei pattern aptici alla vera chiamata del servizio `Vibrator`, e le schermate di stato a colpo d'occhio/E-STOP - tutto richiede un vero HYDRA-UMC-SERVER in esecuzione e/o un vero orologio/emulatore per essere testato end-to-end.

---

## 🔗 Progetti Correlati

Questo progetto fa parte di un ecosistema robotico più ampio dello stesso autore (JuanenRac / Electro Hobby 3D), che copre firmware, software di controllo, nodi IA e strumenti di flotta. Utile saperlo, perché una richiesta potrebbe in realtà riguardare uno di questi progetti anziché questo repository.

### Relazione Diretta

- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — l'app con cui si accoppia questo wearable.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — l'app con cui si accoppia questo wearable.

### Resto dell'Ecosistema

**Piattaforma HYDRA-UMC** — la cella di micro-fabbrica multi-robot
- **[HYDRA-UMC](https://github.com/JuanenRac/HYDRA-UMC)** — la scheda madre CM5 + STM32H745 che orchestra fino a 8 bracci robotici.
- **[HYDRA-UMC-SERVER](https://github.com/JuanenRac/HYDRA-UMC-SERVER)** — il backend Express/WebSocket con cui parla ogni client di controllo.
- **[HYDRA-UMC-STUDIO](https://github.com/JuanenRac/HYDRA-UMC-STUDIO)** — dashboard di controllo web, visualizzazione 3D multi-robot.
- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — app di controllo Android via Wi-Fi/Bluetooth.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — app di controllo iOS/iPadOS costruita in Flutter.
- **[HYDRA-UMC-SUITE](https://github.com/JuanenRac/HYDRA-UMC-SUITE)** — centro di comando sciame desktop (Python/PySide6).
- **[HYDRA-UMC-EDITOR-URDF](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-URDF)** — editor desktop di modelli URDF per il catalogo robot.
- **[HYDRA-UMC-DSI](https://github.com/JuanenRac/HYDRA-UMC-DSI)** — interfaccia touch nativa per lo schermo DSI a bordo.

**Piattaforma URTC** — il controller della testa utensile che ogni braccio HYDRA-UMC porta con sé
- **[URTC](https://github.com/JuanenRac/URTC)** — controller testa utensile su bus CAN, 25 profili utensile.
- **[URTC-FLASHER](https://github.com/JuanenRac/URTC-FLASHER)** — strumento desktop di flashing CAN-OTA + SWD/JTAG.
- **[URTC-TESTER](https://github.com/JuanenRac/URTC-TESTER)** — strumento desktop di diagnostica CAN live.
- **[URTC-WEB-STUDIO](https://github.com/JuanenRac/URTC-WEB-STUDIO)** — alternativa basata su browser via Web Serial API.

**🎥 Vision AI Node (Hailo-8)**
- [HYDRA-UMC-VISION-NODE](https://github.com/JuanenRac/HYDRA-UMC-VISION-NODE)
- [HYDRA-UMC-VISION-STREAMER](https://github.com/JuanenRac/HYDRA-UMC-VISION-STREAMER)
- [HYDRA-UMC-DETECTION-HEF](https://github.com/JuanenRac/HYDRA-UMC-DETECTION-HEF)
- [HYDRA-UMC-SAFETY-ZONES](https://github.com/JuanenRac/HYDRA-UMC-SAFETY-ZONES)
- [HYDRA-UMC-VISUAL-SERVOING-API](https://github.com/JuanenRac/HYDRA-UMC-VISUAL-SERVOING-API)

**🧠 Cognitive AI Node (Hailo-10)**
- [HYDRA-UMC-COGNITIVE-NODE](https://github.com/JuanenRac/HYDRA-UMC-COGNITIVE-NODE)
- [HYDRA-UMC-VLA-ENGINE](https://github.com/JuanenRac/HYDRA-UMC-VLA-ENGINE)
- [HYDRA-UMC-VOICE-UI](https://github.com/JuanenRac/HYDRA-UMC-VOICE-UI)
- [HYDRA-UMC-SEMANTIC-PLANNER](https://github.com/JuanenRac/HYDRA-UMC-SEMANTIC-PLANNER)
- [HYDRA-UMC-DOCS-QA](https://github.com/JuanenRac/HYDRA-UMC-DOCS-QA)

**🐝 Orchestration & Swarm**
- [HYDRA-UMC-ORCHESTRATOR](https://github.com/JuanenRac/HYDRA-UMC-ORCHESTRATOR)
- [HYDRA-UMC-SWARM-SYNC](https://github.com/JuanenRac/HYDRA-UMC-SWARM-SYNC)
- [HYDRA-UMC-PATH-PLANNER-3D](https://github.com/JuanenRac/HYDRA-UMC-PATH-PLANNER-3D)
- [HYDRA-UMC-JOB-DISPATCHER](https://github.com/JuanenRac/HYDRA-UMC-JOB-DISPATCHER)
- [HYDRA-UMC-NODE-HEALING](https://github.com/JuanenRac/HYDRA-UMC-NODE-HEALING)

**🎮 Digital Twin & Simulation**
- [HYDRA-UMC-TWIN](https://github.com/JuanenRac/HYDRA-UMC-TWIN)
- [HYDRA-UMC-PHYSICS-REPLICA](https://github.com/JuanenRac/HYDRA-UMC-PHYSICS-REPLICA)
- [HYDRA-UMC-HIL-BRIDGE](https://github.com/JuanenRac/HYDRA-UMC-HIL-BRIDGE)
- [HYDRA-UMC-SYNTHETIC-DATA-GEN](https://github.com/JuanenRac/HYDRA-UMC-SYNTHETIC-DATA-GEN)

**📊 Data & Analytics**
- [HYDRA-UMC-DATALAKE](https://github.com/JuanenRac/HYDRA-UMC-DATALAKE)
- [HYDRA-UMC-TELEMETRY-COLLECTOR](https://github.com/JuanenRac/HYDRA-UMC-TELEMETRY-COLLECTOR)
- [HYDRA-UMC-ANOMALY-DETECTOR](https://github.com/JuanenRac/HYDRA-UMC-ANOMALY-DETECTOR)
- [HYDRA-UMC-PRODUCTION-REPORTS](https://github.com/JuanenRac/HYDRA-UMC-PRODUCTION-REPORTS)

**🏭 Industrial Gateway**
- [HYDRA-UMC-GATEWAY-INDUSTRIAL](https://github.com/JuanenRac/HYDRA-UMC-GATEWAY-INDUSTRIAL)
- [HYDRA-UMC-OPCUA-SERVER](https://github.com/JuanenRac/HYDRA-UMC-OPCUA-SERVER)
- [HYDRA-UMC-MQTT-BROKER](https://github.com/JuanenRac/HYDRA-UMC-MQTT-BROKER)
- [HYDRA-UMC-MTCONNECT-ADAPTER](https://github.com/JuanenRac/HYDRA-UMC-MTCONNECT-ADAPTER)

**🛠️ Complementary Tools**
- [URTC-SMART-RACK](https://github.com/JuanenRac/URTC-SMART-RACK)
- [URTC-VISION-TOOL](https://github.com/JuanenRac/URTC-VISION-TOOL)
- [HYDRA-UMC-TOOL-CLI](https://github.com/JuanenRac/HYDRA-UMC-TOOL-CLI)
- [HYDRA-UMC-DASHBOARD-AI](https://github.com/JuanenRac/HYDRA-UMC-DASHBOARD-AI)


## 👤 AUTORE
**JuanenRac** (Electro Hobby 3D)
📧 electrohobby3d@gmail.com

## 📜 LICENZA
GPL-3.0 - Vedi LICENSE per i dettagli.

## 🛠️ BUILD & RUN

Usa il controllo di compilazione senza versionamento prima di una compilazione di rilascio:

| Azione | Windows | Linux / macOS |
|---|---|---|
| Controllo di compilazione (senza modificare versione o CHANGELOG) | `build-test.bat` | `./build-test.sh` |
| Esecuzione / sviluppo (se disponibile) | `run*.bat` o `dev*.bat` | `./run*.sh` o `./dev*.sh` |

`build-test.bat` e `build-test.sh` compilano o convalidano lo stack del progetto senza incrementare `hydra-umc.project.json` né modificare `CHANGELOG.md`. Possono creare solo i normali output del compilatore. Gli script esistenti `build*.bat`, `build*.sh`, `run*` e `dev*` mantengono il comportamento specifico di versione o esecuzione; usali quando tale comportamento è necessario.