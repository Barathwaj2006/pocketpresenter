using System;
using System.Text.Json;

namespace PocketPresenter.Protocol
{
    public readonly struct CommandExecutionResult
    {
        public bool Success { get; }
        public string? ErrorCode { get; }
        public string? ErrorMessage { get; }

        public static CommandExecutionResult Ok() => new CommandExecutionResult(true, null, null);
        public static CommandExecutionResult Fail(string code, string message) => new CommandExecutionResult(false, code, message);

        private CommandExecutionResult(bool success, string? code, string? message)
        {
            Success = success;
            ErrorCode = code;
            ErrorMessage = message;
        }
    }

    public interface ICommandHandler
    {
        CommandExecutionResult HandleSlideNext(int seq);
        CommandExecutionResult HandleSlidePrevious(int seq);
        void HandlePointerDelta(int dx, int dy);
        void HandlePointerButton(string button, string action);
        void HandlePointerScroll(int dy);
        void HandleEffectSet(string effect, double intensity, double scale, bool reducedMotion);
        void HandleDrawStart(double x, double y, string color, int width);
        void HandleDrawPoint(JsonElement points);
        void HandleDrawEnd();
        void HandleStateRequest();
    }

    /// <summary>
    /// Strictly allow-lists commands and routes validated incoming frames to handlers.
    /// Rejects unknown, malformed, or unsafe commands without generic execution surface.
    /// </summary>
    public class CommandRouter
    {
        private readonly ICommandHandler _handler;

        public event Action<Envelope>? SendReplyRequested;

        public CommandRouter(ICommandHandler handler)
        {
            _handler = handler ?? throw new ArgumentNullException(nameof(handler));
        }

        public ValidationResult Route(Envelope envelope, ProtocolSession session)
        {
            var valResult = ProtocolValidator.Validate(envelope, session);
            if (!valResult.IsOk)
            {
                // Generate error response for discrete commands if appropriate
                if (envelope.Type == MessageTypes.SlideNext || envelope.Type == MessageTypes.SlidePrevious ||
                    envelope.Type == MessageTypes.EffectSet || envelope.Type == MessageTypes.DrawStart)
                {
                    SendError(envelope.Sequence, valResult.ErrorCode ?? ErrorCodes.Internal, valResult.ErrorMessage ?? "Error", session);
                }
                return valResult;
            }

            session.MarkSequenceExecuted(envelope.Sequence);

            switch (envelope.Type)
            {
                case MessageTypes.SlideNext:
                    var nextResult = _handler.HandleSlideNext(envelope.Sequence);
                    if (nextResult.Success)
                    {
                        SendAck(envelope.Sequence, session);
                    }
                    else
                    {
                        SendError(envelope.Sequence, nextResult.ErrorCode ?? ErrorCodes.Internal, nextResult.ErrorMessage ?? "Next failed", session);
                    }
                    break;

                case MessageTypes.SlidePrevious:
                    var prevResult = _handler.HandleSlidePrevious(envelope.Sequence);
                    if (prevResult.Success)
                    {
                        SendAck(envelope.Sequence, session);
                    }
                    else
                    {
                        SendError(envelope.Sequence, prevResult.ErrorCode ?? ErrorCodes.Internal, prevResult.ErrorMessage ?? "Previous failed", session);
                    }
                    break;

                case MessageTypes.PointerDelta:
                    int dx = envelope.Payload.TryGetProperty("dx", out var dxp) ? dxp.GetInt32() : 0;
                    int dy = envelope.Payload.TryGetProperty("dy", out var dyp) ? dyp.GetInt32() : 0;
                    _handler.HandlePointerDelta(dx, dy);
                    break;

                case MessageTypes.PointerButton:
                    string btn = envelope.Payload.TryGetProperty("button", out var bp) ? bp.GetString() ?? "" : "";
                    string act = envelope.Payload.TryGetProperty("action", out var ap) ? ap.GetString() ?? "" : "";
                    _handler.HandlePointerButton(btn, act);
                    break;

                case MessageTypes.PointerScroll:
                    int sdy = envelope.Payload.TryGetProperty("dy", out var sdp) ? sdp.GetInt32() : 0;
                    _handler.HandlePointerScroll(sdy);
                    break;

                case MessageTypes.EffectSet:
                    string eff = envelope.Payload.TryGetProperty("effect", out var ep) ? ep.GetString() ?? "off" : "off";
                    double intensity = envelope.Payload.TryGetProperty("intensity", out var ip) ? ip.GetDouble() : 0.35;
                    double scale = envelope.Payload.TryGetProperty("scale", out var scp) ? scp.GetDouble() : 1.0;
                    bool rm = envelope.Payload.TryGetProperty("reducedMotion", out var rmp) && rmp.GetBoolean();
                    _handler.HandleEffectSet(eff, intensity, scale, rm);
                    SendAck(envelope.Sequence, session);
                    break;

                case MessageTypes.DrawStart:
                    double x = envelope.Payload.TryGetProperty("x", out var xp) ? xp.GetDouble() : 0.0;
                    double y = envelope.Payload.TryGetProperty("y", out var yp) ? yp.GetDouble() : 0.0;
                    string color = envelope.Payload.TryGetProperty("color", out var cp) ? cp.GetString() ?? "#FF0000" : "#FF0000";
                    int width = envelope.Payload.TryGetProperty("width", out var wp) ? wp.GetInt32() : 3;
                    _handler.HandleDrawStart(x, y, color, width);
                    SendAck(envelope.Sequence, session);
                    break;

                case MessageTypes.DrawPoint:
                    if (envelope.Payload.TryGetProperty("points", out var pts))
                    {
                        _handler.HandleDrawPoint(pts);
                    }
                    break;

                case MessageTypes.DrawEnd:
                    _handler.HandleDrawEnd();
                    SendAck(envelope.Sequence, session);
                    break;

                case MessageTypes.StateRequest:
                    _handler.HandleStateRequest();
                    break;

                case MessageTypes.SessionPing:
                    // Respond with session.pong echoing timestamp t
                    long t = envelope.Payload.TryGetProperty("t", out var tp) ? tp.GetInt64() : 0;
                    SendPong(session.SessionId, t, session);
                    break;
            }

            return ValidationResult.Success();
        }

        private void SendAck(int seq, ProtocolSession? session = null)
        {
            var payload = JsonDocument.Parse($"{{\"ref\":{seq},\"ok\":true}}").RootElement;
            SendReplyRequested?.Invoke(new Envelope
            {
                Version = 1,
                SessionId = session?.SessionId,
                Sequence = session?.NextOutboundSequence() ?? 0,
                Type = MessageTypes.CommandAck,
                Payload = payload
            });
        }

        private void SendError(int seq, string code, string message, ProtocolSession? session = null)
        {
            var payload = JsonDocument.Parse($"{{\"ref\":{seq},\"code\":\"{code}\",\"message\":\"{message}\"}}").RootElement;
            SendReplyRequested?.Invoke(new Envelope
            {
                Version = 1,
                SessionId = session?.SessionId,
                Sequence = session?.NextOutboundSequence() ?? 0,
                Type = MessageTypes.CommandError,
                Payload = payload
            });
        }

        private void SendPong(string? sid, long timestamp, ProtocolSession? session = null)
        {
            var payload = JsonDocument.Parse($"{{\"t\":{timestamp}}}").RootElement;
            SendReplyRequested?.Invoke(new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = session?.NextOutboundSequence() ?? 0,
                Type = MessageTypes.SessionPong,
                Payload = payload
            });
        }
    }
}
