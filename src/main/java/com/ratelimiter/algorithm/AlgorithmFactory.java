package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Resolves algorithm implementations by persistent rule type.
 */
@Component
public class AlgorithmFactory {

    private final Map<AlgorithmType, RateLimitAlgorithm> algorithms;

    public AlgorithmFactory(List<RateLimitAlgorithm> algorithms) {
        this.algorithms = new EnumMap<>(AlgorithmType.class);
        for (RateLimitAlgorithm algorithm : algorithms) {
            this.algorithms.put(algorithm.type(), algorithm);
        }
    }

    /**
     * Returns the implementation for a configured algorithm type.
     *
     * @param type requested algorithm type
     * @return matching algorithm implementation
     */
    public RateLimitAlgorithm getAlgorithm(AlgorithmType type) {
        RateLimitAlgorithm algorithm = algorithms.get(type);
        if (algorithm == null) {
            throw new IllegalArgumentException("Unsupported rate limit algorithm: " + type);
        }
        return algorithm;
    }
}
