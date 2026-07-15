package africa.credresearch.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import africa.credresearch.common.config.CredResearchProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    /** In-memory store so the policy can be tested without Redis. */
    static class FakeStore implements ThrottleStore {
        final Map<String, Long> counters = new HashMap<>();

        public long increment(String key, Duration ttl) {
            return counters.merge(key, 1L, Long::sum);
        }

        public long current(String key) {
            return counters.getOrDefault(key, 0L);
        }

        public void clear(String key) {
            counters.remove(key);
        }
    }

    private final FakeStore store = new FakeStore();
    private final LoginThrottle throttle = new LoginThrottle(store, new CredResearchProperties(
            new CredResearchProperties.Auth(null, null, Duration.ofMinutes(15), Duration.ofDays(30),
                    Duration.ofHours(24), "t"),
            new CredResearchProperties.Throttle(3, Duration.ofMinutes(15)),
            new CredResearchProperties.App("http://localhost"),
            new CredResearchProperties.Email("x@y.z"),
            new CredResearchProperties.Cors(java.util.List.of("http://localhost:3000"))));

    @Test
    void blocksAfterMaxFailures() {
        assertThat(throttle.isBlocked("user@x.com")).isFalse();
        throttle.recordFailure("user@x.com");
        throttle.recordFailure("user@x.com");
        assertThat(throttle.isBlocked("user@x.com")).isFalse();
        throttle.recordFailure("user@x.com");
        assertThat(throttle.isBlocked("user@x.com")).isTrue();
    }

    @Test
    void resetClearsCounter() {
        throttle.recordFailure("user@x.com");
        throttle.recordFailure("user@x.com");
        throttle.recordFailure("user@x.com");
        assertThat(throttle.isBlocked("user@x.com")).isTrue();
        throttle.reset("user@x.com");
        assertThat(throttle.isBlocked("user@x.com")).isFalse();
    }

    @Test
    void isCaseInsensitiveOnIdentifier() {
        throttle.recordFailure("USER@x.com");
        throttle.recordFailure("user@X.com");
        throttle.recordFailure("User@x.Com");
        assertThat(throttle.isBlocked("user@x.com")).isTrue();
    }
}
