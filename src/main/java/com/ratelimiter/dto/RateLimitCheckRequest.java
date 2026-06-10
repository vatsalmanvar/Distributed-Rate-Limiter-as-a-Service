package com.ratelimiter.dto;

import com.ratelimiter.model.RuleScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RateLimitCheckRequest(
        @NotNull RuleScope scope,
        @Size(max = 255) String identifier,
        @Size(max = 500) String endpoint
) {
}
