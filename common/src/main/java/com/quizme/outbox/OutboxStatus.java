package com.quizme.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSED,
    /**
     * Exceeded max attempts; needs manual attention
     */
    FAILED
}
