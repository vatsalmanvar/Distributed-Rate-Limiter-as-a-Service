package com.ratelimiter.dto;

import com.ratelimiter.model.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 255) String name,
        PlanType plan
) {
}
