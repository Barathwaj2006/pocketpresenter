## Summary

<!-- What does this change and why? Link the issue: Closes #__ -->

## Type

- [ ] Gate 0 proof-of-concept
- [ ] Feature (only after Gate 0 passes)
- [ ] Fix
- [ ] Docs
- [ ] Refactor / chore

## How was it tested?

<!-- Automated tests, plus hardware used for anything touching Bluetooth, volume keys or the overlay (phone model, Android version, laptop, PowerPoint build). -->

## Checklist

- [ ] Consistent with `docs/architecture.md` and Accepted entries in `docs/decisions.md` (or an ADR is linked)
- [ ] Docs updated (`docs/protocol.md` for protocol changes; revision history for design changes)
- [ ] No separate Windows executable, cloud, Wi-Fi or analytics added
- [ ] No arbitrary command/keystroke/file/script messages added to the protocol
- [ ] No Office COM calls off the UI/STA thread; no blocking work on the UI thread
- [ ] Disconnect/error/shutdown paths release held input and hide the overlay
- [ ] Gate 0 results recorded in `docs/feasibility-report.md` (if applicable)
