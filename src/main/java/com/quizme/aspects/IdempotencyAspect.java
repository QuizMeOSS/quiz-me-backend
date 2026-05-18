package com.quizme.aspects;

import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.services.idempotency.IdempotencyRecord;
import com.quizme.services.idempotency.IdempotencyService;
import com.quizme.services.idempotency.IdempotencyStatus;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.security.MessageDigest;
import java.util.HexFormat;
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
        Object idempotencyKey = extractArg(joinPoint, idempotent.paramName());
        if (idempotencyKey == null || idempotencyKey.toString().isEmpty()) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Idempotency-Key is missing"));
        }

        Object payload = extractArg(joinPoint, idempotent.payload());
        String payloadHash = hash(payload);
        JavaType returnType = extractReturnType(joinPoint);

        Optional<IdempotencyRecord> existing = idempotencyService.tryReserve(idempotencyKey.toString(), payloadHash, returnType);
        if (existing.isEmpty()) {
            // First time — key was free, proceed normally
            try {
                Object result = joinPoint.proceed();
                idempotencyService.storeResponse(idempotencyKey.toString(), payloadHash, result);
                return result;
            } catch (Exception e) {
                idempotencyService.deleteKey(idempotencyKey.toString());
                throw e;
            }
        }

        var idempotencyRecord = existing.get();

        // Same key, different payload -> reject immediately
        if (!idempotencyRecord.payloadHash().equals(payloadHash)) {
            return Result.failure(new Failure(FailureReason.UNPROCESSABLE_CONTENT,
                    "Idempotency key '" + idempotencyKey + "' was already used with a different payload."));
        }

        // Same key, same payload, still processing -> conflict
        if (IdempotencyStatus.PROCESSING.equals(idempotencyRecord.status())) {
            return Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Request is already being processed"));
        }

        // Same key, same payload, already done -> return cached response
        return idempotencyRecord.response();
    }

    @Nullable
    private Object extractArg(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                return args[i];
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

    @NonNull
    private String hash(Object payload) {
        try {
            ObjectMapper mapper = JsonMapper.builder()
                    // Sort keys for consistent hashing regardless of field order
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .build();
            byte[] serialized = mapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(serialized);
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash payload", e);
        }
    }
}
