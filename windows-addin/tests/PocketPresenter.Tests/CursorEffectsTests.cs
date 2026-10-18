using System;
using System.Text.Json;
using PocketPresenter.Core.Effects;
using PocketPresenter.Core.Input;
using PocketPresenter.Core.Overlay;
using PocketPresenter.Core.PowerPoint;
using PocketPresenter.Protocol;
using Xunit;

namespace PocketPresenter.Tests
{
    public class CursorEffectsTests
    {
        [Fact]
        public void HoneycombPulseEffect_RendersHexLatticeAndSettles()
        {
            var effect = new HoneycombPulseEffect();
            var surface = new MemoryEffectSurface(200, 200);

            Assert.Equal(EffectId.Honeycomb, effect.Id);
            Assert.False(effect.IsIdle);

            // Active motion
            effect.Update(new EffectInput(100, 100, velocity: 20.0, intensity: 0.8, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            Assert.False(effect.IsIdle);

            effect.Render(surface);
            bool hasPixels = false;
            foreach (var px in surface.Pixels)
            {
                if (px != 0) { hasPixels = true; break; }
            }
            Assert.True(hasPixels);

            // Stop moving -> settles after 300ms
            for (int i = 0; i < 25; i++)
            {
                effect.Update(new EffectInput(100, 100, velocity: 0.0, intensity: 0.8, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            }
            Assert.True(effect.IsIdle);
        }

        [Fact]
        public void DiamondFacetsEffect_RendersDiamondLatticeAndSettles()
        {
            var effect = new DiamondFacetsEffect();
            var surface = new MemoryEffectSurface(200, 200);

            Assert.Equal(EffectId.Diamond, effect.Id);
            Assert.False(effect.IsIdle);

            effect.Update(new EffectInput(100, 100, velocity: 15.0, intensity: 0.8, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            Assert.False(effect.IsIdle);

            effect.Render(surface);
            bool hasPixels = false;
            foreach (var px in surface.Pixels)
            {
                if (px != 0) { hasPixels = true; break; }
            }
            Assert.True(hasPixels);

            for (int i = 0; i < 25; i++)
            {
                effect.Update(new EffectInput(100, 100, velocity: 0.0, intensity: 0.8, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            }
            Assert.True(effect.IsIdle);
        }

        [Fact]
        public void GeometricRippleEffect_RendersConcentricContoursAndSettles()
        {
            var effect = new GeometricRippleEffect();
            var surface = new MemoryEffectSurface(200, 200);

            Assert.Equal(EffectId.Ripple, effect.Id);
            Assert.False(effect.IsIdle);

            effect.Update(new EffectInput(100, 100, velocity: 10.0, intensity: 0.8, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            Assert.False(effect.IsIdle);

            effect.Render(surface);
            bool hasPixels = false;
            foreach (var px in surface.Pixels)
            {
                if (px != 0) { hasPixels = true; break; }
            }
            Assert.True(hasPixels);

            for (int i = 0; i < 30; i++)
            {
                effect.Update(new EffectInput(100, 100, velocity: 0.0, intensity: 0.8, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            }
            Assert.True(effect.IsIdle);
        }

        [Fact]
        public void OffCursorEffect_RemainsIdleAndDrawsNothing()
        {
            var effect = new OffCursorEffect();
            var surface = new MemoryEffectSurface(200, 200);

            Assert.Equal(EffectId.Off, effect.Id);
            Assert.True(effect.IsIdle);

            effect.Update(new EffectInput(100, 100, velocity: 50.0, intensity: 1.0, scale: 1.0, reducedMotion: false), TimeSpan.FromMilliseconds(16));
            Assert.True(effect.IsIdle);

            effect.Render(surface);
            foreach (var px in surface.Pixels)
            {
                Assert.Equal(0u, px);
            }
        }

        [Fact]
        public void EffectFactory_InstantiatesAllKnownEffects()
        {
            Assert.IsType<HoneycombPulseEffect>(EffectFactory.Create("honeycomb"));
            Assert.IsType<DiamondFacetsEffect>(EffectFactory.Create("diamond"));
            Assert.IsType<GeometricRippleEffect>(EffectFactory.Create("ripple"));
            Assert.IsType<OffCursorEffect>(EffectFactory.Create("off"));
            Assert.IsType<HoneycombPulseEffect>(EffectFactory.Create("unknown"));
        }

        [Fact]
        public void LiveEffectSwitching_PreservesCursorPositionAndPresentationState()
        {
            var mockOverlay = new MockNativeOverlayWindow();
            var overlayController = new OverlayWindowController(mockOverlay, canvasSize: 200);
            var mockNativeInput = new MockNativeInputService { CursorX = 400, CursorY = 300 };
            var pointerController = new PointerController(mockNativeInput, watchdogTimeoutMs: 0)
            {
                Sensitivity = 1.0,
                AccelerationEnabled = false
            };

            var coordinator = new PresentationSessionCoordinator(
                inputController: pointerController,
                overlayController: overlayController);

            // Connect & start slideshow
            string sid = Guid.NewGuid().ToString();
            coordinator.SessionStateMachine.StartConnecting();
            coordinator.SessionStateMachine.HandleInboundEnvelope(new Envelope
            {
                Version = 1,
                Type = MessageTypes.HelloAck,
                Payload = JsonDocument.Parse($"{{\"accepted\":true,\"version\":1,\"sessionId\":\"{sid}\"}}").RootElement
            });

            var mockView = new MockSlideShowView();
            coordinator.PowerPointController.AttachSlideShowView(mockView);
            Assert.Equal(SessionState.PresentingArmed, coordinator.SessionStateMachine.State);
            Assert.True(overlayController.IsVisible);

            // Switch to Diamond
            var effDiamond = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 1,
                Type = MessageTypes.EffectSet,
                Payload = JsonDocument.Parse("{\"effect\":\"diamond\",\"intensity\":0.7,\"scale\":1.2,\"reducedMotion\":false}").RootElement
            };
            coordinator.CommandRouter.Route(effDiamond, coordinator.SessionStateMachine.Session);
            Assert.Equal("diamond", coordinator.RibbonController.ActiveEffect);
            Assert.True(overlayController.IsVisible);
            Assert.Equal(400, mockNativeInput.CursorX); // Cursor position unchanged
            Assert.Equal(300, mockNativeInput.CursorY);

            // Switch to Ripple
            var effRipple = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 2,
                Type = MessageTypes.EffectSet,
                Payload = JsonDocument.Parse("{\"effect\":\"ripple\",\"intensity\":0.5,\"scale\":1.0,\"reducedMotion\":true}").RootElement
            };
            coordinator.CommandRouter.Route(effRipple, coordinator.SessionStateMachine.Session);
            Assert.Equal("ripple", coordinator.RibbonController.ActiveEffect);
            Assert.True(overlayController.IsVisible);
            Assert.Equal(400, mockNativeInput.CursorX);

            // Switch to Off -> hides overlay
            var effOff = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 3,
                Type = MessageTypes.EffectSet,
                Payload = JsonDocument.Parse("{\"effect\":\"off\",\"intensity\":0.0,\"scale\":1.0,\"reducedMotion\":false}").RootElement
            };
            coordinator.CommandRouter.Route(effOff, coordinator.SessionStateMachine.Session);
            Assert.Equal("off", coordinator.RibbonController.ActiveEffect);
            Assert.False(overlayController.IsVisible);
            Assert.Equal(400, mockNativeInput.CursorX);

            // Switch back to Honeycomb -> overlay shows again
            var effHoneycomb = new Envelope
            {
                Version = 1,
                SessionId = sid,
                Sequence = 4,
                Type = MessageTypes.EffectSet,
                Payload = JsonDocument.Parse("{\"effect\":\"honeycomb\",\"intensity\":0.8,\"scale\":1.0,\"reducedMotion\":false}").RootElement
            };
            coordinator.CommandRouter.Route(effHoneycomb, coordinator.SessionStateMachine.Session);
            Assert.Equal("honeycomb", coordinator.RibbonController.ActiveEffect);
            Assert.True(overlayController.IsVisible);
            Assert.Equal(400, mockNativeInput.CursorX);

            coordinator.Dispose();
        }
    }
}
