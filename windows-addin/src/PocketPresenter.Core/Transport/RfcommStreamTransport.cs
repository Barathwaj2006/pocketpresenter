using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace PocketPresenter.Core.Transport
{
    /// <summary>
    /// Stream-based implementation of IBluetoothTransport for Bluetooth RFCOMM sockets.
    /// Operates strictly on background threads; never blocks PowerPoint's STA/UI thread.
    /// </summary>
    public class RfcommStreamTransport : IBluetoothTransport
    {
        private Stream? _stream;
        private CancellationTokenSource? _cts;
        private Task? _readLoopTask;
        private readonly object _lock = new object();
        private bool _disposed;

        public event Action<ReadOnlyMemory<byte>>? BytesReceived;
        public event Action<TransportState>? StateChanged;

        public TransportState State { get; private set; } = TransportState.Disconnected;

        public void AttachStream(Stream stream)
        {
            lock (_lock)
            {
                if (_disposed) throw new ObjectDisposedException(nameof(RfcommStreamTransport));
                _stream = stream ?? throw new ArgumentNullException(nameof(stream));
                _cts = new CancellationTokenSource();
                SetState(TransportState.Connected);
                _readLoopTask = Task.Run(() => ReadLoopAsync(_stream, _cts.Token));
            }
        }

        public Task ConnectAsync(PeerId peer, CancellationToken ct = default)
        {
            SetState(TransportState.Connecting);
            // In live environment, WinRT RfcommDeviceService or Winsock connects here
            return Task.CompletedTask;
        }

        public async Task SendAsync(ReadOnlyMemory<byte> frame, CancellationToken ct = default)
        {
            Stream? stream;
            lock (_lock)
            {
                if (State != TransportState.Connected || _stream == null)
                {
                    throw new InvalidOperationException("Cannot send frame: transport is not connected.");
                }
                stream = _stream;
            }

#if NETSTANDARD2_0
            byte[] bytes = frame.ToArray();
            await stream.WriteAsync(bytes, 0, bytes.Length, ct).ConfigureAwait(false);
#else
            await stream.WriteAsync(frame, ct).ConfigureAwait(false);
#endif
            await stream.FlushAsync(ct).ConfigureAwait(false);
        }

        public async Task DisconnectAsync()
        {
            CancellationTokenSource? cts;
            Stream? stream;
            Task? readTask;

            lock (_lock)
            {
                if (State == TransportState.Disconnected) return;

                cts = _cts;
                stream = _stream;
                readTask = _readLoopTask;

                _cts = null;
                _stream = null;
                _readLoopTask = null;
                SetState(TransportState.Disconnected);
            }

            try
            {
                cts?.Cancel();
                stream?.Dispose();
                if (readTask != null)
                {
                    await Task.WhenAny(readTask, Task.Delay(500)).ConfigureAwait(false);
                }
            }
            catch
            {
                // Ignore cleanup exceptions
            }
            finally
            {
                cts?.Dispose();
            }
        }

        private async Task ReadLoopAsync(Stream stream, CancellationToken ct)
        {
            byte[] buffer = new byte[4096];
            try
            {
                while (!ct.IsCancellationRequested)
                {
                    int bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length, ct).ConfigureAwait(false);
                    if (bytesRead <= 0)
                    {
                        // End of stream / peer disconnected
                        break;
                    }

                    byte[] chunk = new byte[bytesRead];
                    Buffer.BlockCopy(buffer, 0, chunk, 0, bytesRead);
                    BytesReceived?.Invoke(chunk);
                }
            }
            catch (OperationCanceledException)
            {
                // Normal cancellation
            }
            catch (Exception)
            {
                // Socket / stream fault
            }
            finally
            {
                SetState(TransportState.Disconnected);
            }
        }

        private void SetState(TransportState newState)
        {
            if (State != newState)
            {
                State = newState;
                StateChanged?.Invoke(newState);
            }
        }

        public void Dispose()
        {
            lock (_lock)
            {
                if (_disposed) return;
                _disposed = true;
            }
            DisconnectAsync().GetAwaiter().GetResult();
        }
    }
}
