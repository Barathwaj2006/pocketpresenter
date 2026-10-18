using System;

namespace PocketPresenter.Core.Effects
{
    /// <summary>
    /// Factory for instantiating ICursorEffect instances from wire protocol string identifiers.
    /// </summary>
    public static class EffectFactory
    {
        public static ICursorEffect Create(string effectId)
        {
            if (string.IsNullOrWhiteSpace(effectId))
            {
                return new HoneycombPulseEffect();
            }

            switch (effectId.Trim().ToLowerInvariant())
            {
                case "honeycomb":
                    return new HoneycombPulseEffect();

                case "diamond":
                    return new DiamondFacetsEffect();

                case "ripple":
                    return new GeometricRippleEffect();

                case "off":
                    return new OffCursorEffect();

                default:
                    return new HoneycombPulseEffect();
            }
        }
    }
}
