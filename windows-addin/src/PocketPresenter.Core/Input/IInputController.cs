namespace PocketPresenter.Core.Input
{
    public enum MouseButton
    {
        Left,
        Right,
        Middle
    }

    /// <summary>
    /// Pure input controller. The ONLY component that invokes Win32 cursor/mouse APIs.
    /// ReleaseAll() MUST be called on every disconnect, error, and shutdown path.
    /// </summary>
    public interface IInputController
    {
        void MoveBy(int dx, int dy);
        void SetAbsolute(int x, int y);
        void ButtonDown(MouseButton button);
        void ButtonUp(MouseButton button);
        void Scroll(int dy);

        /// <summary>
        /// Releases all held mouse buttons and clears drag states.
        /// Mandatory on disconnect or error.
        /// </summary>
        void ReleaseAll();
    }
}
