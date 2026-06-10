package com.ratelimiter.model;

import java.util.UUID;

/**
 * Immutable event emitted after every rate limit decision.
 *
 * @param tenantId tenant that owns the checked rule
 * @param identifier request identifier such as IP address or user id
 * @param endpoint endpoint evaluated by the rate limiter
 * @param allowed whether the request was allowed
 * @param algorithm algorithm that made the decision
 * @param timestamp event timestamp in epoch milliseconds
 * @param remainingTokens remaining quota after the decision
 */
public record RateLimitEvent(
    UUID tenantId,
    String identifier,
    String endpoint,
    boolean allowed,
    String algorithm,
    long timestamp,
    long remainingTokens
) {
}
