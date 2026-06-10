package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitRule;
import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Approximate sliding-window counter using weighted current and previous buckets.
 */
@Component
public class SlidingWindowCounterRateLimitAlgorithm extends AbstractRedisRateLimitAlgorithm {

    private static final RedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local current_key = KEYS[1]
            local previous_key = KEYS[2]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local elapsed = tonumber(ARGV[3])
            local current_count = tonumber(redis.call('GET', current_key) or '0')
            local previous_count = tonumber(redis.call('GET', previous_key) or '0')
            local weighted_previous = previous_count * ((window - elapsed) / window)
            local estimated = current_count + weighted_previous
            local allowed = 0
            local retry = 0
            if estimated < limit then
                allowed = 1
                current_count = redis.call('INCR', current_key)
                redis.call('PEXPIRE', current_key, window * 2)
                estimated = current_count + weighted_previous
            else
                retry = math.max(1, window - elapsed)
            end
            local remaining = math.floor(limit - estimated)
            if remaining < 0 then remaining = 0 end
            return { allowed, remaining, retry, window - elapsed }
            """, List.class);

    public SlidingWindowCounterRateLimitAlgorithm(RedisTemplate<String, String> redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public AlgorithmType type() {
        return AlgorithmType.SLIDING_WINDOW_COUNTER;
    }

    @Override
    public RateLimitResult check(String key, RateLimitRule rule, long nowMs) {
        long window = rule.getWindowSizeMs();
        long bucketStart = (nowMs / window) * window;
        long previousBucketStart = bucketStart - window;
        long elapsed = nowMs - bucketStart;
        return execute(SCRIPT, List.of(key + ":" + bucketStart, key + ":" + previousBucketStart),
                String.valueOf(rule.getLimit()), String.valueOf(window), String.valueOf(elapsed));
    }
}
