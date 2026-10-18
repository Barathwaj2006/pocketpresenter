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

## 5. Phase 4 — Pointer Control, Gestures & Latency Verification (P4-07, AT-02, AT-04)

### Test 4.1: Pointer Coordinate Mapping and Sensitivity
1. Move finger across trackpad surface at varied speeds.
2. Verify smooth proportional cursor translation across single and multiple monitor setups.
3. Test edge clamping: push finger continuously past right/bottom edges.
4. Verify cursor stops at virtual desktop bounds (`Right - 1`, `Bottom - 1`) and does not disappear off-screen.

### Test 4.2: Gesture State Machine Verification
1. **Single Tap:** Tap lightly once (< 250ms); verify Windows executes left click at cursor position.
2. **Two-Finger Tap:** Tap two fingers simultaneously; verify Windows executes right click (context menu opens).
3. **Two-Finger Scroll:** Drag two fingers vertically; verify document/slide scrolls. Invert `reverseScroll` setting and confirm scroll direction reverses naturally.
4. **Long-Press Drag:** Hold single finger stationary for > 400ms then move across surface.
   - Verify Windows executes left mouse button down followed by mouse drag.
   - Lift finger: verify left mouse button is released immediately.

### Test 4.3: Injected Disconnect Mid-Drag Safety (AT-04)
1. Initiate a long-press left-click drag on Windows desktop selecting multiple icons.
2. While moving with the mouse button held, disconnect Bluetooth (turn off phone Bluetooth or disconnect cable).
3. Verify within ≤ 1,000 ms that PocketPresenter triggers `ReleaseAll()`.
4. Verify the mouse button is NOT left stuck down; normal physical mouse clicking functions immediately.

### Test 4.4: ADR-0013 Held-Button Watchdog
1. Emulate or trigger a held left button without sending updates.
2. Wait 3,000 ms of radio silence.
3. Verify add-in watchdog triggers automatic `ReleaseAll()`, releasing the held button.

### Test 4.5: Touch-to-Cursor Latency Measurement Method (TRD §8)
- **Method:**
  1. Phone records a monotonic timestamp $T_{phone}$ upon dispatching `pointer.delta`.
  2. Add-in records local timestamp $T_{win}$ upon executing `SetCursorPos`.
  3. Estimate one-way clock difference using periodic `session.ping` / `session.pong` round-trip time: $\Delta = T_{win\_pong} - T_{phone\_ping} - \frac{RTT}{2}$.
  4. One-way latency sample: $L_i = T_{win} - T_{phone} - \Delta$.
  5. Collect 500 samples across steady movement.
- **Criteria:**
  - Median latency target: $< 50\text{ ms}$.
  - p95 latency target: $< 100\text{ ms}$.

## 6. Phase 5 — Overlay Lifecycle & Baseline Cursor Verification (P5-06, AT-04, AT-05)

### Test 5.1: Click-Through & Non-Activation (AT-04)
1. In active Slide Show, position the baseline cursor ring over an interactive hyperlink or slide navigation button.
2. Click the touchpad (single-finger tap or physical mouse click).
3. Verify the underlying link/button triggers immediately; the overlay window does NOT steal focus, intercept the click, or flash a title bar.

### Test 5.2: Display Configurations (Laptop, Projector, Presenter View)
1. **Laptop-only:** Verify overlay ring aligns with native Windows cursor across full screen.
2. **Extended / Projector Display:** Drag slide show to secondary display; verify overlay renders on the target presentation screen.
3. **Presenter View:** Verify overlay displays on the public audience slide show, NOT on the private speaker notes window.

### Test 5.3: Display Scaling & Transitions (DPI 100%, 150%, 200%)
1. Change Windows display scaling to 150% and 200%.
2. Verify overlay dot/ring tracks cursor position without offset or visual clipping.
3. Trigger slide transitions (e.g. Fade, Push) and embedded video playback; verify overlay does not flicker or break video acceleration.

### Test 5.4: Automatic Teardown & Leak Prevention (AT-05)
1. End slide show (Esc) while phone remains connected.
2. Verify overlay window disappears immediately; no residual ring or ghost artifact remains on desktop.
3. Re-enter slide show (F5): verify overlay reappears automatically.
4. With slide show running, disconnect Bluetooth or close PowerPoint; verify overlay is destroyed cleanly with zero orphan processes.



