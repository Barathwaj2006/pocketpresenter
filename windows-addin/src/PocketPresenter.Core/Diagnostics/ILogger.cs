using System;

namespace PocketPresenter.Core.Diagnostics
{
    public enum LogLevel
    {
        Debug,
        Info,
        Warning,
        Error
    }

    public interface ILogger
    {
        bool IsEnabled { get; }
        void Log(LogLevel level, string component, string eventName, string message, string? category = null);
    }

    /// <summary>
    /// Enforces the diagnostics policy: structured logging, no secrets, no presentation content.
    /// </summary>
    public static class DiagnosticsPolicy
    {
        public static bool IsSafeToLog(string text)
        {
            if (string.IsNullOrEmpty(text)) return true;

            // Reject paths, presentations, or potential auth secrets
            if (text.IndexOf(".pptx", StringComparison.OrdinalIgnoreCase) >= 0 ||
                text.IndexOf(".ppt", StringComparison.OrdinalIgnoreCase) >= 0 ||
                text.IndexOf("password", StringComparison.OrdinalIgnoreCase) >= 0 ||
                text.IndexOf("secret", StringComparison.OrdinalIgnoreCase) >= 0 ||
                text.IndexOf("token", StringComparison.OrdinalIgnoreCase) >= 0)
            {
                return false;
            }

            return true;
        }

        public static string Sanitize(string message)
        {
            if (!IsSafeToLog(message))
            {
                return "[REDACTED - SENSITIVE CONTENT]";
            }
            return message;
        }
    }
}
