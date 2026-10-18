# PocketPresenter — Technical Requirements Document

| | |
|---|---|
| **Version** | 1.0 |
| **Status** | Proposed |
| **Owns** | Technology stack, module boundaries, interfaces, platform requirements, tooling |
| **Related** | [prd.md](prd.md) · [architecture.md](architecture.md) · [protocol.md](protocol.md) · [backend_schema.md](backend_schema.md) · [implementation_plan.md](implementation_plan.md) |

> **Provenance note.** The earlier documentation pack referenced a `trd.md` that was not supplied when the pack was reviewed. This file was **derived from** [architecture.md](architecture.md), [protocol.md](protocol.md), [backend_schema.md](backend_schema.md) and the rules in [rules.md](rules.md) so that every cross-reference resolves. It introduces no new product scope. Where it adds an implementation detail (marked *proposed*), that detail is open to change at Phase 1. If you hold an original `trd.md`, merge it here and keep the conflict-resolution rule in [rules.md §1](rules.md#1-source-of-truth-and-scope-discipline).

## Table of contents

1. [Purpose and precedence](#1-purpose-and-precedence)
2. [Repository structure](#2-repository-structure)
3. [Module boundaries](#3-module-boundaries)
4. [Technology stack](#4-technology-stack)
5. [Key interfaces (illustrative)](#5-key-interfaces-illustrative)
6. [Platform requirements](#6-platform-requirements)
7. [Protocol constants and shared fixtures](#7-protocol-constants-and-shared-fixtures)
8. [Latency budget](#8-latency-budget)
9. [Diagnostics policy](#9-diagnostics-policy)
10. [Tooling, testing and build hygiene](#10-tooling-testing-and-build-hygiene)
11. [Revision history](#11-revision-history)

## 1. Purpose and precedence

The PRD says *what*, the architecture says *how the system is shaped*, and this document says *with what and where*. Order of authority: PRD → architecture → TRD → implementation plan ([rules.md §1](rules.md#1-source-of-truth-and-scope-discipline)). The wire format itself is owned by [protocol.md](protocol.md).

## 2. Repository structure

```text
pocketpresenter/
├── AGENTS.md                   Entry point for AI coding agents → docs/rules.md
├── README.md  LICENSE  SECURITY.md  CONTRIBUTING.md  CHANGELOG.md
├── .gitignore  .editorconfig  .gitattributes
├── .github/                    Issue and pull request templates (CI added in Phase 1)
├── docs/                       prd, architecture, trd, protocol, app_flow, ui_ux_brief,
│                               backend_schema, implementation_plan, rules, decisions,
│                               feasibility-report, traceability
├── protocol/
│   └── fixtures/vectors.json   Shared test vectors consumed by both platforms
├── tests/
│   └── manual-test-matrix.md   Device/configuration evidence (hardware tests)
├── android/                    Kotlin + Jetpack Compose app
└── windows-addin/              VSTO / .NET Framework 4.8 add-in
```

## 3. Module boundaries

Module lists are defined in [architecture.md §5](architecture.md#5-component-boundaries). Enforced dependency rules:

| Rule | Why |
|---|---|
| `Protocol` depends on nothing platform-specific (no Bluetooth, PowerPoint, Input, Overlay). | Pure and unit-testable; shared fixtures keep both platforms identical. |
| `Effects` depends only on cursor/time/settings input and a drawing surface. | Effects stay testable and independent of I/O. |
| Only `PowerPoint/` touches Office Interop types, and only via the UI-thread dispatcher. | COM threading safety. |
| Only `Input/` calls cursor/mouse Win32 APIs. | One place to guarantee `ReleaseAll()` on every exit path. |
| Only `Overlay/` creates or destroys the overlay window. | One owner for lifecycle and cleanup. |
| Android UI composables hold presentation logic only; connection and gesture state live in testable classes. | See [rules.md §3](rules.md#3-code-quality-and-module-boundaries). |

## 4. Technology stack

Versions are **pinned at Phase 1** and recorded in the repository build files; do not upgrade opportunistically ([rules.md §9](rules.md#9-dependencies-and-build-hygiene)).

### Android

| Concern | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose |
| Build | Gradle (Kotlin DSL) with a pinned wrapper and version catalog *(proposed)* |
| Settings | Android DataStore ([backend_schema.md §3](backend_schema.md#3-android-local-preferences)) |
| Bluetooth | Classic RFCOMM, project 128-bit service UUID |
| Background | User-started foreground service (connected-device type) |
| Volume keys | Active `MediaSession` + `VolumeProvider` |
| Min Android | Android 12 / API 31 *(proposed; confirm on the reference phone and widen only with test evidence)* |
| Tests | JUnit for pure logic (protocol codec, gesture state machine, session state); instrumented tests for service/permission flows |
| Static analysis | ktlint + detekt *(proposed)* |

### Windows add-in

| Concern | Choice |
|---|---|
| Add-in model | VSTO/COM add-in for desktop PowerPoint |
| Runtime | .NET Framework 4.8 |
| PowerPoint API | Office PowerPoint Interop / object model |
| Bluetooth | `IBluetoothTransport` — WinRT RFCOMM first; native Winsock/Bluetooth RFCOMM behind the same interface if Gate 0.1 requires. **Never** a helper executable. |
| Cursor / input | Win32 `SetCursorPos`; mouse input only for button down/up and scroll |
| Overlay | Layered Win32 window (`WS_EX_LAYERED \| WS_EX_TRANSPARENT \| WS_EX_NOACTIVATE \| WS_EX_TOOLWINDOW`), GDI+ off-screen bitmap, `UpdateLayeredWindow` |
| Settings | `%LocalAppData%\PocketPresenter\settings.json` ([backend_schema.md §4](backend_schema.md#4-windows-local-settings)) |
| Tests | xUnit (or MSTest) with fakes for COM wrappers *(proposed)* |
| Installer / signing | Decided in Wave 5 (architecture open question Q5) |

## 5. Key interfaces (illustrative)

These sketches show intent and boundaries, not final signatures.

```csharp
// Windows — transport (runs off the UI thread)
public interface IBluetoothTransport : IDisposable
{
    event Action<ReadOnlyMemory<byte>> BytesReceived;
    event Action<TransportState> StateChanged;     // Disconnected, Connecting, Connected
    Task ConnectAsync(PeerId peer, CancellationToken ct);
    Task SendAsync(ReadOnlyMemory<byte> frame, CancellationToken ct);
    Task DisconnectAsync();
}

// Windows — PowerPoint access (UI/STA thread only, via dispatcher)
public interface IPowerPointController
{
    bool HasActiveSlideShow { get; }
    CommandResult Next();        // returns NO_ACTIVE_SLIDESHOW instead of throwing
    CommandResult Previous();
}

// Windows — input (the only place that touches cursor/mouse APIs)
public interface IInputController
{
    void MoveBy(int dx, int dy);
    void ButtonDown(MouseButton b);
    void ButtonUp(MouseButton b);
    void Scroll(int dy);
    void ReleaseAll();           // MUST be called on every disconnect/error/shutdown path
}

// Windows — effects (pure update/render, no I/O)
public interface ICursorEffect
{
    EffectId Id { get; }          // Honeycomb, Diamond, Ripple
    void Update(in EffectInput input, TimeSpan dt);   // cursor position, speed, click events, settings
    bool IsIdle { get; }          // renderer stops scheduling frames when true
    void Render(IEffectSurface surface);
}
```

```kotlin
// Android — transport and session
interface RfcommTransport {
    val events: Flow<TransportEvent>          // Connected, Frame(bytes), Closed(reason)
    suspend fun start()                        // open listener (user-started session only)
    suspend fun send(frame: ByteArray)
    fun close()
}

sealed interface SessionState {
    data object Disconnected : SessionState
    data object PhoneReady : SessionState
    data object Connecting : SessionState
    data object ConnectedIdle : SessionState
    data object PresentingArmed : SessionState
    data object Disconnecting : SessionState
}
```

`Off` is a first-class effect value in settings and the protocol; the renderer treats it as "overlay shows nothing", not as a separate code path that can leak a visible window.

## 6. Platform requirements

### Android (expected; verify in Gate 0 against the target SDK)

| Need | Detail |
|---|---|
| Bluetooth | `BLUETOOTH_CONNECT` for connecting/listening on Android 12+. Request `BLUETOOTH_SCAN` / `BLUETOOTH_ADVERTISE` only if the final discovery/discoverability design needs them. Pairing itself is done in system settings. |
| Foreground service | Foreground-service permission plus the connected-device service type and its permission; start only after a user action (background-start restrictions apply). |
| Notifications | A visible notification with a **Stop** action while a session is active; notification runtime permission on versions that require it. |
| Media | `MediaSession` active during a session; behaviour with screen off varies by OEM and is validated per device (Gate 0.3). |
| Not used | AccessibilityService, location, storage, internet. |

### Windows

| Need | Detail |
|---|---|
| OS | Windows 10/11, 64-bit |
| PowerPoint | Desktop PowerPoint; **bitness (32/64) must match the add-in build** and is recorded in the supported-configuration matrix |
| Runtime | VSTO runtime / .NET Framework 4.8 |
| DPI | The host process's DPI awareness is set by Office, not by the add-in. Query it at runtime and verify pixel↔point conversion for overlay, `SetCursorPos` and Draw Mode in Gate 0.2 at 100%/150%/200% scaling. |
| Privilege | Run as the normal user. Do not request elevation as a workaround; detect and explain privilege mismatches (e.g., PowerPoint run as administrator). |
| Trust | Document signing/trust steps for enterprise-managed Office ([prd.md §11](prd.md#11-key-risks-and-feasibility-gates)). |

## 7. Protocol constants and shared fixtures

- The wire format and limits are defined only in [protocol.md](protocol.md).
- Each platform keeps its constants (enums, limits, error codes) in **one module**: `Protocol/` (C#) and `bluetooth/` codec package (Kotlin).
- Both test suites load [`protocol/fixtures/vectors.json`](../protocol/fixtures/vectors.json). A protocol change is complete only when the document, both implementations and the fixtures change together.

## 8. Latency budget

Illustrative starting budget for the median path (touch → visible cursor). **Replace with measured values** from Gate 0.1/0.2; target remains median < 50 ms and p95 < 100 ms ([prd.md AT-02](prd.md#5-goals-and-success-measures)).

| Stage | Owner | Starting budget |
|---|---|---|
| Touch sampling + coalescing | Phone | ≤ 16 ms |
| Bluetooth transmit | Radio / OS | ≤ 20 ms *(highly variable; measure)* |
| Receive, decode, validate | Add-in (background) | ≤ 2 ms |
| Hand-off to input dispatch | Add-in | ≤ 2 ms |
| `SetCursorPos` + display refresh | Windows | ≤ 16 ms |

Measurement method: phone logs a monotonic timestamp per sampled delta; add-in logs receive and apply times; compare using a one-time clock-offset estimate over ping/pong. Record the method alongside the numbers.

## 9. Diagnostics policy

- Off by default; user can enable, open and clear logs ([backend_schema.md §6](backend_schema.md#6-local-diagnostics-model)).
- Structured entries only: component, event, state transition, command *type*, duration, error category. No payload dumps.
- Never log slide content, presentation paths, pairing material or device addresses beyond what a diagnosis strictly needs.
- Bounded rotation; logging must never block the PowerPoint UI thread.

## 10. Tooling, testing and build hygiene

- Pin Gradle, Kotlin, Compose, Office interop and NuGet versions; keep Android and Windows dependency sets separate.
- Test obligations: [architecture.md §17](architecture.md#17-test-plan-and-acceptance-criteria) and [rules.md §8](rules.md#8-testing-rules).
- CI: build and unit-test both projects on a clean checkout without introducing a cloud-service dependency. Hardware tests remain manual and are recorded in [../tests/manual-test-matrix.md](../tests/manual-test-matrix.md).
- Generated binaries, build output, local SDK paths and secrets are never committed ([../.gitignore](../.gitignore)).

## 11. Revision history

| Version | Change |
|---|---|
| 1.0 | Created from the architecture, protocol, data-model and rules documents to resolve the missing `trd.md` reference. |
