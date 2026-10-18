# PocketPresenter — Manual Test Matrix

Hardware and configuration results that automated tests cannot prove. Gate 0 evidence is also summarised in [../docs/feasibility-report.md](../docs/feasibility-report.md). A configuration may be listed as **supported** only if every required row passes; unlisted configurations are **not validated**.

Result legend: ✅ pass · ❌ fail · ➖ not applicable · ⬜ not run. Always record exact versions and steps.

## 1. Tested configurations

| ID | Windows build | PowerPoint (version / bitness) | BT adapter | Phone / Android / OEM skin | Display setup | Status |
|---|---|---|---|---|---|---|
| C1 | | | | | | ⬜ not validated |

## 2. Per-configuration checks

Copy this table once per configuration ID.

**Configuration C1**

| Area | Check | Result | Notes / evidence link |
|---|---|---|---|
| Connection | Pair via OS, start session, connect from ribbon, handshake completes | ⬜ | |
| Connection | Pairing alone does not move cursor or change slides | ⬜ | |
| Slides | 100 consecutive next/previous, none duplicated or lost (AT-01) | ⬜ | |
| Slides | First/last slide behavior natural, no unintended exit | ⬜ | |
| Slides | Command with no slide show returns `NO_ACTIVE_SLIDESHOW` and affects no other app | ⬜ | |
| Volume | Screen on, foreground | ⬜ | |
| Volume | Screen on, background | ⬜ | |
| Volume | Locked | ⬜ | |
| Volume | Screen off (also after 10 min idle) | ⬜ | |
| Volume | Battery saver / OEM battery restrictions | ⬜ | |
| Volume | Another media app active | ⬜ | |
| Volume | Media volume unchanged; exactly one command per press | ⬜ | |
| Pointer | Median / p95 latency measured (AT-02): ___ / ___ ms | ⬜ | Method: |
| Pointer | Tap, two-finger tap, scroll, drag; no stuck buttons | ⬜ | |
| Pointer | Disconnect mid-drag releases button; cleanup ≤ 1 s (AT-04) | ⬜ | |
| Overlay | Laptop-only display aligned | ⬜ | |
| Overlay | Extended / projector aligned | ⬜ | |
| Overlay | Presenter View correct monitor | ⬜ | |
| Overlay | Scaling 100% / 150% / 200% | ⬜ | |
| Overlay | Click-through and no focus steal | ⬜ | |
| Overlay | Hidden on show end, disconnect, Alt-Tab, minimize | ⬜ | |
| Effects | Honeycomb, Diamond, Ripple, Off switch live; cursor unchanged (AT-03) | ⬜ | |
| Effects | Light, dark and saturated slides readable | ⬜ | |
| Effects | Reduced motion works | ⬜ | |
| Recovery | Bluetooth off/on, range loss, laptop sleep/resume | ⬜ | |
| Recovery | PowerPoint close/reopen; add-in disabled/enabled | ⬜ | |
| Recovery | No stale command replay after reconnect | ⬜ | |
| Soak | 60-minute presentation: no memory growth or leaks (AT-07) | ⬜ | |
| Sharing | Display-level share shows overlay; window-only share documented | ⬜ | |
| Privacy | Diagnostics contain no slide content or secrets | ⬜ | |

## 3. Supported configuration summary

Fill from the sections above. Publish this table in the README when a release is prepared.

| Windows | PowerPoint | Android phone (model / version) | Screen-off volume | Status |
|---|---|---|---|---|
| — | — | — | — | No configuration validated yet |

## 4. Phase 3 — PowerPoint Command Verification Procedure (P3-05)

### Test 3.1: Repeated Slide Commands (AT-01)
1. Open a test deck with ≥ 20 slides in PowerPoint and start Slide Show (F5).
2. Connect PocketPresenter via Bluetooth and verify ribbon indicates `Phone: Presenting Active`.
3. Send 50 rapid `slide.next` commands from the controller.
4. Verify slide count advances strictly 1-by-1 without skipping or lost commands.
5. Send 50 rapid `slide.previous` commands.
6. Verify slide count decrements accurately back to Slide 1.

### Test 3.2: First / Last Slide Boundary Handling
1. Navigate to Slide 1 and send 5 consecutive `slide.previous` commands.
2. Verify slide stays on Slide 1; no black screen, no loop, no exception thrown.
3. Navigate to final slide (e.g., Slide 20) and send 5 consecutive `slide.next` commands.
4. Verify slide stays on final slide without exiting slide show into editing mode.

### Test 3.3: Focus Independence (No Keystroke Bleed)
1. Launch Notepad and place it in the foreground with keyboard focus while PowerPoint Slide Show runs in fullscreen/background.
2. Send `slide.next` from the phone.
3. Verify the slide show advances.
4. Verify NO characters, spaces, or PageDown keystrokes are typed into Notepad.
5. Exit Slide Show mode. Send `slide.next` while Notepad has focus.
6. Verify `NO_ACTIVE_SLIDESHOW` error envelope is returned; Notepad remains completely untouched.

### Test 3.4: Clean Add-in Unload & Event Cleanup (P3-06)
1. With slide show running, close PowerPoint or disable add-in via COM Add-ins dialog.
2. Confirm add-in unloads without crash dialog or COM orphan process hanging in Task Manager.
3. Confirm all event delegates are cleanly detached and transport closes gracefully.

