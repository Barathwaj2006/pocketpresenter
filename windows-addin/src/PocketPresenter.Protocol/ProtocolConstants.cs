using System;
using System.Collections.Generic;

namespace PocketPresenter.Protocol
{
    /// <summary>
    /// Authoritative wire protocol constants and validation limits.
    /// Keep strictly synchronized with docs/protocol.md section 5.
    /// </summary>
    public static class ProtocolConstants
    {
        public const int CurrentProtocolVersion = 1;

        /// <summary>
        /// Maximum frame size in bytes including the trailing newline '\n'.
        /// Frames exceeding this must be rejected with BAD_FRAME and connection closed.
        /// </summary>
        public const int MaxFrameBytes = 4096;

        public const int MinPointerDelta = -2000;
        public const int MaxPointerDelta = 2000;

        public const int MinPointerScroll = -1000;
        public const int MaxPointerScroll = 1000;

        public const int MaxDrawPointsPerMessage = 32;

        public const double MinEffectIntensity = 0.0;
        public const double MaxEffectIntensity = 1.0;

        public const double MinEffectScale = 0.5;
        public const double MaxEffectScale = 2.0;

        public static readonly HashSet<string> AllowedEffects = new HashSet<string>(StringComparer.Ordinal)
        {
            "honeycomb",
            "diamond",
            "ripple",
            "off"
        };

        public static readonly HashSet<string> AllowedPointerButtons = new HashSet<string>(StringComparer.Ordinal)
        {
            "left",
            "right"
        };

        public static readonly HashSet<string> AllowedButtonActions = new HashSet<string>(StringComparer.Ordinal)
        {
            "down",
            "up"
        };
    }
}
