<!-- =============================================================================
HYDRA-UMC-WATCH - Wear OS companion: status cards, haptic alerts and a voice relay
Copyright (C) JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
GPL-3.0-or-later - see LICENSE
============================================================================= -->

<p align="center">
  <img src="images/HYDRA_UMC_BANNER.svg" alt="HYDRA-UMC-WATCH banner" width="100%">
</p>

# ⌚ HYDRA-UMC-WATCH

<p align="center"><a href="README.md">🇺🇸 English</a> | <a href="README_spa.md">🇪🇸 Español</a> | 🇫🇷 <b>Français</b> | <a href="README_ita.md">🇮🇹 Italiano</a> | <a href="README_deu.md">🇩🇪 Deutsch</a> | <a href="README_zho.md">🇨🇳 简体中文</a> | <a href="README_jpn.md">🇯🇵 日本語</a> | <a href="README_por.md">🇵🇹 Português</a></p>

### ⌚ Compagnon Wear OS : cartes d'état, alertes haptiques et relais vocal (pas d'E-STOP sans fil)

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 or later">
  <img src="https://img.shields.io/badge/Platform-Wear%20OS-4285F4.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Feature-Voice%20%26%20haptics-blue.svg" alt="Feature">
  <img src="https://img.shields.io/badge/Tests-94%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.2.7-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-functional-0F6CBD.svg" alt="Maturity">
</p>

---

