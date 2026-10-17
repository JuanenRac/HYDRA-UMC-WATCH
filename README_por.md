<p align="center">
  <img src="images/HYDRA_UMC_BANNER.svg" alt="HYDRA-UMC-WATCH banner" width="100%">
</p>

# ⌚ HYDRA-UMC-WATCH

<p align="center"><a href="README.md">🇺🇸 English</a> | <a href="README_spa.md">🇪🇸 Español</a> | <a href="README_fra.md">🇫🇷 Français</a> | <a href="README_ita.md">🇮🇹 Italiano</a> | <a href="README_deu.md">🇩🇪 Deutsch</a> | <a href="README_zho.md">🇨🇳 简体中文</a> | <a href="README_jpn.md">🇯🇵 日本語</a> | 🇵🇹 <b>Português</b></p>

### 🛡️ Painel de segurança para vestir e sistema de alerta háptico de emergência

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Platform-Wear%20OS-4285F4.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Feature-Wireless%20E--STOP-red.svg" alt="E-STOP">
</p>

**Verificação de honestidade - o que realmente funciona hoje:** os padrões de vibração háptica (`haptics/HapticPatterns.kt`, `HapticAlertPlayer.kt`), o codec de mensagens de sincronização SERVER<->WATCH (`protocol/SyncMessage.kt`), o transporte de relé através do telemóvel emparelhado, com seleção de nó verificado pelo companheiro, repetição com recuo exponencial, encaminhamento de mensagens e acompanhamento da desatualização do último estado conhecido (`transport/WatchRelayTransport.kt`, `RelayRetryPolicy.kt`, `WatchRelayRouting.kt`, `LastKnownStateCache.kt`), e o fluxo de voz de tocar para falar (`MainActivity.kt`) são Kotlin real, apoiado em 64 testes JUnit a passar (`./gradlew test`) que correm numa JVM simples, sem precisar de relógio, emulador nem telemóvel emparelhado. Essa cobertura de testes prova que a lógica de mensagens/políticas/encaminhamento está correta; não prova a aplicação num relógio real - nenhuma das chamadas reais ao `Vibrator` do `HapticAlertPlayer`, o relé da Data Layer nem o fluxo de voz foram verificados em hardware Wear OS físico, apenas através do `assembleDebug` e de testes unitários nesta máquina de desenvolvimento. O botão de E-STOP sem fios, um WebSocket direto Relógio<->Servidor e o emparelhamento com o Servidor baseado em JWT continuam planeados - só a forma da mensagem `EStopCommand` que usariam é real e testada hoje, não o transporte nem a ligação ao hardware. Veja o `CHANGELOG.md` para saber exatamente o que foi entregue até agora e a lista de funcionalidades da secção 1, mais abaixo, para o detalhe real/futuro de cada uma.

---

## 1. 🛠️ VISÃO TÉCNICA GERAL

O **HYDRA-UMC-WATCH** é a extensão tática para o operador da fábrica. Fornece informação crítica, legível de relance, e controlos de segurança diretamente no pulso, garantindo que o operador está sempre no controlo, mesmo longe do HMI principal.

Construído como uma aplicação Wear OS independente (Kotlin + Jetpack Compose para Wear), reutilizando a mesma cadeia de ferramentas Gradle/Kotlin do repositório irmão HYDRA-UMC-ANDROID-CONTROL em vez de introduzir uma nova.

