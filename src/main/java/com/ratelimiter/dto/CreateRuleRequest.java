package com.ratelimiter.dto;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.FailStrategy;
import com.ratelimiter.model.RuleScope;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRuleRequest(
        @NotNull RuleScope scope,
        @NotNull AlgorithmType algorithm,
        @Min(1) long limit,
        @Min(1) long windowSizeMs,
        @Size(max = 500) String endpoint,
        FailStrategy failStrategy
) {
}
