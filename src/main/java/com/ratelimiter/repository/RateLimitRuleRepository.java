package com.ratelimiter.repository;

import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.model.RuleScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for tenant rate limiting rules.
 */
public interface RateLimitRuleRepository extends JpaRepository<RateLimitRule, UUID> {

    /**
     * Lists all rules for a tenant.
     *
     * @param tenantId owning tenant id
     * @return rules ordered by database natural order
     */
    List<RateLimitRule> findByTenantId(UUID tenantId);

    /**
     * Finds tenant rules matching a scope and endpoint-specific override.
     *
     * @param tenantId owning tenant id
     * @param scope rule scope
     * @param endpoint endpoint value
     * @return matching endpoint-specific rules
     */
    List<RateLimitRule> findByTenantIdAndScopeAndEndpoint(UUID tenantId, RuleScope scope, String endpoint);

    /**
     * Finds a tenant-wide rule for a scope where endpoint is null.
     *
     * @param tenantId owning tenant id
     * @param scope rule scope
     * @return first global endpoint rule if present
     */
    Optional<RateLimitRule> findFirstByTenantIdAndScopeAndEndpointIsNull(UUID tenantId, RuleScope scope);

    /**
     * Removes every rule owned by a tenant.
     *
     * @param tenantId owning tenant id
     */
    void deleteByTenantId(UUID tenantId);
}
