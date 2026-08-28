package com.quizme.outbox;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class OutboxService {

    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    public OutboxService(ObjectMapper objectMapper,
                         OutboxRepository outboxRepository) {
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    public void saveEvent(@NonNull OutboxEventTypes eventType,
                          @NonNull Map<String, ?> eventPayload) {
        outboxRepository.save(new OutboxEvent(eventType, toJson(eventPayload)));
    }

    @NonNull
    private String toJson(@NonNull Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}
