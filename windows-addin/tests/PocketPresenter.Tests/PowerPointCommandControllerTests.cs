using System;
using System.Collections.Generic;
using System.Text.Json;
using PocketPresenter.Core.PowerPoint;
using PocketPresenter.Protocol;
using Xunit;

namespace PocketPresenter.Tests
{
    public class MockSlideShowView : ISlideShowView
    {
        public int CurrentSlideIndex { get; private set; } = 1;
        public int TotalSlidesCount { get; } = 10;
        public int NextCallCount { get; private set; }
        public int PreviousCallCount { get; private set; }

        public void Next()
        {
            if (CurrentSlideIndex < TotalSlidesCount)
            {
                CurrentSlideIndex++;
            }
            NextCallCount++;
        }

        public void Previous()
        {
            if (CurrentSlideIndex > 1)
            {
                CurrentSlideIndex--;
            }
            PreviousCallCount++;
        }

        public void GoToSlide(int index)
        {
            if (index >= 1 && index <= TotalSlidesCount)
            {
                CurrentSlideIndex = index;
            }
        }
    }

    public class PowerPointCommandControllerTests
    {
        [Fact]
        public void NoActiveSlideShow_ReturnsNoActiveSlideShow_WithoutThrowing()
        {
            var controller = new PowerPointInteropController();
            Assert.False(controller.HasActiveSlideShow);
            Assert.Equal(0, controller.CurrentSlideIndex);
            Assert.Equal(0, controller.TotalSlidesCount);

            var nextRes = controller.Next();
            Assert.False(nextRes.Success);
            Assert.Equal(ErrorCodes.NoActiveSlideShow, nextRes.ErrorCode);

            var prevRes = controller.Previous();
            Assert.False(prevRes.Success);
            Assert.Equal(ErrorCodes.NoActiveSlideShow, prevRes.ErrorCode);

            var gotoRes = controller.GoToSlide(1);
            Assert.False(gotoRes.Success);
            Assert.Equal(ErrorCodes.NoActiveSlideShow, gotoRes.ErrorCode);
        }

        [Fact]
        public void AttachedSlideShow_NextAndPrevious_AdvanceProperly()
        {
            var controller = new PowerPointInteropController();
            var mockView = new MockSlideShowView();

            bool startedFired = false;
            int lastReportedSlide = 0;
            int lastReportedTotal = 0;

            controller.SlideShowStarted += () => startedFired = true;
            controller.SlideChanged += (curr, tot) =>
            {
                lastReportedSlide = curr;
                lastReportedTotal = tot;
            };

            controller.AttachSlideShowView(mockView);

            Assert.True(controller.HasActiveSlideShow);
            Assert.True(startedFired);
            Assert.Equal(1, controller.CurrentSlideIndex);
            Assert.Equal(10, controller.TotalSlidesCount);
            Assert.Equal(1, lastReportedSlide);
            Assert.Equal(10, lastReportedTotal);

            // Test Next
            var nextRes = controller.Next();
            Assert.True(nextRes.Success);
            Assert.Equal(2, controller.CurrentSlideIndex);
            Assert.Equal(2, lastReportedSlide);

            // Test Previous
            var prevRes = controller.Previous();
            Assert.True(prevRes.Success);
            Assert.Equal(1, controller.CurrentSlideIndex);
            Assert.Equal(1, lastReportedSlide);

            // Test GoToSlide valid
            var gotoRes = controller.GoToSlide(5);
            Assert.True(gotoRes.Success);
            Assert.Equal(5, controller.CurrentSlideIndex);

            // Test GoToSlide invalid bounds
            var badGoto = controller.GoToSlide(99);
            Assert.False(badGoto.Success);
            Assert.Equal(ErrorCodes.BadField, badGoto.ErrorCode);
        }

        [Fact]
        public void DetachSlideShow_FiresSlideShowEnded_AndRevertsState()
        {
            var controller = new PowerPointInteropController();
            var mockView = new MockSlideShowView();
            controller.AttachSlideShowView(mockView);

            bool endedFired = false;
            controller.SlideShowEnded += () => endedFired = true;

            controller.DetachSlideShowView();

            Assert.True(endedFired);
            Assert.False(controller.HasActiveSlideShow);
            Assert.Equal(0, controller.CurrentSlideIndex);

            var nextRes = controller.Next();
            Assert.False(nextRes.Success);
            Assert.Equal(ErrorCodes.NoActiveSlideShow, nextRes.ErrorCode);
        }

