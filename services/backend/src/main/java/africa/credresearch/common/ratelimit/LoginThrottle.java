package africa.credresearch.common.ratelimit;

import africa.credresearch.common.config.CredResearchProperties;
import org.springframework.stereotype.Service;

/** Per-identifier brute-force protection for login (Security spec §1, §6). */
@Service
public class LoginThrottle {

    private final ThrottleStore store;
    private final CredResearchProperties props;

    public LoginThrottle(ThrottleStore store, CredResearchProperties props) {
        this.store = store;
        this.props = props;
    }

    public boolean isBlocked(String identifier) {
        return store.current(key(identifier)) >= props.throttle().loginMaxAttempts();
    }

    public void recordFailure(String identifier) {
        store.increment(key(identifier), props.throttle().loginWindow());
    }

    public void reset(String identifier) {
        store.clear(key(identifier));
    }

    private String key(String identifier) {
        return "throttle:login:" + identifier.toLowerCase();
    }
}
