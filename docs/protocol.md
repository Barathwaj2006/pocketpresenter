# PocketPresenter — Wire Protocol

| | |
|---|---|
| **Protocol version** | 1 |
| **Document version** | 1.1 |
| **Transport** | Bluetooth Classic RFCOMM (ordered byte stream) |
| **Encoding** | UTF-8 JSON, one object per line (`\n` terminated) |
| **Parent doc** | [architecture.md](architecture.md) §7 |
| **Machine-readable test vectors** | [../protocol/fixtures/vectors.json](../protocol/fixtures/vectors.json) |

This document is the contract between the Android app and the PowerPoint add-in. Both sides must implement it identically. If this file and the code disagree, the disagreement is a bug: fix one and update the other in the same pull request. Constants (limits, enums) must live in **one module per language** and be cross-checked against the shared fixtures ([rules.md §3](rules.md#3-code-quality-and-module-boundaries)).

## 1. Framing

- Each message is a single JSON object followed by `\n`.
- A Bluetooth read is **not** a message boundary. Receivers must buffer bytes, split on `\n`, and handle partial and multiple messages per read.
- Maximum frame size: **4096 bytes** including the newline. Larger frames are rejected and the connection is closed.
- Messages are never pretty-printed; no embedded raw newlines (JSON escapes only).

## 2. Envelope

```json
{ "v": 1, "sid": "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10", "seq": 42, "type": "slide.next", "payload": {} }
```

| Field | Type | Rules |
|---|---|---|
| `v` | integer | Protocol version. Must equal a version both sides negotiated. |
| `sid` | string (UUID) | Session ID chosen by the phone in `hello.ack`. **Required on every message after the handshake**, in both directions; **absent** on `hello` and `hello.ack` (the ack carries `payload.sessionId` instead). A mismatching `sid` is rejected (`BAD_SESSION`) so late frames from an earlier session can never execute. |
| `seq` | integer ≥ 0 | Per-direction, per-session counter. Starts at 0 after the handshake and increments by 1 per message sent. |
| `type` | string | One of the types in §4. Unknown types are rejected. |
| `payload` | object | Type-specific. Unknown fields are ignored; missing required fields are rejected. |

*Change in 1.1:* `sid` was added so the session-validation requirements in [backend_schema.md](backend_schema.md) and [rules.md §7](rules.md#7-protocol-and-security-rules) are enforceable on the wire. This is the pre-implementation revision of protocol version 1, so no version bump is needed.

## 3. Session handshake

1. The connecting side (add-in) sends `hello` (no `sid`).
2. The listening side (phone) generates a fresh `sessionId` and replies `hello.ack` (no `sid`, `payload.sessionId` set).
3. From then on, every message in both directions carries `sid`.
4. Until a valid `hello`/`hello.ack` exchange completes, **all other types are rejected** (`NOT_READY`).
5. On any reconnect, a new handshake is required, a new `sessionId` is issued and `seq` restarts at 0. No commands are replayed.

```json
{ "v": 1, "seq": 0, "type": "hello",
  "payload": { "client": "addin", "appVersion": "0.1.0", "supports": [1], "capabilities": ["pointer", "slides", "effects"] } }

{ "v": 1, "seq": 0, "type": "hello.ack",
  "payload": { "accepted": true, "version": 1, "sessionId": "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10",
               "capabilities": ["pointer", "slides", "effects", "draw"] } }
```

If versions are incompatible the phone replies `hello.ack` with `"accepted": false` and a `reason`, then closes.

## 4. Message types

Direction: **P→W** phone to Windows add-in, **W→P** add-in to phone.

| Type | Dir | Payload | Delivery |
|---|---|---|---|
| `hello` | W→P | see §3 | Required first |
| `hello.ack` | P→W | see §3 | Required reply |
| `session.ping` | both | `{ "t": <ms> }` | Periodic |
| `session.pong` | both | `{ "t": <ms echoed> }` | Reply to ping |
| `state.request` | P→W | `{}` | On demand |
| `state.update` | W→P | `{ "link": "...", "show": "...", "effect": "...", "slide": n, "slideCount": n }` | Latest wins |
| `pointer.delta` | P→W | `{ "dx": int, "dy": int }` | Ordered; sums preserved |
| `pointer.button` | P→W | `{ "button": "left"\|"right", "action": "down"\|"up" }` | Ordered |
| `pointer.scroll` | P→W | `{ "dy": int }` | Ordered; may aggregate |
| `slide.next` | P→W | `{}` | Ordered, de-duplicated by `seq` |
| `slide.previous` | P→W | `{}` | Ordered, de-duplicated by `seq` |
| `effect.set` | P→W | `{ "effect": "honeycomb"\|"diamond"\|"ripple"\|"off", "intensity": 0.0-1.0, "scale": 0.5-2.0, "reducedMotion": bool }` | Latest wins |
| `draw.start` | P→W | `{ "x": 0.0-1.0, "y": 0.0-1.0, "color": "#RRGGBB", "width": 1-12 }` | Ordered |
| `draw.point` | P→W | `{ "points": [[x, y], ...] }` (max 32 per message) | Ordered, bounded |
| `draw.end` | P→W | `{}` | Ordered |
| `command.ack` | W→P | `{ "ref": <seq>, "ok": true }` | Reply to discrete commands |
| `command.error` | W→P | `{ "ref": <seq>, "code": "...", "message": "..." }` | Reply on failure |

`effect` value `off` means "standard cursor with no custom effect" (see [app_flow.md Screen G](app_flow.md#screen-g--effect-selector)). It changes visual state only.

`state.update` enum values:

- `link`: `disconnected` · `connected_idle` · `presenting_armed` (wire names for the internal states in [architecture.md §12](architecture.md#12-state-model))
- `show`: `none` · `running` · `suspended`
- `effect`: `honeycomb` · `diamond` · `ripple` · `off`

### Examples

```json
{ "v": 1, "sid": "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10", "seq": 118, "type": "pointer.delta", "payload": { "dx": 7, "dy": -3 } }
{ "v": 1, "sid": "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10", "seq": 119, "type": "pointer.button", "payload": { "button": "left", "action": "down" } }
{ "v": 1, "sid": "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10", "seq": 120, "type": "pointer.button", "payload": { "button": "left", "action": "up" } }
{ "v": 1, "sid": "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10", "seq": 121, "type": "slide.next", "payload": {} }
{ "v": 1, "sid": "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10", "seq": 17,  "type": "command.ack", "payload": { "ref": 121, "ok": true } }
```

## 5. Limits and validation

Receivers must reject (with `command.error` where a reply is possible, otherwise by closing) any message that violates these limits.

| Field | Limit |
|---|---|
| Frame size | ≤ 4096 bytes |
| `pointer.delta` `dx`, `dy` | −2000 … 2000 per message |
| `pointer.scroll` `dy` | −1000 … 1000 per message |
| `draw.point` points | ≤ 32 per message, each coordinate 0.0 … 1.0 |
| `effect.set` `intensity` | 0.0 … 1.0 |
| `effect.set` `scale` | 0.5 … 2.0 |
| Pointer message rate | ≤ 200 messages/s sustained (sender should coalesce) |
| Unacknowledged discrete commands | ≤ 8 in flight |
| Ping interval | 5 s; link considered dead after 15 s without any inbound traffic |

*All numeric limits above are starting values to be tuned from Gate 0 measurements; changing one requires updating this table, both implementations and the fixtures together.*

Further rules:

- Unknown `type`, wrong JSON types, out-of-range numbers, invalid enum strings and malformed JSON are all rejected.
- **No generic execution surface.** There is no message type that carries a command line, script, file path, URL or arbitrary key sequence, and none may be added without an architecture decision.
- Slide commands are idempotent per `seq`: a duplicate `seq` for `slide.next` / `slide.previous` is acknowledged but not re-executed.
- High-frequency pointer data is not individually acknowledged.
- Late-bound state is never trusted: the add-in re-checks that a slide show is active when it *executes* a command, not only when it received it.

## 6. Error codes

| Code | Meaning |
|---|---|
| `BAD_FRAME` | Malformed JSON or oversize frame |
| `BAD_VERSION` | Unsupported protocol version |
| `BAD_TYPE` | Unknown message type |
| `BAD_FIELD` | Missing, mistyped or out-of-range field |
| `BAD_SESSION` | `sid` missing or not equal to the current session |
| `NOT_READY` | Command before a valid handshake |
| `NO_ACTIVE_SLIDESHOW` | Slide or draw command with no running show |
| `NOT_ARMED` | Control disabled (not in `presenting_armed`) |
| `RATE_LIMITED` | Sender exceeded the rate limit |
| `DUPLICATE` | Duplicate `seq` already executed (informational) |
| `INTERNAL` | Unexpected failure; details only in local diagnostics |

User-facing wording for these codes is in [app_flow.md §4](app_flow.md#4-error-and-recovery-catalog).

## 7. Disconnect behaviour

On link loss, on `session` timeout, or on explicit disconnect, **both** sides must:

1. Stop sending and processing commands.
2. Release any held mouse button (add-in) and clear touch/drag state (phone).
3. Discard all queued, unsent and unacknowledged messages.
4. Reset `seq` state and forget the `sid`; a new handshake is required.

### Proposed addition: held-button watchdog (open, see architecture Q7)

A dropped link is normally detected from the socket error immediately, but a silent radio stall could keep a button down until the 15 s ping timeout. Proposal to evaluate in Wave 2: while any button is held, the phone sends `session.ping` every 1 s and the add-in releases all buttons after 3 s without inbound traffic. Not part of protocol version 1 until accepted in [decisions.md](decisions.md).

## 8. Compatibility and versioning

- `v` is an integer, incremented only for incompatible changes after the first implementation ships.
- Adding an optional payload field or a new message type that older peers can safely ignore does not require a version bump, but capability negotiation (§3) must gate its use.
- The add-in lists the versions it supports in `hello.supports`; the phone selects one.

## 9. Test vectors

Implementations must pass these in their automated tests. The same cases live in [../protocol/fixtures/vectors.json](../protocol/fixtures/vectors.json) so Kotlin and C# test suites consume identical data.

| ID | Input | Expected |
|---|---|---|
| V01 | Valid `slide.next` before handshake | `NOT_READY` |
| V02 | Two complete frames in one read | Both processed in order |
| V03 | One frame split across three reads | Processed once, after the newline |
| V04 | Unknown type `run.shell` | `BAD_TYPE` |
| V05 | `pointer.delta` with `dx: 99999` | `BAD_FIELD` |
| V06 | `slide.next` repeated with the same `seq` | Executed once; second gets `DUPLICATE` |
| V07 | Frame of 5000 bytes | `BAD_FRAME`, connection closed |
| V08 | `slide.next` with a `sid` from a previous session | `BAD_SESSION`; nothing executed |
| V09 | `effect.set` with `effect: "off"` | Accepted; effect becomes `off` |
| V10 | `effect.set` with `effect: "sparkle"` | `BAD_FIELD` |
