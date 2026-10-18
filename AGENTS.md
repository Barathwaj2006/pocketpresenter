# Agent instructions — PocketPresenter

You are working in a **validate-first** project. Read these before changing anything:

1. [docs/rules.md](docs/rules.md) — the working rules (mandatory)
2. [docs/prd.md](docs/prd.md) → [docs/architecture.md](docs/architecture.md) → [docs/trd.md](docs/trd.md) → [docs/implementation_plan.md](docs/implementation_plan.md)
3. [docs/protocol.md](docs/protocol.md) and [protocol/fixtures/vectors.json](protocol/fixtures/vectors.json) for any message or limit

## Non-negotiables

- No separate Windows executable, Wi-Fi/BLE/cloud transport, database or custom PPTX renderer.
- No PowerPoint COM calls off the UI/STA thread; no blocking work on that thread.
- Every disconnect/error/shutdown path releases held input and hides the overlay.
- Do not claim a platform behavior works until it was tested on the real target; record evidence.
- If documents conflict, or a gate fails, stop and report.

## How to work

- One plan task ID at a time (for example `P2-02`).
- Run the relevant build and tests; report exact commands and results.
- End each milestone with the completion report in [docs/rules.md §11](docs/rules.md#11-required-completion-report).