> **État de la sûreté et de la cybersécurité :** cet écosystème n'est pas un système de sécurité certifié. Il n'existe pas encore de chaîne d'E-STOP matérielle, ni de limites ou de verrouillages imposés par le firmware, ni de commande de mouvement vérifiée ; les images de firmware sont contrôlées par CRC-32 et une signature Ed25519, et une compilation de version refuse les clés de développement publiées (les têtes d'outil et les cartes d'extension derrière le relais utilisent encore les chargeurs d'URTC, qui prennent un HMAC-SHA256 à clé partagée au lieu d'une signature) ; TLS et TLS mutuel sont optionnels et désactivés par défaut ; aucune carte propre n'a encore été assemblée ni testée. Lisez la [base de sécurité](https://github.com/JuanenRac/HYDRA-UMC-DOCS/blob/main/docs/SECURITY_BASELINE.md) et le [modèle de menaces](https://github.com/JuanenRac/HYDRA-UMC-DOCS/blob/main/docs/THREAT_MODEL.md) avant de vous fier à une affirmation d'ici pour la sécurité d'une personne.

---

## 📋 En un coup d'œil

| | |
|---|---|
| Version | `0.2.7` |
| Maturité | Fonctionnel |
| Tests | 94 (`./gradlew testDebugUnitTest`) |
| Technologie | Kotlin / Wear OS |
| Cible | Une montre Wear OS (appairée au téléphone) |
| Sur matériel réel | Pas encore : aucune exécution sur du matériel réel n'est consignée pour cette version |

**Aller à :** [Présentation](#-présentation) · [Architecture](#-architecture) · [Sécurité et protection](#-sécurité-et-protection) · [Démarrage rapide](#-démarrage-rapide) · [Tests](#-tests) · [Structure du dépôt](#-structure-du-dépôt) · [Projets liés](#-projets-liés)

## 🚦 État du projet

Chaque ligne est une partie de ce dépôt et le feu dit où elle en est aujourd'hui. Elle est écrite d'après ce que le dépôt peut montrer (tests, compilations, exécutions), pas d'après ce qui est prévu.

🟢 Fait et vérifié · 🟡 Fonctionne, avec des limites connues · 🟠 À moitié fait ou pas encore vérifié · 🔴 Pas fait ou ne fonctionne pas

| Domaine | État | Ce que cela veut dire ici |
|---|---|---|
| Logiciel | 🟡 | Fonctionne pour son usage principal ; la vérification d'honnêteté ci-dessus liste ce qui manque encore |
| Tests | 🟢 | 94 tests automatiques, exécutés à chaque modification |
| Matériel réel | 🔴 | Pas encore : aucune exécution sur du matériel réel n'est consignée pour cette version |
| Documentation | 🟢 | README en huit langues, vérifié par rapport au manifeste à chaque modification |

## 🎯 Présentation

**HYDRA-UMC-WATCH** est le compagnon de poignet de l'opérateur d'usine. Il affiche l'état et les alertes en un coup d'œil au poignet et relaie de brefs échanges vocaux via le téléphone jumelé. Ce n'est pas un dispositif de sécurité : il n'a pas d'E-STOP sans fil et n'a pas encore été exécuté sur une montre physique.

Construit comme une application Wear OS autonome (Kotlin + Jetpack Compose pour Wear), réutilisant la même chaîne d'outils Gradle/Kotlin que le dépôt frère HYDRA-UMC-ANDROID-CONTROL plutôt que d'en introduire une nouvelle.

### Fonctionnalités Clés :
* ✅ **Réel v0 - motifs haptiques et protocole de synchronisation :** `haptics/HapticPatterns.kt` définit un motif de vibration réel et distinct par sévérité d'alerte (Critique/Avertissement/Info) ; `protocol/SyncMessage.kt` définit et (dé)sérialise les formes réelles des messages `EStopCommand`/`Alert` du flux de synchronisation SERVER<->WATCH ci-dessous. Les deux sont du Kotlin pur, testable - aucun matériel de montre, émulateur, ou WebSocket ouvert nécessaire pour les exécuter ou les tester.
* 🛑 **E-STOP Sans Fil (prévu)** — bouton d'urgence dédié. *(le message `EStopCommand` qu'il enverrait est réel et testé ; le transport WebSocket et le câblage du bouton physique restent prévus — nécessite l'appairage avec HYDRA-UMC-SERVER.)*
* 📳 **Alertes Haptiques** — motifs de vibration différenciés selon le type d'alerte (Critique, Avertissement, Info), joués via le vrai service Android `Vibrator`/`VibratorManager`. *(implémenté - `haptics/HapticAlertPlayer.kt` ; encore non vérifié sur un vrai appareil Wear OS, comme le reste de cette app.)*
* 🎙️ **Voix** — appuyer pour parler : demande explicite de la permission `RECORD_AUDIO`, l'intent de reconnaissance vocale système (`RecognizerIntent`) pour la transcription, la transcription relayée à HYDRA-UMC-ANDROID-CONTROL sous forme de message de sync `voice_turn` borné, et une réponse locale par `TextToSpeech` sur la montre. *(implémenté - `MainActivity.kt` ; la voix ne peut jamais actionner un robot directement, voir Architecture ci-dessous et [docs/VOICE_AI_PROTOCOL.md](docs/VOICE_AI_PROTOCOL.md) pour le flux de bout en bout et la limite de sécurité.)*
* ⌚ **Statut en un Coup d'Œil** — un bouton **Actualiser le statut** relaie une requête de statut bornée à HYDRA-UMC-ANDROID-CONTROL via le Data Layer appairé ; la carte `system_status` reçue est affichée avec un indicateur « dernier connu - peut être obsolète » dès qu'elle devient périmée. *(implémenté - `MainActivity.kt`/`WatchRelayTransport.requestSystemStatus()` ; à la demande via le relais téléphone, pas un socket direct montre-serveur, et encore non vérifié sur un vrai appareil Wear OS.)*
* 🔐 **JWT Auth** — appairage basé sur JWT avec HYDRA-UMC-SERVER. *(prévu.)*
* ✅ **Chaîne d'outils Wear OS autonome** — une véritable application Gradle/Kotlin/Compose pour Wear qui compile un APK de débogage fonctionnel. *(implémenté — voir COMPILATION ET EXÉCUTION ci-dessous)*
* 🔁 **Politique de Reconnexion du Relais** — `transport/RelayRetryPolicy.kt` est une politique réelle et pure de backoff exponentiel pour un envoi de relais échoué (par ex. pas encore de téléphone appairé), plafonnée à un délai maximal borné ; une nouvelle tentative en attente est annulée dès que l'écran se ferme, sans jamais se déclencher contre une activité déjà détruite. *(implémenté)*
* 🧭 **Sélection de nœud vérifiée par le companion** — `WatchRelayTransport` envoie via une vraie requête `CapabilityClient` (`hydra_umc_phone_companion`, déclarée dans le propre `res/values/wear.xml` de HYDRA-UMC-ANDROID-CONTROL), jamais `connectedNodes.firstOrNull()` - un second nœud connecté sans l'application companion installée ne peut jamais être choisi par accident ; un nœud à proximité (directement joignable en Bluetooth) est préféré à un nœud relayé par le cloud lorsque plusieurs sont valides. *(implémenté - `pickCompanionNode()`)*
* 🗂️ **Cache du Dernier État Connu** — `transport/LastKnownStateCache.kt` suit l'obsolescence réelle du dernier statut/alerte relayé, afin qu'un ancien ne soit jamais affiché comme actuel. *(implémenté)*
* 📥 **Boîte du relais** — `transport/RelayInbox.kt` : sous Wear OS l'écran est détruit dès que le poignet est baissé, et une réponse arrivée entre-temps n'avait plus de récepteur et était perdue. Le service d'écoute garde maintenant les messages les plus récents (8 au plus, aucun de plus de 10 minutes) et l'écran les affiche quand il est recréé, sans les énoncer ni vibrer. *(implémenté ; non essayé sur une vraie montre)*
* 🌐 **Interface en 8 langues** — `app/src/main/res/values-xx/strings.xml` pour l'anglais, l'espagnol, le français, l'allemand, l'italien, le japonais, le chinois et le portugais, choisi automatiquement selon la langue système de la montre ; `LocalizedStringsTest` vérifie que chaque fichier de langue a exactement les clés et les spécificateurs de format de l'anglais, de sorte qu'une chaîne manquante ou un marqueur cassé fasse échouer la compilation au lieu d'apparaître au poignet. *(implémenté)*

## 🧩 Rôle dans HYDRA-UMC

**HYDRA-UMC-WATCH** est le compagnon Wear OS des applications de contrôle HYDRA-UMC : il affiche le dernier état connu du système avec son âge, joue une vibration distincte pour chaque gravité d'alerte et relaie une demande vocale via le téléphone appairé. Il ne détient aucun identifiant du serveur et n'offre pas d'arrêt d'urgence sans fil.

## 🏛️ Architecture

<p align="center">
  <img src="images/architecture.svg" alt="HYDRA-UMC-WATCH architecture" width="100%">
</p>

<details>
<summary>Version texte du diagramme (Mermaid)</summary>

```mermaid
flowchart LR
    SERVER["HYDRA-UMC-SERVER"] --> WS["Synchronisation WebSocket"]
    WS --> WATCH["HYDRA-UMC-WATCH"]
    WATCH -- Commande E-STOP --> SERVER
    SERVER -- Alerte Critique --> WATCH
    WATCH -- Retour Haptique --> OPERATOR["Opérateur de Production"]
```

</details>

### FLUX DE SYNCHRONISATION DU WEARABLE

*Architecture cible une fois qu'un WebSocket direct avec le Server existera.
Le chemin réel et testé aujourd'hui passe par le téléphone appairé : Watch
-> Data Layer -> HYDRA-UMC-ANDROID-CONTROL -> HYDRA-UMC-SERVER/Voice UI
authentifié, et retour - voir Architecture ci-dessous et
[docs/VOICE_AI_PROTOCOL.md](docs/VOICE_AI_PROTOCOL.md) pour ce flux réel de
bout en bout. La montre ne détient jamais elle-même d'identifiant Server ;
un WebSocket direct Watch-Server et le bouton E-STOP sans fil restent un
travail futur conditionné au matériel.*

### ARCHITECTURE & DÉCISIONS DE CONCEPTION

* **Pourquoi c'est une application Wear OS autonome, pas une fonctionnalité de l'application téléphone.** Une montre exécute son propre processus indépendant sur son propre système d'exploitation - elle ne peut pas être simplement un mode d'interface de HYDRA-UMC-ANDROID-CONTROL, elle a besoin de son propre manifeste, sa propre compilation, et sa propre interface (bien plus contrainte) pour un statut en un coup d'œil/un E-STOP rapide.
* **Pourquoi `minSdk 30` (Wear OS 3), inférieur au propre minSdk de l'application téléphone.** Cela cible délibérément la génération matérielle Wear OS 3+ actuelle, pas les anciens appareils Wear OS 2 - contrairement à HYDRA-UMC-ANDROID-CONTROL, qui supporte les téléphones plus anciens, une application montre compagnon a une base matérielle réaliste à supporter bien plus étroite.
* **Pourquoi les motifs haptiques et le protocole de synchronisation arrivent avant la connexion WebSocket.** Définir les formes d'onde de vibration et les formes des messages que les deux côtés doivent s'accorder est du vrai travail Kotlin pur - inutile d'avoir un socket ouvert, un serveur appairé ou une montre physique pour l'écrire ou le tester. Ouvrir réellement cette connexion est la prochaine étape.
* **Pourquoi la politique de reconnexion et le cache du dernier état connu sont leurs propres modules purs, et non du code intégré dans `WatchRelayTransport`.** Les deux sont des classes Kotlin réelles et découplées (`RelayRetryPolicy`, `LastKnownStateCache`) testables sur une simple JVM sans montre, émulateur, ni téléphone appairé - la même norme déjà établie par `HapticPatterns.kt`/`SyncMessage.kt`. `WatchRelayTransport` lui-même reste l'élément mince, nécessairement Android, qui planifie réellement une nouvelle tentative ou enregistre un message reçu, et non l'endroit où vit la logique mathématique de la politique sous-jacente.
* **Pourquoi un état mis en cache périmé est signalé, et non masqué.** Masquer entièrement un ancien statut laisserait l'écran de la montre vide exactement quand la connexion au téléphone est instable - le véritable moment à risque. L'afficher clairement marqué "dernier connu - peut être obsolète" tient l'opérateur informé sans laisser passer une lecture vieille de plusieurs minutes pour une lecture en direct.
* **Comment cela s'intègre dans le reste de l'écosystème.** Fait paire avec HYDRA-UMC-ANDROID-CONTROL et HYDRA-UMC-IOS-CONTROL comme compagnon au poignet, en un coup d'œil - pas un remplacement de l'un ou l'autre, une surface de statut rapide/E-STOP rapide.

### MISES À JOUR SANS GOOGLE PLAY

Ce projet n'est pas distribué via Google Play. Le chemin de mise à jour
répétable est un APK GitHub Release installé via ADB :

```bash
# depuis la racine du projet, montre appairée et débogage sans fil activé
update-from-github.bat    # Windows
./update-from-github.sh   # Linux / macOS / WSL
```

Le script lit `releases/latest` sur GitHub, exige un tag stable
`vMAJOR.MINOR.PATCH`, vérifie la version déjà installée, demande une
confirmation explicite de l'opérateur, puis télécharge et invoque
`adb install -r`. Il ne touche jamais la version du dépôt, le manifeste ou
`CHANGELOG.md`. Une installation manuelle, au mieux (ouvrir l'APK téléchargé
avec l'installeur de paquets directement sur la montre) est également
documentée pour les appareils sans chemin ADB utilisable. Voir
[docs/GITHUB_ADB_UPDATES.md](docs/GITHUB_ADB_UPDATES.md) pour la procédure
complète, le nom exigé de l'APK de release, et la configuration de
signature de release.

`HYDRA-UMC-ANDROID-CONTROL` peut informer l'opérateur qu'une nouvelle
version de la montre existe une fois le message de statut de version du
compagnon câblé — c'est purement informatif et cela ne peut jamais
installer quoi que ce soit sur la montre elle-même. Voir
[docs/COMPANION_VERSION_PROTOCOL.md](docs/COMPANION_VERSION_PROTOCOL.md)
pour cette forme de message.

## 🔒 Sécurité et protection

| Menace | Défense | Où |
|---|---|---|
| Une autre application ou le téléphone d'un inconnu envoyant des messages à la montre | Le relais utilise la Data Layer de Wear OS, qui ne transporte que le trafic entre APK de même nom de paquet et de même certificat de signature ; la cible est choisie par une requête de capacité, jamais en devinant un nœud connecté | `WatchRelayTransport.kt` |
| Une montre volée ou perdue qui donne accès au serveur | La montre ne détient jamais d'identifiant du serveur : l'application du téléphone appairé s'authentifie et la montre ne relaie que des messages de protocole bornés | `SyncMessage.kt`, `docs/VOICE_AI_PROTOCOL.md` |
| Une action proposée alors que le téléphone est hors de portée, puis qui échoue | Sans message récent du téléphone compagnon, une action qui passe par lui est retenue ; un rafraîchissement n'est jamais bloqué, car c'est ainsi qu'on obtient le signal de vie, et un ancien état est affiché comme ancien, pas caché | `CriticalActionGate.kt`, `LastKnownStateCache.kt` |

**Ce qu'il ne fait jamais :** servir d'arrêt d'urgence. Un bouton d'arrêt d'urgence sans fil et une liaison directe avec le serveur sont des travaux futurs qui exigent du matériel.

## 🚀 Démarrage rapide

Nécessite le SDK Android avec un JDK accepté par l'enveloppe Gradle fournie, et une montre ou un émulateur sous Wear OS 3 ou plus récent.

```bash
./build.sh               # Linux, macOS (build.bat on Windows)
./run.sh                 # installs the debug app on the connected watch and starts it
./update-from-github.sh  # installs the newest release by ADB, without Google Play
```

## 🧪 Tests

94 tests automatiques s'exécutent avec `./gradlew testDebugUnitTest`. Ils n'ont besoin ni de matériel ni de rien en dehors de cette machine.

La CI exécute `HYDRA_UMC_CI=1 python tools/ci_validate.py`, qui vérifie le manifeste, les documents, les huit README et les images.

## 🏗️ Compilation et publication

Nécessite JDK 21, le SDK Android (`local.properties` → `sdk.dir`, ignoré par git — pointez-le vers votre propre installation du SDK) et un appareil ou émulateur Wear OS pour `run`.

```bash
# Linux/macOS
chmod +x gradlew   # une seule fois
./build.sh
./run.sh            # nécessite un appareil/émulateur Wear OS connecté

# Windows
build.bat
run.bat
```

`build` ne change jamais la version (seul `python tools/hydra_project_tool.py bump` la fait monter), lance la vraie suite de tests JUnit (`haptics`, `protocol`, `transport`), et compile l'APK de débogage vers `app/build/outputs/apk/debug/app-debug.apk` - le tout en une seule invocation Gradle (les scripts passent encore `-PhydraUmcReadOnly=true`, mais cela n'a plus d'effet : le build Gradle ne change jamais la version). `run` installe l'APK via `gradlew installDebug` et lance `MainActivity` avec `adb`.

Exemple réel - les deux scripts d'incrémentation de version sont exécutés par la commande `bump` explicite et fonctionnent aussi de manière autonome (ils modifient les fichiers de version quand ils s'exécutent) :

```bash
python3 bump_manifest_version.py   # ex. "HYDRA-UMC version: v0.1.2 -> v0.1.3"
python3 bump_version_code.py       # ex. "versionCode: 12 -> 13"
```

## 📂 Structure du dépôt

<p align="center">
  <img src="images/repository-structure.svg" alt="Repository structure of HYDRA-UMC-WATCH" width="100%">
</p>

<details>
<summary>Version texte de l'arborescence</summary>

```text
HYDRA-UMC-WATCH/
├── tools/                                code source
│   ├── build_test.py                     Vérification CI sans mutation : vérification du contrat + assembleDebug
│   ├── ci_validate.py                    Validation manifeste/CHANGELOG/docs utilisée par CI
│   ├── verify_paired_relay_contract.py   Vérification statique de la limite de sécurité Data Layer Watch<->Android Control
│   ├── _doc_policy.py                    Règles de documentation partagées par la famille (copie de celle de HYDRA-UMC-SDK, tenue synchronisée)
│   ├── _readme_parity.py                 Contrôle que les huit README partagent la même structure (copie de celui de HYDRA-UMC-SDK, tenue synchronisée)
│   ├── hydra_project_tool.py             Les mêmes commandes dans chaque dépôt : validate, test, ci, version, status, bump
│   └── extra_build_steps.py              Les étapes de compilation propres à ce dépôt en plus du test de compilation partagé (ses tests unitaires)
├── app/
├── gradle/
├── docs/                                 Documentation et protocoles de sécurité
├── images/                               Médias et diagrammes
├── .github/                              workflow de CI et modèles de tickets
├── build.gradle.kts                      Build racine Gradle
├── settings.gradle.kts                   Câblage des modules
├── gradlew / gradlew.bat                 Lanceur du Gradle wrapper
├── hydra-umc.project.json                Manifeste de l'écosystème (version, métadonnées build/santé)
├── keystore.properties.example           Modèle pour la config de signature de release ignorée par git
├── bump_manifest_version.py              Incrémente major/minor/patch + le manifeste, ensemble
├── bump_version_code.py                  Incrément du compteur versionCode d'Android (exécuté seulement par la commande `bump` explicite)
├── build.sh / build.bat                  Build réel : lance les tests, assembleDebug
├── build-test.sh / build-test.bat        Enveloppe sans mutation pour tools/build_test.py
├── run.sh / run.bat                      Exécution réelle : gradlew installDebug + lancement adb
├── CHANGELOG.md                          ce qui a changé à chaque version
├── test.sh                               lance les tests
├── update-from-github.bat                installe la dernière version par ADB
└── update-from-github.sh                 installe la dernière version par ADB
```

</details>

## 🗺️ Feuille de route et limites connues

**Limites connues aujourd'hui**

**Réel aujourd'hui :** lecture locale des alertes par le service Android `Vibrator`, permission explicite du microphone et reconnaissance vocale système, transcription visible et synthèse vocale locale, ainsi que des messages typés et testés pour `voice_turn`, `assistant_reply`, `system_status`, `EStopCommand`, `Alert` et l'état de version du compagnon. Le transport officiel Wear OS Data Layer relaie les demandes vocales limitées et les cartes d'état via HYDRA-UMC-ANDROID-CONTROL vers Server authentifié et Voice UI.

**Limite d'intégration :** l'application Android appairée conserve le JWT chiffré de Server ; Server conserve le jeton Voice UI. Data Layer exige le même nom de paquet et le même certificat de signature pour les deux APK. La voix ne peut jamais actionner directement un robot ; une réponse liée au mouvement doit demander une confirmation et l'E-STOP physique reste indépendant.

**À venir :** validation de l'appairage, du transport radio, du microphone/haut-parleur et de l'état de bout en bout sur un appareil Wear OS réel ; l'E-STOP sans fil et la télémétrie CM5 en direct restent des travaux distincts soumis au matériel.

## 🌍 Langues

Ce README et les documents du dépôt existent en huit langues : anglais, espagnol, allemand, français, italien, japonais, chinois et portugais (Portugal). Ils sont écrits ensemble et la CI vérifie que les huit fichiers ont les mêmes sections et les mêmes liens.

## 🔗 Projets liés

Ce projet fait partie de l'écosystème robotique HYDRA-UMC du même auteur (JuanenRac / Electro Hobby 3D). Bon à savoir, car une demande pourrait en réalité concerner l'un de ceux-ci plutôt que ce dépôt.

**Directement Liés**
- **[HYDRA-UMC-ANDROID-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-ANDROID-CONTROL)** — application de contrôle Android native avec connexion biométrique et un compagnon Wear OS jumelé — l'application compagnon avec laquelle se jumelle ce dispositif portable.
- **[HYDRA-UMC-IOS-CONTROL](https://github.com/JuanenRac/HYDRA-UMC-IOS-CONTROL)** — application de contrôle iOS/iPadOS (Flutter) avec synchronisation WebSocket en temps réel — l'application compagnon avec laquelle se jumelle ce dispositif portable.
- **[HYDRA-UMC-VOICE-UI](https://github.com/JuanenRac/HYDRA-UMC-VOICE-UI)** — vrai front-end vocal (VAD + analyseur d'intention) avec un relais Watch borné et soumis à confirmation — envoie les messages voice_turn que ce dispositif portable affiche sous forme d'alertes haptiques.

**Fait Également Partie de l'Écosystème**

*Matériel & Plateforme de Base*
- **[HYDRA-UMC](https://github.com/JuanenRac/HYDRA-UMC)** — la conception de la carte mère du bras robotique : hôte CM5 + coprocesseur STM32H745 double cœur, destinée à coordonner jusqu'à 8 bras-outils via CAN-OTA/SPI-OTA (firmware à un stade précoce ; aucune carte assemblée pour l'instant).
- **[HYDRA-UMC-OS](https://github.com/JuanenRac/HYDRA-UMC-OS)** — couche produit reproductible sur Raspberry Pi OS pour le CM5 : agent en lecture seule, config/profils validés, provisionnement WiFi de premier contact.
- **[HYDRA-UMC-SDK](https://github.com/JuanenRac/HYDRA-UMC-SDK)** — le contrat JSON-Schema partagé et la barrière logicielle de contrôle des jobs contre laquelle chaque bridge valide ses commandes (pas une fonction de sécurité certifiée).
- **[HYDRA-UMC-CONNECTOR-HUB](https://github.com/JuanenRac/HYDRA-UMC-CONNECTOR-HUB)** — registre déclaratif et validateur de manifestes d'adaptateur pour les connecteurs de machines externes ; étend la propre idée de contrat du SDK aux machines externes sans remplacer les projets de passerelle industrielle.

*Backend Central & Clients*
- **[HYDRA-UMC-SERVER](https://github.com/JuanenRac/HYDRA-UMC-SERVER)** — le vrai backend headless (REST/WebSocket) auquel parle réellement chaque client de contrôle.
- **[HYDRA-UMC-STUDIO](https://github.com/JuanenRac/HYDRA-UMC-STUDIO)** — tableau de bord de contrôle web avec visualisation 3D multi-robot en temps réel.
- **[HYDRA-UMC-SUITE](https://github.com/JuanenRac/HYDRA-UMC-SUITE)** — centre de commande d'essaim de bureau (PySide6) pour plusieurs serveurs à la fois, empaqueté en exécutable autonome.
- **[HYDRA-UMC-DSI](https://github.com/JuanenRac/HYDRA-UMC-DSI)** — interface tactile native pour l'écran tactile DSI 7" embarqué, intégrée directement sur le CM5.
- **[HYDRA-UMC-EDITOR-URDF](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-URDF)** — créateur/éditeur graphique de bureau pour URDF qui envoie les modèles terminés vers le propre catalogue de STUDIO.
- **[HYDRA-UMC-EDITOR-STL](https://github.com/JuanenRac/HYDRA-UMC-EDITOR-STL)** — éditeur de bureau de modèles STL, transformant/remplaçant/retirant/ajoutant de vraies pièces dans le même catalogue de modèles qu'édite aussi HYDRA-UMC-EDITOR-URDF.
- **[HYDRA-UMC-BRIDGE-AMR](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-AMR)** — frontière de coordination pour les flottes AGV/AMR via un éditeur MQTT VDA 5050 (testé uniquement contre une flotte émulée).
- **[HYDRA-UMC-BRIDGE-CNC](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-CNC)** — coordinateur haut niveau pour cellules CNC avec accès au statut/octets de contrôle GRBL (testé uniquement contre un émulateur).
- **[HYDRA-UMC-BRIDGE-DROIDS](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-DROIDS)** — frontière de coordination pour droïdes à pattes/humanoïdes, avec un émetteur de commandes Boston Dynamics Spot (testé uniquement contre un émulateur).
- **[HYDRA-UMC-BRIDGE-LASER](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-LASER)** — coordinateur de cellules laser lisant 3 signaux GPIO de clé/enceinte/verrouillage (testé uniquement contre une puce simulée ; il ne peut ni armer ni tirer un laser).
- **[HYDRA-UMC-BRIDGE-OPENPNP](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-OPENPNP)** — coordinateur haut niveau sûr pour le flux de cartes du pick-and-place OpenPnP.
- **[HYDRA-UMC-BRIDGE-PRINTER3D](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-PRINTER3D)** — frontière de coordination sûre pour imprimantes 3D Moonraker/Klipper, avec de vraies commandes de tâche contrôlées.
- **[HYDRA-UMC-BRIDGE-ROS2](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-ROS2)** — coordinateur avec un transport ROS 2 rclpy à importation paresseuse (pas encore exécuté contre une installation ROS 2).
- **[HYDRA-UMC-BRIDGE-UAV](https://github.com/JuanenRac/HYDRA-UMC-BRIDGE-UAV)** — frontière de coordination pour UAV équipés de caméra, avec un émetteur de commandes MAVLink (testé uniquement contre un émulateur).

*Plateforme d'Outils URTC*
- **[URTC](https://github.com/JuanenRac/URTC)** — firmware pour la carte physique Universal Robot Tool Controller, plus de 25 profils d'outil sur bus CAN.
- **[URTC-FLASHER](https://github.com/JuanenRac/URTC-FLASHER)** — outil de bureau à interface graphique pour flasher les cartes URTC, CAN-OTA plus SWD/JTAG puce complète.
- **[URTC-TESTER](https://github.com/JuanenRac/URTC-TESTER)** — outil de bureau de diagnostic CAN-bus en direct pour cartes URTC, un panneau par profil d'outil.
- **[URTC-WEB-STUDIO](https://github.com/JuanenRac/URTC-WEB-STUDIO)** — alternative basée navigateur à URTC-TESTER via la Web Serial API, sans installation locale.
- **[URTC-GATEWAY](https://github.com/JuanenRac/URTC-GATEWAY)** — passerelle devant le bus CAN d'URTC : liste d'identifiants autorisés, limite de débit et clé de session ; logiciel testé sur ordinateur, pas encore sur un bus réel.

*Nœud IA de Vision (Hailo-8)*
- **[HYDRA-UMC-VISION-NODE](https://github.com/JuanenRac/HYDRA-UMC-VISION-NODE)** — hub d'intégration pour le pipeline de vision Hailo-8, avec une vraie vérification de disponibilité matérielle par étape.
- **[HYDRA-UMC-DETECTION-HEF](https://github.com/JuanenRac/HYDRA-UMC-DETECTION-HEF)** — registre réel de modèles compilés avec vérification de chargement sécurisé par architecture Hailo/checksum.
- **[HYDRA-UMC-VISION-STREAMER](https://github.com/JuanenRac/HYDRA-UMC-VISION-STREAMER)** — générateur réel de pipeline GStreamer + config MediaMTX, avec une vraie frontière d'intégration HailoRT.
- **[HYDRA-UMC-VISUAL-SERVOING-API](https://github.com/JuanenRac/HYDRA-UMC-VISUAL-SERVOING-API)** — loi de correction Position-Based Visual Servoing avec autorisation conditionnée à l'état de zone ; pas encore d'estimation de pose par caméra.
- **[HYDRA-UMC-SAFETY-ZONES](https://github.com/JuanenRac/HYDRA-UMC-SAFETY-ZONES)** — logique v0 de violation de zone, de fraîcheur de calibration et de demande d'E-STOP qui lit les positions d'objets dans un fichier ; aucun détecteur ne l'alimente et aucun E-STOP physique n'y est encore relié.

*Nœud IA Cognitif (Hailo-10)*
- **[HYDRA-UMC-COGNITIVE-NODE](https://github.com/JuanenRac/HYDRA-UMC-COGNITIVE-NODE)** — hub d'intégration pour le pipeline cognitif Hailo-10 (orchestration LLM/VLA/voix).
- **[HYDRA-UMC-VLA-ENGINE](https://github.com/JuanenRac/HYDRA-UMC-VLA-ENGINE)** — vrai encodage/décodage de jetons d'action et génération de trajectoire pour un modèle Vision-Language-Action.
- **[HYDRA-UMC-SEMANTIC-PLANNER](https://github.com/JuanenRac/HYDRA-UMC-SEMANTIC-PLANNER)** — vraie décomposition de tâches basée sur des règles et récupération sémantique d'erreurs sur les codes d'erreur MCU.
- **[HYDRA-UMC-DOCS-QA](https://github.com/JuanenRac/HYDRA-UMC-DOCS-QA)** — vraie recherche documentaire TF-IDF (bibliothèque standard uniquement) sur les propres documents Markdown de cet écosystème.
- **[HYDRA-UMC-LOCAL-TECHNICIAN](https://github.com/JuanenRac/HYDRA-UMC-LOCAL-TECHNICIAN)** — technicien de maintenance IA local et à portes de politique pour l'écosystème lui-même - observe, diagnostique et propose des corrections ; les deux niveaux de risque les plus élevés ne sont délibérément pas encore implémentés.

*Orchestration & Essaim*
- **[HYDRA-UMC-ORCHESTRATOR](https://github.com/JuanenRac/HYDRA-UMC-ORCHESTRATOR)** — hub d'intégration avec un vrai contrat de rapport de santé gRPC/Protobuf et une machine à états de mission.
- **[HYDRA-UMC-JOB-DISPATCHER](https://github.com/JuanenRac/HYDRA-UMC-JOB-DISPATCHER)** — vraie file de tâches basée sur la priorité avec déduplication, via une vraie API HTTP.
- **[HYDRA-UMC-NODE-HEALING](https://github.com/JuanenRac/HYDRA-UMC-NODE-HEALING)** — vrai chien de garde de santé de flotte basé sur gRPC, avec retry/backoff et détection d'incohérence d'identité.
- **[HYDRA-UMC-PATH-PLANNER-3D](https://github.com/JuanenRac/HYDRA-UMC-PATH-PLANNER-3D)** — vrai planificateur de trajectoire 3D basé sur RRT, avec vraie validation des collisions obstacle/espace de travail.
- **[HYDRA-UMC-SWARM-SYNC](https://github.com/JuanenRac/HYDRA-UMC-SWARM-SYNC)** — vraie synchronisation d'état CRDT LWW-Element-Map, testée par propriétés pour la convergence multi-cellule.

*Jumeau Numérique & Simulation*
- **[HYDRA-UMC-TWIN](https://github.com/JuanenRac/HYDRA-UMC-TWIN)** — hub d'intégration pour le moteur de jumeau numérique, avec un vrai contrat de synchronisation par compatibilité de version.
- **[HYDRA-UMC-HIL-BRIDGE](https://github.com/JuanenRac/HYDRA-UMC-HIL-BRIDGE)** — routeur hardware-in-the-loop avec un verrouillage logiciel de collision entre simulation et matériel réel (testé sans matériel réel).
- **[HYDRA-UMC-PHYSICS-REPLICA](https://github.com/JuanenRac/HYDRA-UMC-PHYSICS-REPLICA)** — vraie cinématique directe et validation des limites articulaires sur un vrai sous-ensemble URDF.
- **[HYDRA-UMC-SYNTHETIC-DATA-GEN](https://github.com/JuanenRac/HYDRA-UMC-SYNTHETIC-DATA-GEN)** — vrai générateur procédural de scènes 2D avec export d'annotations YOLO/COCO.

*Données & Analytique*
- **[HYDRA-UMC-DATALAKE](https://github.com/JuanenRac/HYDRA-UMC-DATALAKE)** — vrai magasin de séries temporelles basé sur sqlite3, avec une vraie API HTTP d'ingestion/requête.
- **[HYDRA-UMC-ANOMALY-DETECTOR](https://github.com/JuanenRac/HYDRA-UMC-ANOMALY-DETECTOR)** — vrai détecteur d'anomalies FFT + ligne de base statistique, avec surveillance de dérive.
- **[HYDRA-UMC-PRODUCTION-REPORTS](https://github.com/JuanenRac/HYDRA-UMC-PRODUCTION-REPORTS)** — vrai calcul OEE/disponibilité sur l'historique de DATALAKE, avec export CSV reproductible.
- **[HYDRA-UMC-TELEMETRY-COLLECTOR](https://github.com/JuanenRac/HYDRA-UMC-TELEMETRY-COLLECTOR)** — vrai pipeline d'ingestion CAN/WebSocket vers DATALAKE, avec déduplication par séquence.

*Passerelle Industrielle*
- **[HYDRA-UMC-GATEWAY-INDUSTRIAL](https://github.com/JuanenRac/HYDRA-UMC-GATEWAY-INDUSTRIAL)** — hub d'intégration relayant vers les protocoles industriels, avec une vraie couche de liste blanche de commandes/contre-pression.
- **[HYDRA-UMC-OPCUA-SERVER](https://github.com/JuanenRac/HYDRA-UMC-OPCUA-SERVER)** — vrai espace d'adressage OPC-UA, vérifié avec une vraie session client du protocole binaire.
- **[HYDRA-UMC-MQTT-BROKER](https://github.com/JuanenRac/HYDRA-UMC-MQTT-BROKER)** — vrai broker MQTT avec authentification par client optionnelle et ACL de sujets.
- **[HYDRA-UMC-MTCONNECT-ADAPTER](https://github.com/JuanenRac/HYDRA-UMC-MTCONNECT-ADAPTER)** — vrais points de terminaison XML MTConnect `/probe` et `/current`, avec sortie en mode dégradé.

*Outils Complémentaires & Opérations de l'Écosystème*
- **[HYDRA-UMC-DASHBOARD-AI](https://github.com/JuanenRac/HYDRA-UMC-DASHBOARD-AI)** — panneaux Smart Summaries et Anomaly Highlighting sur DATALAKE/ANOMALY-DETECTOR, avec un repli statistique honnête.
- **[HYDRA-UMC-TOOL-CLI](https://github.com/JuanenRac/HYDRA-UMC-TOOL-CLI)** — CLI de flotte avec un vrai contrat de codes de sortie stable, un vrai client en direct de la propre API de HYDRA-UMC-SERVER.
- **[URTC-SMART-RACK](https://github.com/JuanenRac/URTC-SMART-RACK)** — firmware pour un rack de montage de cartes avec décodage réel d'ID d'outil et logique de préchauffage Smart Idle.
- **[URTC-VISION-TOOL](https://github.com/JuanenRac/URTC-VISION-TOOL)** — firmware plus un vrai compagnon de vision Python pour une tête d'outil d'inspection thermique/RGB.
- **[HYDRA-UMC-UPDATER](https://github.com/JuanenRac/HYDRA-UMC-UPDATER)** — outil administratif de bureau qui découvre, clone et met à jour chaque dépôt de cet écosystème.
- **[HYDRA-UMC-OS-REBUILDER](https://github.com/JuanenRac/HYDRA-UMC-OS-REBUILDER)** — outil de bureau Windows/Linux qui construit une image de la CM5 prête à graver, préchargée avec les versions les plus actuelles de l'écosystème, avec une configuration de premier démarrage Wi-Fi/utilisateur/SSH façon Raspberry Pi Imager.
- **[HYDRA-UMC-OPS-AGENT](https://github.com/JuanenRac/HYDRA-UMC-OPS-AGENT)** — coordinateur d'incidents de maintenance : un rôle edge à faible privilège collecte un instantané d'inventaire/santé assaini, un rôle control-plane le rend en lecture seule et demande à un fournisseur d'IA de suggérer un diagnostic - n'applique jamais de correctif ni ne déploie rien.
- **[HYDRA-UMC-DEV-SERVER](https://github.com/JuanenRac/HYDRA-UMC-DEV-SERVER)** — hôte de développement reproductible (Raspberry Pi 5 / CM5) qui héberge le code source de l'écosystème et exécute des tâches de compilation/test délimitées via une file d'attente durable ; un rôle de développement dédié, explicitement distinct d'un CM5 opérationnel.

## 📚 Documentation

Où lire la suite :

* [Base de sécurité de l'écosystème](https://github.com/JuanenRac/HYDRA-UMC-DOCS/blob/main/docs/SECURITY_BASELINE.md)
* [Modèle de menaces de l'écosystème](https://github.com/JuanenRac/HYDRA-UMC-DOCS/blob/main/docs/THREAT_MODEL.md)
* [Journal des modifications de ce dépôt](CHANGELOG.md)
* [Licence de ce dépôt](LICENSE.md)

## 🤝 Contribuer et communauté

* Avant une pull request, exécutez `HYDRA_UMC_CI=1 py -3 tools/ci_validate.py` (ou `HYDRA_UMC_CI=1 python3 tools/ci_validate.py`) : il doit afficher `CI_VALIDATION=PASS`. N'exécutez pas `build.sh` ni `build.bat` pour cette vérification ; compiler n'est pas vérifier.
* Les huit README sont écrits à partir d'une source unique dans HYDRA-UMC-DOCS ; ne les modifiez pas à la main. Changez la source et réécrivez-les avec `tools/make_readmes.py`.
* N'affirmez pas dans un README ce qui n'a pas tourné : écrivez « simulé » ou « non essayé sur matériel » quand c'est la vérité.
* Voir [CONTRIBUTING.md](CONTRIBUTING.md), le [code de conduite](CODE_OF_CONDUCT.md), [SECURITY.md](SECURITY.md) et [SUPPORT.md](SUPPORT.md).

## 👤 AUTEUR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCE

GPL-3.0-or-later - [LICENSE](LICENSE).
