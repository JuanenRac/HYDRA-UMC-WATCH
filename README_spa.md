<p align="center">
  <img src="images/HYDRA_UMC_BANNER.svg" alt="HYDRA-UMC-WATCH banner" width="100%">
</p>

# ⌚ HYDRA-UMC-WATCH

<p align="center"><a href="README.md">🇺🇸 English</a> | 🇪🇸 <b>Español</b> | <a href="README_fra.md">🇫🇷 Français</a> | <a href="README_ita.md">🇮🇹 Italiano</a> | <a href="README_deu.md">🇩🇪 Deutsch</a> | <a href="README_zho.md">🇨🇳 简体中文</a> | <a href="README_jpn.md">🇯🇵 日本語</a></p>

### 🛡️ Dashboard de Seguridad Portable y Sistema de Alerta Háptica de Emergencia

<p align="left">
  <img src="https://img.shields.io/badge/Licencia-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Platform-Wear%20OS-4285F4.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Feature-Wireless%20E--STOP-red.svg" alt="E-STOP">
</p>

---

## 1. 🛠️ VISIÓN TÉCNICA GENERAL

**HYDRA-UMC-WATCH** es la extension tactica para el operario de planta. Ofrece informacion critica de un vistazo y controles de seguridad directamente en la muneca, garantizando que el operario siempre tenga el control, incluso lejos del HMI principal.

Construido como una app Wear OS independiente (Kotlin + Jetpack Compose para Wear), reutilizando el mismo toolchain Gradle/Kotlin que el repositorio hermano HYDRA-UMC-ANDROID-CONTROL en vez de introducir uno nuevo.

### Caracteristicas Clave:
* 🛑 **E-STOP Inalambrico** — boton de emergencia dedicado con latencia inferior a 50ms sobre Wi-Fi industrial. *(planeado — necesita emparejamiento con HYDRA-UMC-SERVER)*
* 📳 **Alertas Hapticas** — patrones de vibracion diferenciados para distintos tipos de alerta (Critica, Advertencia, Info). *(planeado)*
* ⌚ **Estado de un Vistazo** — resumen en tiempo real de la actividad de la flota y el progreso de la mision. *(planeado)*
* 🔐 **Autenticacion Segura** — emparejamiento basado en JWT con HYDRA-UMC-SERVER. *(planeado)*
* ✅ **Toolchain Wear OS independiente** — una app real de Gradle/Kotlin/Compose para Wear que compila un APK de depuracion funcional. *(implementado — ver COMPILACIÓN Y EJECUCIÓN abajo)*

---

## 2. 🔄 FLUJO DE SINCRONIZACION DEL WEARABLE

```mermaid
flowchart LR
    SERVER["HYDRA-UMC-SERVER"] --> WS["Sincronizacion WebSocket"]
    WS --> WATCH["HYDRA-UMC-WATCH"]
    WATCH -- Comando E-STOP --> SERVER
    SERVER -- Alerta Critica --> WATCH
    WATCH -- Retroalimentacion Haptica --> OPERATOR["Operario de Planta"]
```

---

## 3. 🧱 ARQUITECTURA Y DECISIONES DE DISEÑO

* **Por qué es una app Wear OS independiente, no una función de la app de teléfono.** Un reloj corre su propio proceso independiente en su propio sistema operativo - no puede ser simplemente un modo de interfaz de HYDRA-UMC-ANDROID-CONTROL, necesita su propio manifiesto, su propia compilación, y su propia interfaz (mucho más limitada) para estado de un vistazo/E-STOP rápido.
* **Por qué `minSdk 30` (Wear OS 3), menor que el propio minSdk de la app de teléfono.** Esto apunta deliberadamente a la generación actual de hardware Wear OS 3+, no a dispositivos Wear OS 2 antiguos - a diferencia de HYDRA-UMC-ANDROID-CONTROL, que soporta teléfonos más viejos, una app de reloj complementaria tiene una base realista de hardware que soportar mucho más estrecha.
* **Por qué el punto de entrada solo imprime identidad/versión/rol hoy.** Etapa de andamiaje: probar que `./gradlew assembleDebug` tiene éxito precede a la lógica real de sincronización con la app complementaria del teléfono.
* **Cómo encaja en el resto del ecosistema.** Hace pareja con HYDRA-UMC-ANDROID-CONTROL y HYDRA-UMC-IOS-CONTROL como complemento de un vistazo, en la muñeca - no un sustituto de ninguna de las dos, sino una superficie de estado rápido/E-STOP rápido.

---

## 📂 ESTRUCTURA DE DIRECTORIOS

App Wear OS independiente — sin hardware/firmware/os propios (corre sobre hardware de reloj comercial estandar), podados de la plantilla (ver `SONNET/5.PLAN_EJECUCION_32_PROYECTOS_NUEVOS.txt` para la regla de poda de todo el ecosistema).

