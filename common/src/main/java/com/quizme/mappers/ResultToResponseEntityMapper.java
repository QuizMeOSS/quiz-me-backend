package com.quizme.mappers;

import com.quizme.exceptionhandler.ApiError;
import com.quizme.exceptionhandler.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResultToResponseEntityMapper {
    /**
     * Maps a {@link Result} into a ResponseEntity.
     */
    public <T> ResponseEntity<?> map(Result<T> result, String endpointPath) {
        if (result.success() != null) {
            return ResponseEntity.ok(result.success());
        }

        // Logic to determine HTTP status based on domain reason
        HttpStatus status = switch (result.failure().reason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case UNPROCESSABLE_CONTENT -> HttpStatus.UNPROCESSABLE_CONTENT;
        };

        // Create the standard ApiError DTO
        ApiError errorBody = new ApiError(
                status.value(),
                result.failure().reason().name(),
                result.failure().message(),
                endpointPath
        );

        return ResponseEntity.status(status).body(errorBody);
    }
}