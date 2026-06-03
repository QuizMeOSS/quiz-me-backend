package com.quizme.exceptionhandler;

import com.quizme.exceptionhandler.result.FailureReason;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class AppExceptionHandlerTest {

    private AppExceptionHandler handler;

    @BeforeEach
    public void setup() {
        handler = new AppExceptionHandler();
    }

    @Test
    void inputValidator_RETURNS_400_status() {
        // arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("", "", "");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                bindingResult
        );

        // act
        ResponseEntity<ApiError> response = handler.handleInputValidationExceptions(ex, mock(HttpServletRequest.class));

        // assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals(FailureReason.VALIDATION_FAILED.name(), response.getBody().error());
    }

    @Test
    void inputValidator_RETURNS_validationError() {
        // arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("", "", "");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                bindingResult
        );

        // act
        ResponseEntity<ApiError> response = handler.handleInputValidationExceptions(ex, mock(HttpServletRequest.class));

        // assert
        assertEquals(FailureReason.VALIDATION_FAILED.name(), response.getBody().error());
    }

    @Test
    void inputValidator_RETURNS_errorMessage() {
        // arrange
        var errorMessage = "Some Error Message";
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("", "", errorMessage);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                bindingResult
        );

        // act
        ResponseEntity<ApiError> response = handler.handleInputValidationExceptions(ex, mock(HttpServletRequest.class));

        // assert
        assertEquals("Invalid request parameters: " + errorMessage, response.getBody().message());
    }

    @Test
    void inputValidator_RETURNS_endpointURI() {
        // arrange
        var uri = "/api/test";
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("", "", "");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                bindingResult
        );

        var mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn(uri);

        // act
        ResponseEntity<ApiError> response = handler.handleInputValidationExceptions(ex, mockRequest);

        // assert
        assertEquals(uri, response.getBody().path());
    }

    @Test
    void inputValidator_RETURNS_concatenatedErrors() {
        // arrange
        var message1 = "Some Error Message";
        var message2 = "Another message";
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("", "", message1);
        FieldError fieldError2 = new FieldError("", "", message2);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                bindingResult
        );

        // act
        ResponseEntity<ApiError> response = handler.handleInputValidationExceptions(ex, mock(HttpServletRequest.class));

        // assert
        assertEquals("Invalid request parameters: " + message1 + " & " + message2,
                response.getBody().message());
    }
}