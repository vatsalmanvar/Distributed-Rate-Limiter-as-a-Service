package com.ratelimiter.dto;

import com.ratelimiter.model.AlgorithmType;
import java.util.UUID;

public record RateLimitCheckResponse(
        boolean allowed,
        long remaining,
        long retryAfterMs,
        long resetAfterMs,
        UUID tenantId,
        UUID ruleId,
        AlgorithmType algorithm
) {
}
