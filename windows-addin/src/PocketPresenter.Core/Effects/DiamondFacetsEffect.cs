using System;

namespace PocketPresenter.Core.Effects
{
    /// <summary>
    /// Effect 2: Diamond Facets.
    /// Draws small rhombus/diamond facets with directional edge shading and dual-tone strokes.
    /// Preserves exact cursor anchor and footprint identical to Honeycomb Pulse.
    /// </summary>
    public class DiamondFacetsEffect : ICursorEffect
    {
        public EffectId Id => EffectId.Diamond;
        public bool IsIdle { get; private set; }

        private double _intensity = 0.35;
        private double _scale = 1.0;
        private double _velocity;
        private bool _reducedMotion;
        private TimeSpan _stationaryDuration = TimeSpan.Zero;
        private const double SettleThresholdSeconds = 0.30;

        // Diamond center offsets (cross and corners)
        private static readonly (float X, float Y)[] DiamondOffsets = new (float X, float Y)[]
        {
            (0f, 0f),
            (0f, -1.8f),
            (1.8f, 0f),
            (0f, 1.8f),
            (-1.8f, 0f),
            (1.2f, -1.2f),
            (1.2f, 1.2f),
            (-1.2f, 1.2f),
            (-1.2f, -1.2f)
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
            float facetRadius = (float)(12.0 * _scale);

            byte darkAlpha = (byte)(80 * _intensity);
            uint darkUnderStroke = 0x05101A00u | darkAlpha;

            byte tealAlpha = (byte)(190 * _intensity);
            uint tealHighlight = 0x00838F00u | tealAlpha;

            byte cyanAlpha = (byte)(230 * _intensity);
            uint cyanHighlight = 0x00E5FF00u | cyanAlpha;

            var diamondPoints = new (float X, float Y)[4];

            foreach (var offset in DiamondOffsets)
            {
                float dx = centerX + (offset.X * facetRadius);
                float dy = centerY + (offset.Y * facetRadius);

                // 4 diamond vertices: top, right, bottom, left
                diamondPoints[0] = (dx, dy - facetRadius);
                diamondPoints[1] = (dx + (facetRadius * 0.8f), dy);
                diamondPoints[2] = (dx, dy + facetRadius);
                diamondPoints[3] = (dx - (facetRadius * 0.8f), dy);

                // 1. Dark under-stroke
                surface.DrawPolygon(diamondPoints, darkUnderStroke);

                // 2. Teal / cyan facet highlight
                surface.DrawPolygon(diamondPoints, tealHighlight);
            }

            // True center marker
            surface.DrawCircle(centerX, centerY, (float)(3.0 * _scale), cyanHighlight);
        }

        public void Reset()
        {
            _stationaryDuration = TimeSpan.Zero;
            IsIdle = false;
        }
    }
}
