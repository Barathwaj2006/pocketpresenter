# PocketPresenter — Android app

Kotlin + Jetpack Compose controller. **Not implemented yet.** Start with the Gate 0 work below.

## Planned module layout

See [architecture.md §5](../docs/architecture.md#5-component-boundaries).

```text
app/
  ui/ input/ presentation/ bluetooth/ session/ settings/
```

## Gate 0 tasks for this folder

1. **Proof 0.1:** RFCOMM listener with the project service UUID; accept one bonded connection; implement `hello`/`hello.ack`; send `pointer.delta` bursts. Codec must follow [protocol.md](../docs/protocol.md) including its test vectors.
2. **Proof 0.3:** user-started foreground service (connected-device type), active `MediaSession` with `VolumeProvider`; map Up → `slide.next`, Down → `slide.previous`; test screen on, locked and screen off.

Record all results in [feasibility-report.md](../docs/feasibility-report.md).

## Rules

- Request only the Bluetooth permissions the target Android version needs.
- Visible foreground notification with a Stop action whenever a session is active.
- Clear touch, drag and pressed-button state on disconnect or pause.
- Never queue or replay stale commands.
