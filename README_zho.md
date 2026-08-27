<p align="center">
  <img src="images/HYDRA_UMC_BANNER.svg" alt="HYDRA-UMC-WATCH banner" width="100%">
</p>

# ⌚ HYDRA-UMC-WATCH

<p align="center"><a href="README.md">🇺🇸 English</a> | <a href="README_spa.md">🇪🇸 Español</a> | <a href="README_fra.md">🇫🇷 Français</a> | <a href="README_ita.md">🇮🇹 Italiano</a> | <a href="README_deu.md">🇩🇪 Deutsch</a> | 🇨🇳 <b>简体中文</b> | <a href="README_jpn.md">🇯🇵 日本語</a></p>

### 🛡️ 可穿戴安全仪表盘与触觉紧急告警系统

<p align="left">
  <img src="https://img.shields.io/badge/Licencia-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Platform-Wear%20OS-4285F4.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Feature-Wireless%20E--STOP-red.svg" alt="E-STOP">
</p>

---

## 1. 🛠️ 技术概述

**HYDRA-UMC-WATCH** 是面向工厂操作员的战术延伸设备。它直接在手腕上提供
关键的、一目了然的信息和安全控制，确保操作员即使远离主 HMI 也始终掌控
局势。

它是一款独立的 Wear OS 应用（Kotlin + Jetpack Compose for Wear），复用
与兄弟仓库 HYDRA-UMC-ANDROID-CONTROL 相同的 Gradle/Kotlin 工具链，而非
另起炉灶。

### 关键特性：
* 🛑 **无线 E-STOP** —— 通过工业 Wi-Fi 实现亚 50ms 延迟的专用紧急按钮。*（计划中——需要与 HYDRA-UMC-SERVER 配对）*
* 📳 **触觉告警** —— 针对不同告警类型（严重、警告、信息）的差异化振动模式。*（计划中）*
* ⌚ **一目了然的状态：** 集群活动和任务进度的实时摘要。*（计划中）*
* 🔐 **安全认证：** 与 HYDRA-UMC-SERVER 基于 JWT 的配对。*（计划中）*
* ✅ **独立的 Wear OS 工具链** —— 一个真实的 Gradle/Kotlin/Compose-for-Wear 应用，能够构建出可用的调试 APK。*（已实现——见下方"构建与运行"）*

---

## 2. 🔄 可穿戴同步流程

```mermaid
flowchart LR
    SERVER["HYDRA-UMC-SERVER"] --> WS["WebSocket Sync"]
    WS --> WATCH["HYDRA-UMC-WATCH"]
    WATCH -- E-STOP Command --> SERVER
    SERVER -- Critical Alert --> WATCH
    WATCH -- Haptic Feedback --> OPERATOR["Plant Operator"]
```

---

## 3. 🧱 架构与设计决策

* **为何这是一款独立的 Wear OS 应用，而非手机应用的一项功能。** 手表运行在自己独立的操作系统进程上——它不能只是 HYDRA-UMC-ANDROID-CONTROL 的一种 UI 模式，它需要自己的清单文件、自己的构建，以及自己的（约束条件严格得多的）UI，用于一目了然的状态显示/快速 E-STOP。
* **为何 `minSdk 30`（Wear OS 3）低于手机应用自身的 minSdk。** 这是刻意针对当前 Wear OS 3+ 硬件世代，而非旧款 Wear OS 2 设备——与支持较旧手机的 HYDRA-UMC-ANDROID-CONTROL 不同，一款配套手表应用需要支持的现实硬件基础要窄得多。
* **为何入口点今天只打印身份/版本/角色。** 处于脚手架（scaffolding）阶段：证明 `./gradlew assembleDebug` 成功，先于真正的与手机端的配套应用同步逻辑。
* **这如何融入生态系统的其余部分。** 与 HYDRA-UMC-ANDROID-CONTROL 和 HYDRA-UMC-IOS-CONTROL 配对，作为一目了然的手腕端配套设备——它不是取代二者中的任何一个，而是一个快速状态查看/快速 E-STOP 的界面。

---

## 📂 目录结构

独立的 Wear OS 应用——没有自己的硬件/固件/操作系统（它运行在现成的手表
硬件上），已从模板中省略（生态系统统一的省略规则参见
`SONNET/5.PLAN_EJECUCION_32_PROYECTOS_NUEVOS.txt`）。

```text
HYDRA-UMC-WATCH/
├── app/
│   ├── build.gradle.kts       # 应用模块配置，里程表式版本递增
│   ├── version.properties     # versionMajor/Minor/Patch/Code（每次构建自动递增）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/hydraumc/watch/MainActivity.kt   # Compose-for-Wear 入口点
│       └── res/                                        # 字符串、主题、启动器图标
├── gradle/
│   ├── libs.versions.toml     # 依赖版本目录
│   └── wrapper/                # Gradle wrapper（锁定在 9.7.0）
├── build.gradle.kts           # 根 Gradle 构建
├── settings.gradle.kts        # 模块接入
├── gradlew / gradlew.bat      # Gradle wrapper 启动器
├── docs/                      # 文档与安全协议
├── build/                     # 预留（Gradle 自身的 app/build/ 已被 gitignore）
├── images/                    # 媒体与图表
├── scripts/                   # 实用脚本
├── build.sh / build.bat       # 真实构建：gradlew assembleDebug
├── run.sh / run.bat           # 真实运行：gradlew installDebug + adb launch
└── src/                       # 预留（本项目的代码位于 app/src/ 下）
```

