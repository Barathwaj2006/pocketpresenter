namespace PocketPresenter.Protocol
{
    /// <summary>
    /// Protocol error codes as specified in docs/protocol.md section 6.
    /// </summary>
    public static class ErrorCodes
    {
        public const string BadFrame = "BAD_FRAME";
        public const string BadVersion = "BAD_VERSION";
        public const string BadType = "BAD_TYPE";
        public const string BadField = "BAD_FIELD";
        public const string BadSession = "BAD_SESSION";
        public const string NotReady = "NOT_READY";
        public const string NoActiveSlideShow = "NO_ACTIVE_SLIDESHOW";
        public const string NotArmed = "NOT_ARMED";
        public const string RateLimited = "RATE_LIMITED";
        public const string Duplicate = "DUPLICATE";
        public const string Internal = "INTERNAL";
    }
}