### Funcionalidades principais:
* ✅ **Real v0 - padrões hápticos e protocolo de sincronização:** `haptics/HapticPatterns.kt` define uma forma de onda de vibração real e distinta por gravidade de alerta (Crítico/Aviso/Informação); `protocol/SyncMessage.kt` define e (des)serializa as formas reais das mensagens `EStopCommand`/`Alert` para o fluxo de sincronização SERVER<->WATCH abaixo. Ambos são Kotlin simples e testável - não é preciso hardware de relógio, emulador nem um WebSocket aberto para executar ou testar nenhum deles.
* 🛑 **E-STOP sem fios** — botão de emergência dedicado com latência inferior a 50 ms por Wi-Fi industrial. *(a mensagem `EStopCommand` que enviaria é real e testada; o transporte WebSocket e a ligação do botão físico continuam planeados - precisam do emparelhamento com o HYDRA-UMC-SERVER.)*
* 📳 **Alertas hápticos** — padrões de vibração diferenciados para vários tipos de alerta (Crítico, Aviso, Informação), reproduzidos através do serviço real `Vibrator`/`VibratorManager` do Android. *(implementado - `haptics/HapticAlertPlayer.kt`; ainda por verificar num dispositivo Wear OS real, tal como o resto desta aplicação.)*
* 🎙️ **Voz** — tocar para falar: pedido explícito da permissão `RECORD_AUDIO`, a intenção de reconhecimento de voz do sistema (`RecognizerIntent`) para a transcrição, a transcrição reencaminhada para o HYDRA-UMC-ANDROID-CONTROL como uma mensagem de sincronização `voice_turn` limitada e uma resposta local por `TextToSpeech` no relógio. *(implementado - `MainActivity.kt`; a voz nunca pode acionar diretamente um robô, veja a Arquitetura abaixo e o [docs/VOICE_AI_PROTOCOL.md](docs/VOICE_AI_PROTOCOL.md) para o fluxo completo de mensagens de ponta a ponta e a fronteira de segurança.)*
* ⌚ **Estado de relance** — um botão **Atualizar estado** reencaminha um pedido de estado limitado para o HYDRA-UMC-ANDROID-CONTROL através da Data Layer emparelhada; o cartão `system_status` devolvido é mostrado com um indicador "último conhecido - pode estar desatualizado" quando fica obsoleto. *(implementado - `MainActivity.kt`/`WatchRelayTransport.requestSystemStatus()`; por pedido através do relé do telemóvel, não um socket de envio direto do relógio para o servidor, e ainda por verificar num dispositivo Wear OS real.)*
* 🔐 **Autenticação segura** — emparelhamento com o HYDRA-UMC-SERVER baseado em JWT. *(planeado.)*
* ✅ **Cadeia de ferramentas Wear OS independente** — uma aplicação real Gradle/Kotlin/Compose para Wear que compila um APK de depuração funcional. *(implementado — veja COMPILAR E EXECUTAR abaixo)*
* 🔁 **Política de religação do relé** — `transport/RelayRetryPolicy.kt` é uma política real e pura de recuo exponencial para um envio falhado pelo relé (por exemplo, ainda sem telemóvel emparelhado), limitada a um atraso máximo; uma repetição pendente é cancelada de imediato quando o ecrã se fecha, nunca disparando contra uma atividade já destruída. *(implementado)*
* 🧭 **Seleção de nó verificado pelo companheiro** — o `WatchRelayTransport` envia através de uma consulta real ao `CapabilityClient` (`hydra_umc_phone_companion`, declarada no próprio `res/values/wear.xml` do HYDRA-UMC-ANDROID-CONTROL), nunca por `connectedNodes.firstOrNull()` - um segundo nó ligado sem a aplicação companheira instalada nunca pode ser escolhido por acidente; um nó próximo (alcançável diretamente por Bluetooth) é preferido a um retransmitido pela nuvem quando mais de um se qualifica. *(implementado - `pickCompanionNode()`)*
* 🗂️ **Cache do último estado conhecido** — `transport/LastKnownStateCache.kt` acompanha a desatualização real do último estado/alerta reencaminhado, para que um antigo nunca seja mostrado como atual. *(implementado)*
* 🌐 **Interface em 8 idiomas** — `app/src/main/res/values-xx/strings.xml` para inglês, espanhol, francês, alemão, italiano, japonês, chinês e português, escolhido automaticamente a partir do idioma do sistema do relógio; o `LocalizedStringsTest` verifica que cada ficheiro de idioma tem exatamente as chaves e os especificadores de formato do inglês, pelo que uma cadeia em falta ou um marcador partido faz falhar a compilação em vez de aparecer no pulso. *(implementado)*

---

## 2. 🔄 FLUXO DE SINCRONIZAÇÃO DO DISPOSITIVO VESTÍVEL

```mermaid
flowchart LR
    SERVER["HYDRA-UMC-SERVER"] --> WS["WebSocket Sync"]
    WS --> WATCH["HYDRA-UMC-WATCH"]
    WATCH -- E-STOP Command --> SERVER
    SERVER -- Critical Alert --> WATCH
    WATCH -- Haptic Feedback --> OPERATOR["Plant Operator"]
```

