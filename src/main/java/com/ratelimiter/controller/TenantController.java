package com.ratelimiter.controller;

import com.ratelimiter.dto.CreateRuleRequest;
import com.ratelimiter.dto.CreateTenantRequest;
import com.ratelimiter.dto.CreateTenantResponse;
import com.ratelimiter.dto.RateLimitRuleResponse;
import com.ratelimiter.dto.TenantResponse;
import com.ratelimiter.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative tenant and rate limit rule endpoints.
 */
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTenantResponse createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.createTenant(request);
    }

    @GetMapping
    public List<TenantResponse> listTenants() {
        return tenantService.listTenants();
    }

    @GetMapping("/{tenantId}")
    public TenantResponse getTenant(@PathVariable UUID tenantId) {
        return TenantResponse.from(tenantService.getTenant(tenantId));
    }

    @DeleteMapping("/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenant(@PathVariable UUID tenantId) {
        tenantService.deleteTenant(tenantId);
    }

    @PostMapping("/{tenantId}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RateLimitRuleResponse createRule(@PathVariable UUID tenantId, @Valid @RequestBody CreateRuleRequest request) {
        return tenantService.createRule(tenantId, request);
    }

    @GetMapping("/{tenantId}/rules")
    public List<RateLimitRuleResponse> listRules(@PathVariable UUID tenantId) {
        return tenantService.listRules(tenantId);
    }

    @DeleteMapping("/{tenantId}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable UUID tenantId, @PathVariable UUID ruleId) {
        tenantService.deleteRule(tenantId, ruleId);
    }
}
