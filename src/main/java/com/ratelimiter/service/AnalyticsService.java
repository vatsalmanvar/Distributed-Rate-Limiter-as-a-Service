package com.ratelimiter.service;

import com.ratelimiter.dto.AnalyticsEventResponse;
import com.ratelimiter.dto.AnalyticsSummaryResponse;
import com.ratelimiter.model.RateLimitAnalytics;
import com.ratelimiter.model.RateLimitEvent;
import com.ratelimiter.repository.RateLimitAnalyticsRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists consumed rate limit events and exposes tenant analytics queries.
 */
@Service
public class AnalyticsService {

    private final RateLimitAnalyticsRepository analyticsRepository;

    public AnalyticsService(RateLimitAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional
    public void persistEvent(RateLimitEvent event) {
        analyticsRepository.save(new RateLimitAnalytics(event));
    }

    @Transactional(readOnly = true)
    public List<AnalyticsEventResponse> recentEvents(UUID tenantId) {
        return analyticsRepository.findTop100ByTenantIdOrderByEventTimestampDesc(tenantId).stream()
                .map(AnalyticsEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summarize(UUID tenantId, Instant since) {
        Instant effectiveSince = since == null ? Instant.now().minus(24, ChronoUnit.HOURS) : since;
        Object[] result = analyticsRepository.summarizeTenantSince(tenantId, effectiveSince);
        Object[] row = result.length > 0 && result[0] instanceof Object[] nested ? nested : result;
        long total = asLong(row, 0);
        long allowed = asLong(row, 1);
        long rejected = asLong(row, 2);
        return new AnalyticsSummaryResponse(tenantId, effectiveSince, total, allowed, rejected);
    }

    private long asLong(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return 0L;
        }
        if (row[index] instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(row[index]));
    }
}
