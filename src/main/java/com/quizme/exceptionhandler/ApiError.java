package com.quizme.exceptionhandler;

public record ApiError(
        int status,
        String error,
        String message,
        String path
) {}