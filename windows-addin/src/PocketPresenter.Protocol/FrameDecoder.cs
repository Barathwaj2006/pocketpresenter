using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;

namespace PocketPresenter.Protocol
{
    public struct DecodeResult
    {
        public bool Success { get; set; }
        public Envelope? Envelope { get; set; }
        public string? ErrorCode { get; set; }
        public string? ErrorMessage { get; set; }
        public bool CloseConnection { get; set; }

        public static DecodeResult Ok(Envelope envelope) =>
            new DecodeResult { Success = true, Envelope = envelope };

        public static DecodeResult Error(string code, string message, bool closeConnection = false) =>
            new DecodeResult { Success = false, ErrorCode = code, ErrorMessage = message, CloseConnection = closeConnection };
    }

    /// <summary>
    /// Newline-delimited UTF-8 JSON stream decoder.
    /// Handles fragmentation, multiple frames per read, and enforces the 4096-byte limit.
    /// </summary>
    public class FrameDecoder
    {
        private readonly List<byte> _buffer = new List<byte>(ProtocolConstants.MaxFrameBytes * 2);

        public List<DecodeResult> Feed(ReadOnlySpan<byte> chunk)
        {
            var results = new List<DecodeResult>();

            for (int i = 0; i < chunk.Length; i++)
            {
                byte b = chunk[i];
                _buffer.Add(b);

                // Enforce max frame size before newline
                if (_buffer.Count > ProtocolConstants.MaxFrameBytes)
                {
                    _buffer.Clear();
                    results.Add(DecodeResult.Error(ErrorCodes.BadFrame, "Frame exceeded max size of 4096 bytes", closeConnection: true));
                    return results;
                }

                if (b == (byte)'\n')
                {
                    // Frame boundary detected
                    int frameLength = _buffer.Count;
                    if (frameLength > ProtocolConstants.MaxFrameBytes)
                    {
                        _buffer.Clear();
                        results.Add(DecodeResult.Error(ErrorCodes.BadFrame, "Frame exceeded max size of 4096 bytes", closeConnection: true));
                        return results;
                    }

                    // Extract bytes without the trailing \n
                    int jsonLength = frameLength - 1;
                    if (jsonLength > 0 && _buffer[jsonLength - 1] == (byte)'\r')
                    {
                        jsonLength--;
                    }

                    if (jsonLength == 0)
                    {
                        // Empty line, ignore
                        _buffer.Clear();
                        continue;
                    }

                    byte[] lineBytes = new byte[jsonLength];
                    _buffer.CopyTo(0, lineBytes, 0, jsonLength);
                    _buffer.Clear();

                    try
                    {
                        var envelope = JsonSerializer.Deserialize<Envelope>(lineBytes);
                        if (envelope == null || string.IsNullOrEmpty(envelope.Type))
                        {
                            results.Add(DecodeResult.Error(ErrorCodes.BadFrame, "Malformed envelope JSON"));
                        }
                        else
                        {
                            results.Add(DecodeResult.Ok(envelope));
                        }
                    }
                    catch (Exception ex)
                    {
                        results.Add(DecodeResult.Error(ErrorCodes.BadFrame, $"JSON parse error: {ex.Message}"));
                    }
                }
            }

            return results;
        }

        public List<DecodeResult> Feed(string text)
        {
            return Feed(Encoding.UTF8.GetBytes(text));
        }

        public void Reset()
        {
            _buffer.Clear();
        }
    }
}
