# Contributing to PocketPresenter

Thanks for helping. This project is deliberately **validate-first**: read this page and [docs/architecture.md](docs/architecture.md) before writing code.

## Ground rules

1. **Architecture is the source of truth.** If your change contradicts [docs/architecture.md](docs/architecture.md) or an *Accepted* entry in [docs/decisions.md](docs/decisions.md), open an `ADR:` issue first.
2. **Gate 0 comes first.** No feature work (cursor effects, Draw Mode, polish) until the three proofs pass and are recorded in [docs/feasibility-report.md](docs/feasibility-report.md).
3. **Hard constraints (never break these):**
   - No separate Windows executable or service.
   - No cloud, accounts, Wi-Fi transport or analytics.
   - No arbitrary command, keystroke, file or script messages in the protocol.
   - No Office COM calls off the PowerPoint UI/STA thread.
   - No blocking Bluetooth, discovery or rendering work on the UI thread.
   - Every disconnect/error/shutdown path releases held input and hides the overlay.
4. **Docs and code change together.** Protocol changes update [docs/protocol.md](docs/protocol.md), both implementations and [protocol/fixtures/vectors.json](protocol/fixtures/vectors.json) in the same pull request.
5. **Work from the plan.** Pick a task ID from [docs/implementation_plan.md](docs/implementation_plan.md), follow [docs/rules.md](docs/rules.md) and finish with its completion report. AI coding tools should start from [AGENTS.md](AGENTS.md).

## Workflow

1. Open or claim an issue (use the templates).
2. Branch from `main`: `feature/<short-name>`, `fix/<short-name>` or `docs/<short-name>`.
3. Keep pull requests small and focused; describe how you tested, including hardware used for anything touching Bluetooth, volume keys or the overlay.
4. Fill in the pull request checklist. A reviewer must confirm the hard constraints above.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`. Example: `feat(protocol): add effect.set validation`.

## Code standards

| Area | Standard |
|---|---|
| Android | Kotlin, Jetpack Compose, `ktlint`/`detekt` once configured, unit tests for protocol, gesture and session logic |
| Windows add-in | C# on .NET Framework 4.8, nullable-aware style, interop isolated in `PowerPoint/`, unit tests via fakes for COM wrappers |
| Protocol | Pure, dependency-free codec passing the shared vectors in [protocol/fixtures/vectors.json](protocol/fixtures/vectors.json) ([docs/protocol.md §9](docs/protocol.md#9-test-vectors)) |
| Docs | Markdown, Mermaid for diagrams, relative links, update the revision history for design changes |

## Testing expectations

- Automated tests per the [test plan](docs/architecture.md#17-test-plan-and-acceptance-criteria).
- Hardware-dependent claims ("works with screen off", "overlay is click-through") need recorded evidence, not assumptions.
- Never mark a configuration as supported until it has passed tests.

## Reporting security issues

Do **not** open a public issue. See [SECURITY.md](SECURITY.md).

## Code of conduct

Be respectful and constructive. Assume good intent, critique ideas rather than people, and keep discussion focused on making the project better.
