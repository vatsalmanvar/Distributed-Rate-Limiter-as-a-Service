package com.ratelimiter.model;

/**
 * Supported distributed rate limiting algorithms.
 */
public enum AlgorithmType {
    TOKEN_BUCKET,
    SLIDING_WINDOW_LOG,
    SLIDING_WINDOW_COUNTER,
    FIXED_WINDOW
}