---

## 4. ⚙️ 构建与运行

需要 JDK 21、Android SDK（`local.properties` → `sdk.dir`，已被
gitignore——请指向你自己的 SDK 安装路径），以及用于 `run` 的 Wear OS
设备或模拟器。

```bash
# Linux/macOS
chmod +x gradlew   # 仅需一次
./build.sh
./run.sh            # 需要连接的 Wear OS 设备/模拟器

# Windows
build.bat
run.bat
```

`build` 将调试版 APK 编译到 `app/build/outputs/apk/debug/app-debug.apk`。
版本递增（`app/version.properties`）发生在 `app/build.gradle.kts` 自身
内部的 Gradle 配置阶段，因此它会在每次真实构建时自动运行——无需单独的
递增步骤。`run` 通过 `gradlew installDebug` 安装该 APK，并使用 `adb`
启动 `MainActivity`。

---

## 🔗 相关项目

本项目是同一作者（JuanenRac / Electro Hobby 3D）打造的更大规模机器人生态
系统的一部分，涵盖固件、控制软件、AI 节点和车队工具。值得了解，因为某个
需求实际上可能是关于这些项目之一，而非本仓库。

### 直接相关

- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** —— 本可穿戴设备所配对的配套应用。
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** —— 本可穿戴设备所配对的配套应用。

### 生态系统的其余部分

