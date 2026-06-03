package com.quizme.exceptionhandler;

import com.quizme.exceptionhandler.result.FailureReason;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashSet;
import java.util.Set;

@RestControllerAdvice
public class AppExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInputValidationExceptions(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        Set<String> errors = new HashSet<>();

        // Extract all validation errors
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            errors.add(error.getDefaultMessage());
        });

        ApiError errorBody = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                FailureReason.VALIDATION_FAILED.name(),
                "Invalid request parameters: " + String.join(" & ", errors),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
    }
}
