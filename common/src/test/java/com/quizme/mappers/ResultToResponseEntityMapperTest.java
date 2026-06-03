package com.quizme.mappers;

import com.quizme.exceptionhandler.ApiError;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultToResponseEntityMapperTest {
    private final ResultToResponseEntityMapper mapper = new ResultToResponseEntityMapper();

    @Test
    void map_with_NOT_FOUND_error() {
        var errorMsg = "Smth Not Found";
        var endpoint = "/endpoint";
        var mappingOutput = mapper.map(Result.failure(new Failure(FailureReason.NOT_FOUND, errorMsg)),
                endpoint);

        assertEquals(
                ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", errorMsg, endpoint)),
                mappingOutput);
    }

    @Test
    void map_with_VALIDATION_FAILED_error() {
        var errorMsg = "Some validation failed";
        var endpoint = "/endpoint2";
        var mappingOutput = mapper.map(Result.failure(new Failure(FailureReason.VALIDATION_FAILED, errorMsg)),
                endpoint);

        assertEquals(
                ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", errorMsg, endpoint)),
                mappingOutput);
    }

    @Test
    void map_with_ALREADY_EXISTS_error() {
        var errorMsg = "used";
        var endpoint = "/new";
        var mappingOutput = mapper.map(Result.failure(new Failure(FailureReason.ALREADY_EXISTS, errorMsg)),
                endpoint);

        assertEquals(
                ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new ApiError(HttpStatus.CONFLICT.value(), "ALREADY_EXISTS", errorMsg, endpoint)),
                mappingOutput);
    }

    @Test
    void map_with_success_response() {
        var endpoint = "/new";
        var data = List.of("A", "B");
        var mappingOutput = mapper.map(Result.success(data),
                endpoint);

        assertEquals(
                ResponseEntity
                        .ok(data),
                mappingOutput);
    }

}