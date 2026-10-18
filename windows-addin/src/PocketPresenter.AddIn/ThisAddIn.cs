using System;
using PocketPresenter.Core.Diagnostics;
using PocketPresenter.Core.PowerPoint;

namespace PocketPresenter.AddIn
{
    /// <summary>
    /// VSTO Add-in entrypoint scaffold.
    /// Office Interop calls must ONLY occur on PowerPoint's STA/UI thread.
    /// </summary>
    public class ThisAddIn : IDisposable
    {
        private bool _isInitialized;

        public void InternalStartup()
        {
            if (_isInitialized) return;
            _isInitialized = true;
        }

        public void InternalShutdown()
        {
            Dispose();
        }

        public void Dispose()
        {
            _isInitialized = false;
        }
    }
}
