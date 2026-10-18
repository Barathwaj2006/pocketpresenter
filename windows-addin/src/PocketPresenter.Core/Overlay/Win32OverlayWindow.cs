using System;
using System.Runtime.InteropServices;

namespace PocketPresenter.Core.Overlay
{
    /// <summary>
    /// Production Win32 implementation of a click-through, non-activating, topmost transparent layered window.
    /// Uses per-pixel alpha blending via UpdateLayeredWindow.
    /// </summary>
    public class Win32OverlayWindow : INativeOverlayWindow
    {
        [StructLayout(LayoutKind.Sequential)]
        private struct POINT
        {
            public int X;
            public int Y;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct SIZE
        {
            public int cx;
            public int cy;
        }

        [StructLayout(LayoutKind.Sequential, Pack = 1)]
        private struct BLENDFUNCTION
        {
            public byte BlendOp;
            public byte BlendFlags;
            public byte SourceConstantAlpha;
            public byte AlphaFormat;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct BITMAPINFOHEADER
        {
            public uint biSize;
            public int biWidth;
            public int biHeight;
            public ushort biPlanes;
            public ushort biBitCount;
            public uint biCompression;
            public uint biSizeImage;
            public int biXPelsPerMeter;
            public int biYPelsPerMeter;
            public uint biClrUsed;
            public uint biClrImportant;
        }

        [DllImport("user32.dll", SetLastError = true)]
        private static extern IntPtr CreateWindowEx(
            uint dwExStyle,
            string lpClassName,
            string lpWindowName,
            uint dwStyle,
            int x,
            int y,
            int nWidth,
            int nHeight,
            IntPtr hWndParent,
            IntPtr hMenu,
            IntPtr hInstance,
            IntPtr lpParam);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool DestroyWindow(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        [DllImport("user32.dll")]
        private static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool UpdateLayeredWindow(
            IntPtr hwnd,
            IntPtr hdcDst,
            ref POINT pptDst,
            ref SIZE psize,
            IntPtr hdcSrc,
            ref POINT pptSrc,
            uint crKey,
            ref BLENDFUNCTION pblend,
            uint dwFlags);

        [DllImport("user32.dll")]
        private static extern IntPtr GetDC(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern int ReleaseDC(IntPtr hWnd, IntPtr hDC);

        [DllImport("gdi32.dll")]
        private static extern IntPtr CreateCompatibleDC(IntPtr hDC);

        [DllImport("gdi32.dll")]
        private static extern bool DeleteDC(IntPtr hdc);

        [DllImport("gdi32.dll")]
        private static extern IntPtr SelectObject(IntPtr hDC, IntPtr hObject);

        [DllImport("gdi32.dll")]
        private static extern bool DeleteObject(IntPtr hObject);

        [DllImport("gdi32.dll", SetLastError = true)]
        private static extern IntPtr CreateDIBSection(
            IntPtr hdc,
            ref BITMAPINFOHEADER pbmi,
            uint pila,
            out IntPtr ppvBits,
            IntPtr hSection,
            uint dwOffset);

        // Window style constants
        private const uint WS_POPUP = 0x80000000;
        private const uint WS_EX_TOPMOST = 0x00000008;
        private const uint WS_EX_TOOLWINDOW = 0x00000080;
        private const uint WS_EX_LAYERED = 0x00080000;
        private const uint WS_EX_TRANSPARENT = 0x00000020;
        private const uint WS_EX_NOACTIVATE = 0x08000000;

        private const int SW_HIDE = 0;
        private const int SW_SHOWNOACTIVATE = 4;
        private static readonly IntPtr HWND_TOPMOST = new IntPtr(-1);
        private const uint SWP_NOACTIVATE = 0x0010;
        private const uint SWP_NOSIZE = 0x0001;

        private const byte AC_SRC_OVER = 0x00;
        private const byte AC_SRC_ALPHA = 0x01;
        private const uint ULW_ALPHA = 0x00000002;

        public bool IsCreated => WindowHandle != IntPtr.Zero;
        public bool IsVisible { get; private set; }
        public IntPtr WindowHandle { get; private set; } = IntPtr.Zero;

        private readonly bool _isWindows;

        public Win32OverlayWindow()
        {
            _isWindows = RuntimeInformation.IsOSPlatform(OSPlatform.Windows);
        }

        public void Create(int x, int y, int width, int height)
        {
            if (!_isWindows || IsCreated) return;

            uint exStyle = WS_EX_TOPMOST | WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW;
            uint style = WS_POPUP;

            WindowHandle = CreateWindowEx(
                exStyle,
                "STATIC",
                "PocketPresenterOverlay",
                style,
                x,
                y,
                width,
                height,
                IntPtr.Zero,
                IntPtr.Zero,
                IntPtr.Zero,
                IntPtr.Zero);
        }

        public void SetPosition(int x, int y)
        {
            if (!_isWindows || !IsCreated) return;
            SetWindowPos(WindowHandle, HWND_TOPMOST, x, y, 0, 0, SWP_NOACTIVATE | SWP_NOSIZE);
        }

        public void SetVisible(bool visible)
        {
            IsVisible = visible;
            if (!_isWindows || !IsCreated) return;
            ShowWindow(WindowHandle, visible ? SW_SHOWNOACTIVATE : SW_HIDE);
        }

        public void UpdatePixels(uint[] premultipliedBgraPixels, int width, int height, int screenX, int screenY)
        {
            if (!_isWindows || !IsCreated || !IsVisible) return;

            IntPtr screenDc = GetDC(IntPtr.Zero);
            IntPtr memDc = CreateCompatibleDC(screenDc);

            var bmi = new BITMAPINFOHEADER
            {
                biSize = (uint)Marshal.SizeOf<BITMAPINFOHEADER>(),
                biWidth = width,
                biHeight = -height, // top-down DIB
                biPlanes = 1,
                biBitCount = 32,
                biCompression = 0
            };

            IntPtr hBitmap = CreateDIBSection(screenDc, ref bmi, 0, out IntPtr ppvBits, IntPtr.Zero, 0);
            if (hBitmap != IntPtr.Zero && ppvBits != IntPtr.Zero)
            {
                Marshal.Copy(unchecked((int[])(object)premultipliedBgraPixels), 0, ppvBits, premultipliedBgraPixels.Length);

                IntPtr oldBitmap = SelectObject(memDc, hBitmap);

                var blend = new BLENDFUNCTION
                {
                    BlendOp = AC_SRC_OVER,
                    BlendFlags = 0,
                    SourceConstantAlpha = 255,
                    AlphaFormat = AC_SRC_ALPHA
                };

                var ptDst = new POINT { X = screenX, Y = screenY };
                var size = new SIZE { cx = width, cy = height };
                var ptSrc = new POINT { X = 0, Y = 0 };

                UpdateLayeredWindow(WindowHandle, screenDc, ref ptDst, ref size, memDc, ref ptSrc, 0, ref blend, ULW_ALPHA);

                SelectObject(memDc, oldBitmap);
                DeleteObject(hBitmap);
            }

            DeleteDC(memDc);
            ReleaseDC(IntPtr.Zero, screenDc);
        }

        public void Destroy()
        {
            if (!_isWindows || !IsCreated) return;

            ShowWindow(WindowHandle, SW_HIDE);
            DestroyWindow(WindowHandle);
            WindowHandle = IntPtr.Zero;
            IsVisible = false;
        }

        public void Dispose()
        {
            Destroy();
        }
    }
}
