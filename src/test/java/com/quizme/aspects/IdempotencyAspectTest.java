package com.quizme.aspects;

import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.services.idempotency.IdempotencyRecord;
import com.quizme.services.idempotency.IdempotencyService;
import com.quizme.services.idempotency.IdempotencyStatus;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    public static final String IDEMPOTENCY_KEY = "uid-some-key";
    public static final String PAYLOAD_PARAM = "myPayload";
    public static final String IDEMPOTENCY_PARAM = "idempotencyKey";
    public static final String PAYLOAD = "b";
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private ProceedingJoinPoint joinPoint;

    private IdempotencyAspect idempotencyAspect;

    @BeforeEach
    void setup() {
        var redisObjectMapper = JsonMapper.builder()
                .build();
        idempotencyAspect = new IdempotencyAspect(idempotencyService,
                redisObjectMapper);
    }

    @Test
    void WHEN_noKeyInMethodSignature_RETURN_validationFailed() throws Throwable {
        var mockSignature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{PAYLOAD_PARAM});
        when(joinPoint.getArgs()).thenReturn(new Object[]{PAYLOAD});

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Idempotency-Key is missing"))
                , result);
    }

    @Test
    void WHEN_nullKey_RETURN_validationFailed() throws Throwable {
        var mockSignature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(joinPoint.getArgs()).thenReturn(new Object[]{null, PAYLOAD});

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Idempotency-Key is missing"))
                , result);
    }

    @Test
    void WHEN_emptyKey_RETURN_validationFailed() throws Throwable {
        var mockSignature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"", PAYLOAD});

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Idempotency-Key is missing"))
                , result);
    }

    @Test
    void WHEN_newIdempotentRequest_RETURN_serviceResponse() throws Throwable {
        var mockSignature = mock(MethodSignature.class, Answers.RETURNS_DEEP_STUBS);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(mockSignature.getMethod().getGenericReturnType()).thenReturn(new ObjectMapper().getTypeFactory()
                .constructParametricType(Result.class, String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[]{IDEMPOTENCY_KEY, PAYLOAD});
        when(idempotencyService.tryReserve(any(), any(), any())).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn(Result.success("success"));

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());

        assertEquals(Result.success("success"), result);
    }

    @Test
    void WHEN_newIdempotentRequest_THEN_serviceResponseCached() throws Throwable {
        var mockSignature = mock(MethodSignature.class, Answers.RETURNS_DEEP_STUBS);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(mockSignature.getMethod().getGenericReturnType()).thenReturn(new ObjectMapper().getTypeFactory()
                .constructParametricType(Result.class, String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[]{IDEMPOTENCY_KEY, PAYLOAD});
        when(idempotencyService.tryReserve(any(), any(), any())).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn(Result.success("success"));

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());

        verify(idempotencyService).storeResponse(eq(IDEMPOTENCY_KEY), anyString(), eq(result));
    }

    @Test
    void WHEN_newIdempotentRequestThrowsException_THEN_idempotencyKeyDeleted() throws Throwable {
        var mockSignature = mock(MethodSignature.class, Answers.RETURNS_DEEP_STUBS);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(mockSignature.getMethod().getGenericReturnType()).thenReturn(new ObjectMapper().getTypeFactory()
                .constructParametricType(Result.class, String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[]{IDEMPOTENCY_KEY, PAYLOAD});
        when(idempotencyService.tryReserve(any(), any(), any())).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenThrow(new RuntimeException());

        try {
            idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());
        } catch (RuntimeException _) {
            verify(idempotencyService).deleteKey(IDEMPOTENCY_KEY);
        }
    }

    @Test
    void WHEN_duplicateKeyWithSamePayload_RETURN_conflict() throws Throwable {
        var mockSignature = mock(MethodSignature.class, Answers.RETURNS_DEEP_STUBS);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(mockSignature.getMethod().getGenericReturnType()).thenReturn(new ObjectMapper().getTypeFactory()
                .constructParametricType(Result.class, String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[]{IDEMPOTENCY_KEY, PAYLOAD});
        when(idempotencyService.tryReserve(any(), any(), any()))
                .thenReturn(Optional.of(
                        new IdempotencyRecord(IdempotencyStatus.PROCESSING, hash(PAYLOAD), null)
                ));

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());
        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Request is already being processed")),
                result);
    }

    @Test
    void WHEN_duplicateKeyWithDifferentPayload_RETURN_unprocessableContent() throws Throwable {
        var mockSignature = mock(MethodSignature.class, Answers.RETURNS_DEEP_STUBS);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(mockSignature.getMethod().getGenericReturnType()).thenReturn(new ObjectMapper().getTypeFactory()
                .constructParametricType(Result.class, String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[]{IDEMPOTENCY_KEY, PAYLOAD});
        when(idempotencyService.tryReserve(any(), any(), any()))
                .thenReturn(Optional.of(
                        new IdempotencyRecord(IdempotencyStatus.PROCESSING, "diffPayload", null)
                ));

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());
        assertEquals(Result.failure(new Failure(FailureReason.UNPROCESSABLE_CONTENT,
                        "Idempotency key '" + IDEMPOTENCY_KEY + "' was already used with a different payload.")),
                result);
    }

    @Test
    void WHEN_duplicateKeySamePayloadAndRequestProcessed_RETURN_cachedResponse() throws Throwable {
        var mockSignature = mock(MethodSignature.class, Answers.RETURNS_DEEP_STUBS);
        when(joinPoint.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getParameterNames()).thenReturn(new String[]{IDEMPOTENCY_PARAM, PAYLOAD_PARAM});
        when(mockSignature.getMethod().getGenericReturnType()).thenReturn(new ObjectMapper().getTypeFactory()
                .constructParametricType(Result.class, String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[]{IDEMPOTENCY_KEY, PAYLOAD});
        var cachedResponse = "success";
        when(idempotencyService.tryReserve(any(), any(), any()))
                .thenReturn(Optional.of(
                        new IdempotencyRecord(IdempotencyStatus.DONE, hash(PAYLOAD), cachedResponse)
                ));

        var result = idempotencyAspect.handleIdempotency(joinPoint, getIdempotent());
        assertEquals(cachedResponse, result);
    }

    private Idempotent getIdempotent() {
        return new Idempotent() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String keyName() {
                return IDEMPOTENCY_PARAM;
            }

            @Override
            public String payload() {
                return PAYLOAD_PARAM;
            }
        };
    }

    private String hash(Object o) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = IdempotencyAspect.class.getDeclaredMethod("hash", Object.class);
        method.setAccessible(true);
        return method.invoke(idempotencyAspect, o).toString();
    }
}