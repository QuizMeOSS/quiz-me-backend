package com.quizme.exceptionhandler.result;

public record Failure(FailureReason reason, String message) {
}
