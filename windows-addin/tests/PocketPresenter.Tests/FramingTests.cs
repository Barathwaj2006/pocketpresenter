using PocketPresenter.Protocol;
using Xunit;

namespace PocketPresenter.Tests
{
    public class FramingTests
    {
        [Fact]
        public void FrameEncoder_And_Decoder_RoundTrip()
        {
            var envelope = new Envelope
            {
                Version = 1,
                SessionId = "3f2b8c1e-5d7a-4c0e-9b1d-2a6f4e8c7d10",
                Sequence = 42,
                Type = MessageTypes.SlideNext
            };

            var encoded = FrameEncoder.EncodeToString(envelope);
            Assert.EndsWith("\n", encoded);

            var decoder = new FrameDecoder();
            var results = decoder.Feed(encoded);

            Assert.Single(results);
            Assert.True(results[0].Success);
            Assert.Equal(envelope.Sequence, results[0].Envelope!.Sequence);
            Assert.Equal(envelope.Type, results[0].Envelope!.Type);
            Assert.Equal(envelope.SessionId, results[0].Envelope!.SessionId);
        }
    }
}
