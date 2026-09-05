package com.quizme.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setup() {
        outboxRelay = new OutboxRelay(outboxRepository, outboxEventPublisher);
    }

    @Test
    void GIVEN_emptyOutbox_WHEN_read_THEN_doNothing() throws Exception {
        outboxRelay.readOutbox();
        verify(outboxEventPublisher, never()).publish(any());
    }

    @Test
    void GIVEN_outboxEvent_WHEN_successfullyProcessed_THEN_eventsPublished() throws Exception {
        var event1 = new OutboxEvent();
        var event2 = new OutboxEvent();
        when(outboxRepository.findUnprocessedEvents(50)).thenReturn(
                List.of(event1, event2)
        );

        outboxRelay.readOutbox();

        verify(outboxEventPublisher).publish(event1);
        verify(outboxEventPublisher).publish(event2);
    }

    @Test
    void GIVEN_outboxEvent_WHEN_successfullyProcessed_THEN_markAsProcessed() {
        var event1 = new OutboxEvent();
        var event2 = new OutboxEvent();
        when(outboxRepository.findUnprocessedEvents(50)).thenReturn(
                List.of(event1, event2)
        );

        outboxRelay.readOutbox();

        assertEquals(OutboxStatus.PROCESSED, event1.getStatus());
        assertEquals(OutboxStatus.PROCESSED, event2.getStatus());
    }

    @Test
    void GIVEN_outboxEvent_WHEN_publishingFails_THEN_eventStaysPending() throws ExecutionException, InterruptedException {
        var event1 = new OutboxEvent();
        var event2 = new OutboxEvent();
        when(outboxRepository.findUnprocessedEvents(50)).thenReturn(
                List.of(event1, event2)
        );
        doThrow(new ExecutionException("msg", null)).when(outboxEventPublisher).publish(event1);

        outboxRelay.readOutbox();

        assertEquals(OutboxStatus.PENDING, event1.getStatus());
        assertEquals(OutboxStatus.PROCESSED, event2.getStatus());
    }

    @Test
    void WHEN_publishingFailsThreeTimes_THEN_eventMarkedAsFailed() throws ExecutionException, InterruptedException {
        var event = new OutboxEvent();
        when(outboxRepository.findUnprocessedEvents(50)).thenReturn(
                List.of(event)
        );
        doThrow(new ExecutionException("msg", null)).when(outboxEventPublisher).publish(event);

        outboxRelay.readOutbox();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        outboxRelay.readOutbox();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        outboxRelay.readOutbox();
        assertEquals(OutboxStatus.FAILED, event.getStatus());
    }

    @Test
    void taskRunsEveryFiveSeconds() throws NoSuchMethodException {
        Scheduled annotation = OutboxRelay.class
                .getMethod("readOutbox")
                .getAnnotation(Scheduled.class);

        assertEquals(5000L, annotation.fixedRate());
    }

}