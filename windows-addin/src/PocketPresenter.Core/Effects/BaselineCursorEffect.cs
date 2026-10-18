using System;

namespace PocketPresenter.Core.Effects
{
    /// <summary>
    /// Phase 5 Baseline cursor effect.
    /// Draws a crisp dual-tone center dot and subtle accent ring following the Windows cursor.
    /// Settles to idle when stationary to eliminate redundant redraw cycles.
    /// </summary>
    public class BaselineCursorEffect : ICursorEffect
    {
        public EffectId Id => EffectId.Ripple;
        public bool IsIdle { get; private set; }

        private double _intensity = 0.8;
        private double _scale = 1.0;
        private bool _reducedMotion;
        private TimeSpan _stationaryDuration = TimeSpan.Zero;
        private const double IdleThresholdSeconds = 0.5;

        public void Update(in EffectInput input, TimeSpan dt)
        {
            _intensity = Math.Max(0.0, Math.Min(1.0, input.Intensity));
            _scale = Math.Max(0.5, Math.Min(2.0, input.Scale));
            _reducedMotion = input.ReducedMotion;

            if (input.Velocity > 0.5)
            {
                _stationaryDuration = TimeSpan.Zero;
                IsIdle = false;
            }
            else
            {
                _stationaryDuration += dt;
                if (_stationaryDuration.TotalSeconds >= IdleThresholdSeconds)
                {
                    IsIdle = true;
                }
            }
        }

        public void Render(IEffectSurface surface)
        {
            surface.Clear();

            if (_intensity <= 0.001) return;

            float centerX = surface.Width / 2.0f;
            float centerY = surface.Height / 2.0f;

            // 1. Outer subtle accent ring (steel-blue/teal)
            float outerRadius = (float)(14.0 * _scale);
            byte outerAlpha = (byte)(140 * _intensity);
            uint outerColor = 0x00838F00u | outerAlpha; // Teal RGBA
            surface.DrawCircle(centerX, centerY, outerRadius, outerColor);

            // 2. Inner crisp dot (cyan/teal highlight)
            float innerRadius = (float)(5.0 * _scale);
            byte innerAlpha = (byte)(230 * _intensity);
            uint innerColor = 0x00E5FF00u | innerAlpha; // Cyan RGBA
            surface.DrawCircle(centerX, centerY, innerRadius, innerColor);
        }

        public void Reset()
        {
            _stationaryDuration = TimeSpan.Zero;
            IsIdle = false;
        }
    }
}
