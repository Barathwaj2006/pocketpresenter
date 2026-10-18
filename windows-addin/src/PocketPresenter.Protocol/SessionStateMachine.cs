using System;
using System.Text.Json;

namespace PocketPresenter.Protocol
{
    public enum SessionState
    {
        Disconnected,
        Connecting,
        ConnectedIdle,
        PresentingArmed,
        Disconnecting
    }

    /// <summary>
    /// Coordinates session lifecycle, handshake negotiation, and mandatory disconnect cleanup.
    /// </summary>
    public class SessionStateMachine
    {
        public SessionState State { get; private set; } = SessionState.Disconnected;
        public ProtocolSession Session { get; } = new ProtocolSession();

        public event Action<SessionState>? StateChanged;
        public event Action<Envelope>? OutboundMessageReady;
        public event Action? DisconnectCleanupRequired;

        public void StartConnecting()
        {
            SetState(SessionState.Connecting);
            Session.Reset();

            // 1. Send hello (no sid)
            var helloPayload = JsonDocument.Parse("{\"client\":\"addin\",\"appVersion\":\"0.1.0\",\"supports\":[1],\"capabilities\":[\"pointer\",\"slides\",\"effects\"]}").RootElement;
            OutboundMessageReady?.Invoke(new Envelope
            {
                Version = 1,
                Sequence = 0,
                Type = MessageTypes.Hello,
                Payload = helloPayload
            });
        }

        public void HandleInboundEnvelope(Envelope envelope)
        {
            if (State == SessionState.Connecting && envelope.Type == MessageTypes.HelloAck)
            {
                bool accepted = envelope.Payload.TryGetProperty("accepted", out var acc) && acc.GetBoolean();
                if (accepted)
                {
                    string? sid = envelope.Payload.TryGetProperty("sessionId", out var sidProp) ? sidProp.GetString() : null;
                    if (!string.IsNullOrEmpty(sid))
                    {
                        Session.Reset(sid);
                        SetState(SessionState.ConnectedIdle);
                        return;
                    }
                }

                // If not accepted or invalid sid, disconnect
                Disconnect();
            }
        }

        public void ArmPresentation(bool isSlideShowActive)
        {
            if (State == SessionState.ConnectedIdle && isSlideShowActive)
            {
                SetState(SessionState.PresentingArmed);
            }
            else if (State == SessionState.PresentingArmed && !isSlideShowActive)
            {
                SetState(SessionState.ConnectedIdle);
            }
        }

        public void Disconnect()
        {
            if (State == SessionState.Disconnected) return;

            SetState(SessionState.Disconnecting);

            // Mandatory cleanup: release held inputs, hide overlay, purge queues
            DisconnectCleanupRequired?.Invoke();
            Session.Reset();

            SetState(SessionState.Disconnected);
        }

        private void SetState(SessionState newState)
        {
            if (State != newState)
            {
                State = newState;
                StateChanged?.Invoke(newState);
            }
        }
    }
}