        [Fact]
        public void RibbonController_ToggleAndEffects_WorkCorrectly()
        {
            var ribbon = new RibbonController();
            Assert.Equal(RibbonConnectionStatus.Disconnected, ribbon.Status);
            Assert.Equal("Phone: Disconnected", ribbon.GetStatusLabel());

            bool connectRequested = false;
            bool disconnectRequested = false;
            bool invalidateRequested = false;
            string? changedEffect = null;

            ribbon.ConnectRequested += () => connectRequested = true;
            ribbon.DisconnectRequested += () => disconnectRequested = true;
            ribbon.RibbonInvalidateRequested += () => invalidateRequested = true;
            ribbon.EffectChangeRequested += eff => changedEffect = eff;

            ribbon.OnConnectToggleClicked();
            Assert.True(connectRequested);

            ribbon.UpdateStatus(RibbonConnectionStatus.Connected);
            Assert.Equal(RibbonConnectionStatus.Connected, ribbon.Status);
            Assert.Equal("Phone: Connected (Idle)", ribbon.GetStatusLabel());
            Assert.True(invalidateRequested);

            ribbon.OnConnectToggleClicked();
            Assert.True(disconnectRequested);

            ribbon.OnEffectSelected("diamond");
            Assert.Equal("diamond", ribbon.ActiveEffect);
            Assert.Equal("diamond", changedEffect);

            var xml = RibbonController.GetCustomRibbonXml();
            Assert.Contains("tabPocketPresenter", xml);
            Assert.Contains("btnConnect", xml);
            Assert.Contains("effHoneycomb", xml);
        }

        [Fact]
        public void PresentationSessionCoordinator_DispatchesAcksAndErrors()
        {
            var coordinator = new PresentationSessionCoordinator();
            var outboundMessages = new List<Envelope>();
            coordinator.OutboundMessageReady += env => outboundMessages.Add(env);

            // Connect session handshake first
            string sid = Guid.NewGuid().ToString();
            coordinator.SessionStateMachine.StartConnecting();
            Assert.Equal(SessionState.Connecting, coordinator.SessionStateMachine.State);

            coordinator.SessionStateMachine.HandleInboundEnvelope(new Envelope
            {
                Version = 1,
                Type = MessageTypes.HelloAck,
                Payload = JsonDocument.Parse($"{{\"accepted\":true,\"version\":1,\"sessionId\":\"{sid}\"}}").RootElement
            });

            Assert.Equal(SessionState.ConnectedIdle, coordinator.SessionStateMachine.State);

            // 1. Dispatch slide.next when NO slide show is active -> expects command.error with NO_ACTIVE_SLIDESHOW
            var nextEnvelope = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 1,
                Type = MessageTypes.SlideNext,
                Payload = JsonDocument.Parse("{}").RootElement
            };

            coordinator.CommandRouter.Route(nextEnvelope, coordinator.SessionStateMachine.Session);

            var errorMsg = outboundMessages.Find(m => m.Type == MessageTypes.CommandError);
            Assert.NotNull(errorMsg);
            Assert.True(errorMsg.Payload.TryGetProperty("ref", out var refProp));
            Assert.Equal(1, refProp.GetInt32());
            Assert.True(errorMsg.Payload.TryGetProperty("code", out var codeProp));
            Assert.Equal(ErrorCodes.NoActiveSlideShow, codeProp.GetString());

            // 2. Attach slide show view -> arm presentation
            var mockView = new MockSlideShowView();
            coordinator.PowerPointController.AttachSlideShowView(mockView);

            Assert.Equal(SessionState.PresentingArmed, coordinator.SessionStateMachine.State);
            Assert.Equal(RibbonConnectionStatus.Presenting, coordinator.RibbonController.Status);

            var stateUpdate = outboundMessages.Find(m => m.Type == MessageTypes.StateUpdate);
            Assert.NotNull(stateUpdate);
            Assert.Equal("running", stateUpdate.Payload.GetProperty("show").GetString());

            // 3. Dispatch slide.next when slide show IS active -> expects command.ack
            var nextActiveEnv = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 2,
                Type = MessageTypes.SlideNext,
                Payload = JsonDocument.Parse("{}").RootElement
            };

            coordinator.CommandRouter.Route(nextActiveEnv, coordinator.SessionStateMachine.Session);

            var ackMsg = outboundMessages.Find(m => m.Type == MessageTypes.CommandAck && m.Payload.GetProperty("ref").GetInt32() == 2);
            Assert.NotNull(ackMsg);
            Assert.True(ackMsg.Payload.GetProperty("ok").GetBoolean());
            Assert.Equal(2, coordinator.PowerPointController.CurrentSlideIndex);

            // 4. Clean Dispose unhooks all events
            coordinator.Dispose();
            Assert.Equal(SessionState.Disconnected, coordinator.SessionStateMachine.State);
            Assert.False(coordinator.PowerPointController.HasActiveSlideShow);
        }

        [Fact]
        public void PresentationSessionCoordinator_CleanUnloadRemovesAllSubscriptions()
        {
            var pptController = new PowerPointInteropController();
            var sessionMachine = new SessionStateMachine();
            var ribbon = new RibbonController();

            var coordinator = new PresentationSessionCoordinator(pptController, sessionMachine, ribbon);
            int outboundCount = 0;
            coordinator.OutboundMessageReady += _ => outboundCount++;

            coordinator.Dispose();

            // After dispose, firing events on pptController or ribbon should not cause coordinator crashes or callbacks
            var mockView = new MockSlideShowView();
            pptController.AttachSlideShowView(mockView);
            ribbon.OnConnectToggleClicked();

            Assert.Equal(0, outboundCount);
        }
    }
}
