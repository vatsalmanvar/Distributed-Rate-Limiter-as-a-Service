package com.ratelimiter.dto;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsSummaryResponse(
        UUID tenantId,
        Instant since,
        long totalRequests,
        long allowedRequests,
        long rejectedRequests
) {
}
