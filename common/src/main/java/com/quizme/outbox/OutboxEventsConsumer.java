package com.quizme.outbox;

import com.quizme.idempotency.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxEventsConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventsConsumer.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "outbox:";

    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;

    public OutboxEventsConsumer(ObjectMapper objectMapper,
                                IdempotencyService idempotencyService) {
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;

    }

    @KafkaListener(topics = "outbox-events", groupId = "outbox-events-consumer")
    public void onMessage(
            @Header("eventId") String eventId,
            @Header("eventType") String eventType,
            @Payload String payload,
            Acknowledgment ack) {
        try {
            // Outbox delivery is at-least-once: the same eventId can
            // arrive more than once (e.g. producer crash right after a
            // confirmed send but before marking the outbox row
            // PROCESSED). Skip anything we've already handled.
            if (idempotencyService.tryReserve(IDEMPOTENCY_KEY_PREFIX + eventId, "", null).isPresent()) {
                LOGGER.info("Skipping duplicate event {} ({})", eventId, eventType);
                ack.acknowledge();
                return;
            }

            JsonNode event = objectMapper.readTree(payload);
            handle(eventType, event);

            // Record that this event id was handled
            idempotencyService.storeResponse(IDEMPOTENCY_KEY_PREFIX + eventId, "", null);
            ack.acknowledge();
        } catch (Exception ex) {
            LOGGER.error("Failed to process event {} ({}): {}", eventId, eventType, ex.getMessage(), ex);
            // Don't acknowledge; will retry the record until retries are exhausted
            throw ex;
        }
    }

    private void handle(String eventType, JsonNode event) {
        if (OutboxEventTypes.SIGN_UP.name().equals(eventType)) {
            LOGGER.info("Handling signup for category {}", event.get("name").asString());
        } else {
            LOGGER.warn("Unhandled event type: {}", eventType);
        }
    }
}
