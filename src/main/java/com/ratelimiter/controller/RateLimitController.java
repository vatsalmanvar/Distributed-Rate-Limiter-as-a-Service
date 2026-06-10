package com.ratelimiter.controller;

import com.ratelimiter.dto.RateLimitCheckRequest;
import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.service.RateLimiterService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public rate limit decision API.
 */
@RestController
@RequestMapping("/api/v1/rate-limit")
public class RateLimitController {

    private final RateLimiterService rateLimiterService;

    public RateLimitController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/check")
    public RateLimitCheckResponse check(@RequestHeader("X-API-Key") String apiKey,
                                        @Valid @RequestBody RateLimitCheckRequest request) {
        return rateLimiterService.check(apiKey, request);
    }
}
