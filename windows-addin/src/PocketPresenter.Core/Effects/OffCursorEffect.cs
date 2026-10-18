using System;

namespace PocketPresenter.Core.Effects
{
    /// <summary>
    /// Represents the 'off' setting.
    /// Draws nothing to the surface and remains permanently idle,
    /// leaving the native Windows system cursor completely unmodified.
    /// </summary>
    public class OffCursorEffect : ICursorEffect
    {
        public EffectId Id => EffectId.Off;
        public bool IsIdle => true;

        public void Update(in EffectInput input, TimeSpan dt) { }

        public void Render(IEffectSurface surface)
        {
            surface.Clear();
        }

        public void Reset() { }
    }
}
