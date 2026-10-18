using System;
using System.Threading;
using System.Threading.Tasks;

namespace PocketPresenter.Core.Transport
{
    public enum TransportState
    {
        Disconnected,
        Connecting,
        Connected
    }

    public readonly struct PeerId : IEquatable<PeerId>
    {
        public string Value { get; }

        public PeerId(string value)
        {
            Value = value ?? throw new ArgumentNullException(nameof(value));
        }

        public override string ToString() => Value;
        public bool Equals(PeerId other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);
        public override bool Equals(object? obj) => obj is PeerId other && Equals(other);
        public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);
        public static bool operator ==(PeerId left, PeerId right) => left.Equals(right);
        public static bool operator !=(PeerId left, PeerId right) => !left.Equals(right);
    }

    /// <summary>
    /// Bluetooth Classic RFCOMM transport contract. Runs strictly off the UI thread.
    /// </summary>
    public interface IBluetoothTransport : IDisposable
    {
        event Action<ReadOnlyMemory<byte>>? BytesReceived;
        event Action<TransportState>? StateChanged;

        TransportState State { get; }

        Task ConnectAsync(PeerId peer, CancellationToken ct = default);
        Task SendAsync(ReadOnlyMemory<byte> frame, CancellationToken ct = default);
        Task DisconnectAsync();
    }
}
