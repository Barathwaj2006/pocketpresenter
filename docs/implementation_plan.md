# PocketPresenter — Implementation Plan

| | |
|---|---|
| **Version** | 1.1 |
| **Rule** | Complete and verify each gate before proceeding. Do not substitute mock success for a real platform test. |
| **Primary references** | [architecture.md](architecture.md) · [prd.md](prd.md) · [trd.md](trd.md) · [app_flow.md](app_flow.md) · [rules.md](rules.md) |

## Table of contents

1. [Status tracker](#status-tracker)
2. [Delivery principles](#delivery-principles)
3. [How phases map to architecture waves](#how-phases-map-to-architecture-waves)
4. [Gate 0 — Environment and feasibility spikes](#gate-0--environment-and-feasibility-spikes)
5. [Phase 1 — Repository and engineering foundation](#phase-1--repository-and-engineering-foundation)
6. [Phase 2 — Protocol and Bluetooth transport](#phase-2--protocol-and-bluetooth-transport)
7. [Phase 3 — PowerPoint command control](#phase-3--powerpoint-command-control)
8. [Phase 4 — Pointer controller and input safety](#phase-4--pointer-controller-and-input-safety)
9. [Phase 5 — Overlay lifecycle and baseline cursor](#phase-5--overlay-lifecycle-and-baseline-cursor)
10. [Phase 6 — Three effects and live switching](#phase-6--three-effects-and-live-switching)
11. [Phase 7 — Android controller UI and session integration](#phase-7--android-controller-ui-and-session-integration)
12. [Phase 8 — Draw Mode (optional P1)](#phase-8--draw-mode-optional-p1)
13. [Phase 9 — Hardening and release readiness](#phase-9--hardening-and-release-readiness)
14. [Definition of done](#definition-of-done)
15. [Change-control rule](#change-control-rule)

## Status tracker

Update this table at the end of every milestone, using the completion report in [rules.md §11](rules.md#11-required-completion-report). Never mark a row *Passed* without evidence.

| Stage | State | Evidence link | Last updated |
|---|---|---|---|
| Gate 0 | ⏳ Not started | [feasibility-report.md](feasibility-report.md) | — |
| Phase 1 | Blocked on Gate 0 *(repo scaffold may start; see note)* | — | — |
| Phase 2 | Blocked on Gate 0.1 | — | — |
| Phase 3 | Blocked on Phase 2 | — | — |
| Phase 4 | Blocked on Phase 2 | — | — |
| Phase 5 | Blocked on Gate 0.2 | — | — |
| Phase 6 | Blocked on Phases 3, 4, 5 | — | — |
| Phase 7 | Partially parallel (see note) | — | — |
| Phase 8 | Optional, after P0 | — | — |
| Phase 9 | Blocked on all P0 phases | [../tests/manual-test-matrix.md](../tests/manual-test-matrix.md) | — |

*Note:* the repository scaffold in Phase 1 contains no product code and may proceed alongside Gate 0 so that spike code has a home. Phase 7's static screens may be built in parallel after Phase 2, but its volume/Pocket Mode integration depends on Gate 0.3 and must not be marked complete earlier.

## Delivery principles

- Build the smallest end-to-end path first.
- Keep every phase buildable and reviewable.
- Do not begin the three polished effects until Bluetooth, PowerPoint navigation and the overlay pass their feasibility tests.
- Any failed gate pauses dependent work. Fix it, narrow the supported scope with an explicit decision, or document/defer the feature; never mark it complete by assumption.
- No separate Windows executable, cloud backend, web service or unrelated feature waves.

## How phases map to architecture waves

[architecture.md §16](architecture.md#16-performance-targets-feasibility-gates-and-waves) defines five *waves*; this plan breaks them into *phases*.

| Architecture wave | Plan phases | Notes |
|---|---|---|
| Gate 0 | Gate 0 | Same gate; three proofs 0.1 Bluetooth, 0.2 Overlay, 0.3 Volume |
| Wave 1 — Minimal usable controller | Phases 1, 2, 3, basic parts of 4 and 5, first screens of 7 | Cursor move + left click, simple marker only |
| Wave 2 — Production pointer behavior | Remainder of Phases 2 and 4 | Right click, scroll, drag, recovery, multi-monitor |
| Wave 3 — Three cursor effects | Phases 5 (complete) and 6, plus Phase 7 effect UI | Live switching, intensity, reduced motion |
| Wave 4 — Draw Mode and hardening | Phases 8 and 9 | 60-minute soak, disconnect and screen-off tests |
| Wave 5 — Release packaging | Installer items in Phase 9 | Signing, repair/uninstall, supported matrix |

---

## Gate 0 — Environment and feasibility spikes

Maps to architecture proofs: **0.1 Bluetooth** (G0-04), **0.2 Overlay** (G0-05 – G0-07), **0.3 Volume** (G0-08 – G0-09).

- [ ] **G0-01** Confirm Windows version, PowerPoint desktop edition/bitness and installation/trust model.
- [ ] **G0-02** Confirm the reference Android model and Android version.
- [ ] **G0-03** Create a minimal VSTO/COM add-in that loads in PowerPoint and adds a `PocketPresenter` ribbon group.
- [ ] **G0-04** In the real add-in host, prove Bluetooth Classic RFCOMM can connect to the paired Android session and exchange multiple framed messages.
- [ ] **G0-05** Demonstrate a small transparent, click-through, non-activating overlay over an active PowerPoint slideshow.
- [ ] **G0-06** Verify overlay does not steal focus or block a deliberate click.
- [ ] **G0-07** Verify overlay aligns over the correct display in laptop-only and extended-monitor modes.
- [ ] **G0-08** Build a minimal Android test that routes Volume Up to a test `slide.next` event and Volume Down to `slide.previous` using the candidate media-session approach.
- [ ] **G0-09** Test the volume mapping with screen on, locked/screen off, battery restrictions and a competing media session on the reference phone.
- [ ] **G0-10** Record exact test results, versions and limitations in [feasibility-report.md](feasibility-report.md) (gate evidence) and the per-device rows in [../tests/manual-test-matrix.md](../tests/manual-test-matrix.md).

**Gate passes only when:** RFCOMM, the overlay and PowerPoint loading work in the actual target environment, and screen-off button support has a documented outcome on the reference phone. Do not start the full renderer if the overlay cannot work in the intended presentation mode.

**Spike code rule:** spike code is disposable. Do not promote it into `android/` or `windows-addin/` product modules without review against [rules.md §3](rules.md#3-code-quality-and-module-boundaries).

## Phase 1 — Repository and engineering foundation

- [ ] **P1-01** Create the repository structure from [trd.md §2](trd.md#2-repository-structure).
- [ ] **P1-02** Add this documentation pack and `.gitignore`.
- [ ] **P1-03** Create Android and Windows solutions/projects with pinned supported build tools.
- [ ] **P1-04** Add CI/build instructions where practical, without inventing a cloud service dependency.
- [ ] **P1-05** Add formatting/static-analysis configuration suitable for Kotlin and C#.
- [ ] **P1-06** Add a `README.md` section with verified local build and install steps.
- [ ] **P1-07** Establish separate module boundaries for protocol, transport, PowerPoint, input and renderer.
- [ ] **P1-08** Add basic unit-test projects and a diagnostics policy ([trd.md §9](trd.md#9-diagnostics-policy)).
- [ ] **P1-09** Wire both test suites to the shared [protocol fixtures](../protocol/fixtures/vectors.json).

**Exit criteria:** clean checkout builds both projects; no secrets or build outputs are tracked.

## Phase 2 — Protocol and Bluetooth transport

- [ ] **P2-01** Implement RFCOMM listener/connector roles selected by Gate 0.
- [ ] **P2-02** Implement newline-framed JSON decoder that handles partial reads and multiple messages in one read.
- [ ] **P2-03** Implement bounded frame size, JSON validation, `sid`/sequence IDs and version negotiation.
- [ ] **P2-04** Implement `hello`, heartbeat, connection state and acknowledgements.
- [ ] **P2-05** Implement timeout/reconnect state machine with bounded backoff.
- [ ] **P2-06** Implement a command router that rejects unknown or unsafe commands.
- [ ] **P2-07** On connection loss, clear queued movement and release all held inputs.
- [ ] **P2-08** Add unit tests for framing, malformed data, large frames, duplicate command IDs and stale session messages; all fixture vectors V01–V10 pass on both platforms.
- [ ] **P2-09** Evaluate the held-button watchdog proposal (architecture Q7) and record the decision in [decisions.md](decisions.md).

**Exit criteria:** 1,000 scripted protocol messages, including fragmented reads and injected disconnects, complete without unbounded memory growth or stale command replay.

## Phase 3 — PowerPoint command control

- [ ] **P3-01** Add connect/disconnect and status to the PowerPoint ribbon.
- [ ] **P3-02** Detect slide-show start, current view/window and slide-show end.
- [ ] **P3-03** Implement next/previous via PowerPoint Object Model on the PowerPoint STA/UI thread.
- [ ] **P3-04** Implement command acknowledgements and explicit errors when no eligible slide show is active.
- [ ] **P3-05** Add a manual Windows test for repeated commands, last-slide behavior and focus changes.
- [ ] **P3-06** Confirm the add-in unloads cleanly and removes all event subscriptions.

**Exit criteria:** no duplicate or lost slide commands in the acceptance run under stable Bluetooth (AT-01); commands are not sent to unrelated applications.

## Phase 4 — Pointer controller and input safety

- [ ] **P4-01** Implement pointer coordinate mapping and sensitivity settings.
- [ ] **P4-02** Implement left/right click, scroll and button down/up tracking.
- [ ] **P4-03** Implement phone gesture state machine and cancellation behavior.
- [ ] **P4-04** Add timeout handling that releases every held button.
- [ ] **P4-05** Handle monitor bounds, virtual desktop coordinates, DPI scaling and target monitor changes.
- [ ] **P4-06** Test pointer accuracy, edge behavior and disconnects during a drag.
- [ ] **P4-07** Measure touch-to-cursor latency (median and p95) and record the method ([trd.md §8](trd.md#8-latency-budget)).

**Exit criteria:** cursor control remains predictable, and no disconnect path leaves a mouse button held (AT-02, AT-04).

## Phase 5 — Overlay lifecycle and baseline cursor

- [ ] **P5-01** Implement the add-in-owned transparent overlay window.
- [ ] **P5-02** Verify click-through, non-activation and target-window positioning.
- [ ] **P5-03** Display a simple cursor-following dot/ring with a single color and intensity.
- [ ] **P5-04** Link overlay visibility to authenticated connection and active slide-show lifecycle.
- [ ] **P5-05** Hide and clean up on disconnect, slide-show exit, PowerPoint shutdown and rendering errors.
- [ ] **P5-06** Test laptop-only display, projector/extended display, Presenter View, DPI scaling and transitions.

**Exit criteria:** baseline overlay is consistently aligned, never blocks clicks, never steals focus and never remains visible after the session ends (AT-04, AT-05).

## Phase 6 — Three effects and live switching

- [ ] **P6-01** Define common `ICursorEffect` renderer interface ([trd.md §5](trd.md#5-key-interfaces-illustrative)).
- [ ] **P6-02** Implement Honeycomb Pulse with restrained local depth and one click ripple.
- [ ] **P6-03** Implement Diamond Facets using the same cursor anchor/footprint rules.
- [ ] **P6-04** Implement Geometric Ripple with two or three contours.
- [ ] **P6-05** Add live `effect.set`, `off`, intensity and reduced-motion options.
- [ ] **P6-06** Cache geometry/resources and limit redraw to the local effect region; stop scheduling frames when idle.
- [ ] **P6-07** Test light, dark and saturated slides and long-duration presentations; evaluate the dual-tone edge ([ui_ux_brief.md §6](ui_ux_brief.md#6-the-three-windows-cursor-effects)).
- [ ] **P6-08** Verify switching effects never alters cursor position or delays commands (AT-03).

**Exit criteria:** all three effects can be switched repeatedly during a slideshow with no focus change, cursor jump or presentation interruption.

## Phase 7 — Android controller UI and session integration

- [ ] **P7-01** Build setup/permission screen.
- [ ] **P7-02** Build Home/Connection state screen.
- [ ] **P7-03** Build Touchpad and Slides modes.
- [ ] **P7-04** Build effect picker (including Off) and basic settings.
- [ ] **P7-05** Integrate RFCOMM connection status with UI using the state-to-controls table ([app_flow.md §3](app_flow.md#3-state-driven-controls)).
- [ ] **P7-06** Add a user-started foreground session using the correct Android configuration.
- [ ] **P7-07** Implement screen-off volume handling behind a tested capability/status flag (depends on Gate 0.3).
- [ ] **P7-08** Build Pocket Mode activation and user-facing limitation messaging (validated / not validated / unsupported).
- [ ] **P7-09** Implement every row of the [error and recovery catalog](app_flow.md#4-error-and-recovery-catalog).
- [ ] **P7-10** Test screen rotation, process recreation and reconnect.

**Exit criteria:** the phone UI reflects actual transport state and does not pretend to be connected when it is not.

## Phase 8 — Draw Mode (optional P1)

- [ ] **P8-01** Verify supported PowerPoint slideshow annotation APIs.
- [ ] **P8-02** Add explicit Point/Draw mode selection.
- [ ] **P8-03** Convert phone touch coordinates to slide coordinates using the active slide-show viewport.
- [ ] **P8-04** Implement `draw.start` / `draw.point` / `draw.end` and bounded point batching.
- [ ] **P8-05** Verify drawing does not interfere with slide navigation, and define whether annotations are retained/cleared.
- [ ] **P8-06** If annotation behavior is unreliable, defer Draw Mode instead of implementing an unsafe approximation.

## Phase 9 — Hardening and release readiness

- [ ] **P9-01** Test the full acceptance matrix in [prd.md](prd.md) (AT-01 – AT-07) and [architecture.md §17](architecture.md#17-test-plan-and-acceptance-criteria).
- [ ] **P9-02** Test Bluetooth drop/reconnect while moving, clicking, drawing and changing effects.
- [ ] **P9-03** Test next/previous at first and final slides, repeat presses and media-session competition.
- [ ] **P9-04** Test screen-off behavior with exact device/OS/permission/battery settings recorded.
- [ ] **P9-05** Profile Windows CPU/GPU/memory and run the **60-minute** presentation soak ([architecture.md §16](architecture.md#16-performance-targets-feasibility-gates-and-waves), AT-07). *(v1.0 of this plan said 30 minutes; aligned to the architecture's 60.)*
- [ ] **P9-06** Verify local logs contain no slide content or secrets and can be cleared.
- [ ] **P9-07** Verify installer/add-in registration, trust/signing instructions, clean uninstall and add-in shutdown.
- [ ] **P9-08** Document supported Windows/Office/Android combinations and known limitations.
- [ ] **P9-09** Tag a release only after all P0 acceptance criteria pass.

## Definition of done

A task is done only when **all** are true:

1. The behavior works on the real target, not only in a mock or simulator.
2. Automated tests for the new rules exist and pass; the exact commands and results are reported.
3. Hardware-dependent claims have evidence in [feasibility-report.md](feasibility-report.md) or the [test matrix](../tests/manual-test-matrix.md).
4. Documentation and fixtures changed with any contract change.
5. No `TODO`/placeholder/fake success remains in the claimed feature ([rules.md §3](rules.md#3-code-quality-and-module-boundaries)).
6. The completion report ([rules.md §11](rules.md#11-required-completion-report)) is written and the status tracker updated.

## Change-control rule

Any change that introduces a Windows companion executable, Wi-Fi dependency, cloud backend, database, alternative app framework or custom PPTX renderer requires an explicit architecture review and an updated [prd.md](prd.md). Do not add such components merely to work around an unproven assumption; first validate the existing design at the relevant gate.
