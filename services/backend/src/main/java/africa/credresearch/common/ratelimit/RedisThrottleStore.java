package africa.credresearch.common.ratelimit;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisThrottleStore implements ThrottleStore {

    private final StringRedisTemplate redis;

    public RedisThrottleStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(key);
        long count = value == null ? 0L : value;
        if (count == 1L) {
            redis.expire(key, ttl);
        }
        return count;
    }

    @Override
    public long current(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    @Override
    public void clear(String key) {
        redis.delete(key);
    }
}
