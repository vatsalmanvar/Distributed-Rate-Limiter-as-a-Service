package com.ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the distributed rate limiter service.
 */
@SpringBootApplication
public class RateLimiterApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments supplied by the runtime
     */
    public static void main(String[] args) {
        SpringApplication.run(RateLimiterApplication.class, args);
    }
}
