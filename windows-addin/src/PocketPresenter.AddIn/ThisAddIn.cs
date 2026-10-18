using System;
using PocketPresenter.Core.PowerPoint;

namespace PocketPresenter.AddIn
{
    /// <summary>
    /// VSTO Add-in lifecycle controller and entrypoint wrapper.
    /// Strictly routes Office COM interactions to PowerPoint's STA/UI thread.
    /// Unsubscribes all event handlers and disposes controllers on shutdown.
    /// </summary>
    public class ThisAddIn : PresentationSessionCoordinator
    {
        private bool _isStarted;

        public void InternalStartup()
        {
            if (_isStarted) return;
            _isStarted = true;
        }

        public void InternalShutdown()
        {
            Dispose();
        }
    }
}
