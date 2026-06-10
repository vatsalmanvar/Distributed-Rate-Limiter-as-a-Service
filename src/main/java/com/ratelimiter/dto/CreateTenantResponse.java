package com.ratelimiter.dto;

public record CreateTenantResponse(TenantResponse tenant, String apiKey) {
}
