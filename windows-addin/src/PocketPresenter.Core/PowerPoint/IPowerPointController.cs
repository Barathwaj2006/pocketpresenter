using System;

namespace PocketPresenter.Core.PowerPoint
{
    public readonly struct CommandResult
    {
        public bool Success { get; }
        public string? ErrorCode { get; }
        public string? Message { get; }

        private CommandResult(bool success, string? errorCode, string? message)
        {
            Success = success;
            ErrorCode = errorCode;
            Message = message;
        }

        public static CommandResult Ok() => new CommandResult(true, null, null);
        public static CommandResult Fail(string errorCode, string message) =>
            new CommandResult(false, errorCode, message);
    }

    /// <summary>
    /// PowerPoint automation controller. All methods MUST be invoked on the STA/UI thread via dispatcher.
    /// Never sends raw keys to random windows; verifies active slide-show state first.
    /// </summary>
    public interface IPowerPointController : IDisposable
    {
        bool HasActiveSlideShow { get; }
        int CurrentSlideIndex { get; }
        int TotalSlidesCount { get; }

        event Action<int, int>? SlideChanged; // (currentSlide, totalSlides)
        event Action? SlideShowStarted;
        event Action? SlideShowEnded;

        CommandResult Next();
        CommandResult Previous();
        CommandResult GoToSlide(int slideIndex);
    }
}
