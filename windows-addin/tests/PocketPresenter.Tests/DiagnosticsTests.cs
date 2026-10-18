using PocketPresenter.Core.Diagnostics;
using Xunit;

namespace PocketPresenter.Tests
{
    public class DiagnosticsTests
    {
        [Fact]
        public void DiagnosticsPolicy_RedactsSensitiveData()
        {
            Assert.False(DiagnosticsPolicy.IsSafeToLog("User presentation: Quarterly_Report.pptx"));
            Assert.False(DiagnosticsPolicy.IsSafeToLog("Connecting with secret: token123"));
            Assert.True(DiagnosticsPolicy.IsSafeToLog("Bluetooth transport state changed: Connected"));

            Assert.Equal("[REDACTED - SENSITIVE CONTENT]", DiagnosticsPolicy.Sanitize("Opening deck.pptx"));
            Assert.Equal("Safe log event", DiagnosticsPolicy.Sanitize("Safe log event"));
        }
    }
}
