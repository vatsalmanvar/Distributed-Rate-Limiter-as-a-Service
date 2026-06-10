package com.ratelimiter.dto;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.FailStrategy;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.model.RuleScope;
import java.util.UUID;

public record RateLimitRuleResponse(
        UUID id,
        UUID tenantId,
        RuleScope scope,
        AlgorithmType algorithm,
        long limit,
        long windowSizeMs,
        String endpoint,
        FailStrategy failStrategy
) {
    public static RateLimitRuleResponse from(RateLimitRule rule) {
        return new RateLimitRuleResponse(rule.getId(), rule.getTenantId(), rule.getScope(), rule.getAlgorithm(),
                rule.getLimit(), rule.getWindowSizeMs(), rule.getEndpoint(), rule.getFailStrategy());
    }
}
