using System.Text.Json;
using System.Text.Json.Serialization;

namespace PocketPresenter.Protocol
{
    /// <summary>
    /// Standard wire message envelope as specified in docs/protocol.md section 2.
    /// </summary>
    public class Envelope
    {
        [JsonPropertyName("v")]
        public int Version { get; set; } = ProtocolConstants.CurrentProtocolVersion;

        [JsonPropertyName("sid")]
        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public string? SessionId { get; set; }

        [JsonPropertyName("seq")]
        public int Sequence { get; set; }

        [JsonPropertyName("type")]
        public string Type { get; set; } = string.Empty;

        private static readonly JsonElement EmptyObject = JsonDocument.Parse("{}").RootElement;
        private JsonElement _payload = EmptyObject;

        [JsonPropertyName("payload")]
        public JsonElement Payload
        {
            get => _payload.ValueKind == JsonValueKind.Undefined ? EmptyObject : _payload;
            set => _payload = value;
        }
    }
}
