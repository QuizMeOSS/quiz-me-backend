package com.quizme.exceptionhandler.result;

public record Result<T>(T success, Failure failure) {
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null);
    }
    public static <T> Result<T> failure(Failure failure) {
        return new Result<>(null, failure);
    }
}