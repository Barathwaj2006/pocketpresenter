using System;

namespace PocketPresenter.Core.Effects
{
    /// <summary>
    /// Effect 1: Honeycomb Pulse.
    /// Draws connected hexagonal cells with dual-tone edge shading (dark under-stroke with cyan highlights).
    /// Dynamically shifts local cell luminance with movement and settles to idle when stopped.
    /// </summary>
    public class HoneycombPulseEffect : ICursorEffect
    {
        public EffectId Id => EffectId.Honeycomb;
        public bool IsIdle { get; private set; }

        private double _intensity = 0.35;
        private double _scale = 1.0;
        private double _velocity;
        private bool _reducedMotion;
        private TimeSpan _stationaryDuration = TimeSpan.Zero;
        private const double SettleThresholdSeconds = 0.30;

        // Pre-calculated hex center offsets (ring 0: (0,0); ring 1: 6 neighbors)
        private static readonly (float X, float Y)[] HexOffsets = new (float X, float Y)[]
        {
            (0f, 0f),
            (0f, -1.732f),
            (1.5f, -0.866f),
            (1.5f, 0.866f),
            (0f, 1.732f),
            (-1.5f, 0.866f),
            (-1.5f, -0.866f)
        };

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
            float cellRadius = (float)(14.0 * _scale);

            // Dual-tone edge: dark under-stroke for light slides, cyan for dark slides
            byte darkAlpha = (byte)(80 * _intensity);
            uint darkUnderStroke = 0x05101A00u | darkAlpha;

            byte cyanAlpha = (byte)(200 * _intensity);
            uint cyanHighlight = 0x00E5FF00u | cyanAlpha;

            var hexPoints = new (float X, float Y)[6];

            foreach (var offset in HexOffsets)
            {
                float hx = centerX + (offset.X * cellRadius);
                float hy = centerY + (offset.Y * cellRadius);

                // Compute 6 vertices of hexagon
                for (int i = 0; i < 6; i++)
                {
                    double angle = (Math.PI / 3.0 * i) + (Math.PI / 6.0);
                    hexPoints[i] = (hx + (float)(cellRadius * Math.Cos(angle)),
                                    hy + (float)(cellRadius * Math.Sin(angle)));
                }

                // 1. Draw dark under-stroke slightly thicker/expanded
                surface.DrawPolygon(hexPoints, darkUnderStroke);

                // 2. Draw cyan edge highlight
                surface.DrawPolygon(hexPoints, cyanHighlight);
            }

            // Crisp center marker at true cursor location
            surface.DrawCircle(centerX, centerY, (float)(3.0 * _scale), cyanHighlight);
        }

        public void Reset()
        {
            _stationaryDuration = TimeSpan.Zero;
            IsIdle = false;
        }
    }
}
