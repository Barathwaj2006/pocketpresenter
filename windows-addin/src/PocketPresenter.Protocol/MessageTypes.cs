using System;
using System.Collections.Generic;

namespace PocketPresenter.Protocol
{
    /// <summary>
    /// Supported wire message type strings as specified in docs/protocol.md section 4.
    /// </summary>
    public static class MessageTypes
    {
        public const string Hello = "hello";
        public const string HelloAck = "hello.ack";
        public const string SessionPing = "session.ping";
        public const string SessionPong = "session.pong";
        public const string StateRequest = "state.request";
        public const string StateUpdate = "state.update";
        public const string PointerDelta = "pointer.delta";
        public const string PointerButton = "pointer.button";
        public const string PointerScroll = "pointer.scroll";
        public const string SlideNext = "slide.next";
        public const string SlidePrevious = "slide.previous";
        public const string EffectSet = "effect.set";
        public const string DrawStart = "draw.start";
        public const string DrawPoint = "draw.point";
        public const string DrawEnd = "draw.end";
        public const string CommandAck = "command.ack";
        public const string CommandError = "command.error";

        public static readonly HashSet<string> AllValidTypes = new HashSet<string>(StringComparer.Ordinal)
        {
            Hello,
            HelloAck,
            SessionPing,
            SessionPong,
            StateRequest,
            StateUpdate,
            PointerDelta,
            PointerButton,
            PointerScroll,
            SlideNext,
            SlidePrevious,
            EffectSet,
            DrawStart,
            DrawPoint,
            DrawEnd,
            CommandAck,
            CommandError
        };

        public static bool IsValidType(string? type)
        {
            return type != null && AllValidTypes.Contains(type);
        }
    }
}
