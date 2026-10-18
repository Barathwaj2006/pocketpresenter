using System;
using System.Text;
using System.Text.Json;

namespace PocketPresenter.Protocol
{
    public static class FrameEncoder
    {
        public static byte[] Encode(Envelope envelope)
        {
            if (envelope == null) throw new ArgumentNullException(nameof(envelope));

            var jsonBytes = JsonSerializer.SerializeToUtf8Bytes(envelope);
            if (jsonBytes.Length + 1 > ProtocolConstants.MaxFrameBytes)
            {
                throw new InvalidOperationException($"Encoded frame exceeds max allowed size of {ProtocolConstants.MaxFrameBytes} bytes");
            }

            var frame = new byte[jsonBytes.Length + 1];
            Buffer.BlockCopy(jsonBytes, 0, frame, 0, jsonBytes.Length);
            frame[jsonBytes.Length] = (byte)'\n';
            return frame;
        }

        public static string EncodeToString(Envelope envelope)
        {
            return Encoding.UTF8.GetString(Encode(envelope));
        }
    }
}
