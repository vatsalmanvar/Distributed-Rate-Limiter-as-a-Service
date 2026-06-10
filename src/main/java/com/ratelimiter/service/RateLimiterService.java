package com.ratelimiter.service;

import com.ratelimiter.algorithm.AlgorithmFactory;
import com.ratelimiter.algorithm.RateLimitResult;
import com.ratelimiter.config.KafkaConfig;
import com.ratelimiter.dto.RateLimitCheckRequest;
import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.model.FailStrategy;
import com.ratelimiter.model.RateLimitEvent;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.model.RuleScope;
import com.ratelimiter.model.Tenant;
import com.ratelimiter.repository.RateLimitRuleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Main application service for evaluating rate limit checks and emitting analytics events.
 */
@Service
public class RateLimiterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterService.class);

    private final TenantService tenantService;
    private final RateLimitRuleRepository ruleRepository;
    private final AlgorithmFactory algorithmFactory;
    private final KafkaTemplate<String, RateLimitEvent> kafkaTemplate;
    private final Clock clock;
    private final RateLimitEventBroadcaster eventBroadcaster;

    public RateLimiterService(TenantService tenantService, RateLimitRuleRepository ruleRepository,
                              AlgorithmFactory algorithmFactory,
                              KafkaTemplate<String, RateLimitEvent> kafkaTemplate,
                              RateLimitEventBroadcaster eventBroadcaster) {
        this(tenantService, ruleRepository, algorithmFactory, kafkaTemplate, eventBroadcaster, Clock.systemUTC());
    }

    RateLimiterService(TenantService tenantService, RateLimitRuleRepository ruleRepository,
                       AlgorithmFactory algorithmFactory, KafkaTemplate<String, RateLimitEvent> kafkaTemplate,
                       RateLimitEventBroadcaster eventBroadcaster, Clock clock) {
        this.tenantService = tenantService;
        this.ruleRepository = ruleRepository;
        this.algorithmFactory = algorithmFactory;
        this.kafkaTemplate = kafkaTemplate;
        this.eventBroadcaster = eventBroadcaster;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RateLimitCheckResponse check(String apiKey, RateLimitCheckRequest request) {
        Tenant tenant = tenantService.getTenantByApiKey(apiKey);
        RateLimitRule rule = resolveRule(tenant, request);
        String identifier = resolveIdentifier(request.scope(), request.identifier());
        long nowMs = clock.millis();
        RateLimitResult result;
        try {
            String key = buildRedisKey(tenant.getId().toString(), rule, identifier, request.endpoint());
            result = algorithmFactory.getAlgorithm(rule.getAlgorithm()).check(key, rule, nowMs);
        } catch (RuntimeException e) {
            LOGGER.warn("Rate limit algorithm failed for tenant {} and rule {}", tenant.getId(), rule.getId(), e);
            boolean allowed = rule.getFailStrategy() == FailStrategy.FAIL_OPEN;
            result = new RateLimitResult(allowed, 0, allowed ? 0 : rule.getWindowSizeMs(), rule.getWindowSizeMs());
        }
        RateLimitEvent event = new RateLimitEvent(tenant.getId(), identifier, normalizeEndpoint(request.endpoint()),
                result.allowed(), rule.getAlgorithm().name(), nowMs, result.remaining());
        publishEvent(event);
        eventBroadcaster.publish(event);
        return new RateLimitCheckResponse(result.allowed(), result.remaining(), result.retryAfterMs(), result.resetAfterMs(),
                tenant.getId(), rule.getId(), rule.getAlgorithm());
    }

    private RateLimitRule resolveRule(Tenant tenant, RateLimitCheckRequest request) {
        String endpoint = normalizeEndpoint(request.endpoint());
        if (endpoint != null) {
            List<RateLimitRule> endpointRules = ruleRepository.findByTenantIdAndScopeAndEndpoint(
                    tenant.getId(), request.scope(), endpoint);
            if (!endpointRules.isEmpty()) {
                return endpointRules.get(0);
            }
        }
        return ruleRepository.findFirstByTenantIdAndScopeAndEndpointIsNull(tenant.getId(), request.scope())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching rate limit rule found"));
    }

    private String resolveIdentifier(RuleScope scope, String identifier) {
        if (scope == RuleScope.GLOBAL) {
            return "global";
        }
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "identifier is required for " + scope + " checks");
        }
        return identifier.trim();
    }

    private void publishEvent(RateLimitEvent event) {
        try {
            kafkaTemplate.send(KafkaConfig.RATE_LIMIT_EVENTS_TOPIC, event.tenantId().toString(), event)
                    .exceptionally(ex -> {
                        LOGGER.warn("Failed to publish rate limit event for tenant {}", event.tenantId(), ex);
                        return null;
                    });
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to enqueue rate limit event for tenant {}", event.tenantId(), e);
        }
    }

    private String buildRedisKey(String tenantId, RateLimitRule rule, String identifier, String endpoint) {
        return "rl:" + tenantId + ":" + rule.getId() + ":" + rule.getScope() + ":" + sha256(identifier)
                + ":" + sha256(normalizeEndpoint(endpoint) == null ? "*" : normalizeEndpoint(endpoint));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String normalizeEndpoint(String endpoint) {
        return endpoint == null || endpoint.isBlank() ? null : endpoint.trim();
    }
}
