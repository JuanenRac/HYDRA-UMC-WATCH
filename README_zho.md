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
* ✅ **真实 v0 —— 触觉模式与同步协议：** `haptics/HapticPatterns.kt` 为每种告警严重程度（严重/警告/信息）定义了一个真实且各不相同的振动模式；`protocol/SyncMessage.kt` 定义并（反）序列化了下方 SERVER<->WATCH 同步流程中 `EStopCommand`/`Alert` 消息的真实结构。两者都是纯 Kotlin 代码，可测试——运行或测试都不需要手表硬件、模拟器，或者打开的 WebSocket。
* 🛑 **无线 E-STOP** —— 通过工业 Wi-Fi 实现亚 50ms 延迟的专用紧急按钮。*（它会发送的 `EStopCommand` 消息是真实的并且已测试；WebSocket 传输和物理按钮的接线仍是计划中——需要与 HYDRA-UMC-SERVER 配对。）*
* 📳 **触觉告警** —— 针对不同告警类型（严重、警告、信息）的差异化振动模式。*（模式本身是真实的——见上文；将其接入真实的 `Vibrator` 服务调用仍是计划中。）*
* ⌚ **一目了然的状态：** 集群活动和任务进度的实时摘要。*（计划中——需要真实的 WebSocket 连接。）*
* 🔐 **安全认证：** 与 HYDRA-UMC-SERVER 基于 JWT 的配对。*（计划中。）*
* ✅ **独立的 Wear OS 工具链** —— 一个真实的 Gradle/Kotlin/Compose-for-Wear 应用，能够构建出可用的调试 APK。*（已实现——见下方"构建与运行"）*
* 🔁 **中继重连策略** —— `transport/RelayRetryPolicy.kt` 是一个真实的、纯粹的指数退避策略，用于处理失败的中继发送（例如尚未配对手机的情况），并设有一个有上限的最大延迟。*（已实现）*
* 🗂️ **最后已知状态缓存** —— `transport/LastKnownStateCache.kt` 会跟踪最后一次中继的状态/告警的真实过时程度，确保旧数据永远不会被当作最新数据显示。*（已实现）*

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
* **为何触觉模式与同步协议先于 WebSocket 连接落地。** 定义双方必须一致的振动波形和消息结构，是真正的纯 Kotlin 工作——编写和测试都不需要打开的套接字、已配对的服务器，或者物理手表。真正打开那条连接是下一步。
* **为何重连策略和最后已知状态缓存是各自独立的纯模块，而非内嵌在 `WatchRelayTransport` 中。** 两者都是真实的、解耦的 Kotlin 类（`RelayRetryPolicy`、`LastKnownStateCache`），可以在普通 JVM 上测试，无需手表、模拟器或已配对的手机——这与 `HapticPatterns.kt`/`SyncMessage.kt` 已经确立的标准相同。`WatchRelayTransport` 本身仍然是那个精简的、必然依赖 Android 的部分，实际负责调度重试或记录接收到的消息，而不是底层策略运算逻辑所在之处。
* **为何过时的缓存状态会被标记，而不是被隐藏。** 完全隐藏旧状态会导致手表表盘在手机连接不稳定——真正的风险时刻——时恰好一片空白。清晰地标记为"最后已知——可能已过时"能让操作员保持知情，同时不会让几分钟前的读数被误当作实时数据。
* **这如何融入生态系统的其余部分。** 与 HYDRA-UMC-ANDROID-CONTROL 和 HYDRA-UMC-IOS-CONTROL 配对，作为一目了然的手腕端配套设备——它不是取代二者中的任何一个，而是一个快速状态查看/快速 E-STOP 的界面。

---

## 📂 目录结构

独立的 Wear OS 应用——没有自己的硬件/固件/操作系统（它运行在现成的手表
硬件上）；这些目录按照仓库结构策略予以省略。

```text
HYDRA-UMC-WATCH/
├── app/
│   ├── build.gradle.kts       # 应用模块配置（只读取 version.properties，从不写入）
│   ├── version.properties     # versionMajor/Minor/Patch/Code（仅由 build.sh/.bat 递增）
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/hydraumc/watch/
│       │       ├── MainActivity.kt         # Compose-for-Wear 入口点
│       │       ├── haptics/                # 按严重程度划分的振动模式
│       │       ├── protocol/               # SERVER<->WATCH 同步消息编解码器
│       │       └── transport/              # Data Layer 中继、重试策略、最后已知状态缓存
│       └── test/java/com/hydraumc/watch/   # 真实 JUnit 测试（haptics、protocol、transport）
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
├── bump_manifest_version.py   # 同时递增 major/minor/patch 和清单文件
├── bump_version_code.py       # 递增 Android 自身的 versionCode 计数器
├── build.sh / build.bat       # 真实构建：递增版本、运行测试、assembleDebug
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

`build` 会递增版本号（`bump_manifest_version.py` 同步递增 major/minor/patch
与 `hydra-umc.project.json`，`bump_version_code.py` 递增 Android 自身的
`versionCode`），运行真实的 JUnit 测试套件（`haptics`、`protocol`），并将
调试版 APK 编译到 `app/build/outputs/apk/debug/app-debug.apk`——全部在
一次以 `-PhydraUmcReadOnly=true` 执行的 Gradle 调用中完成，这样
`app/build.gradle.kts` 中读取版本号的代码就永远不会*也*去递增它（
`build-test.sh`/`.bat` 的仅编译、不产生变更的 CI 检查也使用同一个标志）。
`run` 通过 `gradlew installDebug` 安装该 APK，并使用 `adb`
启动 `MainActivity`。

真实示例——这两个版本递增脚本也可以独立运行，便于在不触发 Gradle 的情况下检查一次构建会做什么：

```bash
python3 bump_manifest_version.py   # 例如 "HYDRA-UMC version: v0.1.2 -> v0.1.3"
python3 bump_version_code.py       # 例如 "versionCode: 12 -> 13"
```

---

## ✅ 当前状态与后续步骤

**今天的真实进展：** 已通过 Android `Vibrator` 服务播放本地告警，已实现显式
麦克风权限与系统语音识别、可见转写和本地文字转语音；还具有经过测试的
`voice_turn`、`assistant_reply`、`system_status`、`EStopCommand`、`Alert`
及配套版本状态等类型化消息。官方 Wear OS Data Layer 通过 HYDRA-UMC-ANDROID-CONTROL
将受限语音请求和状态卡请求转发到已认证的 Server 与 Voice UI。

**集成边界：** 已配对 Android 应用保留加密的 Server JWT；Server 保留 Voice UI 令牌。
Data Layer 要求两端 APK 使用相同包名和签名证书。语音绝不能直接驱动机器人；涉及运动的回复必须要求确认，物理 E-STOP 保持独立。

**仍待完成：** 在真实 Wear OS 设备上验证配对、无线传输、麦克风/扬声器和端到端状态；无线 E-STOP 与实时 CM5 遥测仍是受硬件限制的独立工作。

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
**JuanenRac** (Electro Hobby 3D)
📧 electrohobby3d@gmail.com
📺 [youtube.com/@electrohobby3d](https://youtube.com/@electrohobby3d)

## 📜 许可证
GPL-3.0 —— 详见 LICENSE。
