# PocketPresenter — Decision Log

Lightweight architecture decision records (ADRs). A decision listed here as **Accepted** must not be reopened without new evidence (a failed Gate 0 proof, a measured result, or a changed product requirement).

## How to change a decision

1. Open an issue titled `ADR: <short title>` with the evidence.
2. Add a new entry below with status **Proposed**, referencing the entry it would supersede.
3. After review, mark the new entry **Accepted** and the old one **Superseded by ADR-XXXX**. Update [architecture.md](architecture.md) and its revision history in the same pull request.

## Index

| ID | Title | Status |
|---|---|---|
| ADR-0001 | PowerPoint-first, in-process VSTO add-in | Accepted |
| ADR-0002 | No separate Windows executable | Accepted |
| ADR-0003 | Bluetooth Classic RFCOMM transport | Accepted |
| ADR-0004 | Fixed volume mapping (Up = next, Down = previous) | Accepted |
| ADR-0005 | Native cursor with a local 2.5D effect | Accepted |
| ADR-0006 | Three subtle effect styles | Accepted |
| ADR-0007 | Validate first (Gate 0) | Accepted |
| ADR-0008 | Newline-delimited JSON protocol | Accepted |
| ADR-0009 | First-connection confirmation code | Proposed |
| ADR-0010 | Session ID (`sid`) on every post-handshake message | Accepted |
| ADR-0011 | `off` is a first-class effect value | Accepted |
| ADR-0012 | PRD owns requirement IDs; architecture uses `AC-*` | Accepted |

---

## ADR-0001 — PowerPoint-first, in-process VSTO add-in

- **Status:** Accepted
- **Context:** Presenters already have `.pptx` files and use desktop PowerPoint. Re-implementing a renderer would be costly and incompatible with transitions, media and fonts.
- **Decision:** Keep PowerPoint as the slide engine. Integrate through a VSTO/COM add-in using the PowerPoint object model.
- **Consequences:** Windows desktop PowerPoint only for V1; .NET Framework 4.8; COM threading discipline required; no web/macOS support.

## ADR-0002 — No separate Windows executable

- **Status:** Accepted
- **Context:** The product promise is that the user does not open or manage a second app.
- **Decision:** The add-in owns Bluetooth, input and the overlay window inside the PowerPoint process.
- **Consequences:** If the in-process Bluetooth transport fails, fix it (for example with native Winsock RFCOMM); do not add a helper EXE as a workaround.

## ADR-0003 — Bluetooth Classic RFCOMM transport

- **Status:** Accepted
- **Context:** Need low-latency, continuous, ordered, offline communication with no venue-network dependency.
- **Decision:** Use Bluetooth Classic RFCOMM with a project-specific 128-bit service UUID and OS-level pairing.
- **Consequences:** Phone and laptop must support Bluetooth Classic; BLE GATT and Wi-Fi are not used for the primary stream.

## ADR-0004 — Fixed volume mapping

- **Status:** Accepted
- **Decision:** Volume Up = next slide, Volume Down = previous slide. Not user-configurable in V1.
- **Consequences:** Predictable muscle memory; screen-off delivery is validated per device in Gate 0.3 and documented, never promised universally.

## ADR-0005 — Native cursor with a local 2.5D effect

- **Status:** Accepted
- **Decision:** The real Windows cursor stays visible and authoritative. The effect is a click-through overlay rendered locally; it never moves or delays the cursor.
- **Consequences:** Effects are visual-only and may be absent in application-window-only screen sharing (documented limitation).

## ADR-0006 — Three subtle effect styles

- **Status:** Accepted
- **Decision:** Ship Honeycomb Pulse, Diamond Facets and Geometric Ripple, restrained and idle when still.
- **Consequences:** No 3D engine or large rendering dependency; GDI+ bitmap with `UpdateLayeredWindow` first, revisit only if profiling demands it.

## ADR-0007 — Validate first (Gate 0)

- **Status:** Accepted
- **Decision:** Build only the three proof-of-concepts first, record results in `docs/feasibility-report.md`, and gate all later work on them.
- **Consequences:** Early, possibly uncomfortable evidence; avoids building features on unproven platform assumptions.

## ADR-0008 — Newline-delimited JSON protocol

- **Status:** Accepted
- **Context:** Low message volume, need for easy debugging and strict validation.
- **Decision:** UTF-8 JSON, one object per line, versioned envelope, allow-listed types only (see [protocol.md](protocol.md)).
- **Consequences:** Slightly larger frames than binary; binary encoding can be revisited if Gate 0 latency measurements require it.

## ADR-0009 — First-connection confirmation code

- **Status:** Proposed (evaluate in Wave 2)
- **Context:** OS pairing authenticates the device but does not confirm user intent to control this specific PowerPoint session.
- **Proposal:** Show the phone name in a PowerPoint prompt on first connect; optionally display a short numeric code on both devices.
- **Open:** UX cost versus benefit; decide with evidence from real usage.

## ADR-0010 — Session ID (`sid`) on every post-handshake message

- **Status:** Accepted (pre-implementation, protocol version 1)
- **Context:** The data model, plan and rules require rejecting stale-session messages, but the original envelope had no session field.
- **Decision:** The phone issues a UUID `sessionId` in `hello.ack`; all later messages carry it as `sid`; mismatches are rejected with `BAD_SESSION`.
- **Consequences:** Late frames from an earlier connection can never execute; fixtures V08 covers it. Costs about 45 bytes per frame.

## ADR-0011 — `off` is a first-class effect value

- **Status:** Accepted
- **Context:** The PRD, app flow and UI brief offer an Off option; the original protocol enum listed only three effects.
- **Decision:** `effect` ∈ {`honeycomb`, `diamond`, `ripple`, `off`} in the protocol, preferences and state updates. `off` draws nothing and leaves the native cursor unchanged.
- **Consequences:** One code path for "no effect"; no separate toggle that can desynchronize.

## ADR-0012 — PRD owns requirement IDs; architecture uses `AC-*`

- **Status:** Accepted
- **Context:** The architecture and PRD both defined `FR-*`/`NFR-*` identifiers with different meanings.
- **Decision:** The PRD owns `FR-*`, `NFR-*` and `AT-*`. The architecture lists `AC-*` constraints that trace to them.
- **Consequences:** One meaning per ID across all documents; traceability lives in [traceability.md](traceability.md).
