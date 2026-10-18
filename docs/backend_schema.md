# PocketPresenter — Backend Schema and Local Data Model

| | |
|---|---|
| **Version** | 1.1 |
| **Status** | **No backend is required for V1.** This document explicitly prevents unnecessary backend development. |
| **Related documents** | [prd.md](prd.md) · [trd.md](trd.md) · [architecture.md](architecture.md) · [protocol.md](protocol.md) |

## Table of contents

1. [Backend decision](#1-backend-decision)
2. [Runtime state (in memory only)](#2-runtime-state-in-memory-only)
3. [Android local preferences](#3-android-local-preferences)
4. [Windows local settings](#4-windows-local-settings)
5. [Bluetooth protocol data is not a database](#5-bluetooth-protocol-data-is-not-a-database)
6. [Local diagnostics model](#6-local-diagnostics-model)
7. [Data lifecycle](#7-data-lifecycle)
8. [Data inventory](#8-data-inventory)
9. [Future backend policy](#9-future-backend-policy)
10. [Revision history](#10-revision-history)

## 1. Backend decision

PocketPresenter V1 is a local, Bluetooth-connected product. It requires **no web backend, database server, cloud authentication, cloud file storage, Supabase, Firebase, PostgreSQL or analytics service**.

The Android phone communicates directly with the PocketPresenter add-in hosted inside desktop PowerPoint over Bluetooth Classic RFCOMM. The add-in executes commands locally through PowerPoint and Windows APIs. The overlay is rendered locally on the same Windows machine.

Adding a backend would increase setup, failure modes, privacy risk and maintenance without helping the MVP.

## 2. Runtime state (in memory only)

### SessionState

| Field | Type | Description |
|---|---|---|
| `sessionId` | Random opaque string/UUID | Chosen by the phone in `hello.ack`; new per connection session; never reused after reconnect. Carried as `sid` in every later message ([protocol.md §2](protocol.md#2-envelope)). |
| `state` | Enum | Canonical internal states: `Disconnected`, `PhoneReady`, `Connecting`, `ConnectedIdle`, `PresentingArmed`, `Disconnecting` ([architecture.md §12](architecture.md#12-state-model)). UI labels such as *Reconnecting* and *Error* are layered presentation states ([app_flow.md §3](app_flow.md#3-state-driven-controls)). |
| `protocolVersion` | Integer | Negotiated protocol version. |
| `peerDeviceId` | OS/device identifier or local alias | Used only to identify the paired peer; avoid collecting unrelated identifiers. |
| `lastHeartbeatAt` | Monotonic timestamp | Detect stale connection. |
| `lastReceivedSequence` | Integer | Duplicate/stale command protection. |
| `activeSlideShow` | Boolean | Whether a valid PowerPoint slide show is active. |
| `targetMonitorBounds` | Local rectangle | Current slide-show monitor bounds for overlay alignment. |
| `effect` | Enum | `honeycomb`, `diamond`, `ripple`, `off`. |
| `inputButtonsDown` | Set of button enums | Tracks held mouse buttons so disconnect can release them. |

Session state must be discarded on process/session shutdown. Do not serialize active input/button-down state.

## 3. Android local preferences

Persist only small, non-sensitive user preferences using Android DataStore.

### `ControllerPreferences`

| Field | Type | Default | Valid range / values | Notes |
|---|---|---|---|---|
| `schemaVersion` | Int | `1` | ≥ 1 | Used for migration. |
| `effectId` | Enum | `honeycomb` | `honeycomb`, `diamond`, `ripple`, `off` | Last selected visual effect. |
| `effectIntensity` | Float | `0.35` | 0.0 – 1.0 | Matches `effect.set` limits. |
| `effectScale` | Float | `1.0` | 0.5 – 2.0 | Matches `effect.set` limits. Optional if scale control does not survive usability testing. |
| `pointerSensitivity` | Float | `1.0` | 0.25 – 4.0 *(proposed; tune in usability tests)* | Calibration setting. |
| `pointerAccelerationEnabled` | Boolean | `false` | — | Default favors predictable motion. |
| `scrollSensitivity` | Float | `1.0` | 0.25 – 4.0 *(proposed)* | Scroll tuning. |
| `hapticsEnabled` | Boolean | `true` | — | Only for discrete events. |
| `reducedMotion` | Boolean | `false` | — | Can inherit system preference. |
| `leftHandedLayout` | Boolean | `false` | — | Layout choice. |
| `lastKnownPcAlias` | String or null | `null` | ≤ 64 chars | Friendly alias only; do not store secrets. |

Persisted values must be versioned/migrated if the schema changes. All numeric values must be validated and clamped **when read**, not only when written. A missing or unreadable store falls back to defaults without crashing.

Example (conceptual serialized view):

```json
{
  "schemaVersion": 1,
  "effectId": "honeycomb",
  "effectIntensity": 0.35,
  "effectScale": 1.0,
  "pointerSensitivity": 1.0,
  "pointerAccelerationEnabled": false,
  "scrollSensitivity": 1.0,
  "hapticsEnabled": true,
  "reducedMotion": false,
  "leftHandedLayout": false,
  "lastKnownPcAlias": null
}
```

## 4. Windows local settings

Use a small per-user settings file under the application's supported per-user configuration location (for example, `%LocalAppData%\PocketPresenter\settings.json`) or the standard .NET settings mechanism.

### `PowerPointAddInSettings`

| Field | Type | Default | Notes |
|---|---|---|---|
| `settingsVersion` | Integer | `1` | Used for migration. |
| `trustedPeerAlias` | String or null | `null` | User-visible selected phone alias; do not rely on alias as authentication. |
| `lastEffectId` | Enum | `honeycomb` | `honeycomb`, `diamond`, `ripple`, `off`. |
| `effectIntensity` | Float 0–1 | `0.35` | Default subtle value. |
| `overlayEnabled` | Boolean | `true` | User preference; overlay still requires active authenticated session and slide show. |
| `diagnosticsEnabled` | Boolean | `false` | Local diagnostics on/off. |
| `lastSelectedMonitorHint` | Optional stable display hint | `null` | Treat as a hint only; resolve actual monitor bounds each session. |

Example JSON Schema (draft 2020-12) for validation on load:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "PowerPointAddInSettings",
  "type": "object",
  "required": ["settingsVersion"],
  "additionalProperties": false,
  "properties": {
    "settingsVersion": { "type": "integer", "minimum": 1 },
    "trustedPeerAlias": { "type": ["string", "null"], "maxLength": 64 },
    "lastEffectId": { "enum": ["honeycomb", "diamond", "ripple", "off"] },
    "effectIntensity": { "type": "number", "minimum": 0, "maximum": 1 },
    "overlayEnabled": { "type": "boolean" },
    "diagnosticsEnabled": { "type": "boolean" },
    "lastSelectedMonitorHint": { "type": ["string", "null"], "maxLength": 128 }
  }
}
```

Rules:
- Invalid, corrupt or unknown-version files are ignored and replaced with defaults; the add-in must still load.
- Never block PowerPoint startup on reading settings; load lazily or off the UI thread.
- Do not store Bluetooth authentication secrets manually if OS pairing/session authentication can be used. If any app-specific secret is eventually necessary, store it through an appropriate OS-protected mechanism rather than in plaintext settings or Git.

## 5. Bluetooth protocol data is not a database

The following are ephemeral messages, not persistent records (full definitions in [protocol.md](protocol.md)):

- `hello` / `hello.ack`
- `session.ping` / `session.pong`
- `pointer.delta`, `pointer.button`, `pointer.scroll`
- `slide.next`, `slide.previous`
- `effect.set`
- `draw.start`, `draw.point`, `draw.end`
- `command.ack`, `command.error`

*Correction in v1.1:* the draw batch message was named `draw.points` in v1.0 of this document; the canonical name is `draw.point` (a message carrying up to 32 points).

The transport decoder must impose frame-size and numeric bounds. Do not retain a history of pointer movements, cursor locations, slides visited or presentation content.

## 6. Local diagnostics model

If diagnostics are enabled, local logs may contain:

- Timestamp.
- Component and event type.
- Connection state transition.
- Protocol version and sequence number.
- Command type (not arbitrary payload contents).
- Duration, error category and reconnect count.
- Monitor dimensions/DPI only when needed to diagnose alignment.

Logs must not contain slide text, screenshots, file paths to the presentation unless essential and redacted, Bluetooth secrets, raw pairing material or personal content. Use bounded log rotation (suggested starting limit: 5 files × 1 MB) and an accessible `Open diagnostics` / `Clear logs` action.

Example allowed entry:

```json
{ "t": "2026-10-18T14:02:11.402Z", "c": "Bluetooth", "e": "state", "from": "ConnectedIdle", "to": "PresentingArmed", "v": 1 }
```

## 7. Data lifecycle

- **Create:** preferences created on first use; session state created when the user starts a connection.
- **Update:** preferences updated after validated user changes; session state updated as messages arrive.
- **Close:** on disconnect, clear session IDs, command queues, pointer/button states and the overlay.
- **Delete:** provide a way to clear saved preferences/peer alias and local diagnostics.
- **Export/cloud sync:** not in V1.
- **Uninstall:** removing the add-in or app should leave no running component; per-user settings may be offered for removal by the installer.

## 8. Data inventory

| Data | Where | Persistent? | Leaves the device? | Sensitive? |
|---|---|---|---|---|
| Effect, sensitivity, haptics, layout | Android DataStore | Yes | No | No |
| Effect, overlay, diagnostics flags | Windows `settings.json` | Yes | No | No |
| Phone/PC friendly alias | Both | Yes (alias only) | Only as the Bluetooth device name the OS already exposes | Low |
| Session ID, sequence numbers | Memory | No | Over RFCOMM only | No |
| Pointer deltas, slide commands | Memory / RFCOMM | No | Over RFCOMM only | Low |
| Held-button state | Memory | No | No | No |
| Slide content / file paths | — | Never stored | Never | — |
| Diagnostics log | Local file (opt-in) | Yes (rotated) | No | Must contain no personal content |
| Bluetooth link keys | OS Bluetooth stack | OS-managed | No | Yes — handled by the OS, never by the app |

## 9. Future backend policy

A backend may be proposed only if a later, approved requirement genuinely needs remote pairing, cross-device settings sync or managed fleet deployment. Such a proposal must include a separate threat model, privacy review, data retention policy, API contract and an explicit revision to the out-of-scope list. Do not add a backend speculatively.

## 10. Revision history

| Version | Change |
|---|---|
| 1.0 | Initial local data model. |
| 1.1 | Aligned state and effect enums with the architecture; added `off` to persisted effect ids; set explicit validated ranges matching protocol limits; added JSON Schema for Windows settings, corrupt-file handling, data inventory, example log entry; corrected `draw.points` to `draw.point`. |
