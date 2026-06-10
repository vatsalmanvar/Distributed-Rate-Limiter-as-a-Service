package com.ratelimiter.repository;

import com.ratelimiter.model.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for tenant persistence and API key lookup operations.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Finds a tenant by the stored one-way API key hash.
     *
     * @param apiKeyHash hashed API key
     * @return tenant if a matching hash exists
     */
    Optional<Tenant> findByApiKeyHash(String apiKeyHash);

    /**
     * Checks whether a tenant name already exists, ignoring case.
     *
     * @param name tenant name
     * @return true when a tenant with the same name exists
     */
    boolean existsByNameIgnoreCase(String name);
}
