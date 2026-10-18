using System;

namespace PocketPresenter.Core.Overlay
{
    /// <summary>
    /// Layered click-through overlay window interface.
    /// WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW
    /// </summary>
    public interface IOverlayWindow : IDisposable
    {
        bool IsVisible { get; }
        IntPtr Handle { get; }

        void Show(int monitorX, int monitorY, int width, int height);
        void Hide();
        void UpdateCursorPosition(int screenX, int screenY);
    }
}
