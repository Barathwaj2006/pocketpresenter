using System;
using System.Text.Json;
using PocketPresenter.Protocol;

namespace PocketPresenter.Core.PowerPoint
{
    /// <summary>
    /// Coordinates PowerPoint presentation control, session state machine,
    /// ribbon UI state, and outbound protocol messaging.
    /// Strictly adheres to STA thread rules and unsubscribes all events on unload.
    /// </summary>
    public class PresentationSessionCoordinator : IDisposable, ICommandHandler
    {
        public PowerPointInteropController PowerPointController { get; }
        public SessionStateMachine SessionStateMachine { get; }
        public CommandRouter CommandRouter { get; }
        public RibbonController RibbonController { get; }

        public event Action<Envelope>? OutboundMessageReady;

        private bool _isDisposed;

        // Explicit delegate fields for reliable unsubscription (P3-06)
        private readonly Action _onConnectRequested;
        private readonly Action _onDisconnectRequested;
        private readonly Action<SessionState> _onSessionStateChanged;
        private readonly Action _onSessionDisconnectCleanup;
        private readonly Action<Envelope> _onSessionOutboundReady;
        private readonly Action<Envelope> _onCommandReplyReady;
        private readonly Action _onSlideShowStarted;
        private readonly Action _onSlideShowEnded;
        private readonly Action<int, int> _onSlideChanged;

        public PresentationSessionCoordinator(
            PowerPointInteropController? powerPointController = null,
            SessionStateMachine? sessionStateMachine = null,
            RibbonController? ribbonController = null)
        {
            PowerPointController = powerPointController ?? new PowerPointInteropController();
            SessionStateMachine = sessionStateMachine ?? new SessionStateMachine();
            CommandRouter = new CommandRouter(this);
            RibbonController = ribbonController ?? new RibbonController();

            _onConnectRequested = () => SessionStateMachine.StartConnecting();
            _onDisconnectRequested = () => SessionStateMachine.Disconnect();
            _onSessionStateChanged = OnSessionStateChanged;
            _onSessionDisconnectCleanup = OnSessionDisconnectCleanup;
            _onSessionOutboundReady = env => OutboundMessageReady?.Invoke(env);
            _onCommandReplyReady = env => OutboundMessageReady?.Invoke(env);

            _onSlideShowStarted = () =>
            {
                SessionStateMachine.ArmPresentation(true);
                BroadcastStateUpdate();
            };

            _onSlideShowEnded = () =>
            {
                SessionStateMachine.ArmPresentation(false);
                BroadcastStateUpdate();
            };

            _onSlideChanged = (slide, count) =>
            {
                BroadcastStateUpdate();
            };

            // Wire Ribbon actions
            RibbonController.ConnectRequested += _onConnectRequested;
            RibbonController.DisconnectRequested += _onDisconnectRequested;

            // Wire Session state
            SessionStateMachine.StateChanged += _onSessionStateChanged;
            SessionStateMachine.DisconnectCleanupRequired += _onSessionDisconnectCleanup;
            SessionStateMachine.OutboundMessageReady += _onSessionOutboundReady;

            // Wire Command replies (CommandAck, CommandError, SessionPong)
            CommandRouter.SendReplyRequested += _onCommandReplyReady;

            // Wire PowerPoint slide show state
            PowerPointController.SlideShowStarted += _onSlideShowStarted;
            PowerPointController.SlideShowEnded += _onSlideShowEnded;
            PowerPointController.SlideChanged += _onSlideChanged;
        }

        private void OnSessionStateChanged(SessionState newState)
        {
            switch (newState)
            {
                case SessionState.Disconnected:
                    RibbonController.UpdateStatus(RibbonConnectionStatus.Disconnected);
                    break;
                case SessionState.Connecting:
                    RibbonController.UpdateStatus(RibbonConnectionStatus.Connecting);
                    break;
                case SessionState.ConnectedIdle:
                    RibbonController.UpdateStatus(RibbonConnectionStatus.Connected);
                    break;
                case SessionState.PresentingArmed:
                    RibbonController.UpdateStatus(RibbonConnectionStatus.Presenting);
                    break;
            }
        }

        private void OnSessionDisconnectCleanup()
        {
            // Mandatory input safety cleanup
        }

        public void BroadcastStateUpdate()
        {
            if (!SessionStateMachine.Session.HandshakeComplete) return;

            string link = SessionStateMachine.State switch
            {
                SessionState.PresentingArmed => "presenting_armed",
                SessionState.ConnectedIdle => "connected_idle",
                _ => "disconnected"
            };

            string show = PowerPointController.HasActiveSlideShow ? "running" : "none";
            string effect = RibbonController.ActiveEffect;

            var payloadJson = $"{{\"link\":\"{link}\",\"show\":\"{show}\",\"effect\":\"{effect}\",\"slide\":{PowerPointController.CurrentSlideIndex},\"slideCount\":{PowerPointController.TotalSlidesCount}}}";
            var envelope = new Envelope
            {
                Version = 1,
                SessionId = SessionStateMachine.Session.SessionId,
                Sequence = SessionStateMachine.Session.NextOutboundSequence(),
                Type = MessageTypes.StateUpdate,
                Payload = JsonDocument.Parse(payloadJson).RootElement
            };

            OutboundMessageReady?.Invoke(envelope);
        }

        #region ICommandHandler Implementation

        public CommandExecutionResult HandleSlideNext(int seq)
        {
            var result = PowerPointController.Next();
            return result.Success
                ? CommandExecutionResult.Ok()
                : CommandExecutionResult.Fail(result.ErrorCode ?? ErrorCodes.Internal, result.Message ?? "Next failed");
        }

        public CommandExecutionResult HandleSlidePrevious(int seq)
        {
            var result = PowerPointController.Previous();
            return result.Success
                ? CommandExecutionResult.Ok()
                : CommandExecutionResult.Fail(result.ErrorCode ?? ErrorCodes.Internal, result.Message ?? "Previous failed");
        }

        public void HandlePointerDelta(int dx, int dy) { }
        public void HandlePointerButton(string button, string action) { }
        public void HandlePointerScroll(int dy) { }
        public void HandleEffectSet(string effect, double intensity, double scale, bool reducedMotion)
        {
            RibbonController.OnEffectSelected(effect);
        }
        public void HandleDrawStart(double x, double y, string color, int width) { }
        public void HandleDrawPoint(JsonElement points) { }
        public void HandleDrawEnd() { }
        public void HandleStateRequest() => BroadcastStateUpdate();

        #endregion

        public void Dispose()
        {
            if (_isDisposed) return;
            _isDisposed = true;

            // Unsubscribe all events (P3-06)
            RibbonController.ConnectRequested -= _onConnectRequested;
            RibbonController.DisconnectRequested -= _onDisconnectRequested;

            SessionStateMachine.StateChanged -= _onSessionStateChanged;
            SessionStateMachine.DisconnectCleanupRequired -= _onSessionDisconnectCleanup;
            SessionStateMachine.OutboundMessageReady -= _onSessionOutboundReady;

            CommandRouter.SendReplyRequested -= _onCommandReplyReady;

            PowerPointController.SlideShowStarted -= _onSlideShowStarted;
            PowerPointController.SlideShowEnded -= _onSlideShowEnded;
            PowerPointController.SlideChanged -= _onSlideChanged;

            SessionStateMachine.Disconnect();
            PowerPointController.Dispose();
        }
    }
}
