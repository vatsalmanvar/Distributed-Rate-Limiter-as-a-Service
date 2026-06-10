package com.ratelimiter.dto;

import com.ratelimiter.model.RateLimitAnalytics;
import java.time.Instant;
import java.util.UUID;

public record AnalyticsEventResponse(
        Long id,
        UUID tenantId,
        String identifier,
        String endpoint,
        Boolean allowed,
        String algorithm,
        Instant eventTimestamp,
        Long remainingTokens
) {
    public static AnalyticsEventResponse from(RateLimitAnalytics analytics) {
        return new AnalyticsEventResponse(analytics.getId(), analytics.getTenantId(), analytics.getIdentifier(),
                analytics.getEndpoint(), analytics.getAllowed(), analytics.getAlgorithm(),
                analytics.getEventTimestamp(), analytics.getRemainingTokens());
    }
}
