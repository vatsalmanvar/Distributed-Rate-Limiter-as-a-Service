package com.ratelimiter.controller;

import com.ratelimiter.dto.AnalyticsEventResponse;
import com.ratelimiter.dto.AnalyticsSummaryResponse;
import com.ratelimiter.service.AnalyticsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics query endpoints backed by Kafka-consumed decision events.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/tenants/{tenantId}/events")
    public List<AnalyticsEventResponse> recentEvents(@PathVariable UUID tenantId) {
        return analyticsService.recentEvents(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/summary")
    public AnalyticsSummaryResponse summary(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return analyticsService.summarize(tenantId, since);
    }
}
