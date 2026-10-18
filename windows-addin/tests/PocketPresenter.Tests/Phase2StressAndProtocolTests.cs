using System;
using System.Collections.Generic;
using System.Text.Json;
using PocketPresenter.Core.Input;
using PocketPresenter.Protocol;
using Xunit;

namespace PocketPresenter.Tests
{
    public class MockCommandHandler : ICommandHandler
    {
        public int NextCount { get; private set; }
        public int PreviousCount { get; private set; }
        public int PointerDeltaCount { get; private set; }
        public int TotalDx { get; private set; }
        public int TotalDy { get; private set; }
        public string? LastButton { get; private set; }
        public string? LastAction { get; private set; }
        public string? LastEffect { get; private set; }

        public CommandExecutionResult HandleSlideNext(int seq)
        {
            NextCount++;
            return CommandExecutionResult.Ok();
        }

        public CommandExecutionResult HandleSlidePrevious(int seq)
        {
            PreviousCount++;
            return CommandExecutionResult.Ok();
        }
        public void HandlePointerDelta(int dx, int dy)
        {
            PointerDeltaCount++;
            TotalDx += dx;
            TotalDy += dy;
        }
        public void HandlePointerButton(string button, string action)
        {
            LastButton = button;
            LastAction = action;
        }
        public void HandlePointerScroll(int dy) { }
        public void HandleEffectSet(string effect, double intensity, double scale, bool reducedMotion)
        {
            LastEffect = effect;
        }
        public void HandleDrawStart(double x, double y, string color, int width) { }
        public void HandleDrawPoint(JsonElement points) { }
        public void HandleDrawEnd() { }
        public void HandleStateRequest() { }
    }

    public class MockInputController : IInputController
    {
        public int ReleaseAllCount { get; private set; }
        public bool IsButtonHeld { get; private set; }

        public void MoveBy(int dx, int dy) { }
        public void SetAbsolute(int x, int y) { }
        public void ButtonDown(MouseButton button) => IsButtonHeld = true;
        public void ButtonUp(MouseButton button) => IsButtonHeld = false;
        public void Scroll(int dy) { }
        public void ReleaseAll()
        {
            ReleaseAllCount++;
            IsButtonHeld = false;
        }
    }

    public class Phase2StressAndProtocolTests
    {
        [Fact]
        public void SessionStateMachine_CompletesHandshake_AndCleansUpOnDisconnect()
        {
            var sm = new SessionStateMachine();
            bool cleanupCalled = false;
            sm.DisconnectCleanupRequired += () => cleanupCalled = true;

            Envelope? sentHello = null;
            sm.OutboundMessageReady += env => sentHello = env;

            // 1. Start connecting
            sm.StartConnecting();
            Assert.Equal(SessionState.Connecting, sm.State);
            Assert.NotNull(sentHello);
            Assert.Equal(MessageTypes.Hello, sentHello!.Type);

            // 2. Receive hello.ack with valid sessionId
            string testSid = Guid.NewGuid().ToString();
            var ackPayload = JsonDocument.Parse($"{{\"accepted\":true,\"version\":1,\"sessionId\":\"{testSid}\"}}").RootElement;
            sm.HandleInboundEnvelope(new Envelope
            {
                Type = MessageTypes.HelloAck,
                Payload = ackPayload
            });

            Assert.Equal(SessionState.ConnectedIdle, sm.State);
            Assert.True(sm.Session.HandshakeComplete);
            Assert.Equal(testSid, sm.Session.SessionId);

            // 3. Disconnect triggers mandatory cleanup
            sm.Disconnect();
            Assert.Equal(SessionState.Disconnected, sm.State);
            Assert.True(cleanupCalled);
            Assert.False(sm.Session.HandshakeComplete);
            Assert.Null(sm.Session.SessionId);
        }

        [Fact]
        public void HeartbeatManager_EnforcesDeadLink_AndHeldButtonWatchdog()
        {
            var hb = new HeartbeatManager();
            bool linkDead = false;
            bool watchdogTriggered = false;

            hb.LinkDeadDetected += () => linkDead = true;
            hb.HeldButtonWatchdogTriggered += () => watchdogTriggered = true;

            long t0 = 1000;
            hb.Reset(t0);

            // Advance by 2,000 ms with button down -> watchdog should NOT trigger yet
            hb.IsButtonHeld = true;
            hb.Tick(t0 + 2000);
            Assert.False(watchdogTriggered);
            Assert.False(linkDead);

            // Advance to 3,500 ms (silence > 3,000 ms while button held) -> watchdog MUST trigger (ADR-0013)
            hb.Tick(t0 + 3500);
            Assert.True(watchdogTriggered);
            Assert.False(linkDead);

            // Advance to 16,000 ms (silence > 15,000 ms) -> link dead MUST trigger
            hb.Tick(t0 + 16000);
            Assert.True(linkDead);
        }

