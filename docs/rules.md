# PocketPresenter — AI Coding Rules

| | |
|---|---|
| **Version** | 1.1 |
| **Purpose** | Keep automated implementation scoped, testable, secure and consistent with the approved architecture. |
| **Read first** | [architecture.md](architecture.md), [prd.md](prd.md), [trd.md](trd.md), [implementation_plan.md](implementation_plan.md) |
| **Applies to** | Any AI coding agent or assistant working in this repository (also a useful human checklist). Entry point: [../AGENTS.md](../AGENTS.md). |

## Table of contents

1. [Source-of-truth and scope discipline](#1-source-of-truth-and-scope-discipline)
2. [Feasibility-first engineering](#2-feasibility-first-engineering)
3. [Code quality and module boundaries](#3-code-quality-and-module-boundaries)
4. [UI and interaction rules](#4-ui-and-interaction-rules)
5. [PowerPoint and Windows safety](#5-powerpoint-and-windows-safety)
6. [Android lifecycle and permissions](#6-android-lifecycle-and-permissions)
7. [Protocol and security rules](#7-protocol-and-security-rules)
8. [Testing rules](#8-testing-rules)
9. [Dependencies and build hygiene](#9-dependencies-and-build-hygiene)
10. [Git and delivery rules](#10-git-and-delivery-rules)
11. [Required completion report](#11-required-completion-report)
12. [Stop-and-ask triggers](#12-stop-and-ask-triggers)
13. [Session preamble for AI agents](#13-session-preamble-for-ai-agents)
14. [Revision history](#14-revision-history)

## 1. Source-of-truth and scope discipline

1. [prd.md](prd.md) defines product scope and non-goals.
2. [architecture.md](architecture.md) defines the system boundaries and key decisions.
3. [trd.md](trd.md) defines technologies, interfaces and platform constraints.
4. [implementation_plan.md](implementation_plan.md) defines the order of work and exit criteria.
5. If documents conflict, stop and describe the conflict; do not silently invent a third architecture.
6. Do not add a feature, dependency, service, database, cloud component, UI screen or architectural layer unless it serves an approved requirement.
7. Do not replace Microsoft PowerPoint, create a custom PPTX rendering engine, or add a separate Windows companion executable for V1.
8. Bluetooth Classic RFCOMM is the primary communication method. Do not switch to Wi-Fi, BLE, HID, a cloud relay or a helper process as an unreviewed workaround.
9. No fake success states, fake device connections, mock data presented as live state, fabricated performance claims or placeholder buttons that appear functional.
10. The wire format is owned by [protocol.md](protocol.md) and its [shared fixtures](../protocol/fixtures/vectors.json); local data by [backend_schema.md](backend_schema.md); look and feel by [ui_ux_brief.md](ui_ux_brief.md); screens and states by [app_flow.md](app_flow.md).

**Precedence when documents appear to disagree:** PRD → architecture → protocol → TRD → backend schema → app flow → UI/UX brief → implementation plan → these rules. Lower documents never override higher ones; report the discrepancy instead.

## 2. Feasibility-first engineering

1. Complete Gate 0 in [implementation_plan.md](implementation_plan.md) before implementing the full product.
2. A simulated test is not proof that the API works in the real VSTO host or on the target Android phone.
3. Record device model, OS version, PowerPoint version/bitness and exact steps for platform-specific tests ([feasibility-report.md](feasibility-report.md), [manual-test-matrix.md](../tests/manual-test-matrix.md)).
4. When a gate fails, stop dependent work. Diagnose and fix the failure, or propose a scoped change explicitly. Never hide a failure by marking the feature complete.
5. Do not promise screen-off volume handling across all Android devices. Report only configurations that passed the test matrix.

## 3. Code quality and module boundaries

- Keep Bluetooth transport, protocol parsing, PowerPoint automation, pointer input, overlay lifecycle and effect rendering separate ([trd.md §3](trd.md#3-module-boundaries)).
- Keep Android UI composables focused on presentation and user actions; move connection/state logic into testable classes.
- Use interfaces for platform boundaries and effects where there is a real need; do not create abstractions with no consumer.
- Keep functions small and cohesive. As a review guideline, investigate files exceeding roughly 400 lines and functions exceeding roughly 60 lines, but do not split code mechanically if it reduces clarity.
- Avoid duplicated validation and business rules. Keep protocol enums/limits authoritative in one module per language and cross-check them with the shared fixtures.
- Use explicit types and sealed/enumerated states rather than magic strings or booleans that hide lifecycle states.
- No `TODO`, `FIXME`, unimplemented method, or placeholder success result in a feature claimed as complete.
- Do not swallow exceptions. Handle expected failures at the correct boundary, emit a safe diagnostic and update state.

Illustrative contrast:

```text
Avoid:  var connected = true; var presenting = true;     // two booleans hide invalid combinations
Prefer: SessionState = ConnectedIdle | PresentingArmed   // one explicit state, invalid combos unrepresentable
```

## 4. UI and interaction rules

- Default to the restrained graphite/cyan design defined in [ui_ux_brief.md](ui_ux_brief.md).
- Do not add a purple gradient, excessive pill buttons, loud glow, particle explosions or decorative dashboards.
- The cursor follows real finger input; animation never quantizes, delays or modifies cursor movement.
- Effect switching must preserve cursor position and presentation state.
- Provide an Off/reduced-motion path and understandable connection errors ([app_flow.md §4](app_flow.md#4-error-and-recovery-catalog)).
- Never make permission prompts, connection states or button behavior misleading.
- Do not trigger click-and-drag accidentally. Model gesture states explicitly and always reset on cancellation/disconnect.
- Do not rely on `borderSubtle` alone as a control affordance (measured contrast < 3:1; see [ui_ux_brief.md §2](ui_ux_brief.md#2-visual-direction)).

## 5. PowerPoint and Windows safety

- All PowerPoint COM/Interop access occurs on PowerPoint's STA/UI thread.
- Never block the PowerPoint UI thread on Bluetooth reads, waits, sleeps, pairing, network calls or long rendering operations.
- Do not send slide-navigation keystrokes to a random active window. Confirm the target slide-show state first.
- The overlay must be click-through and non-activating, and must never be the input source of truth.
- On disconnect, add-in shutdown, slide-show exit or error: hide/destroy the overlay, release held mouse/key states, cancel tasks and unsubscribe events.
- Do not modify or save the source PPTX to provide cursor effects.
- Do not request administrator elevation as a blanket workaround. Detect and explain privilege mismatches.

## 6. Android lifecycle and permissions

- Request only permissions required for approved Bluetooth/background functionality.
- Explain permissions in context; never use AccessibilityService as an unreviewed shortcut.
- Start foreground services only following a valid user action and with the correct service type/permissions for the target Android version.
- Handle missing permissions, Bluetooth disabled, process recreation, screen-off transitions and connection termination explicitly.
- Release or invalidate pending gesture state when touch is cancelled, connection drops or the app session ends.
- Never claim that force-stopped apps can continue handling buttons.

## 7. Protocol and security rules

- Validate every incoming frame: version, type, size, sequence, **session (`sid`)**, enum and numeric bounds ([protocol.md](protocol.md)).
- Never implement arbitrary shell/PowerShell execution, arbitrary keystroke strings, file transfer or remote code execution.
- Do not trust device names as authentication. Rely on OS pairing and the explicit active session.
- Prevent duplicate slide commands using command IDs/sequence tracking.
- Drop stale pointer samples rather than building an unbounded queue; preserve total movement appropriately.
- Release all held mouse buttons on disconnect/timeout, even if a release packet was lost.
- Never commit credentials, pairing material, signing keys, `.pfx`/private certificates or local settings with secrets.
- Any protocol change updates [protocol.md](protocol.md), both implementations and [vectors.json](../protocol/fixtures/vectors.json) in the same change.

## 8. Testing rules

- Add unit tests for every pure business rule or parser.
- Every new protocol command requires valid, invalid and stale/duplicate test cases.
- Every gesture requires success, cancellation and disconnect-state tests.
- New renderer effects require state/coordinate tests and a real visual smoke test on the target display.
- A UI-only mock is not an integration test.
- Run the relevant build, tests and static checks after each milestone. Report exact pass/fail results; never claim tests ran if they did not.
- Do not remove or weaken tests to get a green build without explaining why and adding an adequate replacement.

## 9. Dependencies and build hygiene

- Prefer platform capabilities over new dependencies.
- Pin dependency/tool versions. Do not upgrade Gradle, Kotlin, Compose, Office interop or NuGet packages opportunistically.
- Keep Android and Windows dependency sets separate.
- Retain required license notices.
- Do not commit generated binaries, build outputs, IDE caches, local SDK paths or user-specific configuration.
- `.gitignore` is mandatory; do not broaden ignore patterns so much that source files or required build wrappers disappear from version control.

## 10. Git and delivery rules

- Work only in the designated repository and approved branch.
- Inspect `git status` before editing or committing.
- Never reset, force-push, delete branches, rewrite history or discard user changes without explicit approval.
- Never commit unrelated files or secrets.
- Use focused commits with descriptive messages ([Conventional Commits](https://www.conventionalcommits.org/)) after a milestone passes.
- If a build/test fails, report the failure and evidence. Do not create a success commit that hides it.
- Update relevant documentation whenever an approved architecture/contract/scope decision changes.

## 11. Required completion report

At the end of every milestone, report:

1. What was implemented.
2. Files changed.
3. Commands/builds/tests actually run and their results.
4. Known failures and limitations.
5. The exact next milestone and any gate it depends on.

Template:

```markdown
## Milestone report — <phase/task IDs>
**Implemented:** …
**Files changed:** …
**Commands run → result:**
- `<command>` → PASS/FAIL (<summary>)
**Known failures / limitations:** …
**Next milestone:** <ID> (depends on: <gate/phase>)
```

Do not claim the project is complete until all P0 acceptance criteria and the final release checks pass on real target hardware.

## 12. Stop-and-ask triggers

Stop and ask for a decision (do not guess) when:

| Trigger | Why |
|---|---|
| Two documents conflict | §1.5 |
| A Gate 0 proof fails or is ambiguous | §2.4 |
| The "easy" fix would add a helper executable, Wi-Fi, BLE, cloud, database or renderer | §1.7–1.8, change-control rule |
| A protocol change is needed | §7 — requires doc + fixtures + both implementations |
| A new permission or foreground-service type seems necessary | §6 |
| A test must be weakened or removed to pass | §8 |
| A destructive git operation seems necessary | §10 |
| Hardware or a real PowerPoint installation is unavailable and a claim depends on it | §2 — report "not verified" instead |

## 13. Session preamble for AI agents

Paste at the start of an AI coding session (or rely on [../AGENTS.md](../AGENTS.md)):

```text
You are working on PocketPresenter. Before changing anything, read docs/rules.md,
docs/architecture.md, docs/prd.md, docs/trd.md and docs/implementation_plan.md.
Work on one plan task ID at a time. Do not add a Windows helper executable, Wi-Fi,
cloud, database or custom PPTX renderer. Do not claim a platform behavior works until
it was tested on the real target. If documents conflict, stop and report. End every
milestone with the completion report from docs/rules.md section 11.
```

## 14. Revision history

| Version | Change |
|---|---|
| 1.0 | Initial rules. |
| 1.1 | Added document precedence order, `sid` validation and fixture-sync rule, contrast rule, stop-and-ask table, completion-report template, agent session preamble, anchors/links. Original rules unchanged. |
