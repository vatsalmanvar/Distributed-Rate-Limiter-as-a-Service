package com.ratelimiter.config;

import com.ratelimiter.model.RateLimitEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Kafka producer, consumer, and local topic configuration.
 */
@Configuration
public class KafkaConfig {

    public static final String RATE_LIMIT_EVENTS_TOPIC = "rate-limit-events";

    /**
     * Configures the producer used for asynchronous rate limit event publication.
     *
     * @param bootstrapServers comma-separated Kafka bootstrap servers
     * @return producer factory serializing RateLimitEvent as JSON
     */
    @Bean
    public ProducerFactory<String, RateLimitEvent> rateLimitEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "1");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    /**
     * Creates the Kafka template for rate limit event publication.
     *
     * @param producerFactory event producer factory
     * @return Kafka template
     */
    @Bean
    public KafkaTemplate<String, RateLimitEvent> rateLimitEventKafkaTemplate(
            ProducerFactory<String, RateLimitEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Configures the consumer factory for analytics event processing.
     *
     * @param bootstrapServers comma-separated Kafka bootstrap servers
     * @param groupId analytics consumer group id
     * @param autoOffsetReset consumer offset reset policy
     * @return consumer factory deserializing RateLimitEvent JSON payloads
     */
    @Bean
    public ConsumerFactory<String, RateLimitEvent> rateLimitEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset:earliest}") String autoOffsetReset) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ratelimiter.model");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RateLimitEvent.class.getName());
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    /**
     * Creates the listener container factory used by analytics consumers.
     *
     * @param consumerFactory event consumer factory
     * @return concurrent listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RateLimitEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, RateLimitEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, RateLimitEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        return factory;
    }

    /**
     * Declares the rate limit events topic for local development.
     *
     * @return topic definition with six partitions and one replica
     */
    @Bean
    public NewTopic rateLimitEventsTopic() {
        return TopicBuilder.name(RATE_LIMIT_EVENTS_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
