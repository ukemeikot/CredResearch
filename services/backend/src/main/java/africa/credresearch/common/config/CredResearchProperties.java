package africa.credresearch.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly-typed configuration for the platform, bound from {@code credresearch.*}. */
@ConfigurationProperties(prefix = "credresearch")
public record CredResearchProperties(Auth auth, Throttle throttle, App app, Email email) {

    public record Auth(
            String jwtPrivateKey,
            String jwtPublicKey,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            Duration emailTokenTtl,
            String issuer) {}

    public record Throttle(int loginMaxAttempts, Duration loginWindow) {}

    public record App(String baseUrl) {}

    public record Email(String from) {}
}
