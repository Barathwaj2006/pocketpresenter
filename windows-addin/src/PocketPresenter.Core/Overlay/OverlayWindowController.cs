using System;
using PocketPresenter.Core.Effects;

namespace PocketPresenter.Core.Overlay
{
    /// <summary>
    /// Coordinates overlay window lifecycle, positioning, canvas rendering,
    /// and ensures click-through, non-activating presentation display.
    /// Hides and cleans up unconditionally on disconnect, slide-show end, or shutdown.
    /// </summary>
    public class OverlayWindowController : IOverlayWindow
    {
        private readonly INativeOverlayWindow _nativeWindow;
        private ICursorEffect _effect;
        private readonly MemoryEffectSurface _surface;
        private readonly object _lock = new object();

        public const int DefaultCanvasSize = 200; // 200x200 px local effect canvas

        public bool IsVisible { get; private set; }
        public IntPtr Handle => _nativeWindow.WindowHandle;
        public int CanvasSize { get; }

        public int CurrentCursorX { get; private set; }
        public int CurrentCursorY { get; private set; }
        public double CurrentVelocity { get; private set; }

        public double Intensity { get; set; } = 0.8;
        public double Scale { get; set; } = 1.0;
        public bool ReducedMotion { get; set; }

        private bool _disposed;

        public OverlayWindowController(
            INativeOverlayWindow? nativeWindow = null,
            ICursorEffect? effect = null,
            int canvasSize = DefaultCanvasSize)
        {
            CanvasSize = Math.Max(64, canvasSize);
            _nativeWindow = nativeWindow ?? new Win32OverlayWindow();
            _effect = effect ?? new BaselineCursorEffect();
            _surface = new MemoryEffectSurface(CanvasSize, CanvasSize);
        }

        public void SetEffect(ICursorEffect effect)
        {
            lock (_lock)
            {
                _effect = effect ?? throw new ArgumentNullException(nameof(effect));
                _effect.Reset();
                if (IsVisible)
                {
                    RenderFrame();
                }
            }
        }

        public void Show(int monitorX, int monitorY, int width, int height)
        {
            lock (_lock)
            {
                if (_disposed) return;

                if (!_nativeWindow.IsCreated)
                {
                    _nativeWindow.Create(monitorX, monitorY, CanvasSize, CanvasSize);
                }

                _nativeWindow.SetVisible(true);
                IsVisible = true;
                _effect.Reset();
                RenderFrame();
            }
        }

        public void Hide()
        {
            lock (_lock)
            {
                if (!IsVisible) return;

                IsVisible = false;
                _nativeWindow.SetVisible(false);
                _surface.Clear();
                _effect.Reset();
            }
        }

        public void UpdateCursorPosition(int screenX, int screenY)
        {
            lock (_lock)
            {
                if (_disposed) return;

                double dx = screenX - CurrentCursorX;
                double dy = screenY - CurrentCursorY;
                CurrentVelocity = Math.Sqrt((dx * dx) + (dy * dy));

                CurrentCursorX = screenX;
                CurrentCursorY = screenY;

                if (IsVisible)
                {
                    RenderFrame();
                }
            }
        }

        public void RenderFrame()
        {
            lock (_lock)
            {
                if (!IsVisible || _disposed) return;

                var input = new EffectInput(
                    CurrentCursorX,
                    CurrentCursorY,
                    CurrentVelocity,
                    Intensity,
                    Scale,
                    ReducedMotion);

                _effect.Update(input, TimeSpan.FromMilliseconds(16.67));

                // Always render when active or moving
                _effect.Render(_surface);

                int winX = CurrentCursorX - (CanvasSize / 2);
                int winY = CurrentCursorY - (CanvasSize / 2);

                _nativeWindow.UpdatePixels(_surface.Pixels, _surface.Width, _surface.Height, winX, winY);
            }
        }

        public void Dispose()
        {
            lock (_lock)
            {
                if (_disposed) return;
                _disposed = true;

                Hide();
                _nativeWindow.Destroy();
                _nativeWindow.Dispose();
            }
        }
    }
}
