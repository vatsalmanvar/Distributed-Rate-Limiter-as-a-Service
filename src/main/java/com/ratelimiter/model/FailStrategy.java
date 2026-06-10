package com.ratelimiter.model;

/**
 * Behavior to use when backing infrastructure cannot be reached safely.
 */
public enum FailStrategy {
    FAIL_OPEN,
    FAIL_CLOSE
}
