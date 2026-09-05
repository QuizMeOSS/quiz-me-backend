package com.quizme.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class OutboxRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelay.class);

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;
    private final OutboxEventPublisher publisher;

    public OutboxRelay(OutboxRepository outboxRepository, OutboxEventPublisher publisher) {
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
    }

    @Scheduled(fixedRate = 5000)
    public void readOutbox() {
        List<OutboxEvent> pendingEvents = outboxRepository.findUnprocessedEvents(BATCH_SIZE);

        if (pendingEvents.isEmpty()) {
            return;
        }

        LOGGER.info("Processing {} outbox events", pendingEvents.size());

        for (var event : pendingEvents) {
            try {
                publisher.publish(event);
                event.markAsProcessed();
                LOGGER.info("Published event {} of type {} to queue", event.getId(), event.getType());
            } catch (ExecutionException | InterruptedException e) {
                event.incrementRetryCount();
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.markAsFailed();
                    LOGGER.error("Failed to publish outbox event {} - marking as failed", event.getId());
                } else {
                    LOGGER.error("Failed to publish outbox event {} - will retry again later", event.getId());
                }
            }

        }

        outboxRepository.saveAll(pendingEvents);
    }
}
