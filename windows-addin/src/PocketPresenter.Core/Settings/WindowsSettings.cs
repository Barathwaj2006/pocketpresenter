using System.Text.Json.Serialization;

namespace PocketPresenter.Core.Settings
{
    /// <summary>
    /// Local preferences model for Windows add-in.
    /// Saved to %LocalAppData%\PocketPresenter\settings.json.
    /// </summary>
    public class WindowsSettings
    {
        [JsonPropertyName("pairedDeviceId")]
        public string? PairedDeviceId { get; set; }

        [JsonPropertyName("activeEffect")]
        public string ActiveEffect { get; set; } = "honeycomb";

        [JsonPropertyName("effectIntensity")]
        public double EffectIntensity { get; set; } = 0.35;

        [JsonPropertyName("effectScale")]
        public double EffectScale { get; set; } = 1.0;

        [JsonPropertyName("reducedMotion")]
        public bool ReducedMotion { get; set; } = false;

        [JsonPropertyName("pointerSensitivity")]
        public double PointerSensitivity { get; set; } = 1.0;

        [JsonPropertyName("diagnosticsEnabled")]
        public bool DiagnosticsEnabled { get; set; } = false;
    }
}
