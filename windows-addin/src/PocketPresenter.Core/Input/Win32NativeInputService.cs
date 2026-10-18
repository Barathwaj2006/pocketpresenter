using System;
using System.Runtime.InteropServices;

namespace PocketPresenter.Core.Input
{
    /// <summary>
    /// Production Win32 user32.dll implementation of INativeInputService.
    /// Safely checks for Windows OS platform before invoking P/Invoke methods.
    /// </summary>
    public class Win32NativeInputService : INativeInputService
    {
        [StructLayout(LayoutKind.Sequential)]
        private struct POINT
        {
            public int X;
            public int Y;
        }

        [DllImport("user32.dll")]
        private static extern bool GetCursorPos(out POINT lpPoint);

        [DllImport("user32.dll")]
        private static extern bool SetCursorPos(int x, int y);

        [DllImport("user32.dll")]
        private static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, UIntPtr dwExtraInfo);

        [DllImport("user32.dll")]
        private static extern int GetSystemMetrics(int nIndex);

        private const int SM_XVIRTUALSCREEN = 76;
        private const int SM_YVIRTUALSCREEN = 77;
        private const int SM_CXVIRTUALSCREEN = 78;
        private const int SM_CYVIRTUALSCREEN = 79;
        private const int SM_CXSCREEN = 0;
        private const int SM_CYSCREEN = 1;

        private const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
        private const uint MOUSEEVENTF_LEFTUP = 0x0004;
        private const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
        private const uint MOUSEEVENTF_RIGHTUP = 0x0010;
        private const uint MOUSEEVENTF_MIDDLEDOWN = 0x0020;
        private const uint MOUSEEVENTF_MIDDLEUP = 0x0040;
        private const uint MOUSEEVENTF_WHEEL = 0x0800;

        private readonly bool _isWindows;

        public Win32NativeInputService()
        {
            _isWindows = RuntimeInformation.IsOSPlatform(OSPlatform.Windows);
        }

        public (int X, int Y) GetCursorPosition()
        {
            if (!_isWindows) return (0, 0);
            if (GetCursorPos(out var pt))
            {
                return (pt.X, pt.Y);
            }
            return (0, 0);
        }

        public void SetCursorPosition(int x, int y)
        {
            if (!_isWindows) return;
            SetCursorPos(x, y);
        }

        public void SendMouseButton(MouseButton button, bool down)
        {
            if (!_isWindows) return;

            uint flag = button switch
            {
                MouseButton.Left => down ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP,
                MouseButton.Right => down ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP,
                MouseButton.Middle => down ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_MIDDLEUP,
                _ => 0
            };

            if (flag != 0)
            {
                mouse_event(flag, 0, 0, 0, UIntPtr.Zero);
            }
        }

        public void SendMouseScroll(int dy)
        {
            if (!_isWindows) return;

            // In Windows, WHEEL_DELTA is 120. dy is standard lines or pixel ticks.
            // Scale by 120 so one unit of dy produces a natural scroll notch.
            uint wheelDelta = unchecked((uint)(dy * 120));
            mouse_event(MOUSEEVENTF_WHEEL, 0, 0, wheelDelta, UIntPtr.Zero);
        }

        public VirtualScreenBounds GetVirtualScreenBounds()
        {
            if (!_isWindows)
            {
                return new VirtualScreenBounds(0, 0, 1920, 1080);
            }

            int left = GetSystemMetrics(SM_XVIRTUALSCREEN);
            int top = GetSystemMetrics(SM_YVIRTUALSCREEN);
            int width = GetSystemMetrics(SM_CXVIRTUALSCREEN);
            int height = GetSystemMetrics(SM_CYVIRTUALSCREEN);

            if (width <= 0 || height <= 0)
            {
                width = GetSystemMetrics(SM_CXSCREEN);
                height = GetSystemMetrics(SM_CYSCREEN);
                left = 0;
                top = 0;
            }

            if (width <= 0) width = 1920;
            if (height <= 0) height = 1080;

            return new VirtualScreenBounds(left, top, width, height);
        }
    }
}
