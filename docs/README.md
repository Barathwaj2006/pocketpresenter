# PocketPresenter documentation pack

PocketPresenter is an Android controller for existing Windows desktop Microsoft PowerPoint presentations. It uses Bluetooth Classic RFCOMM and a PowerPoint VSTO/COM add-in. The add-in creates an internal overlay window for three subtle cursor effects; there is no separate Windows companion application, Wi-Fi requirement or backend in V1.

## Read these documents in order

| # | Document | What it answers | Owner of |
|---|---|---|---|
| 1 | [prd.md](prd.md) | Why and what: purpose, MVP, non-goals, acceptance measures | Requirement IDs (`FR-*`, `NFR-*`, `AT-*`) |
| 2 | [architecture.md](architecture.md) | How the system is shaped; key technical decisions; threat model | Boundaries, states, gates |
| 3 | [trd.md](trd.md) | With what: stack, module boundaries, interfaces, platform requirements | Technology choices |
| 4 | [protocol.md](protocol.md) | The exact phone ⇄ add-in wire contract | Message formats, limits, error codes |
| 5 | [app_flow.md](app_flow.md) | Phone screens, states and end-to-end journeys | State-to-controls table, error catalog |
| 6 | [ui_ux_brief.md](ui_ux_brief.md) | Visual language and interaction behavior | Tokens, effect look and feel |
| 7 | [backend_schema.md](backend_schema.md) | Records that **no backend/database** is required; local preferences and runtime state | Local data model |
| 8 | [implementation_plan.md](implementation_plan.md) | Phased plan with mandatory feasibility gates | Order of work, exit criteria |
| 9 | [rules.md](rules.md) | Guardrails for AI-assisted (and human) implementation | Working rules |
| — | [decisions.md](decisions.md) | Why key choices were made; how to change them | ADRs |
| — | [feasibility-report.md](feasibility-report.md) | Gate 0 evidence log (fill during PoC testing) | Evidence |
| — | [traceability.md](traceability.md) | Requirement → design → plan → test mapping | Coverage |
| — | [../.gitignore](../.gitignore) | Excludes secrets and generated files | — |

Hardware and device results live in [../tests/manual-test-matrix.md](../tests/manual-test-matrix.md).

## Non-negotiable product constraints

- Existing desktop PowerPoint remains the presentation engine.
- Do not create a separate Windows companion executable for V1.
- Bluetooth Classic RFCOMM is the primary phone-to-add-in connection.
- Volume Up means next slide; Volume Down means previous slide.
- The three effects are Honeycomb Pulse, Diamond Facets and Geometric Ripple (plus an **Off** option).
- The cursor position is controlled by touch input; the animation is visual-only.
- Validate the RFCOMM integration inside the real VSTO host, transparent overlay behavior and screen-off volume handling before implementing the full product.
- No database, web backend, cloud service or Wi-Fi dependency is required.

## Canonical names (use these everywhere)

| Concept | Canonical value(s) | Defined in |
|---|---|---|
| Internal session states | `Disconnected`, `PhoneReady`, `Connecting`, `ConnectedIdle`, `PresentingArmed`, `Disconnecting` | [architecture.md §12](architecture.md#12-state-model) |
| Wire link values | `disconnected`, `connected_idle`, `presenting_armed` | [protocol.md §4](protocol.md#4-message-types) |
| UI labels for states | Not connected · Waiting for PowerPoint · Connecting… · Connected · Presenting · Reconnecting… · Error | [app_flow.md §3](app_flow.md#3-state-driven-controls) |
| Effect ids | `honeycomb`, `diamond`, `ripple`, `off` | [protocol.md §4](protocol.md#4-message-types) |
| Draw messages | `draw.start`, `draw.point`, `draw.end` | [protocol.md §4](protocol.md#4-message-types) |
| Volume mapping | Up → `slide.next`, Down → `slide.previous` | [architecture.md §8](architecture.md#8-android-volume-button-handling) |
| Soak test | 60 minutes | [prd.md AT-07](prd.md#5-goals-and-success-measures) |
| Gate 0 evidence | `docs/feasibility-report.md` (gate) and `tests/manual-test-matrix.md` (per-device) | [implementation_plan.md](implementation_plan.md) |

## Document precedence

PRD → architecture → protocol → TRD → backend schema → app flow → UI/UX brief → implementation plan → rules. If two documents disagree, stop and report the conflict instead of choosing silently ([rules.md §1](rules.md#1-source-of-truth-and-scope-discipline)).

## Using this pack with AI coding tools

Point the tool at [../AGENTS.md](../AGENTS.md), which links the rules, or paste the preamble from [rules.md §13](rules.md#13-session-preamble-for-ai-agents). Give it one plan task ID at a time (for example `P2-02`) and require the completion report format.

## Implementation status

These documents define the proposed design; they do not indicate that software has already been implemented or that feasibility tests have passed. Track actual test evidence and supported device configurations during implementation.

## Pack maintenance

- Change a contract in one place, then update everything that cites it (use the [traceability matrix](traceability.md)).
- Add a dated line to the relevant document's revision history and to [../CHANGELOG.md](../CHANGELOG.md).
- Keep links relative so the pack works offline and on any Git host.
