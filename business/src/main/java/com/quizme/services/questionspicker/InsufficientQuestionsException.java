package com.quizme.services.questionspicker;

public class InsufficientQuestionsException extends RuntimeException {
    private final int requestedCount;
    private final int availableCount;

    public InsufficientQuestionsException(
            int requestedCount,
            int availableCount
    ) {
        super();
        this.requestedCount = requestedCount;
        this.availableCount = availableCount;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getAvailableCount() {
        return availableCount;
    }
}
