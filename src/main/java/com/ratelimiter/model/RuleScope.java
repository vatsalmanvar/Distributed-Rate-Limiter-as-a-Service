package com.ratelimiter.model;

/**
 * Scope at which a rate limit rule is evaluated.
 */
public enum RuleScope {
    PER_IP,
    PER_USER,
    GLOBAL
}
