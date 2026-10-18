using System;
using System.Collections.Generic;
using System.Text.Json;
using PocketPresenter.Core.Effects;
using PocketPresenter.Core.Input;
using PocketPresenter.Core.Overlay;
using PocketPresenter.Core.PowerPoint;
using PocketPresenter.Protocol;
using Xunit;

namespace PocketPresenter.Tests
{
    public class MockNativeOverlayWindow : INativeOverlayWindow
    {
        public bool IsCreated { get; private set; }
        public bool IsVisible { get; private set; }
        public IntPtr WindowHandle { get; private set; } = new IntPtr(0x1234);

        public int LastX { get; private set; }
        public int LastY { get; private set; }
        public int LastWidth { get; private set; }
        public int LastHeight { get; private set; }
        public int UpdatePixelCallCount { get; private set; }
        public uint[]? LastPixels { get; private set; }

        public void Create(int x, int y, int width, int height)
        {
            IsCreated = true;
            LastX = x;
            LastY = y;
            LastWidth = width;
            LastHeight = height;
        }

        public void SetPosition(int x, int y)
        {
            LastX = x;
            LastY = y;
        }

        public void SetVisible(bool visible)
        {
            IsVisible = visible;
        }

        public void UpdatePixels(uint[] premultipliedBgraPixels, int width, int height, int screenX, int screenY)
        {
            UpdatePixelCallCount++;
            LastPixels = (uint[])premultipliedBgraPixels.Clone();
            LastX = screenX;
            LastY = screenY;
        }

        public void Destroy()
        {
            IsCreated = false;
            IsVisible = false;
        }

        public void Dispose()
        {
            Destroy();
        }
    }