**HYDRA-UMC 平台** —— 多机器人微工厂单元
- **[HYDRA-UMC](https://github.com/JuanenRac/HYDRA-UMC)** —— 协调最多 8 条机械臂的 CM5 + STM32H745 主板。
- **[HYDRA-UMC-SERVER](https://github.com/JuanenRac/HYDRA-UMC-SERVER)** —— 每个控制客户端所对接的 Express/WebSocket 后端。
- **[HYDRA-UMC-STUDIO](https://github.com/JuanenRac/HYDRA-UMC-STUDIO)** —— 基于 Web 的控制仪表盘，多机器人 3D 可视化。
- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** —— 通过 Wi-Fi/蓝牙的 Android 控制应用。
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** —— 基于 Flutter 构建的 iOS/iPadOS 控制应用。
- **[HYDRA-UMC-SUITE](https://github.com/JuanenRac/HYDRA-UMC-SUITE)** —— 桌面端集群指挥中心（Python/PySide6）。
- **[HYDRA-UMC-EDITOR-URDF](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-URDF)** —— 用于机器人目录的桌面端 URDF 模型编辑器。
- **[HYDRA-UMC-DSI](https://github.com/JuanenRac/HYDRA-UMC-DSI)** —— 机载 DSI 触摸屏的原生触控 UI。

**URTC 平台** —— 每台 HYDRA-UMC 机械臂搭载的工具头控制器
- **[URTC](https://github.com/JuanenRac/URTC)** —— CAN 总线工具头控制器，25 种工具配置。
- **[URTC-FLASHER](https://github.com/JuanenRac/URTC-FLASHER)** —— 桌面端 CAN-OTA + SWD/JTAG 刷写工具。
- **[URTC-TESTER](https://github.com/JuanenRac/URTC-TESTER)** —— 桌面端实时 CAN 总线诊断工具。
- **[URTC-WEB-STUDIO](https://github.com/JuanenRac/URTC-WEB-STUDIO)** —— 通过 Web Serial API 的浏览器端替代方案。

**🎥 视觉 AI 节点（Hailo-8）**
- [HYDRA-UMC-VISION-NODE](https://github.com/JuanenRac/HYDRA-UMC-VISION-NODE)
- [HYDRA-UMC-VISION-STREAMER](https://github.com/JuanenRac/HYDRA-UMC-VISION-STREAMER)
- [HYDRA-UMC-DETECTION-HEF](https://github.com/JuanenRac/HYDRA-UMC-DETECTION-HEF)
- [HYDRA-UMC-SAFETY-ZONES](https://github.com/JuanenRac/HYDRA-UMC-SAFETY-ZONES)
- [HYDRA-UMC-VISUAL-SERVOING-API](https://github.com/JuanenRac/HYDRA-UMC-VISUAL-SERVOING-API)

**🧠 认知 AI 节点（Hailo-10）**
- [HYDRA-UMC-COGNITIVE-NODE](https://github.com/JuanenRac/HYDRA-UMC-COGNITIVE-NODE)
- [HYDRA-UMC-VLA-ENGINE](https://github.com/JuanenRac/HYDRA-UMC-VLA-ENGINE)
- [HYDRA-UMC-VOICE-UI](https://github.com/JuanenRac/HYDRA-UMC-VOICE-UI)
- [HYDRA-UMC-SEMANTIC-PLANNER](https://github.com/JuanenRac/HYDRA-UMC-SEMANTIC-PLANNER)
- [HYDRA-UMC-DOCS-QA](https://github.com/JuanenRac/HYDRA-UMC-DOCS-QA)

**🐝 编排与集群**
- [HYDRA-UMC-ORCHESTRATOR](https://github.com/JuanenRac/HYDRA-UMC-ORCHESTRATOR)
- [HYDRA-UMC-SWARM-SYNC](https://github.com/JuanenRac/HYDRA-UMC-SWARM-SYNC)
- [HYDRA-UMC-PATH-PLANNER-3D](https://github.com/JuanenRac/HYDRA-UMC-PATH-PLANNER-3D)
- [HYDRA-UMC-JOB-DISPATCHER](https://github.com/JuanenRac/HYDRA-UMC-JOB-DISPATCHER)
- [HYDRA-UMC-NODE-HEALING](https://github.com/JuanenRac/HYDRA-UMC-NODE-HEALING)

**🎮 数字孪生与仿真**
- [HYDRA-UMC-TWIN](https://github.com/JuanenRac/HYDRA-UMC-TWIN)
- [HYDRA-UMC-PHYSICS-REPLICA](https://github.com/JuanenRac/HYDRA-UMC-PHYSICS-REPLICA)
- [HYDRA-UMC-HIL-BRIDGE](https://github.com/JuanenRac/HYDRA-UMC-HIL-BRIDGE)
- [HYDRA-UMC-SYNTHETIC-DATA-GEN](https://github.com/JuanenRac/HYDRA-UMC-SYNTHETIC-DATA-GEN)

**📊 数据与分析**
- [HYDRA-UMC-DATALAKE](https://github.com/JuanenRac/HYDRA-UMC-DATALAKE)
- [HYDRA-UMC-TELEMETRY-COLLECTOR](https://github.com/JuanenRac/HYDRA-UMC-TELEMETRY-COLLECTOR)
- [HYDRA-UMC-ANOMALY-DETECTOR](https://github.com/JuanenRac/HYDRA-UMC-ANOMALY-DETECTOR)
- [HYDRA-UMC-PRODUCTION-REPORTS](https://github.com/JuanenRac/HYDRA-UMC-PRODUCTION-REPORTS)

**🏭 工业网关**
- [HYDRA-UMC-GATEWAY-INDUSTRIAL](https://github.com/JuanenRac/HYDRA-UMC-GATEWAY-INDUSTRIAL)
- [HYDRA-UMC-OPCUA-SERVER](https://github.com/JuanenRac/HYDRA-UMC-OPCUA-SERVER)
- [HYDRA-UMC-MQTT-BROKER](https://github.com/JuanenRac/HYDRA-UMC-MQTT-BROKER)
- [HYDRA-UMC-MTCONNECT-ADAPTER](https://github.com/JuanenRac/HYDRA-UMC-MTCONNECT-ADAPTER)

**🛠️ 配套工具**
- [URTC-SMART-RACK](https://github.com/JuanenRac/URTC-SMART-RACK)
- [URTC-VISION-TOOL](https://github.com/JuanenRac/URTC-VISION-TOOL)
- [HYDRA-UMC-TOOL-CLI](https://github.com/JuanenRac/HYDRA-UMC-TOOL-CLI)
- [HYDRA-UMC-DASHBOARD-AI](https://github.com/JuanenRac/HYDRA-UMC-DASHBOARD-AI)


## 👤 作者
**JuanenRac**（Electro Hobby 3D）
📧 electrohobby3d@gmail.com

## 📜 许可证
GPL-3.0 —— 详见 LICENSE。

## 关联项目

> Canonical public ecosystem relationship map.

**Direct integrations:**
[HYDRA-UMC-OS](https://github.com/JuanenRac/HYDRA-UMC-OS) · [HYDRA-UMC-SDK](https://github.com/JuanenRac/HYDRA-UMC-SDK) · [HYDRA-UMC-SERVER](https://github.com/JuanenRac/HYDRA-UMC-SERVER) · [URTC](https://github.com/JuanenRac/URTC) · [HYDRA-UMC-ORCHESTRATOR](https://github.com/JuanenRac/HYDRA-UMC-ORCHESTRATOR) · [HYDRA-UMC-JOB-DISPATCHER](https://github.com/JuanenRac/HYDRA-UMC-JOB-DISPATCHER) · [HYDRA-UMC-SWARM-SYNC](https://github.com/JuanenRac/HYDRA-UMC-SWARM-SYNC) · [HYDRA-UMC-NODE-HEALING](https://github.com/JuanenRac/HYDRA-UMC-NODE-HEALING) · [HYDRA-UMC-UPDATER](https://github.com/JuanenRac/HYDRA-UMC-UPDATER)

**Platform and contracts:**
[HYDRA-UMC-OS](https://github.com/JuanenRac/HYDRA-UMC-OS) · [HYDRA-UMC-SDK](https://github.com/JuanenRac/HYDRA-UMC-SDK)

**Rest of the ecosystem:**
All remaining public repositories are grouped by the seven ecosystem layers in the [JuanenRac ecosystem dashboard](https://juanenrac.github.io/JuanenRac/).
