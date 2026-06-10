package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitRule;

/**
 * Contract for atomic distributed rate limiting algorithms.
 */
public interface RateLimitAlgorithm {

    /**
     * Algorithm type implemented by this component.
     *
     * @return supported algorithm type
     */
    AlgorithmType type();

    /**
     * Evaluates a request against the supplied rule and Redis key.
     *
     * @param key stable Redis key for the tenant/rule/identifier tuple
     * @param rule persistent rate limiting rule
     * @param nowMs current epoch time in milliseconds
     * @return rate limit decision and remaining quota metadata
     */
    RateLimitResult check(String key, RateLimitRule rule, long nowMs);
}
