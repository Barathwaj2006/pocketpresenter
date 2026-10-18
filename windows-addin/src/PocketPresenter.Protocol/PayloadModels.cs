using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace PocketPresenter.Protocol
{
    public class HelloPayload
    {
        [JsonPropertyName("client")]
        public string Client { get; set; } = "addin";

        [JsonPropertyName("appVersion")]
        public string AppVersion { get; set; } = "0.1.0";

        [JsonPropertyName("supports")]
        public List<int> Supports { get; set; } = new List<int> { 1 };

        [JsonPropertyName("capabilities")]
        public List<string> Capabilities { get; set; } = new List<string> { "pointer", "slides", "effects" };
    }

    public class HelloAckPayload
    {
        [JsonPropertyName("accepted")]
        public bool Accepted { get; set; }

        [JsonPropertyName("version")]
        public int Version { get; set; } = 1;

        [JsonPropertyName("sessionId")]
        public string? SessionId { get; set; }

        [JsonPropertyName("capabilities")]
        public List<string> Capabilities { get; set; } = new List<string>();

        [JsonPropertyName("reason")]
        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public string? Reason { get; set; }
    }

    public class PointerDeltaPayload
    {
        [JsonPropertyName("dx")]
        public int Dx { get; set; }

        [JsonPropertyName("dy")]
        public int Dy { get; set; }
    }

    public class PointerButtonPayload
    {
        [JsonPropertyName("button")]
        public string Button { get; set; } = string.Empty;

        [JsonPropertyName("action")]
        public string Action { get; set; } = string.Empty;
    }

    public class PointerScrollPayload
    {
        [JsonPropertyName("dy")]
        public int Dy { get; set; }
    }

    public class EffectSetPayload
    {
        [JsonPropertyName("effect")]
        public string Effect { get; set; } = "off";

        [JsonPropertyName("intensity")]
        public double Intensity { get; set; } = 0.35;

        [JsonPropertyName("scale")]
        public double Scale { get; set; } = 1.0;

        [JsonPropertyName("reducedMotion")]
        public bool ReducedMotion { get; set; }
    }

    public class StateUpdatePayload
    {
        [JsonPropertyName("link")]
        public string Link { get; set; } = "disconnected";

        [JsonPropertyName("show")]
        public string Show { get; set; } = "none";

        [JsonPropertyName("effect")]
        public string Effect { get; set; } = "off";

        [JsonPropertyName("slide")]
        public int Slide { get; set; }

        [JsonPropertyName("slideCount")]
        public int SlideCount { get; set; }
    }

    public class CommandAckPayload
    {
        [JsonPropertyName("ref")]
        public int Ref { get; set; }

        [JsonPropertyName("ok")]
        public bool Ok { get; set; } = true;
    }

    public class CommandErrorPayload
    {
        [JsonPropertyName("ref")]
        public int Ref { get; set; }

        [JsonPropertyName("code")]
        public string Code { get; set; } = string.Empty;

        [JsonPropertyName("message")]
        public string Message { get; set; } = string.Empty;
    }

    public class DrawStartPayload
    {
        [JsonPropertyName("x")]
        public double X { get; set; }

        [JsonPropertyName("y")]
        public double Y { get; set; }

        [JsonPropertyName("color")]
        public string Color { get; set; } = "#FF0000";

        [JsonPropertyName("width")]
        public int Width { get; set; } = 3;
    }

    public class DrawPointPayload
    {
        [JsonPropertyName("points")]
        public List<List<double>> Points { get; set; } = new List<List<double>>();
    }
}