    public class OverlayLifecycleTests
    {
        [Fact]
        public void BaselineCursorEffect_RendersAndSettlesToIdle()
        {
            var effect = new BaselineCursorEffect();
            var surface = new MemoryEffectSurface(200, 200);

            // Initial state
            Assert.False(effect.IsIdle);

            // Update with movement
            effect.Update(new EffectInput(100, 100, velocity: 15.0, intensity: 1.0, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            Assert.False(effect.IsIdle);

            effect.Render(surface);
            // Verify surface has drawn non-zero pixels
            bool hasDrawnPixels = false;
            foreach (var pixel in surface.Pixels)
            {
                if (pixel != 0)
                {
                    hasDrawnPixels = true;
                    break;
                }
            }
            Assert.True(hasDrawnPixels);

            // Stationary updates > 500ms -> settles to idle
            for (int i = 0; i < 35; i++)
            {
                effect.Update(new EffectInput(100, 100, velocity: 0.0, intensity: 1.0, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            }
            Assert.True(effect.IsIdle);

            // New movement -> wakes up from idle
            effect.Update(new EffectInput(110, 100, velocity: 10.0, intensity: 1.0, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            Assert.False(effect.IsIdle);
        }

        [Fact]
        public void OverlayWindowController_ShowHideAndPositioning_WorkCorrectly()
        {
            var mockNative = new MockNativeOverlayWindow();
            var controller = new OverlayWindowController(mockNative, canvasSize: 200);

            Assert.False(controller.IsVisible);

            controller.Show(100, 100, 1920, 1080);
            Assert.True(controller.IsVisible);
            Assert.True(mockNative.IsCreated);
            Assert.True(mockNative.IsVisible);
            Assert.True(mockNative.UpdatePixelCallCount > 0);

            // Update cursor position: centered at (500, 400) -> window at (400, 300)
            controller.UpdateCursorPosition(500, 400);
            Assert.Equal(500, controller.CurrentCursorX);
            Assert.Equal(400, controller.CurrentCursorY);
            Assert.Equal(400, mockNative.LastX); // 500 - 200/2
            Assert.Equal(300, mockNative.LastY); // 400 - 200/2

            // Hide
            controller.Hide();
            Assert.False(controller.IsVisible);
            Assert.False(mockNative.IsVisible);

            // Clean Dispose
            controller.Dispose();
            Assert.False(mockNative.IsCreated);
        }

        [Fact]
        public void PresentationCoordinator_LinksOverlayLifecycleToPresentationState()
        {
            var mockOverlay = new MockNativeOverlayWindow();
            var overlayController = new OverlayWindowController(mockOverlay, canvasSize: 200);
            var mockNativeInput = new MockNativeInputService { CursorX = 500, CursorY = 500 };
            var pointerController = new PointerController(mockNativeInput, watchdogTimeoutMs: 0)
            {
                Sensitivity = 1.0,
                AccelerationEnabled = false
            };

            var coordinator = new PresentationSessionCoordinator(
                inputController: pointerController,
                overlayController: overlayController);

            // 1. Initially disconnected -> overlay is hidden
            Assert.False(overlayController.IsVisible);

            // 2. Connect session handshake
            string sid = Guid.NewGuid().ToString();
            coordinator.SessionStateMachine.StartConnecting();
            coordinator.SessionStateMachine.HandleInboundEnvelope(new Envelope
            {
                Version = 1,
                Type = MessageTypes.HelloAck,
                Payload = JsonDocument.Parse($"{{\"accepted\":true,\"version\":1,\"sessionId\":\"{sid}\"}}").RootElement
            });

            // ConnectedIdle -> still NO overlay (slideshow not running)
            Assert.Equal(SessionState.ConnectedIdle, coordinator.SessionStateMachine.State);
            Assert.False(overlayController.IsVisible);

            // 3. Attach slide show view -> arms presentation -> overlay shows!
            var mockView = new MockSlideShowView();
            coordinator.PowerPointController.AttachSlideShowView(mockView);

            Assert.Equal(SessionState.PresentingArmed, coordinator.SessionStateMachine.State);
            Assert.True(overlayController.IsVisible);
            Assert.True(mockOverlay.IsVisible);

            // 4. Move cursor -> overlay follows
            var deltaEnv = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 1,
                Type = MessageTypes.PointerDelta,
                Payload = JsonDocument.Parse("{\"dx\":50,\"dy\":-20}").RootElement
            };
            coordinator.CommandRouter.Route(deltaEnv, coordinator.SessionStateMachine.Session);
            Assert.Equal(550, mockNativeInput.CursorX);
            Assert.Equal(480, mockNativeInput.CursorY);
            Assert.Equal(450, mockOverlay.LastX); // 550 - 200/2
            Assert.Equal(380, mockOverlay.LastY); // 480 - 200/2

            // 5. Effect set to 'off' -> overlay hides
            var effectOffEnv = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 2,
                Type = MessageTypes.EffectSet,
                Payload = JsonDocument.Parse("{\"effect\":\"off\",\"intensity\":0.5,\"scale\":1.0,\"reducedMotion\":false}").RootElement
            };
            coordinator.CommandRouter.Route(effectOffEnv, coordinator.SessionStateMachine.Session);
            Assert.False(overlayController.IsVisible);

            // 6. Effect set back to 'honeycomb' -> overlay reappears
            var effectOnEnv = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 3,
                Type = MessageTypes.EffectSet,
                Payload = JsonDocument.Parse("{\"effect\":\"honeycomb\",\"intensity\":0.8,\"scale\":1.0,\"reducedMotion\":false}").RootElement
            };
            coordinator.CommandRouter.Route(effectOnEnv, coordinator.SessionStateMachine.Session);
            Assert.True(overlayController.IsVisible);

            // 7. Slide show ends -> overlay immediately hides!
            coordinator.PowerPointController.DetachSlideShowView();
            Assert.False(overlayController.IsVisible);
            Assert.False(mockOverlay.IsVisible);

            // 8. Re-attach and disconnect -> overlay hides and clean up
            coordinator.PowerPointController.AttachSlideShowView(mockView);
            Assert.True(overlayController.IsVisible);

            coordinator.SessionStateMachine.Disconnect();
            Assert.False(overlayController.IsVisible);
            Assert.False(mockOverlay.IsVisible);

            coordinator.Dispose();
        }
    }
}
