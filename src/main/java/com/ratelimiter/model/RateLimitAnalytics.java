package com.ratelimiter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent analytics row generated from rate limit decision events.
 */
@Entity
@Table(
    name = "rate_limit_analytics",
    indexes = @Index(name = "idx_tenant_time", columnList = "tenant_id,event_timestamp")
)
public class RateLimitAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "allowed")
    private Boolean allowed;

    @Column(name = "algorithm", length = 50)
    private String algorithm;

    @Column(name = "event_timestamp")
    private Instant eventTimestamp;

    @Column(name = "remaining_tokens")
    private Long remainingTokens;

    protected RateLimitAnalytics() {
    }

    public RateLimitAnalytics(RateLimitEvent event) {
        this.tenantId = event.tenantId();
        this.identifier = event.identifier();
        this.endpoint = event.endpoint();
        this.allowed = event.allowed();
        this.algorithm = event.algorithm();
        this.eventTimestamp = Instant.ofEpochMilli(event.timestamp());
        this.remainingTokens = event.remainingTokens();
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Boolean getAllowed() {
        return allowed;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Long getRemainingTokens() {
        return remainingTokens;
    }
}
