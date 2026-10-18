using System;

namespace PocketPresenter.Protocol
{
    /// <summary>
    /// Manages heartbeat timing, dead-link detection (15s), and held-button safety watchdog (3s, ADR-0013).
    /// </summary>
    public class HeartbeatManager
    {
        public const int PingIntervalMs = 5000;
        public const int FastPingIntervalMs = 1000;
        public const int DeadLinkTimeoutMs = 15000;
        public const int HeldButtonTimeoutMs = 3000;

        private long _lastInboundTimestampMs;
        private long _lastPingSentTimestampMs;

        public bool IsButtonHeld { get; set; }

        public event Action? SendPingRequested;
        public event Action? LinkDeadDetected;
        public event Action? HeldButtonWatchdogTriggered;

        public HeartbeatManager()
        {
            Reset();
        }

        public void Reset(long nowMs = 0)
        {
            _lastInboundTimestampMs = nowMs;
            _lastPingSentTimestampMs = nowMs;
            IsButtonHeld = false;
        }

        public void RecordInboundTraffic(long nowMs)
        {
            _lastInboundTimestampMs = nowMs;
        }

        public void Tick(long nowMs)
        {
            // 1. Check general link dead timeout (15 s)
            if (_lastInboundTimestampMs > 0 && (nowMs - _lastInboundTimestampMs) > DeadLinkTimeoutMs)
            {
                LinkDeadDetected?.Invoke();
                return;
            }

            // 2. Check held-button watchdog (3 s, ADR-0013)
            if (IsButtonHeld && (nowMs - _lastInboundTimestampMs) > HeldButtonTimeoutMs)
            {
                HeldButtonWatchdogTriggered?.Invoke();
                IsButtonHeld = false; // Reset held flag after releasing
            }

            // 3. Check outbound ping interval
            int interval = IsButtonHeld ? FastPingIntervalMs : PingIntervalMs;
            if (nowMs - _lastPingSentTimestampMs >= interval)
            {
                _lastPingSentTimestampMs = nowMs;
                SendPingRequested?.Invoke();
            }
        }
    }
}
