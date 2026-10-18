using System;

namespace PocketPresenter.Core.Effects
{
    /// <summary>
    /// Pure memory-backed RGBA 32-bit raster surface for effect rendering.
    /// Thread-safe for off-screen drawing before updating layered windows.
    /// </summary>
    public class MemoryEffectSurface : IEffectSurface
    {
        public int Width { get; }
        public int Height { get; }
        public uint[] Pixels { get; }

        public MemoryEffectSurface(int width, int height)
        {
            Width = Math.Max(1, width);
            Height = Math.Max(1, height);
            Pixels = new uint[Width * Height];
        }

        public void Clear()
        {
            Array.Clear(Pixels, 0, Pixels.Length);
        }

        public void DrawCircle(float centerX, float centerY, float radius, uint colorRgba)
        {
            if (radius <= 0) return;

            int minX = Math.Max(0, (int)Math.Floor(centerX - radius - 1));
            int maxX = Math.Min(Width - 1, (int)Math.Ceiling(centerX + radius + 1));
            int minY = Math.Max(0, (int)Math.Floor(centerY - radius - 1));
            int maxY = Math.Min(Height - 1, (int)Math.Ceiling(centerY + radius + 1));

            byte r = (byte)((colorRgba >> 24) & 0xFF);
            byte g = (byte)((colorRgba >> 16) & 0xFF);
            byte b = (byte)((colorRgba >> 8) & 0xFF);
            byte a = (byte)(colorRgba & 0xFF);

            float r2 = radius * radius;
            float innerR2 = Math.Max(0, (radius - 1f) * (radius - 1f));

            for (int y = minY; y <= maxY; y++)
            {
                float dy = y - centerY;
                float dy2 = dy * dy;

                for (int x = minX; x <= maxX; x++)
                {
                    float dx = x - centerX;
                    float d2 = (dx * dx) + dy2;

                    if (d2 <= r2)
                    {
                        // Simple alpha edge feathering
                        float edgeFactor = 1.0f;
                        if (d2 > innerR2)
                        {
                            edgeFactor = Math.Max(0f, 1.0f - ((float)Math.Sqrt(d2) - (radius - 1f)));
                        }

                        byte finalA = (byte)(a * edgeFactor);
                        if (finalA > 0)
                        {
                            // Premultiplied BGRA for Windows UpdateLayeredWindow
                            uint premulBgra = ((uint)finalA << 24) |
                                              ((uint)((r * finalA) / 255) << 16) |
                                              ((uint)((g * finalA) / 255) << 8) |
                                              (uint)((b * finalA) / 255);

                            Pixels[(y * Width) + x] = premulBgra;
                        }
                    }
                }
            }
        }

        public void DrawPolygon(ReadOnlySpan<(float X, float Y)> points, uint colorRgba)
        {
            // Optional polygon rasterization used by honeycomb/diamond in Phase 6
            if (points.Length < 3) return;

            for (int i = 0; i < points.Length; i++)
            {
                var pt = points[i];
                DrawCircle(pt.X, pt.Y, 2.0f, colorRgba);
            }
        }
    }
}
