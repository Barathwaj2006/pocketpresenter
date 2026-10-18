using System;
using PocketPresenter.Protocol;

namespace PocketPresenter.Core.PowerPoint
{
    public interface ISlideShowView
    {
        int CurrentSlideIndex { get; }
        int TotalSlidesCount { get; }
        void Next();
        void Previous();
        void GoToSlide(int index);
    }

    /// <summary>
    /// Thread-safe PowerPoint automation controller adhering to COM STA rules.
    /// Returns NO_ACTIVE_SLIDESHOW when no slide show is running. Never sends keys to arbitrary windows.
    /// </summary>
    public class PowerPointInteropController : IPowerPointController
    {
        private readonly object _lock = new object();
        private ISlideShowView? _activeView;
        private bool _disposed;

        public bool HasActiveSlideShow
        {
            get
            {
                lock (_lock)
                {
                    return _activeView != null;
                }
            }
        }

        public int CurrentSlideIndex
        {
            get
            {
                lock (_lock)
                {
                    return _activeView?.CurrentSlideIndex ?? 0;
                }
            }
        }

        public int TotalSlidesCount
        {
            get
            {
                lock (_lock)
                {
                    return _activeView?.TotalSlidesCount ?? 0;
                }
            }
        }

        public event Action<int, int>? SlideChanged;
        public event Action? SlideShowStarted;
        public event Action? SlideShowEnded;

        public void AttachSlideShowView(ISlideShowView view)
        {
            lock (_lock)
            {
                if (_disposed) return;
                _activeView = view ?? throw new ArgumentNullException(nameof(view));
            }
            SlideShowStarted?.Invoke();
            SlideChanged?.Invoke(view.CurrentSlideIndex, view.TotalSlidesCount);
        }

        public void DetachSlideShowView()
        {
            lock (_lock)
            {
                if (_activeView == null) return;
                _activeView = null;
            }
            SlideShowEnded?.Invoke();
        }

        public void NotifySlideChanged(int currentSlide, int totalSlides)
        {
            SlideChanged?.Invoke(currentSlide, totalSlides);
        }

        public CommandResult Next()
        {
            ISlideShowView? view;
            lock (_lock)
            {
                view = _activeView;
            }

            if (view == null)
            {
                return CommandResult.Fail(ErrorCodes.NoActiveSlideShow, "No active slide show");
            }

            try
            {
                view.Next();
                NotifySlideChanged(view.CurrentSlideIndex, view.TotalSlidesCount);
                return CommandResult.Ok();
            }
            catch (Exception ex)
            {
                return CommandResult.Fail(ErrorCodes.Internal, $"Slide navigation failed: {ex.Message}");
            }
        }

        public CommandResult Previous()
        {
            ISlideShowView? view;
            lock (_lock)
            {
                view = _activeView;
            }

            if (view == null)
            {
                return CommandResult.Fail(ErrorCodes.NoActiveSlideShow, "No active slide show");
            }

            try
            {
                view.Previous();
                NotifySlideChanged(view.CurrentSlideIndex, view.TotalSlidesCount);
                return CommandResult.Ok();
            }
            catch (Exception ex)
            {
                return CommandResult.Fail(ErrorCodes.Internal, $"Slide navigation failed: {ex.Message}");
            }
        }

        public CommandResult GoToSlide(int slideIndex)
        {
            ISlideShowView? view;
            lock (_lock)
            {
                view = _activeView;
            }

            if (view == null)
            {
                return CommandResult.Fail(ErrorCodes.NoActiveSlideShow, "No active slide show");
            }

            if (slideIndex < 1 || slideIndex > view.TotalSlidesCount)
            {
                return CommandResult.Fail(ErrorCodes.BadField, $"Slide index {slideIndex} is out of bounds [1, {view.TotalSlidesCount}]");
            }

            try
            {
                view.GoToSlide(slideIndex);
                NotifySlideChanged(view.CurrentSlideIndex, view.TotalSlidesCount);
                return CommandResult.Ok();
            }
            catch (Exception ex)
            {
                return CommandResult.Fail(ErrorCodes.Internal, $"GoToSlide failed: {ex.Message}");
            }
        }

        public void Dispose()
        {
            lock (_lock)
            {
                if (_disposed) return;
                _disposed = true;
                _activeView = null;
            }
            SlideShowEnded?.Invoke();
        }
    }
}
