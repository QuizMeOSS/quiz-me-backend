package com.quizme.outbox;

import com.quizme.AppProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka // enable kafka listener annotated methods
@Configuration
public class KafkaConsumerConfig {

    private final AppProperties appProperties;

    public KafkaConsumerConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appProperties.getKafka().getUrl());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-events-consumer");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Don't periodically auto-commit: we want to commit only after our handler
        // has actually finished processing, so a crash mid-processing
        // re-delivers the message instead of silently skipping it.
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Commit the offset ourselves, right after successful processing.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Retry a failing record 3 times (1s apart) before giving up on
        // it and moving to the next record, instead of retrying forever
        // and blocking the partition.
        factory.setCommonErrorHandler(new DefaultErrorHandler((record, exception) -> System.out.println(">>> RECOVERER CALLED (retries exhausted or non-retryable): " + exception), new FixedBackOff(1000L, 3)));

        return factory;
    }
}