package africa.credresearch.common.ratelimit;

import java.time.Duration;

/** Counter store backing rate limits. Redis in prod; an in-memory fake in unit tests. */
public interface ThrottleStore {

    /** Increment the counter for {@code key}, setting {@code ttl} on first creation; returns new value. */
    long increment(String key, Duration ttl);

    /** Current counter value, or 0 if absent/expired. */
    long current(String key);

    void clear(String key);
}
