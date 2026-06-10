package com.ratelimiter.dto;

import com.ratelimiter.model.PlanType;
import com.ratelimiter.model.Tenant;
import java.time.Instant;
import java.util.UUID;

public record TenantResponse(UUID id, String name, PlanType plan, Instant createdAt) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getPlan(), tenant.getCreatedAt());
    }
}
