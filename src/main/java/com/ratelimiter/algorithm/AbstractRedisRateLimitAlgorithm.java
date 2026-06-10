package com.ratelimiter.algorithm;

import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

abstract class AbstractRedisRateLimitAlgorithm implements RateLimitAlgorithm {

    protected final RedisTemplate<String, String> redisTemplate;

    protected AbstractRedisRateLimitAlgorithm(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    protected RateLimitResult execute(RedisScript<List> script, List<String> keys, String... args) {
        List<?> values = redisTemplate.execute(script, keys, (Object[]) args);
        if (values == null || values.size() < 4) {
            throw new IllegalStateException("Redis Lua script returned an invalid rate limit result");
        }
        return new RateLimitResult(
                asLong(values.get(0)) == 1L,
                Math.max(0L, asLong(values.get(1))),
                Math.max(0L, asLong(values.get(2))),
                Math.max(0L, asLong(values.get(3)))
        );
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
