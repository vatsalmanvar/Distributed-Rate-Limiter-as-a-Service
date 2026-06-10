package com.ratelimiter.algorithm;

/**
 * Result returned by a rate limiting algorithm after evaluating one request.
 *
 * @param allowed whether the request may proceed
 * @param remaining remaining quota after this decision
 * @param retryAfterMs milliseconds until a rejected request should be retried
 * @param resetAfterMs milliseconds until the active limiting window or bucket fully resets
 */
public record RateLimitResult(
        boolean allowed,
        long remaining,
        long retryAfterMs,
        long resetAfterMs
) {
}
