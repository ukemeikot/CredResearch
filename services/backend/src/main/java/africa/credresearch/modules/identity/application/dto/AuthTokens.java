package africa.credresearch.modules.identity.application.dto;

public record AuthTokens(String accessToken, String refreshToken, long expiresIn, AuthenticatedUser user) {}
