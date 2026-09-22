# KYC Liveness Sandbox

Standalone Android QA/testing simulator for liveness-related UI, state transitions, local video playback, retries, and sandbox app profiles.

**Version:** v1.0.0

This application is intended for **internal sandbox/QA testing only**. It does **not** bypass, spoof, inject into, or replay media against third-party KYC or liveness systems.

## Purpose

Exercise our own KYC/liveness UI and state-machine logic using local test clips inside this app’s sandbox.

Out of scope (not implemented):

- third-party KYC camera injection
- virtual-camera spoofing
- replay attacks against external KYC providers
- liveness bypass
- presenting prerecorded video as a live camera to another application

## Features

- Sandbox app profiles (bKash, Nagad, Binance, plus More)
- Liveness action simulator (blink, head turns, smile, and advanced actions)
- Local Media3/ExoPlayer test-video playback
- Dedicated test state machine with retry handling
- PASS / FAIL results and elapsed-time tracking
- Local debug logging (`KycSandbox` logcat tag)
- Full reset to IDLE

## Profiles

Primary:

- bKash
- Nagad
- Binance

Under **More** (sandbox labels only — not real KYC requirement claims):

Rocket, Upay, Tap, OK Wallet, CellFin, SureCash, Payoneer, Skrill, Wise, PayPal, Coinbase, Bybit, OKX, KuCoin, Custom

## Requirements

From the project configuration:

| Item | Value |
| --- | --- |
| minSdk | 26 |
| compileSdk / targetSdk | 35 |
| Kotlin | 2.1.0 |
| Android Gradle Plugin | 8.13.0 |
| Gradle Wrapper | 8.13 |
| JDK | 17 |
| UI | AndroidX + Jetpack Compose + Material 3 |
| Video | AndroidX Media3 ExoPlayer 1.5.1 |

Android Studio (or command-line SDK + JDK 17) is recommended.

## Download

[Download the latest APK](https://github.com/Taibur-Rahaman/The-Right-to-Hide---TRH/releases/download/v1.0.0/KYC-Liveness-Sandbox-v1.0.0.apk)

## Releases

[View all releases](https://github.com/Taibur-Rahaman/The-Right-to-Hide---TRH/releases)

Current release: [KYC Liveness Sandbox v1.0.0](https://github.com/Taibur-Rahaman/The-Right-to-Hide---TRH/releases/tag/v1.0.0)

APK asset: `KYC-Liveness-Sandbox-v1.0.0.apk`  
SHA-256: `dd6df25463cbb151af4810dd63db7ec4a7313b60ca43b35b228fe7806de159d7`

## Build

```bash
git clone https://github.com/Taibur-Rahaman/The-Right-to-Hide---TRH.git
cd The-Right-to-Hide---TRH
# Ensure JDK 17 is available, then:
./gradlew assembleDebug
```

Debug APK output (default path):

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Testing

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

## Project structure

```text
app/src/main/java/com/rth/kycliveness/
  data/       # TestAction, KycTestProfile, ProfileCatalog, ClipCatalog
  state/      # KycTestStateMachine and phases
  player/     # LivenessPlayer (Media3)
  ui/         # MainActivity, SandboxScreen, SandboxViewModel
  logging/    # SandboxLogger
  theme/      # Material 3 theme
app/src/test/ # Unit tests (state machine, profiles, clips, UI flow)
gradle/       # Gradle wrapper
```

## Safety / Scope

KYC Liveness Sandbox is an **internal QA simulator**. Test clips live under `app/src/main/res/raw/` when provided. Missing or corrupt clips show **Test clip unavailable** and must not crash the app. There is no camera permission, no network KYC integration, and no third-party injection.

## License / Use

Internal QA use only. Do not use this project to interfere with third-party identity verification systems.
