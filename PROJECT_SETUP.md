# PocketPresenter — Project Setup & Contributor Guide

This guide details the prerequisites, tooling, and development environment setup for PocketPresenter contributors and AI agents.

> **Important:** PocketPresenter is currently in **Gate 0 (Pre-implementation)**. No feature code should be merged until the three core feasibility proofs pass on real target hardware.

---

## 1. Approved Native Architecture

PocketPresenter strictly adheres to the native architecture defined in [docs/architecture.md](docs/architecture.md):

- **Android Controller**: Native Kotlin and Jetpack Compose (`android/`).
- **PowerPoint Integration**: C# VSTO (Visual Studio Tools for Office) / COM add-in on .NET Framework 4.8 (`windows-addin/`).
- **Transport**: Bluetooth Classic RFCOMM (SPP) using bonded OS-level pairing.
- **Presentation Engine**: Microsoft PowerPoint itself (no custom PPTX rendering, no separate companion executable).
- **Network / Storage**: Completely offline and private. No cloud, database, server, or web frameworks.

---

## 2. Prerequisites & Environment Setup

### A. Android Development (`android/`)
1. **IDE**: Android Studio (Ladybug / Hedgehog 2024.x or newer).
2. **JDK**: OpenJDK 17 or higher.
3. **Android SDK**:
   - `minSdkVersion`: 31 (Android 12)
   - `compileSdkVersion`: 34
   - `targetSdkVersion`: 34
4. **Key Platform APIs**:
   - `android.bluetooth.BluetoothAdapter` & `BluetoothServerSocket` (RFCOMM SPP)
   - `android.media.session.MediaSession` & `VolumeProvider` (Hardware volume key interception)
   - `android.app.ForegroundService` with `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`
5. **Setup Steps**:
   - Open Android Studio.
   - Configure SDK Platforms 31–34 and SDK Build-Tools.
   - Pair an Android test device running Android 12+ with the development Windows machine via Bluetooth.

### B. Windows PowerPoint Add-in (`windows-addin/`)
1. **IDE**: Visual Studio 2022 (Community, Professional, or Enterprise).
2. **Workloads**:
   - **Office/SharePoint development** workload (includes VSTO templates and Office Primary Interop Assemblies).
   - **.NET desktop development** workload.
3. **Framework**: .NET Framework 4.8 Developer Pack.
4. **Target Application**: Desktop Microsoft PowerPoint (Microsoft 365 or 2019+ 64-bit/32-bit) on Windows 10/11.
5. **Key Platform APIs**:
   - Microsoft Office PowerPoint COM Interop (`Microsoft.Office.Interop.PowerPoint`)
   - WinRT Bluetooth API (`Windows.Devices.Bluetooth.Rfcomm`) or native Winsock Bluetooth (`ws2_32.dll`)
   - Win32 User32 APIs: `WS_EX_LAYERED`, `WS_EX_TRANSPARENT`, `WS_EX_NOACTIVATE`, `WS_EX_TOOLWINDOW`, `SendInput`
6. **Setup Steps**:
   - Install Visual Studio 2022 with Office/SharePoint development tools enabled.
   - Verify PowerPoint desktop installation and add-in debugging permissions.

---

## 3. Protocol Test Vectors & Verification

Before implementing protocol serialization/deserialization:
- Review [docs/protocol.md](docs/protocol.md) for frame limits (max 4096 bytes, newline-delimited JSON).
- Ensure all unit tests in Kotlin and C# validate against [protocol/fixtures/vectors.json](protocol/fixtures/vectors.json).

---

## 4. Gate 0 Feasibility Proofs

Before any feature wave begins, three proofs must be validated and documented in [docs/feasibility-report.md](docs/feasibility-report.md) and [tests/manual-test-matrix.md](tests/manual-test-matrix.md):

1. **Proof 0.1**: In-process Bluetooth RFCOMM connection between phone and VSTO add-in.
2. **Proof 0.2**: Click-through, non-activating layered overlay window active during full-screen slide shows.
3. **Proof 0.3**: Background/screen-off volume button capture via `MediaSession` and `VolumeProvider`.

---

## 5. Working Rules

- Refer to [docs/rules.md](docs/rules.md) for coding discipline, STA thread requirements for COM, and cleanup policies.
- AI Agents must follow instructions in [AGENTS.md](AGENTS.md).
