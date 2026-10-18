# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project will adopt [Semantic Versioning](https://semver.org/) from the first release.

## [Unreleased]

### Added
- Architecture document v1.2 with requirement IDs, alternatives considered, sequence and state diagrams, threading model, threat model, supported-configuration matrix, open questions and glossary.
- Wire protocol specification with limits, error codes and test vectors.
- Gate 0 feasibility report template.
- Decision log (ADR-0001 to ADR-0009).
- Project root: README, LICENSE (MIT), CONTRIBUTING, SECURITY, issue and pull request templates, repository configuration files, and `android/` and `windows-addin/` scaffolds.
- Full documentation pack integrated under `docs/`: PRD, TRD, app flow, UI/UX brief, local data model, implementation plan and AI coding rules, each enhanced (IDs, diagrams, tables, revision history).
- `docs/traceability.md`, `tests/manual-test-matrix.md`, `protocol/fixtures/vectors.json` and `AGENTS.md`.

### Changed (cross-document reconciliation)
- Requirement IDs: the PRD owns `FR-*`/`NFR-*`/`AT-*`; the architecture now uses `AC-*` constraints (ADR-0012). Added FR-08 (Pocket Mode) and FR-09 (ribbon), and numbered the PRD's NFRs.
- Protocol: added `off` effect value (ADR-0011) and session ID `sid` plus `BAD_SESSION` (ADR-0010); canonical draw message is `draw.point` (the data model said `draw.points`).
- Soak test is 60 minutes everywhere (the plan said 30).
- Effect size: 60–100 px radius inside a 180–240 px render canvas (the design brief and architecture previously looked contradictory).
- Canonical state names, wire values and UI labels unified in one mapping table.
- Gate 0 evidence lives in `docs/feasibility-report.md`; per-device results in `tests/manual-test-matrix.md`.
- `trd.md` was referenced by the pack but not supplied; it was derived from the other documents and marked as such.
- UI brief now records measured token contrast: `borderSubtle` and `accentMuted` are not usable for control affordances or text.

### Notes
- No implementation exists yet. Gate 0 proofs are the next milestone.
