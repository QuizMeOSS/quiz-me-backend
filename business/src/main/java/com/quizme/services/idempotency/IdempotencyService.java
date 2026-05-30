package com.quizme.services.idempotency;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {
    private static final String KEY_PREFIX = "idempotency:";
    /**
     * TTL used while the request is still being processed.
     * Should be short to avoid "zombie" locks,
     * where a lock is acquired, then server crashes, but lock remains in redis for a long time.
     */
    private final static int LOCK_TTL_MINUTES = 1;
    /**
     * TTL used for the response after the request is processed.
     * Industry standard is to keep the idempotency key for a day.
     */
    private final static int KEY_TTL_DAYS = 1;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Attempts to reserve an idempotency key.
     * Returns the existing record if the key already exists, or empty if successfully reserved.
     */
    public Optional<IdempotencyRecord> tryReserve(String idempotencyKey, String payloadHash, JavaType returnType) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        var idempotencyRecord = new IdempotencyRecord(IdempotencyStatus.PROCESSING, payloadHash, null);
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, idempotencyRecord, LOCK_TTL_MINUTES, TimeUnit.MINUTES);
        if (Boolean.TRUE.equals(isNew)) {
            return Optional.empty(); // Key was free, we reserved it
        }

        // Key already exists — return the existing record for the caller to inspect
        var record = objectMapper.convertValue(
                redisTemplate.opsForValue().get(redisKey), IdempotencyRecord.class);
        // Re-deserialize response into its original concrete type
        IdempotencyRecord result;
        if (record.response() != null && returnType != null) {
            Object typedResponse = objectMapper.convertValue(record.response(), returnType);
            result = new IdempotencyRecord(record.status(), record.payloadHash(), typedResponse);
            return Optional.of(result);
        }

        return Optional.of(record);
    }

    /**
     * Stores the final response once processing is complete.
     * Subsequent duplicate requests will get this cached response.
     */
    public void storeResponse(String idempotencyKey, String payloadHash, Object response) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        var idempotencyRecord = new IdempotencyRecord(IdempotencyStatus.DONE, payloadHash, response);
        redisTemplate.opsForValue()
                .set(redisKey, idempotencyRecord, KEY_TTL_DAYS, TimeUnit.DAYS);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(KEY_PREFIX + key);
    }
}
