using System;

namespace PocketPresenter.AddIn
{
    /// <summary>
    /// PowerPoint Ribbon UI controller scaffold for the 'PocketPresenter' tab/group.
    /// </summary>
    public class RibbonController
    {
        public bool IsPhoneConnected { get; private set; }

        public event Action? ConnectRequested;
        public event Action? DisconnectRequested;

        public void OnConnectClicked()
        {
            ConnectRequested?.Invoke();
        }

        public void OnDisconnectClicked()
        {
            DisconnectRequested?.Invoke();
        }

        public void UpdateConnectionStatus(bool connected)
        {
            IsPhoneConnected = connected;
        }
    }
}
