class DeepSeekError(Exception):
    """Base exception for DeepSeek integration failures."""


class DeepSeekConfigurationError(DeepSeekError):
    """Raised when DeepSeek is enabled but not configured enough to call."""


class DeepSeekAuthenticationError(DeepSeekError):
    """Raised when DeepSeek rejects credentials."""


class DeepSeekRateLimitError(DeepSeekError):
    """Raised when DeepSeek rate limits the request."""


class DeepSeekTimeoutError(DeepSeekError):
    """Raised when DeepSeek does not respond in time."""


class DeepSeekRequestError(DeepSeekError):
    """Raised for transport or non-successful HTTP responses."""


class DeepSeekInvalidResponseError(DeepSeekError):
    """Raised when DeepSeek returns a response that cannot be used."""

