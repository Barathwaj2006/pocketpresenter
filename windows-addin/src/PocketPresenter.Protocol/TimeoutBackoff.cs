using System;

namespace PocketPresenter.Protocol
{
    /// <summary>
    /// Bounded exponential backoff policy for reconnect attempts.
    /// </summary>
    public class TimeoutBackoff
    {
        private readonly int _initialDelayMs;
        private readonly int _maxDelayMs;
        private readonly double _multiplier;
        private readonly Random _random;

        public int Attempt { get; private set; }
        public int CurrentDelayMs { get; private set; }

        public TimeoutBackoff(int initialDelayMs = 1000, int maxDelayMs = 10000, double multiplier = 2.0)
        {
            _initialDelayMs = initialDelayMs;
            _maxDelayMs = maxDelayMs;
            _multiplier = multiplier;
            _random = new Random();
            Reset();
        }

        public int NextDelay()
        {
            Attempt++;
            double rawDelay = _initialDelayMs * Math.Pow(_multiplier, Attempt - 1);
            if (rawDelay > _maxDelayMs)
            {
                rawDelay = _maxDelayMs;
            }

            // Apply 10% jitter
            double jitter = 1.0 + (_random.NextDouble() * 0.2 - 0.1);
            CurrentDelayMs = (int)(rawDelay * jitter);
            return CurrentDelayMs;
        }

        public void Reset()
        {
            Attempt = 0;
            CurrentDelayMs = 0;
        }
    }
}