*Arquitetura de destino quando existir um WebSocket direto com o Servidor. O caminho real e testado hoje
reencaminha antes através do telemóvel emparelhado: Relógio -> Data Layer ->
HYDRA-UMC-ANDROID-CONTROL -> HYDRA-UMC-SERVER/Voice UI autenticado, e de
volta - veja a Arquitetura abaixo e o
[docs/VOICE_AI_PROTOCOL.md](docs/VOICE_AI_PROTOCOL.md) para esse fluxo real
de ponta a ponta. O Relógio nunca guarda ele próprio uma credencial do Servidor; um WebSocket
direto Relógio-Servidor e o botão de E-STOP sem fios continuam a ser trabalho
futuro, dependente de hardware.*

---

## 3. 🧱 ARQUITETURA E DECISÕES DE DESENHO

* **Porque é uma aplicação Wear OS independente e não uma funcionalidade da aplicação do telemóvel.** Um relógio corre o seu próprio processo independente no seu próprio sistema operativo - não pode ser simplesmente um modo de interface do HYDRA-UMC-ANDROID-CONTROL, precisa do seu próprio manifesto, da sua própria compilação e da sua própria interface (muito mais limitada) para o estado de relance/E-STOP rápido.
* **Porque `minSdk 30` (Wear OS 3), inferior ao minSdk da própria aplicação do telemóvel.** Isto visa deliberadamente a geração atual de hardware Wear OS 3+, não dispositivos antigos com Wear OS 2 - ao contrário do HYDRA-UMC-ANDROID-CONTROL, que suporta telemóveis mais antigos, uma aplicação de relógio companheira tem uma base de hardware realista mais estreita para suportar.
* **Porque o relé emparelhado usa a Data Layer em vez de um socket próprio.** O canal oficial do Wear OS impõe o mesmo nome de pacote e o mesmo certificado de assinatura no Relógio e no telemóvel, e transporta apenas mensagens de protocolo limitadas. O Android Control mantém a sessão do Servidor; isto mantém o Relógio fora dos caminhos diretos de credenciais do Servidor e de controlo de robôs.
* **Porque a política de religação e a cache do último estado conhecido são módulos puros próprios, e não código dentro do `WatchRelayTransport`.** Ambos são classes Kotlin reais e desacopladas (`RelayRetryPolicy`, `LastKnownStateCache`), testáveis numa JVM simples sem relógio, emulador nem telemóvel emparelhado - o mesmo padrão que `HapticPatterns.kt`/`SyncMessage.kt` já definiram. O próprio `WatchRelayTransport` continua a ser a peça fina, necessariamente Android, que de facto agenda uma repetição ou regista uma mensagem recebida, e não o local onde vive a matemática da política.
* **Porque um estado em cache desatualizado é assinalado e não escondido.** Retirar por completo um estado antigo deixaria o mostrador do relógio em branco precisamente quando a ligação ao telemóvel é instável - o momento de risco real. Mostrá-lo claramente marcado como "último conhecido - pode estar desatualizado" mantém o operador informado sem deixar que uma leitura com minutos passe por uma em direto.
* **Como isto se encaixa no resto do ecossistema.** Emparelha com o HYDRA-UMC-ANDROID-CONTROL e o HYDRA-UMC-IOS-CONTROL como um companheiro de pulso de relance - não um substituto de nenhum deles, mas uma superfície de estado rápido/E-STOP rápido.

---

## 📂 ESTRUTURA DE DIRETÓRIOS

Aplicação Wear OS independente — sem hardware, firmware nem sistema operativo próprios (corre em hardware de relógio comum); essas pastas são omitidas por política de estrutura do repositório.

