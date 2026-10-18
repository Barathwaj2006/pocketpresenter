using System;

namespace PocketPresenter.Core.Overlay
{
    /// <summary>
    /// Abstraction for platform-specific transparent layered window operations.
    /// Enables full testability of overlay positioning, click-through, and lifecycle.
    /// </summary>
    public interface INativeOverlayWindow : IDisposable
    {
        bool IsCreated { get; }
        bool IsVisible { get; }
        IntPtr WindowHandle { get; }

        void Create(int x, int y, int width, int height);
        void SetPosition(int x, int y);
        void SetVisible(bool visible);
        void UpdatePixels(uint[] premultipliedBgraPixels, int width, int height, int screenX, int screenY);
        void Destroy();
    }
}
