using System;

namespace PocketPresenter.Core.PowerPoint
{
    public enum RibbonConnectionStatus
    {
        Disconnected,
        Connecting,
        Connected,
        Presenting
    }

    /// <summary>
    /// PowerPoint Ribbon UI controller managing the 'PocketPresenter' tab/group controls,
    /// connection toggle, and effect selector callbacks.
    /// </summary>
    public class RibbonController
    {
        public RibbonConnectionStatus Status { get; private set; } = RibbonConnectionStatus.Disconnected;
        public string ActiveEffect { get; private set; } = "honeycomb";

        public event Action? ConnectRequested;
        public event Action? DisconnectRequested;
        public event Action<string>? EffectChangeRequested;
        public event Action? RibbonInvalidateRequested;

        public void OnConnectToggleClicked()
        {
            if (Status == RibbonConnectionStatus.Disconnected)
            {
                ConnectRequested?.Invoke();
            }
            else
            {
                DisconnectRequested?.Invoke();
            }
        }

        public void OnEffectSelected(string effectId)
        {
            ActiveEffect = effectId;
            EffectChangeRequested?.Invoke(effectId);
            RibbonInvalidateRequested?.Invoke();
        }

        public void UpdateStatus(RibbonConnectionStatus newStatus)
        {
            if (Status != newStatus)
            {
                Status = newStatus;
                RibbonInvalidateRequested?.Invoke();
            }
        }

        public string GetStatusLabel()
        {
            switch (Status)
            {
                case RibbonConnectionStatus.Disconnected:
                    return "Phone: Disconnected";
                case RibbonConnectionStatus.Connecting:
                    return "Phone: Connecting...";
                case RibbonConnectionStatus.Connected:
                    return "Phone: Connected (Idle)";
                case RibbonConnectionStatus.Presenting:
                    return "Phone: Presenting Active";
                default:
                    return "Phone: Unknown";
            }
        }

        public static string GetCustomRibbonXml()
        {
            return @"<?xml version=""1.0"" encoding=""UTF-8""?>
<customUI xmlns=""http://schemas.microsoft.com/office/2009/07/customui"" onLoad=""OnRibbonLoad"">
  <ribbon>
    <tabs>
      <tab id=""tabPocketPresenter"" label=""PocketPresenter"">
        <group id=""grpConnection"" label=""Connection"">
          <button id=""btnConnect"" label=""Connect Phone"" size=""large"" onAction=""OnConnectClicked"" getLabel=""GetConnectButtonLabel"" getImage=""GetConnectButtonImage""/>
          <labelControl id=""lblStatus"" getLabel=""GetStatusLabel""/>
        </group>
        <group id=""grpEffects"" label=""Cursor Effects"">
          <dropDown id=""drpEffects"" label=""Active Effect"" onAction=""OnEffectChanged"" getSelectedItemIndex=""GetSelectedEffectIndex"">
            <item id=""effHoneycomb"" label=""Honeycomb Pulse""/>
            <item id=""effDiamond"" label=""Diamond Facets""/>
            <item id=""effRipple"" label=""Geometric Ripple""/>
            <item id=""effOff"" label=""Off (Standard Cursor)""/>
          </dropDown>
        </group>
      </tab>
    </tabs>
  </ribbon>
</customUI>";
        }
    }
}