```text
HYDRA-UMC-WATCH/
├── app/
│   ├── build.gradle.kts       # Configuração do módulo da aplicação (lê version.properties, nunca o escreve)
│   ├── version.properties     # versionMajor/Minor/Patch/Code (subidos apenas pelo build.sh/.bat)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/hydraumc/watch/
│       │       ├── MainActivity.kt         # Ponto de entrada Compose para Wear
│       │       ├── haptics/                # Padrões de vibração por gravidade
│       │       ├── protocol/               # Codec de mensagens de sincronização SERVER<->WATCH
│       │       └── transport/              # Relé pela Data Layer, política de repetição, cache do último estado conhecido
│       └── test/java/com/hydraumc/watch/   # Testes JUnit reais (háptica, protocolo, transporte)
├── gradle/
│   ├── libs.versions.toml     # Catálogo de versões de dependências
│   └── wrapper/                # Gradle wrapper (fixado em 9.7.0)
├── tools/
│   ├── build_test.py          # Verificação de CI sem alterações: verificação de contrato + assembleDebug
│   ├── ci_validate.py         # Validação de manifesto/CHANGELOG/documentação usada pelo CI
│   └── verify_paired_relay_contract.py # Verificação estática da fronteira de segurança da Data Layer Relógio<->Android Control
├── build.gradle.kts           # Compilação Gradle raiz
├── settings.gradle.kts        # Ligação dos módulos
├── gradlew / gradlew.bat      # Lançador do Gradle wrapper
├── docs/                      # Documentação e protocolos de segurança
├── build/                     # Reservado (o app/build/ do próprio Gradle está no gitignore)
├── images/                    # Multimédia e diagramas
├── hydra-umc.project.json     # Manifesto do ecossistema (versão, metadados de compilação/saúde)
├── keystore.properties.example # Modelo da configuração de assinatura de versões, ignorada pelo git
├── bump_manifest_version.py   # Sobe major/minor/patch + o manifesto, em conjunto
├── bump_version_code.py       # Sobe o contador separado versionCode do Android
├── build.sh / build.bat       # Compilação real: sobe a versão, executa os testes, assembleDebug
├── build-test.sh / build-test.bat # Invólucro sem alterações para tools/build_test.py
├── run.sh / run.bat           # Execução real: gradlew installDebug + arranque por adb
├── update-from-github.sh / .bat # Canal de atualização fora da Play: APK da Release do GitHub + `adb install -r`
└── src/                       # Reservado (o código deste projeto vive em app/src/)
```

---

## 4. ⚙️ GUIA DE COMPILAÇÃO E EXECUÇÃO

Requer o JDK 21, o SDK do Android (`local.properties` → `sdk.dir`, ignorado pelo git — aponte-o para a sua própria instalação do SDK) e um dispositivo ou emulador Wear OS para o `run`.

```bash
# Linux/macOS
chmod +x gradlew   # uma só vez
./build.sh
./run.sh            # precisa de um dispositivo/emulador Wear OS ligado

# Windows
build.bat
run.bat
```

O `build` sobe a versão (`bump_manifest_version.py` para major/minor/patch em conjunto com o `hydra-umc.project.json`, `bump_version_code.py` para o próprio `versionCode` do Android), executa a verdadeira suite de testes JUnit (`haptics`, `protocol`) e compila o APK de depuração para `app/build/outputs/apk/debug/app-debug.apk` - tudo numa única invocação do Gradle com `-PhydraUmcReadOnly=true`, para que o próprio código de leitura de versão do `app/build.gradle.kts` nunca suba *também* nada (essa opção é também a forma como o `build-test.sh`/`.bat` executam a sua verificação de CI separada, só de compilação e sem alterações). O `run` instala o APK com `gradlew installDebug` e lança a `MainActivity` com o `adb`.

Exemplo real - os dois scripts de subida de versão também correm isolados, úteis para inspecionar o que uma compilação fará sem acionar o Gradle:

```bash
python3 bump_manifest_version.py   # por exemplo "HYDRA-UMC version: v0.1.2 -> v0.1.3"
python3 bump_version_code.py       # por exemplo "versionCode: 12 -> 13"
```

---

## 5. 📲 ATUALIZAÇÕES SEM GOOGLE PLAY

Este projeto não é distribuído através da Google Play. O caminho de atualização
repetível é um APK de uma Release do GitHub instalado por ADB:

```bash
# a partir da raiz do projeto, com o relógio emparelhado e a depuração sem fios ativada
update-from-github.bat    # Windows
./update-from-github.sh   # Linux / macOS / WSL
```

O script lê o `releases/latest` do GitHub, exige uma etiqueta estável
`vMAJOR.MINOR.PATCH`, verifica a versão já instalada, pede confirmação
explícita ao operador e depois descarrega e executa `adb install -r`. Nunca
toca na versão do repositório, no manifesto nem no `CHANGELOG.md`. Também está documentada
uma instalação manual, no melhor esforço (abrir diretamente o instalador de pacotes
do APK descarregado no relógio) para dispositivos sem um caminho ADB utilizável. Veja
[docs/GITHUB_ADB_UPDATES.md](docs/GITHUB_ADB_UPDATES.md) para o procedimento completo,
o requisito de nomenclatura do APK de release e a configuração de assinatura de versões.

O `HYDRA-UMC-ANDROID-CONTROL` pode informar o operador de que existe uma
versão mais recente do Relógio assim que a mensagem de estado de versão do companheiro
emparelhado estiver ligada — é meramente informativo e nunca pode instalar nada no
próprio relógio. Veja [docs/COMPANION_VERSION_PROTOCOL.md](docs/COMPANION_VERSION_PROTOCOL.md)
para a forma dessa mensagem.

