package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitRule;
import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Token-bucket implementation with continuous refill in Redis Lua.
 */
@Component
public class TokenBucketRateLimitAlgorithm extends AbstractRedisRateLimitAlgorithm {

    private static final RedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local values = redis.call('HMGET', key, 'tokens', 'timestamp')
            local tokens = tonumber(values[1])
            local timestamp = tonumber(values[2])
            if tokens == nil then tokens = capacity end
            if timestamp == nil then timestamp = now end
            local elapsed = math.max(0, now - timestamp)
            local refill = elapsed * capacity / window
            tokens = math.min(capacity, tokens + refill)
            local allowed = 0
            local retry = 0
            if tokens >= 1 then
                allowed = 1
                tokens = tokens - 1
            else
                retry = math.ceil((1 - tokens) * window / capacity)
            end
            redis.call('HMSET', key, 'tokens', tokens, 'timestamp', now)
            redis.call('PEXPIRE', key, window * 2)
            local remaining = math.floor(tokens)
            local reset = math.ceil((capacity - tokens) * window / capacity)
            return { allowed, remaining, retry, reset }
            """, List.class);

    public TokenBucketRateLimitAlgorithm(RedisTemplate<String, String> redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public AlgorithmType type() {
        return AlgorithmType.TOKEN_BUCKET;
    }

    @Override
    public RateLimitResult check(String key, RateLimitRule rule, long nowMs) {
        return execute(SCRIPT, List.of(key), String.valueOf(rule.getLimit()), String.valueOf(rule.getWindowSizeMs()),
                String.valueOf(nowMs));
    }
}
