using System;

namespace PocketPresenter.Core.Effects
{
    public enum EffectId
    {
        Honeycomb,
        Diamond,
        Ripple,
        Off
    }

    public readonly struct EffectInput
    {
        public int CursorX { get; }
        public int CursorY { get; }
        public double Velocity { get; }
        public double Intensity { get; }
        public double Scale { get; }
        public bool ReducedMotion { get; }

        public EffectInput(int cursorX, int cursorY, double velocity, double intensity, double scale, bool reducedMotion)
        {
            CursorX = cursorX;
            CursorY = cursorY;
            Velocity = velocity;
            Intensity = intensity;
            Scale = scale;
            ReducedMotion = reducedMotion;
        }
    }

    public interface IEffectSurface
    {
        int Width { get; }
        int Height { get; }
        void Clear();
        void DrawPolygon(ReadOnlySpan<(float X, float Y)> points, uint colorRgba);
        void DrawCircle(float centerX, float centerY, float radius, uint colorRgba);
    }

    /// <summary>
    /// Pure update/render interface for cursor effects. No direct I/O.
    /// </summary>
    public interface ICursorEffect
    {
        EffectId Id { get; }
        bool IsIdle { get; }

        void Update(in EffectInput input, TimeSpan dt);
        void Render(IEffectSurface surface);
        void Reset();
    }
}