---

## ✅ Estado atual e próximos passos

**Real hoje:** reprodução local de alertas através do serviço `Vibrator` do Android, permissão explícita do microfone e reconhecimento de voz do sistema, retorno visível da transcrição e texto para fala local, além de mensagens tipadas e testadas para `voice_turn`, `assistant_reply`, `system_status`, `EStopCommand`, `Alert` e o estado de versão do companheiro. A Data Layer oficial do Wear OS reencaminha turnos de voz limitados e pedidos de cartão de estado através do HYDRA-UMC-ANDROID-CONTROL para o Servidor autenticado e o Voice UI.

**Fronteira de integração:** a aplicação Android emparelhada mantém o JWT cifrado do Servidor; o Servidor mantém o token do Voice UI. A Data Layer exige o mesmo nome de pacote e o mesmo certificado de assinatura nos dois APKs. A voz nunca pode acionar diretamente um robô; uma resposta relacionada com movimento tem de exigir confirmação, enquanto o E-STOP físico continua independente.

**Ainda por fazer:** validação do emparelhamento, do transporte de rádio, do microfone/altifalante e do estado de ponta a ponta num dispositivo Wear OS real; o E-STOP sem fios e a telemetria em direto do CM5 continuam a ser trabalho separado, dependente de hardware.

---

## 🔗 Projetos relacionados

Este projeto faz parte do ecossistema de robótica HYDRA-UMC do mesmo autor (JuanenRac / Electro Hobby 3D). Vale a pena conhecê-los, porque um pedido pode ser, na verdade, sobre um destes e não sobre este repositório.

