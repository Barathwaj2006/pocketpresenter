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
