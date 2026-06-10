package com.ratelimiter.service;

import com.ratelimiter.config.KafkaConfig;
import com.ratelimiter.model.RateLimitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that stores rate limit decision events for analytics queries.
 */
@Component
public class RateLimitAnalyticsConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitAnalyticsConsumer.class);

    private final AnalyticsService analyticsService;

    public RateLimitAnalyticsConsumer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(topics = KafkaConfig.RATE_LIMIT_EVENTS_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void consume(RateLimitEvent event) {
        analyticsService.persistEvent(event);
        LOGGER.debug("Persisted rate limit event for tenant {}", event.tenantId());
    }
}
