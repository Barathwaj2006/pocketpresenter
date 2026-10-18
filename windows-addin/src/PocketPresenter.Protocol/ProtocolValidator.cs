using System;
using System.Collections.Generic;
using System.Text.Json;

namespace PocketPresenter.Protocol
{
    public class ValidationResult
    {
        public bool IsOk { get; set; }
        public string? ErrorCode { get; set; }
        public string? ErrorMessage { get; set; }
        public bool CloseConnection { get; set; }
        public bool IsDuplicate { get; set; }

        public static ValidationResult Success() => new ValidationResult { IsOk = true };
        public static ValidationResult Error(string code, string message, bool closeConnection = false) =>
            new ValidationResult { IsOk = false, ErrorCode = code, ErrorMessage = message, CloseConnection = closeConnection };
        public static ValidationResult Duplicate(string message) =>
            new ValidationResult { IsOk = false, ErrorCode = ErrorCodes.Duplicate, ErrorMessage = message, IsDuplicate = true };
    }

    public class ProtocolSession
    {
        public bool HandshakeComplete { get; set; }
        public string? SessionId { get; set; }
        public int LastSequenceReceived { get; set; } = -1;
        private readonly HashSet<int> _executedSequences = new HashSet<int>();
        private int _nextOutboundSeq;

        public void Reset(string? newSessionId = null)
        {
            HandshakeComplete = !string.IsNullOrEmpty(newSessionId);
            SessionId = newSessionId;
            LastSequenceReceived = -1;
            _executedSequences.Clear();
            _nextOutboundSeq = 0;
        }

        public bool HasExecutedSequence(int seq) => _executedSequences.Contains(seq);
        public void MarkSequenceExecuted(int seq) => _executedSequences.Add(seq);
        public int NextOutboundSequence() => _nextOutboundSeq++;
    }

    public static class ProtocolValidator
    {
        public static ValidationResult Validate(Envelope envelope, ProtocolSession session)
        {
            if (envelope == null)
            {
                return ValidationResult.Error(ErrorCodes.BadFrame, "Envelope is null", true);
            }

            // 1. Check Protocol Version
            if (envelope.Version != ProtocolConstants.CurrentProtocolVersion)
            {
                return ValidationResult.Error(ErrorCodes.BadVersion, $"Unsupported protocol version {envelope.Version}");
            }

            // 2. Check Type
            if (!MessageTypes.IsValidType(envelope.Type))
            {
                return ValidationResult.Error(ErrorCodes.BadType, $"Unknown message type '{envelope.Type}'");
            }

            // 3. Handshake check
            if (!session.HandshakeComplete)
            {
                if (envelope.Type != MessageTypes.Hello && envelope.Type != MessageTypes.HelloAck)
                {
                    return ValidationResult.Error(ErrorCodes.NotReady, $"Message '{envelope.Type}' rejected: handshake not complete");
                }
            }
            else
            {
                // After handshake, sid is mandatory on every message
                if (string.IsNullOrEmpty(envelope.SessionId) || envelope.SessionId != session.SessionId)
                {
                    return ValidationResult.Error(ErrorCodes.BadSession, $"Invalid or mismatching session id: '{envelope.SessionId}'");
                }
            }

            // 4. Duplicate sequence check for discrete commands (e.g. slide.next, slide.previous)
            if (envelope.Type == MessageTypes.SlideNext || envelope.Type == MessageTypes.SlidePrevious)
            {
                if (session.HasExecutedSequence(envelope.Sequence))
                {
                    return ValidationResult.Duplicate($"Duplicate sequence {envelope.Sequence} for {envelope.Type}");
                }
            }

            // 5. Payload field bounds validation
            var payloadResult = ValidatePayload(envelope.Type, envelope.Payload);
            if (!payloadResult.IsOk)
            {
                return payloadResult;
            }

            return ValidationResult.Success();
        }

