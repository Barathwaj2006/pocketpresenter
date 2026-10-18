namespace PocketPresenter.Core.Input
{
    /// <summary>
    /// Abstraction over platform Win32 cursor and mouse simulation APIs.
    /// Enables full testability without requiring a physical mouse or Windows session.
    /// </summary>
    public interface INativeInputService
    {
        (int X, int Y) GetCursorPosition();
        void SetCursorPosition(int x, int y);
        void SendMouseButton(MouseButton button, bool down);
        void SendMouseScroll(int dy);
        VirtualScreenBounds GetVirtualScreenBounds();
    }
}
