package com.ratelimiter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent rate limiting rule owned by a tenant.
 */
@Entity
@Table(
    name = "rate_limit_rules",
    indexes = {
        @Index(name = "idx_rate_limit_rules_tenant", columnList = "tenant_id"),
        @Index(name = "idx_rate_limit_rules_lookup", columnList = "tenant_id,scope,endpoint")
    }
)
public class RateLimitRule {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private RuleScope scope;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false, length = 64)
    private AlgorithmType algorithm;

    @Min(1)
    @Column(name = "request_limit", nullable = false)
    private long limit;

    @Min(1)
    @Column(name = "window_size_ms", nullable = false)
    private long windowSizeMs;

    @Size(max = 500)
    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "fail_strategy", nullable = false, length = 32)
    private FailStrategy failStrategy;

    protected RateLimitRule() {
    }

    /**
     * Creates a tenant-scoped rate limiting rule.
     *
     * @param tenantId owning tenant id
     * @param scope identifier scope
     * @param algorithm algorithm used to evaluate requests
     * @param limit maximum requests in the configured window
     * @param windowSizeMs rule window size in milliseconds
     * @param endpoint endpoint this rule applies to, or null for all endpoints
     * @param failStrategy behavior when Redis is unavailable
     */
    public RateLimitRule(UUID tenantId, RuleScope scope, AlgorithmType algorithm, long limit,
                         long windowSizeMs, String endpoint, FailStrategy failStrategy) {
        this.tenantId = tenantId;
        this.scope = scope;
        this.algorithm = algorithm;
        this.limit = limit;
        this.windowSizeMs = windowSizeMs;
        this.endpoint = endpoint;
        this.failStrategy = failStrategy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public RuleScope getScope() {
        return scope;
    }

    public void setScope(RuleScope scope) {
        this.scope = scope;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmType algorithm) {
        this.algorithm = algorithm;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getWindowSizeMs() {
        return windowSizeMs;
    }

    public void setWindowSizeMs(long windowSizeMs) {
        this.windowSizeMs = windowSizeMs;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public FailStrategy getFailStrategy() {
        return failStrategy;
    }

    public void setFailStrategy(FailStrategy failStrategy) {
        this.failStrategy = failStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RateLimitRule that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
