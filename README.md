# KYC Liveness Sandbox

Controlled Android **KYC / Liveness Verification TEST SANDBOX**.

Developers can choose:

1. **Live Camera** — real device camera (CameraX)
2. **Test Video** — developer-selected prerecorded video
3. **Test Image** — developer-selected image (rejected for motion liveness)

The selected source feeds the **same** sandbox preview + face-analysis + state-machine pipeline. Selecting a video does **not** auto-pass verification.

**Version:** v1.1.0

## Purpose

Exercise the app’s own camera/liveness pipeline with deterministic media.

**Not in scope:**

- system-wide virtual camera
- injecting media into third-party / production KYC apps
- bypassing face detection or forging a successful verification
- spoofing another application’s camera

A normal Android app can only control **its own** camera input. It cannot make arbitrary other apps receive this test video as their camera.

## Architecture

```text
CameraSource
├── LiveCameraSource   (CameraX Preview + ImageAnalysis)
└── TestMediaSource
    ├── Test Video     (ExoPlayer + frame sampling)
    └── Test Image     (single frame; blocked for motion liveness)
        ↓
Shared face analyzer (ML Kit)
        ↓
Action matcher → existing KycTestStateMachine
        ↓
PASS / FAIL / RETRY
```

## Modes

### Normal mode

Clean KYC verification UI: preview, face guide, required action, progress, status. Uses **Live Camera**.

### Developer Mode

Hidden by default. When enabled:

- Input Source: Live Camera / Test Video / Test Image
- Pick / preview / replace / remove test media
- Play / Pause / Restart for test video
- Front/back switch for live camera
- Sandbox profiles + manual action inject (for state-machine QA)

Test media is labeled **TEST SOURCE** on the preview.

## Requirements

| Item | Value |
| --- | --- |
| minSdk | 26 |
| compileSdk / targetSdk | 35 |
| Kotlin | 2.1.0 |
| AGP | 8.13.0 |
| Gradle | 8.13 |
| JDK | 17 |
| Camera | CameraX 1.4.1 |
| Face analysis | ML Kit Face Detection |
| Video | Media3 ExoPlayer 1.5.1 |

## Build / test

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

## Download

- **v1.1.0 (latest):** [Release](https://github.com/Taibur-Rahaman/The-Right-to-Hide---TRH/releases/tag/v1.1.0) · [APK](https://github.com/Taibur-Rahaman/The-Right-to-Hide---TRH/releases/download/v1.1.0/KycLivenessSandbox-v1.1.0-debug.apk)
- **v1.0.0:** [Release](https://github.com/Taibur-Rahaman/The-Right-to-Hide---TRH/releases/tag/v1.0.0)

## Platform limitation

This sandbox cannot replace the phone camera globally or feed prerecorded media into other KYC apps. It only replaces the input source **inside this application**.

## Safety

Internal QA use only. Do not use this project to interfere with third-party identity verification systems.
