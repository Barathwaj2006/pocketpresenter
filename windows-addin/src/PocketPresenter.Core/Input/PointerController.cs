using System;
using System.Collections.Generic;
using System.Threading;

namespace PocketPresenter.Core.Input
{
    /// <summary>
    /// Thread-safe pointer and mouse input controller.
    /// Manages cursor coordinate transformation, multi-monitor virtual screen clamping,
    /// sensitivity/acceleration scaling, and the mandatory ADR-0013 held-button safety watchdog.
    /// </summary>
    public class PointerController : IInputController, IDisposable
    {
        private readonly INativeInputService _nativeService;
        private readonly object _lock = new object();
        private readonly HashSet<MouseButton> _heldButtons = new HashSet<MouseButton>();
        private readonly Timer? _watchdogTimer;
        private readonly int _watchdogTimeoutMs;

        private double _sensitivity = 1.0;
        private bool _accelerationEnabled = true;
        private bool _disposed;

        public double Sensitivity
        {
            get
            {
                lock (_lock) return _sensitivity;
            }
            set
            {
                lock (_lock)
                {
                    _sensitivity = Math.Max(0.1, Math.Min(5.0, value));
                }
            }
        }

        public bool AccelerationEnabled
        {
            get
            {
                lock (_lock) return _accelerationEnabled;
            }
            set
            {
                lock (_lock) _accelerationEnabled = value;
            }
        }

        public int HeldButtonsCount
        {
            get
            {
                lock (_lock) return _heldButtons.Count;
            }
        }

        public bool IsButtonHeld(MouseButton button)
        {
            lock (_lock) return _heldButtons.Contains(button);
        }

        public event Action? WatchdogTimedOut;

        public PointerController(
            INativeInputService? nativeService = null,
            int watchdogTimeoutMs = 3000)
        {
            _nativeService = nativeService ?? new Win32NativeInputService();
            _watchdogTimeoutMs = watchdogTimeoutMs;

            if (_watchdogTimeoutMs > 0)
            {
                _watchdogTimer = new Timer(OnWatchdogExpired, null, Timeout.Infinite, Timeout.Infinite);
            }
        }

        public void MoveBy(int dx, int dy)
        {
            if (dx == 0 && dy == 0) return;

            lock (_lock)
            {
                if (_disposed) return;

                double effectiveDx = dx * _sensitivity;
                double effectiveDy = dy * _sensitivity;

                if (_accelerationEnabled)
                {
                    double magnitude = Math.Sqrt((dx * dx) + (dy * dy));
                    if (magnitude > 12.0)
                    {
                        double factor = 1.0 + Math.Min(1.5, (magnitude - 12.0) * 0.04);
                        effectiveDx *= factor;
                        effectiveDy *= factor;
                    }
                }

                var (curX, curY) = _nativeService.GetCursorPosition();
                int targetX = curX + (int)Math.Round(effectiveDx);
                int targetY = curY + (int)Math.Round(effectiveDy);

                var bounds = _nativeService.GetVirtualScreenBounds();
                var (clampedX, clampedY) = bounds.Clamp(targetX, targetY);

                _nativeService.SetCursorPosition(clampedX, clampedY);

                if (_heldButtons.Count > 0)
                {
                    ResetWatchdog();
                }
            }
        }

        public void SetAbsolute(int x, int y)
        {
            lock (_lock)
            {
                if (_disposed) return;
                var bounds = _nativeService.GetVirtualScreenBounds();
                var (clampedX, clampedY) = bounds.Clamp(x, y);
                _nativeService.SetCursorPosition(clampedX, clampedY);
            }
        }

        public (int X, int Y) GetCursorPosition()
        {
            lock (_lock)
            {
                return _nativeService.GetCursorPosition();
            }
        }

        public void ButtonDown(MouseButton button)
        {
            lock (_lock)
            {
                if (_disposed) return;

                _heldButtons.Add(button);
                _nativeService.SendMouseButton(button, down: true);
                ResetWatchdog();
            }
        }

        public void ButtonUp(MouseButton button)
        {
            lock (_lock)
            {
                if (_disposed) return;

                _heldButtons.Remove(button);
                _nativeService.SendMouseButton(button, down: false);

                if (_heldButtons.Count == 0)
                {
                    DisarmWatchdog();
                }
                else
                {
                    ResetWatchdog();
                }
            }
        }

        public void Scroll(int dy)
        {
            if (dy == 0) return;

            lock (_lock)
            {
                if (_disposed) return;
                _nativeService.SendMouseScroll(dy);

                if (_heldButtons.Count > 0)
                {
                    ResetWatchdog();
                }
            }
        }

        public void NotifyActivity()
        {
            lock (_lock)
            {
                if (_heldButtons.Count > 0)
                {
                    ResetWatchdog();
                }
            }
        }

        public void ReleaseAll()
        {
            lock (_lock)
            {
                if (_heldButtons.Count == 0) return;

                foreach (var button in _heldButtons)
                {
                    _nativeService.SendMouseButton(button, down: false);
                }
                _heldButtons.Clear();
                DisarmWatchdog();
            }
        }

        private void ResetWatchdog()
        {
            if (_watchdogTimer != null && !_disposed)
            {
                _watchdogTimer.Change(_watchdogTimeoutMs, Timeout.Infinite);
            }
        }

        private void DisarmWatchdog()
        {
            if (_watchdogTimer != null && !_disposed)
            {
                _watchdogTimer.Change(Timeout.Infinite, Timeout.Infinite);
            }
        }

        private void OnWatchdogExpired(object? state)
        {
            lock (_lock)
            {
                if (_disposed || _heldButtons.Count == 0) return;
                ReleaseAll();
            }

            WatchdogTimedOut?.Invoke();
        }

        public void Dispose()
        {
            lock (_lock)
            {
                if (_disposed) return;
                _disposed = true;
            }

            ReleaseAll();
            _watchdogTimer?.Dispose();
        }
    }
}