        [Fact]
        public void TimeoutBackoff_CalculatesExponentialDelaysWithBound()
        {
            var backoff = new TimeoutBackoff(initialDelayMs: 1000, maxDelayMs: 8000, multiplier: 2.0);

            int d1 = backoff.NextDelay();
            Assert.InRange(d1, 800, 1200);

            int d2 = backoff.NextDelay();
            Assert.InRange(d2, 1700, 2300);

            int d3 = backoff.NextDelay();
            Assert.InRange(d3, 3500, 4500);

            int d4 = backoff.NextDelay();
            // Should be bounded near max 8,000 ms with 10% jitter
            Assert.InRange(d4, 7000, 9000);

            backoff.Reset();
            Assert.Equal(0, backoff.Attempt);
        }

        [Fact]
        public void CommandRouter_RejectsUnsafeCommands_AndPreventsDuplicates()
        {
            var handler = new MockCommandHandler();
            var router = new CommandRouter(handler);

            var session = new ProtocolSession();
            string sid = Guid.NewGuid().ToString();
            session.Reset(sid);

            // 1. Valid slide.next
            var env1 = new Envelope { Version = 1, SessionId = sid, Sequence = 1, Type = MessageTypes.SlideNext };
            var res1 = router.Route(env1, session);
            Assert.True(res1.IsOk);
            Assert.Equal(1, handler.NextCount);

            // 2. Duplicate slide.next with same seq is rejected
            var env2 = new Envelope { Version = 1, SessionId = sid, Sequence = 1, Type = MessageTypes.SlideNext };
            var res2 = router.Route(env2, session);
            Assert.False(res2.IsOk);
            Assert.Equal(ErrorCodes.Duplicate, res2.ErrorCode);
            Assert.Equal(1, handler.NextCount); // Handler not invoked second time

            // 3. Unsafe command type rejected
            var envUnsafe = new Envelope { Version = 1, SessionId = sid, Sequence = 2, Type = "system.execute" };
            var resUnsafe = router.Route(envUnsafe, session);
            Assert.False(resUnsafe.IsOk);
            Assert.Equal(ErrorCodes.BadType, resUnsafe.ErrorCode);
        }

        [Fact]
        public void StressTest_1000ScriptedMessages_FragmentedReads_AndInjectedDisconnects()
        {
            var handler = new MockCommandHandler();
            var router = new CommandRouter(handler);
            var decoder = new FrameDecoder();
            var session = new ProtocolSession();

            string sid1 = Guid.NewGuid().ToString();
            string sid2 = Guid.NewGuid().ToString();

            int totalExpectedDeltas = 0;
            int totalExpectedNext = 0;
            var random = new Random(42);

            var streamBuilder = new System.Text.StringBuilder();

            for (int i = 0; i < 1000; i++)
            {
                string activeSid = i < 500 ? sid1 : sid2;
                int seq = i < 500 ? (i + 1) : (i - 500 + 1);

                Envelope msg;
                if (i % 3 == 0)
                {
                    msg = new Envelope
                    {
                        Version = 1,
                        SessionId = activeSid,
                        Sequence = seq,
                        Type = MessageTypes.PointerDelta,
                        Payload = JsonDocument.Parse("{\"dx\":2,\"dy\":3}").RootElement
                    };
                    totalExpectedDeltas++;
                }
                else
                {
                    msg = new Envelope
                    {
                        Version = 1,
                        SessionId = activeSid,
                        Sequence = seq,
                        Type = MessageTypes.SlideNext
                    };
                    totalExpectedNext++;
                }

                streamBuilder.Append(FrameEncoder.EncodeToString(msg));
            }

            var allStreamData = streamBuilder.ToString();
            int processedBytes = 0;
            int decodedMessageCount = 0;

            // Start with sid1
            session.Reset(sid1);

            // Fragment stream randomly between 1 and 35 bytes per read
            while (processedBytes < allStreamData.Length)
            {
                int chunkSize = Math.Min(random.Next(1, 35), allStreamData.Length - processedBytes);
                string chunk = allStreamData.Substring(processedBytes, chunkSize);
                processedBytes += chunkSize;

                var decoded = decoder.Feed(chunk);
                foreach (var dr in decoded)
                {
                    Assert.True(dr.Success, $"Decode error: {dr.ErrorMessage}");
                    decodedMessageCount++;

                    if (decodedMessageCount == 501)
                    {
                        // Simulate reconnect at message 501 with sid2
                        session.Reset(sid2);
                    }

                    var routeRes = router.Route(dr.Envelope!, session);
                    Assert.True(routeRes.IsOk, $"Route error on msg {decodedMessageCount}: {routeRes.ErrorCode}");
                }
            }

            // Verify all 1,000 messages processed without loss or corruption
            Assert.Equal(1000, decodedMessageCount);
            Assert.Equal(totalExpectedDeltas, handler.PointerDeltaCount);
            Assert.Equal(totalExpectedNext, handler.NextCount);
            Assert.Equal(totalExpectedDeltas * 2, handler.TotalDx);
            Assert.Equal(totalExpectedDeltas * 3, handler.TotalDy);
        }
    }
}
