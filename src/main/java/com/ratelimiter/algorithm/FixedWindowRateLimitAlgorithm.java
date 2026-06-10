package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitRule;
import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter backed by an atomic Redis Lua script.
 */
@Component
public class FixedWindowRateLimitAlgorithm extends AbstractRedisRateLimitAlgorithm {

    private static final RedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local count = redis.call('INCR', key)
            if count == 1 then
                redis.call('PEXPIRE', key, window)
            end
            local ttl = redis.call('PTTL', key)
            if ttl < 0 then
                ttl = window
                redis.call('PEXPIRE', key, window)
            end
            local allowed = 0
            if count <= limit then
                allowed = 1
            end
            local remaining = limit - count
            if remaining < 0 then remaining = 0 end
            local retry = 0
            if allowed == 0 then retry = ttl end
            return { allowed, remaining, retry, ttl }
            """, List.class);

    public FixedWindowRateLimitAlgorithm(RedisTemplate<String, String> redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public AlgorithmType type() {
        return AlgorithmType.FIXED_WINDOW;
    }

    @Override
    public RateLimitResult check(String key, RateLimitRule rule, long nowMs) {
        return execute(SCRIPT, List.of(key), String.valueOf(rule.getLimit()), String.valueOf(rule.getWindowSizeMs()));
    }
}
