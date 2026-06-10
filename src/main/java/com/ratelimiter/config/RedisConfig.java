package com.ratelimiter.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis client configuration backed by Lettuce connection pooling.
 */
@Configuration
public class RedisConfig {

    /**
     * Builds a pooled Redis connection factory for low-latency algorithm execution.
     *
     * @param host Redis host
     * @param port Redis port
     * @param timeoutMs command and socket timeout in milliseconds
     * @param maxActive maximum active pooled connections
     * @param maxIdle maximum idle pooled connections
     * @return configured Lettuce connection factory
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${rate-limiter.redis-timeout-ms:100}") long timeoutMs,
            @Value("${spring.data.redis.lettuce.pool.max-active:20}") int maxActive,
            @Value("${spring.data.redis.lettuce.pool.max-idle:10}") int maxIdle) {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(host, port);

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(Math.min(2, maxIdle));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        Duration timeout = Duration.ofMillis(timeoutMs);
        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(true)
                .socketOptions(SocketOptions.builder().connectTimeout(timeout).build())
                .build();

        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(timeout)
                .clientOptions(clientOptions)
                .build();

        return new LettuceConnectionFactory(redisConfiguration, clientConfiguration);
    }

    /**
     * Creates a string RedisTemplate for Lua scripts, counters, and lock keys.
     *
     * @param connectionFactory Redis connection factory
     * @return Redis template with String serializers
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
