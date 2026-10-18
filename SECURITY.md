# Security Policy

## Why this matters

PocketPresenter lets a phone move the cursor and change slides on a laptop. A flaw could let an unintended device control that laptop, so we treat security reports seriously.

## Supported versions

The project is pre-release. Security fixes apply to the `main` branch until the first tagged release; this table will be updated then.

| Version | Supported |
|---|---|
| `main` (pre-release) | ✅ |

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Report privately using GitHub's *Report a vulnerability* button on the repository's **Security** tab (private vulnerability reporting). If that is unavailable, contact the maintainers through the contact method listed on the repository profile.

Include, where possible:

- A description of the issue and its impact
- Steps to reproduce, including device and PowerPoint/Android versions
- Any proof-of-concept (kept minimal and non-destructive)

## What to expect

- Acknowledgement within 5 working days
- An initial assessment and plan within 14 days
- Coordinated disclosure: we will agree a timeline with you and credit you unless you prefer otherwise

## Scope

In scope:

- Control from an unpaired, unbonded or non-selected device
- Bypass of protocol validation, rate or size limits
- Any path to executing commands, scripts, files or arbitrary keystrokes on Windows
- Commands reaching an application other than the active PowerPoint slide show
- Sensitive data in logs or diagnostics

Out of scope:

- Attacks that require physical access to an already unlocked, paired and connected phone or laptop
- Weaknesses in the operating system's Bluetooth stack itself (report to the vendor)
- Denial of service by radio jamming

## Design commitments

See [docs/architecture.md §14](docs/architecture.md#14-security-and-threat-model): bonded and encrypted OS pairing, one selected device, strict allow-listed protocol with no remote-execution surface, queues cleared on disconnect, and no secrets in diagnostics.
