using System;
using System.Collections.Generic;
using System.Text.Json;
using System.Threading;
using PocketPresenter.Core.Input;
using PocketPresenter.Core.PowerPoint;
using PocketPresenter.Protocol;
using Xunit;

namespace PocketPresenter.Tests
{
    public class MockNativeInputService : INativeInputService
    {
        public int CursorX { get; set; } = 500;
        public int CursorY { get; set; } = 500;
        public List<(int X, int Y)> PositionHistory { get; } = new List<(int X, int Y)>();
        public List<(MouseButton Button, bool Down)> ButtonEvents { get; } = new List<(MouseButton Button, bool Down)>();
        public List<int> ScrollEvents { get; } = new List<int>();

        public VirtualScreenBounds Bounds { get; set; } = new VirtualScreenBounds(0, 0, 1920, 1080);

        public (int X, int Y) GetCursorPosition() => (CursorX, CursorY);

        public void SetCursorPosition(int x, int y)
        {
            CursorX = x;
            CursorY = y;
            PositionHistory.Add((x, y));
        }

        public void SendMouseButton(MouseButton button, bool down)
        {
            ButtonEvents.Add((button, down));
        }

        public void SendMouseScroll(int dy)
        {
            ScrollEvents.Add(dy);
        }

        public VirtualScreenBounds GetVirtualScreenBounds() => Bounds;
    }

    public class PointerControllerTests
    {
        [Fact]
        public void MoveBy_AppliesSensitivityAndClampsToVirtualScreen()
        {
            var mock = new MockNativeInputService
            {
                CursorX = 100,
                CursorY = 100,
                Bounds = new VirtualScreenBounds(0, 0, 1000, 1000)
            };

            var controller = new PointerController(mock, watchdogTimeoutMs: 0)
            {
                Sensitivity = 2.0,
                AccelerationEnabled = false
            };

            controller.MoveBy(10, 20);
            Assert.Equal(120, mock.CursorX); // 100 + 10*2
            Assert.Equal(140, mock.CursorY); // 100 + 20*2

            // Move way past bounds
            controller.MoveBy(1000, 1000);
            Assert.Equal(999, mock.CursorX); // clamped to Right - 1
            Assert.Equal(999, mock.CursorY); // clamped to Bottom - 1

            // Move negative past bounds
            controller.MoveBy(-2000, -2000);
            Assert.Equal(0, mock.CursorX);
            Assert.Equal(0, mock.CursorY);
        }

        [Fact]
        public void ButtonDownAndUp_TracksHeldButtons()
        {
            var mock = new MockNativeInputService();
            var controller = new PointerController(mock, watchdogTimeoutMs: 0);

            Assert.Equal(0, controller.HeldButtonsCount);
            Assert.False(controller.IsButtonHeld(MouseButton.Left));

            controller.ButtonDown(MouseButton.Left);
            Assert.Equal(1, controller.HeldButtonsCount);
            Assert.True(controller.IsButtonHeld(MouseButton.Left));
            Assert.Contains((MouseButton.Left, true), mock.ButtonEvents);

            controller.ButtonUp(MouseButton.Left);
            Assert.Equal(0, controller.HeldButtonsCount);
            Assert.False(controller.IsButtonHeld(MouseButton.Left));
            Assert.Contains((MouseButton.Left, false), mock.ButtonEvents);
        }

        [Fact]
        public void ReleaseAll_ReleasesEveryHeldButton()
        {
            var mock = new MockNativeInputService();
            var controller = new PointerController(mock, watchdogTimeoutMs: 0);

            controller.ButtonDown(MouseButton.Left);
            controller.ButtonDown(MouseButton.Right);
            Assert.Equal(2, controller.HeldButtonsCount);

            controller.ReleaseAll();
            Assert.Equal(0, controller.HeldButtonsCount);
            Assert.False(controller.IsButtonHeld(MouseButton.Left));
            Assert.False(controller.IsButtonHeld(MouseButton.Right));

            // Verify both up events sent
            Assert.Contains((MouseButton.Left, false), mock.ButtonEvents);
            Assert.Contains((MouseButton.Right, false), mock.ButtonEvents);
        }

        [Fact]
        public void Watchdog_ReleasesHeldButtonsAfterSilence()
        {
            var mock = new MockNativeInputService();
            var watchdogTriggered = new ManualResetEventSlim(false);

            // Fast watchdog for testing (100 ms)
            var controller = new PointerController(mock, watchdogTimeoutMs: 100);
            controller.WatchdogTimedOut += () => watchdogTriggered.Set();

            controller.ButtonDown(MouseButton.Left);
            Assert.Equal(1, controller.HeldButtonsCount);

            // Wait for watchdog to trigger
            bool triggered = watchdogTriggered.Wait(TimeSpan.FromMilliseconds(1000));
            Assert.True(triggered);

            Assert.Equal(0, controller.HeldButtonsCount);
            Assert.Contains((MouseButton.Left, false), mock.ButtonEvents);

            controller.Dispose();
        }

        [Fact]
        public void PresentationCoordinator_IntegratesPointerAndDisconnectSafety()
        {
            var mockNative = new MockNativeInputService { CursorX = 500, CursorY = 500 };
            var pointerController = new PointerController(mockNative, watchdogTimeoutMs: 0)
            {
                Sensitivity = 1.0,
                AccelerationEnabled = false
            };

            var coordinator = new PresentationSessionCoordinator(inputController: pointerController);

            // Start connection
            string sid = Guid.NewGuid().ToString();
            coordinator.SessionStateMachine.StartConnecting();
            coordinator.SessionStateMachine.HandleInboundEnvelope(new Envelope
            {
                Version = 1,
                Type = MessageTypes.HelloAck,
                Payload = JsonDocument.Parse($"{{\"accepted\":true,\"version\":1,\"sessionId\":\"{sid}\"}}").RootElement
            });

            // 1. Route pointer.delta
            var deltaEnv = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 1,
                Type = MessageTypes.PointerDelta,
                Payload = JsonDocument.Parse("{\"dx\":15,\"dy\":-25}").RootElement
            };
            coordinator.CommandRouter.Route(deltaEnv, coordinator.SessionStateMachine.Session);
            Assert.Equal(515, mockNative.CursorX);
            Assert.Equal(475, mockNative.CursorY);

            // 2. Route pointer.button down (drag start)
            var btnDownEnv = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 2,
                Type = MessageTypes.PointerButton,
                Payload = JsonDocument.Parse("{\"button\":\"left\",\"action\":\"down\"}").RootElement
            };
            coordinator.CommandRouter.Route(btnDownEnv, coordinator.SessionStateMachine.Session);
            Assert.True(pointerController.IsButtonHeld(MouseButton.Left));

            // 3. Route pointer.scroll
            var scrollEnv = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 3,
                Type = MessageTypes.PointerScroll,
                Payload = JsonDocument.Parse("{\"dy\":3}").RootElement
            };
            coordinator.CommandRouter.Route(scrollEnv, coordinator.SessionStateMachine.Session);
            Assert.Contains(3, mockNative.ScrollEvents);

            // 4. Injected disconnect mid-drag -> MUST release held button
            coordinator.SessionStateMachine.Disconnect();
            Assert.Equal(0, pointerController.HeldButtonsCount);
            Assert.False(pointerController.IsButtonHeld(MouseButton.Left));
            Assert.Contains((MouseButton.Left, false), mockNative.ButtonEvents);

            coordinator.Dispose();
        }
    }
}
