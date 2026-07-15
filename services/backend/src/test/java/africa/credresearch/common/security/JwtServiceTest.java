package africa.credresearch.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import africa.credresearch.common.config.CredResearchProperties;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(props(Duration.ofMinutes(15)));

    private static CredResearchProperties props(Duration accessTtl) {
        return new CredResearchProperties(
                new CredResearchProperties.Auth(null, null, accessTtl, Duration.ofDays(30),
                        Duration.ofHours(24), "credresearch-test"),
                new CredResearchProperties.Throttle(5, Duration.ofMinutes(15)),
                new CredResearchProperties.App("http://localhost:3000"),
                new CredResearchProperties.Email("no-reply@test.local"),
                new CredResearchProperties.Cors(java.util.List.of("http://localhost:3000")));
    }

    @Test
    void issuesAndParsesRoundTrip() {
        var principal = new AppUserPrincipal(UUID.randomUUID(), UUID.randomUUID(),
                Set.of("STUDENT", "SUPERVISOR"), "FREE");
        String token = jwtService.issueAccessToken(principal);

        AppUserPrincipal parsed = jwtService.parse(token);
        assertThat(parsed.userId()).isEqualTo(principal.userId());
        assertThat(parsed.institutionId()).isEqualTo(principal.institutionId());
        assertThat(parsed.roles()).containsExactlyInAnyOrder("STUDENT", "SUPERVISOR");
        assertThat(parsed.plan()).isEqualTo("FREE");
    }

    @Test
    void rejectsTamperedToken() {
        var principal = new AppUserPrincipal(UUID.randomUUID(), UUID.randomUUID(), Set.of("STUDENT"), "FREE");
        String token = jwtService.issueAccessToken(principal);
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> jwtService.parse("not-a-jwt"))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(props(Duration.ofMillis(1)));
        var principal = new AppUserPrincipal(UUID.randomUUID(), UUID.randomUUID(), Set.of("STUDENT"), "FREE");
        String token = shortLived.issueAccessToken(principal);
        Thread.sleep(20);
        assertThatThrownBy(() -> shortLived.parse(token))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }
}
