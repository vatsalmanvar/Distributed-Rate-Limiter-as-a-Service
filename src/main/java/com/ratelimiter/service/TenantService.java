package com.ratelimiter.service;

import com.ratelimiter.dto.CreateRuleRequest;
import com.ratelimiter.dto.CreateTenantRequest;
import com.ratelimiter.dto.CreateTenantResponse;
import com.ratelimiter.dto.RateLimitRuleResponse;
import com.ratelimiter.dto.TenantResponse;
import com.ratelimiter.model.FailStrategy;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.model.Tenant;
import com.ratelimiter.repository.RateLimitRuleRepository;
import com.ratelimiter.repository.TenantRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application service for tenant lifecycle, API-key lookup, and tenant rules.
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RateLimitRuleRepository ruleRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final FailStrategy defaultFailStrategy;

    public TenantService(TenantRepository tenantRepository, RateLimitRuleRepository ruleRepository,
                         @Value("${rate-limiter.default-fail-strategy:FAIL_OPEN}") FailStrategy defaultFailStrategy) {
        this.tenantRepository = tenantRepository;
        this.ruleRepository = ruleRepository;
        this.defaultFailStrategy = defaultFailStrategy;
    }

    @Transactional
    public CreateTenantResponse createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant name already exists");
        }
        String apiKey = generateApiKey();
        Tenant tenant = tenantRepository.save(new Tenant(request.name(), hashApiKey(apiKey), request.plan()));
        return new CreateTenantResponse(TenantResponse.from(tenant), apiKey);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll().stream().map(TenantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantByApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing API key");
        }
        return tenantRepository.findByApiKeyHash(hashApiKey(apiKey))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key"));
    }

    @Transactional
    public void deleteTenant(UUID tenantId) {
        Tenant tenant = getTenant(tenantId);
        ruleRepository.deleteByTenantId(tenant.getId());
        tenantRepository.delete(tenant);
    }

    @Transactional
    public RateLimitRuleResponse createRule(UUID tenantId, CreateRuleRequest request) {
        Tenant tenant = getTenant(tenantId);
        RateLimitRule rule = new RateLimitRule(tenant.getId(), request.scope(), request.algorithm(), request.limit(),
                request.windowSizeMs(), normalizeEndpoint(request.endpoint()),
                request.failStrategy() == null ? defaultFailStrategy : request.failStrategy());
        return RateLimitRuleResponse.from(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<RateLimitRuleResponse> listRules(UUID tenantId) {
        getTenant(tenantId);
        return ruleRepository.findByTenantId(tenantId).stream().map(RateLimitRuleResponse::from).toList();
    }

    @Transactional
    public void deleteRule(UUID tenantId, UUID ruleId) {
        RateLimitRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rate limit rule not found"));
        if (!rule.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rate limit rule not found");
        }
        ruleRepository.delete(rule);
    }

    public String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "rl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEndpoint(String endpoint) {
        return endpoint == null || endpoint.isBlank() ? null : endpoint.trim();
    }
}
