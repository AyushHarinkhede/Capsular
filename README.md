<p align="center">
  <img src="app/src/main/res/drawable/logo.png" width="120" alt="Capsular Logo"/>
</p>

<h1 align="center">Capsular</h1>
<p align="center"><b>Dynamic Island for Android</b></p>
<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android"/>
  <img src="https://img.shields.io/badge/Min%20SDK-26-blue"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose"/>
  <img src="https://img.shields.io/badge/Design-Material%20You-6750A4"/>
</p>

---

## What is Capsular?

**Capsular** brings Apple's iconic **Dynamic Island** experience to any Android device with a punch-hole camera.

Just like the Dynamic Island on iPhone 14 Pro and later, Capsular turns your camera cutout from a static, dead area into a **living, animated notification hub** that:

- Morphs around your camera hardware seamlessly
- Shows real-time activity from calls, music, timers, recordings, and more
- Expands into rich interactive cards on tap or long press
- Collapses back to a sleek pill with a satisfying spring animation
- Shows a waveform visualizer for music and voice calls
- Supports a split-view secondary bubble for two concurrent activities

---

## Features

| Feature | Description |
|---|---|
| **Idle Pill** | Rests as a minimal black pill over the camera when nothing is active |
| **Compact State** | Spring-animates to show activity content (leading icon + trailing visualizer) |
| **Expanded Card** | Full interactive card drops down on tap with delayed content fade-in |
| **Split Bubble** | Second activity shows as a circle on the side (like Dynamic Island split view) |
| **Music Visualizer** | 4-bar organic waveform synced to real audio amplitude or simulated |
| **Voice Waveform** | 4-bar green visualizer for active calls and voice recordings |
| **Priority Queue** | Call > Music > Recording > Flashlight > Sound > Notifications |
| **Haptic Feedback** | Synchronized vibration motor feedback on every gesture and state change |
| **Gestures** | Tap to expand, swipe up to collapse, swipe left to hide, swipe right (music skip) |
| **Material You** | Full Dynamic Color — adapts to your wallpaper palette on Android 12+ |
| **M3 Shape Scale** | Official Material 3 shape tokens (ExtraLarge 28dp cards, Full/Pill buttons) |
| **Event Simulator** | Built-in simulator to test all event types: Call, Music, Timer, Stopwatch, Notification, Navigation, Progress, Recording, and more |
| **Camera Calibration** | Precise alignment overlay to match your phone's exact punch-hole position |

---

## Supported Events

- **Phone Calls** — incoming/ongoing with caller name and accept/reject controls
- **Music Playback** — album art, track name, artist, scrubber, waveform
- **Screen Recording** — red dot + recording duration
- **Voice Memo** — green waveform + duration
- **Timer / Stopwatch** — live countdown/countup display
- **Notifications** — app icon, title, message preview
- **Navigation** — turn direction, distance, ETA
- **Progress** — generic progress bar (downloads, uploads, etc.)
- **Flashlight** — flash icon with on/off state
- **Hotspot** — connected device count
- **Bluetooth** — device name and battery
- **System Toggles** — Wi-Fi, DND, Silent, Low Power, Airplane mode

---

## How It Works

Capsular runs as a **Foreground Overlay Service** using `TYPE_APPLICATION_OVERLAY` window. It listens to system broadcasts (media sessions, phone state, notifications) and renders a hardware-accelerated Compose UI directly on top of all other apps.

```
System Events
    │
    ▼
CapsuleStateManager (Priority Queue)
    │
    ├─ mainEvent  ──► Main Capsule Pill (spring animated)
    └─ splitEvent ──► Side Circle Bubble
```

The overlay uses **spring physics** (`dampingRatio=0.50`, `StiffnessMediumLow`) for the characteristic Dynamic Island bounce, matching Apple's underdamped spring model.

---

## Setup

1. Install the APK
2. Grant **Draw Over Other Apps** permission
3. Grant **Notification Listener** access
4. (Optional) Grant **Microphone** for real-time audio visualizer
5. Toggle **Capsular** service ON
6. Use **Camera Calibration** to align the pill over your punch-hole

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: ViewModel + StateFlow (MVI-lite)
- **Animations**: Spring physics (`animateFloatAsState`, `spring()`)
- **Overlay**: `WindowManager` with `TYPE_APPLICATION_OVERLAY`
- **Design**: Material You — Dynamic Color + M3 Shape Scale

---

## Requirements

- Android 8.0 (API 26) or higher
- Punch-hole camera device (works best; pill shape adapts to any phone)
- Android 12+ for full Material You Dynamic Color

---

## License

```
Copyright 2026 Ayush Harinkhede

Licensed under the Apache License, Version 2.0
```

---

<p align="center">Made with love for Android by <b>Ayush Harinkhede</b></p>