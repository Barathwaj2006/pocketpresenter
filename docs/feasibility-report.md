# PocketPresenter — Gate 0 Feasibility Report

> **Status: NOT STARTED.** This file is the evidence log for Gate 0 defined in [architecture.md §16](architecture.md#16-performance-targets-feasibility-gates-and-waves). No feature wave may start until all three proofs below are marked **PASS** on the real target hardware.

Per-device and ongoing results are tracked in [../tests/manual-test-matrix.md](../tests/manual-test-matrix.md); this file is the gate-level evidence. Fill in every field. "Works on my machine" is not a result; record versions, steps and measured numbers.

## 1. Test environment

| Item | Value |
|---|---|
| Date(s) tested | 2026-10-19 |
| Tester | Barathwaj |
| Laptop model / CPU | Windows 11 PC (x64) |
| Windows version and build | Windows 11 Home Single Language (Build 26200, 64-bit) |
| Bluetooth adapter and driver version | Realtek Wireless Bluetooth Adapter |
| PowerPoint version / build / channel | Microsoft PowerPoint 365 / Office 16 (16.0.20430.20146) |
| PowerPoint bitness (32/64) | 64-bit |
| Phone model | OPPO F31 Pro 5G (Paired name: JIO_OPPO F31 Pro 5G) |
| Android version / OEM skin | Android 14 / ColorOS |
| Phone battery-optimization settings for the app | Unrestricted / Don't optimize |
| External display / projector used | Laptop internal display / HDMI output |
| Display scaling (%) | 100% / 125% / 150% |
| Presentation file used (size, media, transitions) | Test presentation (.pptx) |

## 2. Summary

| Proof | Result | Notes / blocker |
|---|---|---|
| 0.1 Bluetooth RFCOMM inside VSTO host | ☐ PASS ☐ FAIL ☐ PARTIAL | |
| 0.2 Click-through overlay over slide show | ☐ PASS ☐ FAIL ☐ PARTIAL | |
| 0.3 Volume buttons, screen off | ☐ PASS ☐ FAIL ☐ PARTIAL | |

**Gate 0 decision:** ☐ Proceed to Wave 1 ☐ Revise design (see §6) ☐ Stop

## 3. Proof 0.1 — Bluetooth inside the add-in

**Goal:** Android RFCOMM listener and a minimal client inside the PowerPoint VSTO process can connect, exchange commands and recover from disconnect. No separate EXE.

Transport tried: ☐ WinRT RFCOMM ☐ Native Winsock/Bluetooth API

| Check | Result | Evidence / notes |
|---|---|---|
| Phone paired through OS settings | | |
| Add-in discovers the phone's custom service UUID | | |
| Connection established from inside PowerPoint | | |
| `hello` / `hello.ack` handshake | | |
| 1000 `pointer.delta` messages received, total delta correct | | |
| Partial/combined frames decoded correctly | | |
| Phone Bluetooth toggled off → add-in detects within 15 s | | |
| Reconnect after disconnect, no stale replay | | |
| PowerPoint UI stays responsive during transfer | | |
| No helper executable was started (Task Manager check) | | |

Measured latency (touch → decoded in add-in), 200 samples: median ___ ms · p95 ___ ms

## 4. Proof 0.2 — Overlay

**Goal:** a basic click-through layered window follows the cursor over a real slide show and projector output without stealing focus or blocking clicks; it hides on `SlideShowEnd`.

| Check | Result | Evidence / notes |
|---|---|---|
| Overlay follows cursor on laptop display | | |
| Overlay follows cursor on external/projector display | | |
| Real clicks pass through (hyperlink / action button on slide) | | |
| Slide show keeps keyboard focus | | |
| Hidden immediately on `SlideShowEnd` | | |
| Hidden on Alt-Tab / minimize / target loss | | |
| No window in taskbar | | |
| Correct at 100% / 150% / 200% scaling | | |
| Presenter View enabled: overlay on the correct monitor | | |
| Video slide and transitions: no flicker or blocking | | |
| 60 FPS target while moving (measured) | | |
| CPU usage while idle / while moving | | |
| Display-level screen share shows the overlay | | |
| Window-level screen share (known limitation) | | |

## 5. Proof 0.3 — Volume buttons with screen off

**Goal:** Volume Up/Down produce exactly one correct command each, with the display off, without changing media volume.

| Condition | Up → next once? | Down → previous once? | System volume unchanged? | Notes |
|---|---|---|---|---|
| Screen on, app foreground | | | | |
| Screen on, app background | | | | |
| Locked, screen on | | | | |
| Screen off | | | | |
| Screen off after 10 min idle | | | | |
| Another media app playing | | | | |
| Battery saver on | | | | |
| After Bluetooth reconnect | | | | |

Was an active/“playing” media state required for delivery? ☐ No ☐ Yes — describe cost: ______

## 6. Blockers, deviations and design revisions

For every FAIL/PARTIAL, record what failed, what you tried, and the proposed change. Any change to a locked decision in [decisions.md](decisions.md) needs a new decision entry before work continues.

| # | Issue | Attempts | Proposed resolution | Decision entry |
|---|---|---|---|---|
| | | | | |

## 7. Sign-off

| Role | Name | Date |
|---|---|---|
| Tester | | |
| Reviewer | | |
