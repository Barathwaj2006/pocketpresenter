using System;

namespace PocketPresenter.Core.Effects
{
    /// <summary>
    /// Effect 3: Geometric Ripple.
    /// Draws two or three concentric geometric contours around the pointer.
    /// Lowest visual density, ideal for technical, dense, or text-heavy slides.
    /// </summary>
    public class GeometricRippleEffect : ICursorEffect
    {
        public EffectId Id => EffectId.Ripple;
        public bool IsIdle { get; private set; }

        private double _intensity = 0.35;
        private double _scale = 1.0;
        private double _velocity;
        private bool _reducedMotion;
        private TimeSpan _stationaryDuration = TimeSpan.Zero;
        private const double SettleThresholdSeconds = 0.40;

        public void Update(in EffectInput input, TimeSpan dt)
        {
            _intensity = Math.Max(0.0, Math.Min(1.0, input.Intensity));
            _scale = Math.Max(0.5, Math.Min(2.0, input.Scale));
            _reducedMotion = input.ReducedMotion;
            _velocity = input.Velocity;

            if (_velocity > 0.5)
            {
                _stationaryDuration = TimeSpan.Zero;
                IsIdle = false;
            }
            else
            {
                _stationaryDuration += dt;
                if (_stationaryDuration.TotalSeconds >= SettleThresholdSeconds)
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

            // Concentric geometric ring radii
            float r1 = (float)(12.0 * _scale);
            float r2 = (float)(24.0 * _scale);
            float r3 = (float)(38.0 * _scale);

            // Shading with dual-tone edge
            byte darkAlpha = (byte)(70 * _intensity);
            uint darkUnderStroke = 0x05101A00u | darkAlpha;

            byte ring3Alpha = (byte)(110 * _intensity);
            uint ring3Color = 0x00838F00u | ring3Alpha;

            byte ring2Alpha = (byte)(160 * _intensity);
            uint ring2Color = 0x00ACC100u | ring2Alpha;

            byte ring1Alpha = (byte)(230 * _intensity);
            uint ring1Color = 0x00E5FF00u | ring1Alpha;

            // Outer ring 3
            surface.DrawCircle(centerX, centerY, r3 + 0.5f, darkUnderStroke);
            surface.DrawCircle(centerX, centerY, r3, ring3Color);

            // Middle ring 2
            surface.DrawCircle(centerX, centerY, r2 + 0.5f, darkUnderStroke);
            surface.DrawCircle(centerX, centerY, r2, ring2Color);

            // Inner ring 1
            surface.DrawCircle(centerX, centerY, r1 + 0.5f, darkUnderStroke);
            surface.DrawCircle(centerX, centerY, r1, ring1Color);

            // True center point
            surface.DrawCircle(centerX, centerY, (float)(3.0 * _scale), ring1Color);
        }

        public void Reset()
        {
            _stationaryDuration = TimeSpan.Zero;
            IsIdle = false;
        }
    }
}
