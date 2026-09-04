package com.quizme.outbox;

import org.jspecify.annotations.NonNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Abstracts publishing the outbox events to message queue.
 * <p>
 * why kafka rather than RabbitMQ or redis streams? just for learning.
 */
@Component
public class OutboxEventPublisher {
    private static final String TOPIC = "outbox-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxEventPublisher(@NonNull KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OutboxEvent event) throws ExecutionException, InterruptedException {
        var message = MessageBuilder
                .withPayload(event.getPayload())
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                // ensure messages of the same type end up in the same partition in an ordered fashion
                .setHeader(KafkaHeaders.KEY, event.getType().name())
                .setHeader("eventType", event.getType().name())
                .setHeader("eventId", event.getId())
                .build();

        // .get() makes the send synchronous so failures throw here,
        // instead of being swallowed as a fire-and-forget future.
        kafkaTemplate.send(message).get();
    }
}
