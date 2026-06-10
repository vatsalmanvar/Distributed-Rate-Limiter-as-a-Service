package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Sliding-window log implementation that stores request timestamps in a sorted set.
 */
@Component
public class SlidingWindowLogRateLimitAlgorithm extends AbstractRedisRateLimitAlgorithm {

    private static final RedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local member = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            local allowed = 0
            local retry = 0
            if count < limit then
                redis.call('ZADD', key, now, member)
                redis.call('PEXPIRE', key, window)
                allowed = 1
                count = count + 1
            else
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                if oldest[2] ~= nil then
                    retry = math.max(0, tonumber(oldest[2]) + window - now)
                else
                    retry = window
                end
            end
            local remaining = limit - count
            if remaining < 0 then remaining = 0 end
            local ttl = redis.call('PTTL', key)
            if ttl < 0 then ttl = window end
            return { allowed, remaining, retry, ttl }
            """, List.class);

    public SlidingWindowLogRateLimitAlgorithm(RedisTemplate<String, String> redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public AlgorithmType type() {
        return AlgorithmType.SLIDING_WINDOW_LOG;
    }

    @Override
    public RateLimitResult check(String key, RateLimitRule rule, long nowMs) {
        return execute(SCRIPT, List.of(key), String.valueOf(rule.getLimit()), String.valueOf(rule.getWindowSizeMs()),
                String.valueOf(nowMs), nowMs + ":" + UUID.randomUUID());
    }
}