**Diretamente relacionados**
- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — aplicação Android nativa de controlo, com início de sessão biométrico e um companheiro Wear OS emparelhado — a aplicação companheira com que este dispositivo vestível se emparelha.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — aplicação de controlo para iOS/iPadOS (Flutter) com sincronização WebSocket em tempo real — a aplicação companheira com que este dispositivo vestível se emparelha.
- **[HYDRA-UMC-VOICE-UI](https://github.com/JuanenRac/HYDRA-UMC-VOICE-UI)** — front-end de voz real (VAD + analisador de intenções) com um relé para o relógio limitado e protegido por confirmação — envia as mensagens voice_turn que este dispositivo vestível apresenta como alertas hápticos.

**Também fazem parte do ecossistema**

*Hardware e plataforma centrais*
- **[HYDRA-UMC](https://github.com/JuanenRac/HYDRA-UMC)** — a placa-mãe física do braço robótico: anfitrião CM5 + STM32H745 de dois núcleos, que orquestra até 8 braços de ferramenta por CAN-OTA/SPI-OTA.
- **[HYDRA-UMC-OS](https://github.com/JuanenRac/HYDRA-UMC-OS)** — camada de produto reproduzível do Raspberry Pi OS para o CM5: agente só de leitura, configuração/perfis validados, aprovisionamento Wi-Fi de primeiro contacto.
- **[HYDRA-UMC-SDK](https://github.com/JuanenRac/HYDRA-UMC-SDK)** — o contrato JSON-Schema partilhado e a fronteira de portas de segurança contra a qual cada bridge valida os seus comandos.
- **[HYDRA-UMC-CONNECTOR-HUB](https://github.com/JuanenRac/HYDRA-UMC-CONNECTOR-HUB)** — registo declarativo de manifestos de adaptador e validador para conectores de máquinas externas; estende a ideia de contrato do SDK a máquinas externas sem substituir os projetos de gateway industrial.

*Backend central e clientes*
- **[HYDRA-UMC-SERVER](https://github.com/JuanenRac/HYDRA-UMC-SERVER)** — o back-end headless real (REST/WebSocket) com que todos os clientes de controlo realmente comunicam.
- **[HYDRA-UMC-STUDIO](https://github.com/JuanenRac/HYDRA-UMC-STUDIO)** — painel web de controlo com visualização 3D em tempo real de vários robôs.
- **[HYDRA-UMC-SUITE](https://github.com/JuanenRac/HYDRA-UMC-SUITE)** — centro de comando de enxame para desktop (PySide6) para vários servidores ao mesmo tempo, empacotado como executável independente.
- **[HYDRA-UMC-DSI](https://github.com/JuanenRac/HYDRA-UMC-DSI)** — interface tátil nativa para o ecrã tátil DSI de 7" integrado, executada no próprio CM5.
- **[HYDRA-UMC-EDITOR-URDF](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-URDF)** — criador/editor gráfico de URDF para desktop que envia os modelos concluídos para o catálogo do STUDIO.
- **[HYDRA-UMC-EDITOR-STL](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-STL)** — editor de modelos STL para desktop que transforma/substitui/remove/adiciona peças reais no mesmo catálogo de modelos que o HYDRA-UMC-EDITOR-URDF também edita.
- **[HYDRA-UMC-BRIDGE-AMR](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-AMR)** — fronteira de coordenação para frotas AGV/AMR através de um publicador MQTT VDA 5050 real.
- **[HYDRA-UMC-BRIDGE-CNC](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-CNC)** — coordenador de células CNC de alto nível com acesso real ao estado GRBL e aos bytes de controlo.
- **[HYDRA-UMC-BRIDGE-DROIDS](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-DROIDS)** — fronteira de coordenação para droides com patas/humanoides, com um emissor real de comandos para o Boston Dynamics Spot.
- **[HYDRA-UMC-BRIDGE-LASER](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-LASER)** — coordenador de segurança de células laser que lê 3 proteções reais por GPIO (chave, caixa e interlock).
- **[HYDRA-UMC-BRIDGE-OPENPNP](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-OPENPNP)** — coordenador seguro de fluxo de placa de alto nível para pick-and-place com OpenPnP.
- **[HYDRA-UMC-BRIDGE-PRINTER3D](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-PRINTER3D)** — fronteira de coordenação segura para impressoras 3D Moonraker/Klipper, com comandos de trabalho reais protegidos por portas.
- **[HYDRA-UMC-BRIDGE-ROS2](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-ROS2)** — coordenador de segurança com um transporte ROS 2 real baseado em rclpy importado sob demanda.
- **[HYDRA-UMC-BRIDGE-UAV](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-UAV)** — fronteira de coordenação para UAV com câmara, com um emissor real de comandos MAVLink.

*Plataforma de ferramentas URTC*
- **[URTC](https://github.com/JuanenRac/URTC)** — firmware da placa física Universal Robot Tool Controller, mais de 25 perfis de ferramenta por barramento CAN.
- **[URTC-FLASHER](https://github.com/JuanenRac/URTC-FLASHER)** — ferramenta gráfica de gravação para desktop das placas URTC, por CAN-OTA e também SWD/JTAG de chip completo.
- **[URTC-TESTER](https://github.com/JuanenRac/URTC-TESTER)** — ferramenta de diagnóstico CAN em tempo real para desktop das placas URTC, com um painel por perfil de ferramenta.
- **[URTC-WEB-STUDIO](https://github.com/JuanenRac/URTC-WEB-STUDIO)** — alternativa no navegador ao URTC-TESTER através da Web Serial API, sem instalação local.

*Nó de IA de visão (Hailo-8)*
- **[HYDRA-UMC-VISION-NODE](https://github.com/JuanenRac/HYDRA-UMC-VISION-NODE)** — centro de integração do pipeline de visão Hailo-8, com uma verificação real de prontidão de hardware por fase.
- **[HYDRA-UMC-DETECTION-HEF](https://github.com/JuanenRac/HYDRA-UMC-DETECTION-HEF)** — registo real de modelos compilados com verificação segura de carga por arquitetura Hailo e soma de verificação.
- **[HYDRA-UMC-VISION-STREAMER](https://github.com/JuanenRac/HYDRA-UMC-VISION-STREAMER)** — pipeline GStreamer real e gerador de configuração MediaMTX com uma fronteira de integração HailoRT real.
- **[HYDRA-UMC-VISUAL-SERVOING-API](https://github.com/JuanenRac/HYDRA-UMC-VISUAL-SERVOING-API)** — lei de correção real de servovisão baseada em posição, protegida pelo estado da zona a montante.
- **[HYDRA-UMC-SAFETY-ZONES](https://github.com/JuanenRac/HYDRA-UMC-SAFETY-ZONES)** — verificação real de violação de zonas e pedido de E-STOP, com exigência de calibração recente.

*Nó de IA cognitiva (Hailo-10)*
- **[HYDRA-UMC-COGNITIVE-NODE](https://github.com/JuanenRac/HYDRA-UMC-COGNITIVE-NODE)** — centro de integração do pipeline cognitivo do Hailo-10 (orquestração de LLM/VLA/voz).
- **[HYDRA-UMC-VLA-ENGINE](https://github.com/JuanenRac/HYDRA-UMC-VLA-ENGINE)** — codificação/descodificação real de tokens de ação e geração de trajetórias para um modelo Visão-Linguagem-Ação.
- **[HYDRA-UMC-SEMANTIC-PLANNER](https://github.com/JuanenRac/HYDRA-UMC-SEMANTIC-PLANNER)** — decomposição real de tarefas baseada em regras e recuperação semântica de erros sobre os códigos de erro do MCU.
- **[HYDRA-UMC-DOCS-QA](https://github.com/JuanenRac/HYDRA-UMC-DOCS-QA)** — pesquisa real de documentos TF-IDF só com a biblioteca padrão sobre a documentação Markdown deste ecossistema.
- **[HYDRA-UMC-LOCAL-TECHNICIAN](https://github.com/JuanenRac/HYDRA-UMC-LOCAL-TECHNICIAN)** — técnico de manutenção de IA local, protegido por políticas, para o próprio ecossistema: observa, diagnostica e propõe correções; os dois níveis de maior risco não estão implementados de propósito.

*Orquestração e enxame*
- **[HYDRA-UMC-ORCHESTRATOR](https://github.com/JuanenRac/HYDRA-UMC-ORCHESTRATOR)** — centro de integração com um contrato real de relatório de saúde gRPC/Protobuf e uma máquina de estados de missões.
- **[HYDRA-UMC-JOB-DISPATCHER](https://github.com/JuanenRac/HYDRA-UMC-JOB-DISPATCHER)** — fila real de trabalhos por prioridade com deduplicação, sobre uma API HTTP real.
- **[HYDRA-UMC-NODE-HEALING](https://github.com/JuanenRac/HYDRA-UMC-NODE-HEALING)** — vigilante real de saúde da frota por gRPC com repetição/recuo e deteção de identidade incoerente.
- **[HYDRA-UMC-PATH-PLANNER-3D](https://github.com/JuanenRac/HYDRA-UMC-PATH-PLANNER-3D)** — planeador real de trajetórias 3D baseado em RRT com validação real de colisões com obstáculos e espaço de trabalho.
- **[HYDRA-UMC-SWARM-SYNC](https://github.com/JuanenRac/HYDRA-UMC-SWARM-SYNC)** — sincronização real de estado CRDT LWW-Element-Map, testada por propriedades para a convergência de várias células.

*Gémeo digital e simulação*
- **[HYDRA-UMC-TWIN](https://github.com/JuanenRac/HYDRA-UMC-TWIN)** — centro de integração do motor de gémeo digital, com um contrato real de sincronização de compatibilidade de versões.
- **[HYDRA-UMC-HIL-BRIDGE](https://github.com/JuanenRac/HYDRA-UMC-HIL-BRIDGE)** — interlock de segurança real de hardware-in-the-loop que encaminha comandos entre a simulação e o hardware real.
- **[HYDRA-UMC-PHYSICS-REPLICA](https://github.com/JuanenRac/HYDRA-UMC-PHYSICS-REPLICA)** — cinemática direta real e validação de limites de juntas sobre um subconjunto real de URDF.
- **[HYDRA-UMC-SYNTHETIC-DATA-GEN](https://github.com/JuanenRac/HYDRA-UMC-SYNTHETIC-DATA-GEN)** — gerador procedural real de cenas 2D com exportação de anotações YOLO/COCO.

*Dados e análise*
- **[HYDRA-UMC-DATALAKE](https://github.com/JuanenRac/HYDRA-UMC-DATALAKE)** — armazém real de séries temporais sobre sqlite3 com uma API HTTP real de ingestão/consulta.
- **[HYDRA-UMC-ANOMALY-DETECTOR](https://github.com/JuanenRac/HYDRA-UMC-ANOMALY-DETECTOR)** — detetor real de anomalias por FFT e linha de base estatística, com monitorização de deriva.
- **[HYDRA-UMC-PRODUCTION-REPORTS](https://github.com/JuanenRac/HYDRA-UMC-PRODUCTION-REPORTS)** — cálculo real de OEE/disponibilidade sobre o histórico do DATALAKE, com exportação CSV reprodutível.
- **[HYDRA-UMC-TELEMETRY-COLLECTOR](https://github.com/JuanenRac/HYDRA-UMC-TELEMETRY-COLLECTOR)** — pipeline real de ingestão CAN/WebSocket para o DATALAKE, com deduplicação por sequência.

*Gateway industrial*
- **[HYDRA-UMC-GATEWAY-INDUSTRIAL](https://github.com/JuanenRac/HYDRA-UMC-GATEWAY-INDUSTRIAL)** — centro de integração que retransmite para protocolos industriais, com uma camada real de lista de comandos permitidos e contrapressão.
- **[HYDRA-UMC-OPCUA-SERVER](https://github.com/JuanenRac/HYDRA-UMC-OPCUA-SERVER)** — espaço de endereços OPC-UA real, verificado com uma sessão de cliente real do protocolo binário.
- **[HYDRA-UMC-MQTT-BROKER](https://github.com/JuanenRac/HYDRA-UMC-MQTT-BROKER)** — broker MQTT real com autenticação opcional por cliente e ACL de tópicos.
- **[HYDRA-UMC-MTCONNECT-ADAPTER](https://github.com/JuanenRac/HYDRA-UMC-MTCONNECT-ADAPTER)** — endpoints XML MTConnect `/probe` e `/current` reais com saída em modo degradado.

*Ferramentas complementares e operações do ecossistema*
- **[HYDRA-UMC-DASHBOARD-AI](https://github.com/JuanenRac/HYDRA-UMC-DASHBOARD-AI)** — painéis de Resumos Inteligentes e Realce de Anomalias sobre o DATALAKE/ANOMALY-DETECTOR, com um recurso estatístico honesto.
- **[HYDRA-UMC-TOOL-CLI](https://github.com/JuanenRac/HYDRA-UMC-TOOL-CLI)** — CLI da frota com um contrato de códigos de saída real e estável, um cliente vivo genuíno da API do HYDRA-UMC-SERVER.
- **[URTC-SMART-RACK](https://github.com/JuanenRac/URTC-SMART-RACK)** — firmware de um rack de montagem de placas com descodificação real do ID da ferramenta e lógica de pré-aquecimento Smart Idle.
- **[URTC-VISION-TOOL](https://github.com/JuanenRac/URTC-VISION-TOOL)** — firmware mais um companheiro de visão em Python real para uma cabeça de ferramenta de inspeção térmica/RGB.
- **[HYDRA-UMC-UPDATER](https://github.com/JuanenRac/HYDRA-UMC-UPDATER)** — ferramenta administrativa de desktop que descobre, clona e atualiza todos os repositórios deste ecossistema.
- **[HYDRA-UMC-OS-REBUILDER](https://github.com/JuanenRac/HYDRA-UMC-OS-REBUILDER)** — ferramenta de desktop para Windows/Linux que constrói uma imagem do CM5 pronta a gravar com as versões mais recentes do ecossistema, com configuração de primeiro arranque de Wi-Fi/utilizador/SSH ao estilo do Raspberry Pi Imager.
- **[HYDRA-UMC-OPS-AGENT](https://github.com/JuanenRac/HYDRA-UMC-OPS-AGENT)** — coordenador de incidentes de manutenção: um papel de borda de poucos privilégios recolhe um inventário/estado higienizado, um papel de plano de controlo mostra-o só de leitura e pede a um fornecedor de IA que sugira um diagnóstico; nunca aplica um patch nem implanta nada.
- **[HYDRA-UMC-DEV-SERVER](https://github.com/JuanenRac/HYDRA-UMC-DEV-SERVER)** — anfitrião de desenvolvimento reproduzível (Raspberry Pi 5 / CM5) que guarda o código-fonte do ecossistema e executa tarefas limitadas de compilação/teste numa fila durável; um papel de desenvolvimento dedicado, explicitamente não um CM5 operacional.

---

## 📚 Documentação e comunidade

- **[CONTRIBUTING.md](CONTRIBUTING.md)** — pilha tecnológica e diretrizes de código para um pull request.
- **[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)** — as normas de comportamento esperadas nesta comunidade.
- **[SECURITY.md](SECURITY.md)** — como comunicar uma vulnerabilidade e as áreas de segurança em que este projeto se concentra.
- **[SUPPORT.md](SUPPORT.md)** — onde fazer perguntas e comunicar erros.
- **[LICENSE.md](LICENSE.md)** — a licença deste projeto.

## 👤 AUTOR
**JuanenRac** (Electro Hobby 3D)
📧 electrohobby3d@gmail.com
📺 [youtube.com/@electrohobby3d](https://youtube.com/@electrohobby3d)

## 📜 LICENÇA
GPL-3.0 - consulte LICENSE para mais detalhes.

