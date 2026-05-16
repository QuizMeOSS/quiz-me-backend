package com.quizme.aspects;

import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.services.IdempotencyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Aspect
@Component
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper redisObjectMapper;

    public IdempotencyAspect(IdempotencyService idempotencyService,
                             ObjectMapper redisObjectMapper) {
        this.idempotencyService = idempotencyService;
        this.redisObjectMapper = redisObjectMapper;
    }

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent)
            throws Throwable {

        // Extract idempotency key from method argument
        String key = extractIdempotencyKey(joinPoint, idempotent.paramName());
        if (key == null || key.isEmpty()) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Idempotency-Key is missing"));
        }

        // Build the JavaType from the method's return type
        JavaType returnType = extractReturnType(joinPoint);

        // Check for a completed cached response
        Optional<Object> cached = idempotencyService.getResponse(key, returnType);
        if (cached.isPresent() && !"PROCESSING".equals(cached.get())) {
            return cached.get();
        }

        // Detect concurrent duplicate requests
        if (idempotencyService.isProcessing(key)) {
            return Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Request is already being processed"));
        }

        // Reserve the key and process
        if (!idempotencyService.tryReserve(key)) {
            return Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Duplicate request detected for key: " + key));
        }

        try {
            Object result = joinPoint.proceed();
            idempotencyService.storeResponse(key, result); // Cache the result
            return result;
        } catch (Exception e) {
            // On failure, delete the key so the client can safely retry
            idempotencyService.deleteKey(key);
            throw e;
        }
    }

    @Nullable
    private String extractIdempotencyKey(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                return args[i] != null ? args[i].toString() : null;
            }
        }
        return null;
    }

    private JavaType extractReturnType(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // getGenericReturnType() preserves generic parameters e.g. Result<Quiz>
        // whereas getReturnType() erases them to Result
        var genericReturnType = signature.getMethod().getGenericReturnType();
        return redisObjectMapper.getTypeFactory().constructType(genericReturnType);
    }
}
