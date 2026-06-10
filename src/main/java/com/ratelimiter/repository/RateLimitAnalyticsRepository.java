package com.ratelimiter.repository;

import com.ratelimiter.model.RateLimitAnalytics;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for persisted rate limit analytics events.
 */
public interface RateLimitAnalyticsRepository extends JpaRepository<RateLimitAnalytics, Long> {

    List<RateLimitAnalytics> findTop100ByTenantIdOrderByEventTimestampDesc(UUID tenantId);

    @Query("""
            select count(a),
                   sum(case when a.allowed = true then 1 else 0 end),
                   sum(case when a.allowed = false then 1 else 0 end)
            from RateLimitAnalytics a
            where a.tenantId = :tenantId and a.eventTimestamp >= :since
            """)
    Object[] summarizeTenantSince(@Param("tenantId") UUID tenantId, @Param("since") Instant since);
}