```text
HYDRA-UMC-WATCH/
├── app/
│   ├── build.gradle.kts       # Config del modulo app, incremento de version estilo cuentakilometros
│   ├── version.properties     # versionMajor/Minor/Patch/Code (incrementado en cada build)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/hydraumc/watch/MainActivity.kt   # Punto de entrada Compose para Wear
│       └── res/                                        # Strings, tema, icono de lanzador
├── gradle/
│   ├── libs.versions.toml     # Catalogo de versiones de dependencias
│   └── wrapper/                # Gradle wrapper (fijado a 9.7.0)
├── build.gradle.kts           # Build raiz de Gradle
├── settings.gradle.kts        # Cableado de modulos
├── gradlew / gradlew.bat      # Lanzador del Gradle wrapper
├── docs/                      # Documentacion y protocolos de seguridad
├── build/                     # Reservado (el propio app/build/ de Gradle esta ignorado por git)
├── images/                    # Medios y diagramas
├── scripts/                   # Scripts de utilidad
├── build.sh / build.bat       # Build real: gradlew assembleDebug
├── run.sh / run.bat           # Ejecucion real: gradlew installDebug + lanzamiento adb
└── src/                       # Reservado (el codigo de este proyecto vive en app/src/)
```

---

## 4. ⚙️ COMPILACIÓN Y EJECUCIÓN

Requiere JDK 21, el Android SDK (`local.properties` → `sdk.dir`, ignorado por git — apunta a tu propia instalacion del SDK) y un dispositivo o emulador Wear OS para `run`.

```bash
# Linux/macOS
chmod +x gradlew   # una sola vez
./build.sh
./run.sh            # necesita un dispositivo/emulador Wear OS conectado

# Windows
build.bat
run.bat
```

`build` compila el APK de depuracion en `app/build/outputs/apk/debug/app-debug.apk`. El incremento de version (`app/version.properties`) ocurre dentro del propio `app/build.gradle.kts` en tiempo de configuracion de Gradle, asi que se ejecuta automaticamente en cada build real — sin paso de incremento separado. `run` instala el APK via `gradlew installDebug` y lanza `MainActivity` con `adb`.

---

## 🔗 Proyectos Relacionados

Este proyecto forma parte de un ecosistema de robótica más amplio del mismo autor (JuanenRac / Electro Hobby 3D), que abarca firmware, software de control, nodos de IA y herramientas de flota. Vale la pena conocerlo, ya que una petición podría en realidad ser sobre uno de estos proyectos en vez de sobre este repositorio.

### Relación Directa

- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — la app con la que se empareja este wearable.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — la app con la que se empareja este wearable.

### Resto del Ecosistema

**Plataforma HYDRA-UMC** — la célula de micro-fábrica multi-robot
- **[HYDRA-UMC](https://github.com/JuanenRac/HYDRA-UMC)** — la placa base CM5 + STM32H745 que orquesta hasta 8 brazos robóticos.
- **[HYDRA-UMC-SERVER](https://github.com/JuanenRac/HYDRA-UMC-SERVER)** — el backend Express/WebSocket con el que habla cada cliente de control.
- **[HYDRA-UMC-STUDIO](https://github.com/JuanenRac/HYDRA-UMC-STUDIO)** — panel de control web, visualización 3D multi-robot.
- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — app de control Android por Wi-Fi/Bluetooth.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — app de control iOS/iPadOS construida en Flutter.
- **[HYDRA-UMC-SUITE](https://github.com/JuanenRac/HYDRA-UMC-SUITE)** — centro de mando de enjambre de escritorio (Python/PySide6).
- **[HYDRA-UMC-EDITOR-URDF](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-URDF)** — editor de modelos URDF de escritorio para el catálogo de robots.
- **[HYDRA-UMC-DSI](https://github.com/JuanenRac/HYDRA-UMC-DSI)** — interfaz táctil nativa para la pantalla DSI integrada.

**Plataforma URTC** — el controlador de cabezal de herramienta que lleva cada brazo HYDRA-UMC
- **[URTC](https://github.com/JuanenRac/URTC)** — controlador de cabezal de herramienta CAN, 25 perfiles de herramienta.
- **[URTC-FLASHER](https://github.com/JuanenRac/URTC-FLASHER)** — herramienta de escritorio de flasheo CAN-OTA + SWD/JTAG.
- **[URTC-TESTER](https://github.com/JuanenRac/URTC-TESTER)** — herramienta de escritorio de diagnóstico CAN en vivo.
- **[URTC-WEB-STUDIO](https://github.com/JuanenRac/URTC-WEB-STUDIO)** — alternativa basada en navegador vía Web Serial API.

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


## 👤 AUTOR
**JuanenRac** (Electro Hobby 3D)
📧 electrohobby3d@gmail.com

## 📜 LICENCIA
GPL-3.0 - Ver LICENSE para más detalles.
