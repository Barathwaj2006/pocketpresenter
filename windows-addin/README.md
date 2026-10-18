# PocketPresenter — PowerPoint add-in

VSTO/COM add-in on .NET Framework 4.8. **Not implemented yet.** Start with the Gate 0 work below.

## Planned module layout

See [architecture.md §5](../docs/architecture.md#5-component-boundaries).

```text
AddIn/ PowerPoint/ Bluetooth/ Protocol/ Input/ Overlay/ Effects/ Settings/ Diagnostics/
```

## Gate 0 tasks for this folder

1. **Proof 0.1:** `IBluetoothTransport` inside the VSTO host connecting to the phone's RFCOMM service. Try the WinRT RFCOMM projection first; if it misbehaves in the .NET Framework host, use native Winsock/Bluetooth APIs behind the same interface. **Never** add a helper executable.
2. **Proof 0.2:** click-through, non-activating layered window (`WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW`) following the cursor during a real slide show; hidden on `SlideShowEnd`.

Record all results in [feasibility-report.md](../docs/feasibility-report.md).

## Rules

- Office Interop only inside `PowerPoint/`, only on the UI/STA thread via the dispatcher.
- Bluetooth read loop and rendering never run on the UI thread.
- Only `Input/` calls cursor/mouse Win32 APIs; only `Overlay/` owns the overlay window.
- Unsubscribe every PowerPoint event on shutdown; dispose overlay and timers.
- Never modify or save the user's presentation.
