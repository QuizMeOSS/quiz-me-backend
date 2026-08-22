package com.quizme.outbox;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Enumerated(EnumType.STRING)
    private OutboxEventTypes type;
    private String payload;
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private int retryCount;

    public OutboxEvent() {
    }

    public OutboxEvent(OutboxEventTypes type, String payload) {
        this.type = type;
        this.payload = payload;
        status = OutboxStatus.PENDING;
        createdAt = LocalDateTime.now();
    }

    public void markAsProcessed() {
        this.status = OutboxStatus.PROCESSED;
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public long getId() {
        return id;
    }

    public OutboxEventTypes getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        retryCount += 1;
    }
}