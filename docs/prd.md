# PocketPresenter — Product Requirements Document

| | |
|---|---|
| **Version** | 1.1 |
| **Status** | Proposed. Feasibility gates are mandatory before full implementation. |
| **Product type** | Native Android controller + Microsoft PowerPoint desktop add-in for Windows |
| **Owns** | Product scope, requirement IDs (`FR-*`, `NFR-*`), acceptance targets (`AT-*`), non-goals |
| **Architecture reference** | [architecture.md](architecture.md) |
| **Related** | [trd.md](trd.md) · [app_flow.md](app_flow.md) · [ui_ux_brief.md](ui_ux_brief.md) · [implementation_plan.md](implementation_plan.md) · [traceability.md](traceability.md) |

> **Precedence.** This document defines *what* the product must do. If it conflicts with another document, stop and resolve the conflict explicitly (see [rules.md §1](rules.md#1-source-of-truth-and-scope-discipline)).

## Table of contents

1. [Product summary](#1-product-summary)
2. [Problem statement](#2-problem-statement)
3. [Target users](#3-target-users)
4. [Product principles](#4-product-principles)
5. [Goals and success measures](#5-goals-and-success-measures)
6. [MVP scope](#6-mvp-scope)
7. [Explicit non-goals](#7-explicit-non-goals)
8. [Functional requirements](#8-functional-requirements)
9. [Non-functional requirements](#9-non-functional-requirements)
10. [Assumptions and dependencies](#10-assumptions-and-dependencies)
11. [Key risks and feasibility gates](#11-key-risks-and-feasibility-gates)
12. [Release definition](#12-release-definition)
13. [Open questions](#13-open-questions)
14. [Revision history](#14-revision-history)

---

## 1. Product summary

PocketPresenter lets a presenter control an existing Microsoft PowerPoint slideshow from an Android phone over Bluetooth Classic. The phone acts as a wireless touchpad and slide remote. A PowerPoint-integrated add-in receives commands and renders a minimal animated geometric effect around the Windows cursor during the slideshow.

The presenter must not need to open a separate Windows companion application. PowerPoint remains responsible for rendering the original `.pptx` file, slides, animations, transitions, fonts and media.

## 2. Problem statement

Presenters often need to advance slides and point precisely at content while standing away from their laptop. A basic slide remote does not provide a phone-based touchpad, and ordinary remote-mouse software does not provide a restrained, customizable cursor effect designed for presenting. PocketPresenter combines these interactions without replacing PowerPoint or requiring an internet service.

## 3. Target users

| Persona | Needs | Notes |
|---|---|---|
| Students presenting classroom or project work | Advance slides and point at content from a distance; minimal setup | Often use unfamiliar laptops and projectors; setup must be short and failure states understandable. |
| Teachers and seminar presenters | Reliable navigation, visible pointer on a projector, no surprises | Value predictable behavior over visual flair. |
| Demonstrators | Point, click and make simple annotations while standing away | Draw Mode (P1) is most relevant. |
| Windows desktop PowerPoint users who prefer a phone controller | No extra hardware, no extra Windows app | Already own a Bluetooth-capable laptop and Android phone. |

## 4. Product principles

1. **PowerPoint stays in control of the presentation.** Do not rebuild the PPTX rendering engine.
2. **No separate Windows app to launch.** Connection and control live in a PowerPoint VSTO/COM add-in. A temporary overlay window is permitted as an internal rendering surface only.
3. **Bluetooth-first.** The MVP must not require Wi-Fi, internet, accounts, cloud services or a backend.
4. **Responsiveness before decoration.** Cursor movement must never wait for an animation to finish.
5. **Subtle by default.** Effects must remain visible but not obscure slide content.
6. **Prove risky integrations first.** Bluetooth inside the PowerPoint add-in, the overlay and screen-off volume handling must be tested before expanding features.
7. **Honest states.** The product never reports success it has not achieved (no fake connection, no unverified "supported" claims).

## 5. Goals and success measures

### MVP goals

- Control an existing desktop PowerPoint slideshow from a paired Android phone.
- Use Volume Up for next slide and Volume Down for previous slide, including screen-off operation on validated device configurations.
- Move and click the Windows cursor from the phone.
- Change among three cursor effects (plus Off) while the slideshow is running: Honeycomb Pulse, Diamond Facets and Geometric Ripple.
- Keep the effect aligned over the presentation and prevent the overlay from stealing focus or blocking clicks.
- Recover safely from Bluetooth disconnects and release any held mouse-button state.

### Acceptance targets

Each target is an engineering target, **not** a claim about already-measured performance. Record results in [feasibility-report.md](feasibility-report.md) (Gate 0) or [../tests/manual-test-matrix.md](../tests/manual-test-matrix.md) (ongoing).

| ID | Target | How verified |
|---|---|---|
| AT-01 | At least 100 consecutive next/previous commands in a controlled test, with no duplicate or lost commands under a stable Bluetooth connection. | Scripted + physical-button run; count commands sent, received, executed. |
| AT-02 | Touchpad responsiveness: median phone-input-to-cursor response below 50 ms and 95th percentile below 100 ms on the reference hardware. | Timestamped measurement (see [trd.md §8](trd.md#8-latency-budget)); report both numbers. |
| AT-03 | Effect switching does not move the cursor, restart the slideshow or block presentation controls. | Switch effects repeatedly during a show; compare cursor coordinates before/after. |
| AT-04 | Disconnect hides the overlay and resets all held input states within one second of detecting the link loss. | Induce disconnect mid-drag; measure from detection to cleanup. |
| AT-05 | Tests cover laptop-only display, extended projector, Presenter View, bright/dark slides and presentation exit. | Manual matrix rows completed with evidence. |
| AT-06 | Screen-off volume navigation is marked supported only on Android models/versions that pass the validation matrix. | Per-device matrix; unsupported devices listed as such. |
| AT-07 | A 60-minute presentation soak shows no memory growth, stuck input, unbounded queues or overlay leaks. | Soak run with memory/handle sampling. |

## 6. MVP scope

### P0 — Required for initial usable release

- Android app built with Kotlin and Jetpack Compose.
- Pair/connect workflow using Bluetooth Classic RFCOMM.
- Connection and session state: disconnected, connecting, connected, presenting and error (canonical names in [architecture.md §12](architecture.md#12-state-model)).
- Fixed hardware mapping: Volume Up → next slide; Volume Down → previous slide.
- Touchpad pointer movement.
- Left-click, right-click, scroll and safe button down/up handling.
- Three live-selectable effects: Honeycomb Pulse, Diamond Facets and Geometric Ripple.
- Minimal effect intensity/size control and an Off option.
- PowerPoint ribbon integration for connecting, disconnecting and viewing status.
- Overlay lifecycle linked to the slide show; transparent, click-through and non-activating behavior.
- Local preferences for device/effect choices and sensitivity.
- Clear connection failure messaging and safe recovery.

### P1 — Add only after P0 passes

- Draw Mode using PowerPoint slideshow annotation APIs.
- Hold-and-drag gesture and optional drag lock.
- Haptic acknowledgement for discrete actions.
- Reduced-motion setting, left-handed layout and pointer calibration.
- PDF/application support only if it can be added without undermining the PowerPoint-first scope.

## 7. Explicit non-goals

- A standalone Windows companion app or background helper executable.
- Replacing PowerPoint with a complete `.pptx` rendering engine.
- PowerPoint for web, macOS, Linux or mobile PowerPoint as targets for V1.
- Cloud relay, hosted API, accounts, analytics, remote storage or internet access.
- A database server or Supabase/Firebase dependency.
- A large game engine or physically based 3D renderer for a small cursor effect.
- Global control of arbitrary desktop applications as a guaranteed MVP feature.
- Guaranteed overlay capture in every application-window-only screen-sharing mode.
- Guaranteed volume-key interception after force-stop, reboot or on every Android vendor/device.
- Advanced slide authoring or editing the underlying presentation file.

## 8. Functional requirements

Each requirement has stable ID, priority, a testable acceptance criterion and links to the design that realises it.

### FR-01 — Pair and connect (P0)

The user pairs the phone and Windows PC using the OS Bluetooth interface, starts a controller session on Android and connects from the PocketPresenter PowerPoint ribbon. Pairing alone must not activate pointer control.

- **Acceptance:** after OS pairing only, no cursor movement or slide command is accepted; after the handshake completes, state is *connected* and controls enable per [app_flow.md §3](app_flow.md#3-state-driven-controls).
- **Design:** [architecture.md §6](architecture.md#6-bluetooth-connection-and-pairing), [protocol.md §3](protocol.md#3-session-handshake).

### FR-02 — Slide navigation (P0)

Volume Up emits `slide.next`; Volume Down emits `slide.previous`. The mapping is fixed and must not be silently remapped. On-screen Previous/Next controls are available as a fallback while the phone is awake. Duplicate events must be prevented through press/release handling, sequence IDs and a suitable repeat policy.

- **Acceptance:** AT-01 and AT-06.
- **Design:** [architecture.md §8–§9](architecture.md#8-android-volume-button-handling).

### FR-03 — Pointer and gestures (P0; drag gesture P1)

Touch movement controls the real Windows cursor. The system supports tap/click, right-click, scroll and a defined drag gesture. Pointer control is suspended when the user stops the session or the trusted presentation target becomes invalid.

- **Acceptance:** AT-02; no held button remains after cancellation or disconnect (AT-04).
- **Design:** [architecture.md §10](architecture.md#10-cursor-touchpad-and-draw-mode).

### FR-04 — Effects (P0)

The user can select `honeycomb`, `diamond`, `ripple` or `off` from the phone during a slideshow. Effect selection changes visual state only. It must not alter cursor coordinates, focus, slide state or pointer sensitivity.

- **Acceptance:** AT-03.
- **Design:** [architecture.md §11](architecture.md#11-cursor-effect-renderer), [ui_ux_brief.md §6](ui_ux_brief.md#6-the-three-windows-cursor-effects).

### FR-05 — Overlay (P0)

The add-in creates an internal transparent overlay only while the session and eligible slide show are active. It follows the presentation monitor, remains aligned through supported window changes and is hidden on slide-show exit, disconnect, add-in shutdown or fatal error.

- **Acceptance:** overlay never blocks a deliberate click, never takes focus, and is not visible after the session ends (AT-04, AT-05).
- **Design:** [architecture.md §9, §11](architecture.md#9-powerpoint-lifecycle-and-slide-commands).

### FR-06 — Draw Mode (P1)

If included in the release, the presenter can deliberately enter Draw Mode, draw on the active slide, and exit back to Point Mode. Draw Mode must not be activated accidentally by ordinary cursor movement.

- **Acceptance:** entering and leaving Draw Mode requires an explicit action; normal movement never draws.
- **Design:** [architecture.md §10](architecture.md#10-cursor-touchpad-and-draw-mode).

### FR-07 — Failure reporting (P0)

The phone and PowerPoint ribbon show understandable states for disconnected, connected, active presentation, unsupported target, reconnecting and error. Do not report success if a command could not be executed.

- **Acceptance:** every row in the [error catalog](app_flow.md#4-error-and-recovery-catalog) produces its documented message and recovery action.
- **Design:** [app_flow.md](app_flow.md), [protocol.md §6](protocol.md#6-error-codes).

### FR-08 — Pocket Mode (P0, clarifies FR-02)

*Added in PRD v1.1 to make the screen-off requirement explicit; it was previously described only in [app_flow.md](app_flow.md).* The user can enter Pocket Mode so that physical volume buttons keep working while the phone display is off, on validated devices. The app states clearly that touchpad use is unavailable with the screen off and shows whether screen-off volume control is validated for this device.

- **Acceptance:** AT-06; the UI never promises screen-off control on an unvalidated device.
- **Design:** [app_flow.md Screen H](app_flow.md#screen-h--pocket-mode), [architecture.md §8](architecture.md#8-android-volume-button-handling).

### FR-09 — PowerPoint ribbon (P0)

*Added in PRD v1.1; derived from the P0 scope item "PowerPoint ribbon integration".* The add-in provides a **PocketPresenter** ribbon group with Connect phone / Disconnect, a connection indicator and a troubleshooting/diagnostics entry. It does not auto-connect or auto-control.

- **Acceptance:** ribbon state always matches the real link and presentation state.
- **Design:** [app_flow.md — PowerPoint ribbon](app_flow.md#powerpoint-ribbon--status-ui).

## 9. Non-functional requirements

*IDs added in PRD v1.1 so tests and design documents can cite them.*

| ID | Area | Requirement |
|---|---|---|
| NFR-01 | Responsiveness | Coalesce stale pointer updates while retaining correct total movement; never queue old pointer positions indefinitely. Targets: AT-02. |
| NFR-02 | Reliability | The PowerPoint UI thread must not block on Bluetooth reads, pairing or rendering. Soak: AT-07. |
| NFR-03 | Security | Only a trusted paired device/session can issue commands. No generic remote shell or arbitrary command execution. See [architecture.md §14](architecture.md#14-security-and-threat-model). |
| NFR-04 | Privacy | No telemetry or cloud upload in the MVP. Logs must not contain personal content from slide decks. See [backend_schema.md §6](backend_schema.md#6-local-diagnostics-model). |
| NFR-05 | Accessibility | Readable controls, clear status labels, adjustable sensitivity and reduced-motion option. See [ui_ux_brief.md §9](ui_ux_brief.md#9-accessibility-and-usability). |
| NFR-06 | Compatibility | Document exact supported Windows, Office and Android combinations based on testing. |
| NFR-07 | Maintainability | Keep transport, protocol, PowerPoint automation, input and rendering as separate modules. See [trd.md §3](trd.md#3-module-boundaries). |

## 10. Assumptions and dependencies

| # | Assumption / dependency | If false |
|---|---|---|
| A1 | Target laptop has a Bluetooth Classic (BR/EDR) adapter usable from desktop apps. | Product cannot work as designed; document requirement. |
| A2 | Users can install and trust a VSTO add-in on their PowerPoint (permissions / Office policy). | Provide signing and trust guidance; enterprise-locked machines are unsupported. |
| A3 | Android can keep a foreground session and deliver volume keys with the screen off on the reference phone. | Mark device unsupported; keep on-screen fallback. |
| A4 | Desktop PowerPoint (Windows) exposes slide-show lifecycle events and navigation through its object model. | Re-evaluate at Gate 0. |
| A5 | Presenters use display-level output (projector/extended display) rather than window-only capture. | Document limitation; separate architecture decision if mandatory. |

## 11. Key risks and feasibility gates

| Risk | Gate / mitigation |
|---|---|
| RFCOMM APIs do not behave correctly inside the VSTO host | Gate 0: prove pairing, connection and bidirectional framed messages in the real add-in process. Stop and fix before full implementation. |
| Overlay blocks clicks, steals focus or appears on the wrong monitor | Gate 0: test layered, click-through, non-activating overlay on a real slideshow and external display. |
| Android routes volume keys to phone volume when screen is off | Gate 0: test `MediaSession`/remote volume handling on the target phone. Do not claim universal support. |
| PowerPoint lifecycle changes during Presenter View/transition/exit | Subscribe to lifecycle events, detect the actual slide-show window and test normal/abnormal exits. |
| Bluetooth loss leaves mouse button down | Reset every held input state immediately on disconnect/timeout. |
| Office security or install policy blocks the add-in | Document trusted installation/signing and provide clear setup diagnostics. |

## 12. Release definition

A release is acceptable only when all P0 functional requirements pass the acceptance tests in [implementation_plan.md](implementation_plan.md), known limitations are documented, and the supported device matrix contains actual test results. Features that fail a feasibility gate are not hidden behind optimistic claims; they are fixed, narrowed to a validated configuration, or explicitly deferred.

**Release checklist**

- [ ] FR-01 … FR-05, FR-07, FR-08, FR-09 pass; FR-06 passes or is explicitly deferred.
- [ ] AT-01 … AT-07 recorded with evidence.
- [ ] Supported-configuration matrix lists only tested combinations.
- [ ] Known limitations published (screen-off, window-only capture).
- [ ] No separate Windows executable, cloud, Wi-Fi or analytics present.

## 13. Open questions

| # | Question | Resolved by |
|---|---|---|
| PQ1 | Is application-window-only screen sharing a required scenario for the target users? | Product decision before Wave 3 |
| PQ2 | Is a first-connection confirmation code acceptable UX? (see [decisions.md ADR-0009](decisions.md)) | Wave 2 usability test |
| PQ3 | Which right-click gesture is primary: two-finger tap or a dedicated button? Default in the design: two-finger tap with visible buttons as fallback. | Usability testing |

## 14. Revision history

| Version | Change |
|---|---|
| 1.0 | Initial PRD. |
| 1.1 | Added requirement/acceptance IDs (`NFR-*`, `AT-*`), personas, assumptions, open questions, release checklist; added FR-08 (Pocket Mode) and FR-09 (ribbon) as explicit requirements; added `off` to FR-04 list in scope text; added AT-07 (60-minute soak) and the median latency target from the architecture. |
