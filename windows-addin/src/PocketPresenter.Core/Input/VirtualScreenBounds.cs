using System;

namespace PocketPresenter.Core.Input
{
    /// <summary>
    /// Represents the bounding rectangle of the virtual desktop across all active monitors.
    /// Used to clamp simulated cursor positions and prevent off-screen pointer loss.
    /// </summary>
    public readonly struct VirtualScreenBounds
    {
        public int Left { get; }
        public int Top { get; }
        public int Width { get; }
        public int Height { get; }

        public int Right => Left + Width;
        public int Bottom => Top + Height;

        public VirtualScreenBounds(int left, int top, int width, int height)
        {
            Left = left;
            Top = top;
            Width = Math.Max(1, width);
            Height = Math.Max(1, height);
        }

        public (int X, int Y) Clamp(int x, int y)
        {
            int clampedX = Math.Max(Left, Math.Min(Right - 1, x));
            int clampedY = Math.Max(Top, Math.Min(Bottom - 1, y));
            return (clampedX, clampedY);
        }
    }
}