        private static ValidationResult ValidatePayload(string type, JsonElement payload)
        {
            if (payload.ValueKind != JsonValueKind.Object && payload.ValueKind != JsonValueKind.Undefined)
            {
                return ValidationResult.Error(ErrorCodes.BadField, "Payload must be a JSON object");
            }

            switch (type)
            {
                case MessageTypes.PointerDelta:
                    if (payload.TryGetProperty("dx", out var dxProp))
                    {
                        if (dxProp.ValueKind != JsonValueKind.Number || !dxProp.TryGetInt32(out var dx) ||
                            dx < ProtocolConstants.MinPointerDelta || dx > ProtocolConstants.MaxPointerDelta)
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, $"dx out of bounds [{ProtocolConstants.MinPointerDelta}, {ProtocolConstants.MaxPointerDelta}]");
                        }
                    }
                    if (payload.TryGetProperty("dy", out var dyProp))
                    {
                        if (dyProp.ValueKind != JsonValueKind.Number || !dyProp.TryGetInt32(out var dy) ||
                            dy < ProtocolConstants.MinPointerDelta || dy > ProtocolConstants.MaxPointerDelta)
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, $"dy out of bounds [{ProtocolConstants.MinPointerDelta}, {ProtocolConstants.MaxPointerDelta}]");
                        }
                    }
                    break;

                case MessageTypes.PointerScroll:
                    if (payload.TryGetProperty("dy", out var scrollDyProp))
                    {
                        if (scrollDyProp.ValueKind != JsonValueKind.Number || !scrollDyProp.TryGetInt32(out var sdy) ||
                            sdy < ProtocolConstants.MinPointerScroll || sdy > ProtocolConstants.MaxPointerScroll)
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, $"scroll dy out of bounds [{ProtocolConstants.MinPointerScroll}, {ProtocolConstants.MaxPointerScroll}]");
                        }
                    }
                    break;

                case MessageTypes.PointerButton:
                    if (payload.TryGetProperty("button", out var btnProp))
                    {
                        var btn = btnProp.GetString();
                        if (btn == null || !ProtocolConstants.AllowedPointerButtons.Contains(btn))
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, $"Invalid button '{btn}'");
                        }
                    }
                    if (payload.TryGetProperty("action", out var actProp))
                    {
                        var act = actProp.GetString();
                        if (act == null || !ProtocolConstants.AllowedButtonActions.Contains(act))
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, $"Invalid action '{act}'");
                        }
                    }
                    break;

                case MessageTypes.EffectSet:
                    if (payload.TryGetProperty("effect", out var effProp))
                    {
                        var eff = effProp.GetString();
                        if (eff == null || !ProtocolConstants.AllowedEffects.Contains(eff))
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, $"Unknown effect '{eff}'");
                        }
                    }
                    if (payload.TryGetProperty("intensity", out var intProp))
                    {
                        if (intProp.ValueKind != JsonValueKind.Number || !intProp.TryGetDouble(out var intensity) ||
                            intensity < ProtocolConstants.MinEffectIntensity || intensity > ProtocolConstants.MaxEffectIntensity)
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, "intensity out of range [0.0, 1.0]");
                        }
                    }
                    if (payload.TryGetProperty("scale", out var scaleProp))
                    {
                        if (scaleProp.ValueKind != JsonValueKind.Number || !scaleProp.TryGetDouble(out var scale) ||
                            scale < ProtocolConstants.MinEffectScale || scale > ProtocolConstants.MaxEffectScale)
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, "scale out of range [0.5, 2.0]");
                        }
                    }
                    break;

                case MessageTypes.DrawPoint:
                    if (payload.TryGetProperty("points", out var ptsProp) && ptsProp.ValueKind == JsonValueKind.Array)
                    {
                        if (ptsProp.GetArrayLength() > ProtocolConstants.MaxDrawPointsPerMessage)
                        {
                            return ValidationResult.Error(ErrorCodes.BadField, $"Exceeded max draw points ({ProtocolConstants.MaxDrawPointsPerMessage})");
                        }
                    }
                    break;
            }

            return ValidationResult.Success();
        }
    }
}
