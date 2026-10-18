# PocketPresenter — Traceability Matrix

Maps each product requirement to the design that realises it, the plan tasks that build it and the evidence that proves it. A requirement with an empty **Evidence** cell is **not verified**. Update this file whenever a requirement, design section or plan task changes.

Legend: ⏳ not started · 🔄 in progress · ✅ verified with evidence

## Functional requirements

| Requirement | Design | Plan tasks | Acceptance / evidence | Status |
|---|---|---|---|---|
| FR-01 Pair and connect | [arch §6](architecture.md#6-bluetooth-connection-and-pairing), [protocol §3](protocol.md#3-session-handshake) | G0-04, P2-01…P2-05, P3-01 | Gate 0.1; handshake vectors | ⏳ |
| FR-02 Slide navigation | [arch §8–§9](architecture.md#8-android-volume-button-handling) | G0-08, G0-09, P3-02…P3-05, P7-07 | AT-01, AT-06; Gate 0.3 | ⏳ |
| FR-03 Pointer and gestures | [arch §10](architecture.md#10-cursor-touchpad-and-draw-mode) | P4-01…P4-07, P7-03 | AT-02, AT-04 | ⏳ |
| FR-04 Effects (incl. Off) | [arch §11](architecture.md#11-cursor-effect-renderer), [ui §6](ui_ux_brief.md#6-the-three-windows-cursor-effects) | P6-01…P6-08, P7-04 | AT-03; vectors V09, V10 | ⏳ |
| FR-05 Overlay | [arch §9, §11](architecture.md#9-powerpoint-lifecycle-and-slide-commands) | G0-05…G0-07, P5-01…P5-06 | Gate 0.2; AT-04, AT-05 | ⏳ |
| FR-06 Draw Mode (P1) | [arch §10](architecture.md#10-cursor-touchpad-and-draw-mode) | P8-01…P8-06 | Phase 8 exit | ⏳ |
| FR-07 Failure reporting | [app_flow §3–§4](app_flow.md#3-state-driven-controls), [protocol §6](protocol.md#6-error-codes) | P7-05, P7-09 | Error-catalog walkthrough | ⏳ |
| FR-08 Pocket Mode | [app_flow Screen H](app_flow.md#screen-h--pocket-mode), [arch §8](architecture.md#8-android-volume-button-handling) | G0-09, P7-07, P7-08 | AT-06 | ⏳ |
| FR-09 PowerPoint ribbon | [app_flow ribbon](app_flow.md#powerpoint-ribbon--status-ui) | G0-03, P3-01 | Manual check of every ribbon state | ⏳ |

## Non-functional requirements

| Requirement | Design | Plan tasks | Acceptance / evidence | Status |
|---|---|---|---|---|
| NFR-01 Responsiveness | [arch §13](architecture.md#13-threading-model), [trd §8](trd.md#8-latency-budget) | P4-01, P4-07 | AT-02 | ⏳ |
| NFR-02 Reliability (UI thread) | [arch §13](architecture.md#13-threading-model) | P3-03, P9-05 | AT-07 | ⏳ |
| NFR-03 Security | [arch §14](architecture.md#14-security-and-threat-model), [protocol §5](protocol.md#5-limits-and-validation) | P2-03, P2-06 | Vectors V01, V04, V05, V08 | ⏳ |
| NFR-04 Privacy | [backend §6](backend_schema.md#6-local-diagnostics-model) | P1-08, P9-06 | Log inspection | ⏳ |
| NFR-05 Accessibility | [ui §9](ui_ux_brief.md#9-accessibility-and-usability) | P7-01…P7-04 | Design acceptance checks | ⏳ |
| NFR-06 Compatibility | [arch §18](architecture.md#18-supported-configurations) | P9-08 | [Test matrix](../tests/manual-test-matrix.md) | ⏳ |
| NFR-07 Maintainability | [trd §3](trd.md#3-module-boundaries) | P1-07 | Review against dependency rules | ⏳ |

## Acceptance targets

| Target | Primary plan task | Where recorded |
|---|---|---|
| AT-01 100 consecutive commands | P3-05, P9-01 | Test matrix |
| AT-02 Latency median < 50 ms / p95 < 100 ms | P4-07 | Feasibility report, test matrix |
| AT-03 Effect switching safe | P6-08 | Test matrix |
| AT-04 Disconnect cleanup ≤ 1 s | P2-07, P4-04, P5-05 | Test matrix |
| AT-05 Display scenarios | P5-06, P9-01 | Test matrix |
| AT-06 Screen-off per device | G0-09, P9-04 | Test matrix |
| AT-07 60-minute soak | P9-05 | Test matrix |
