package com.quizme.services;

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
     * Returns true if this is the FIRST time we've seen this key (proceed with request).
     * Returns false if the key already exists (duplicate request).
     */
    public boolean tryReserve(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", LOCK_TTL_MINUTES, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(isNew);
    }

    /**
     * Stores the final response once processing is complete.
     * Subsequent duplicate requests will get this cached response.
     */
    public void storeResponse(String idempotencyKey, Object response) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue()
                .set(redisKey, response, KEY_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * Retrieves a previously cached response, if any.
     */
    public Optional<Object> getResponse(String idempotencyKey, JavaType targetType) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        Object value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) return Optional.empty();
        // deserialize the LinkedHashMap to correct type
        return Optional.of(objectMapper.convertValue(value, targetType));
    }

    /**
     * Checks if a key is still in PROCESSING state (i.e. first request hasn't finished yet).
     * If request is still being processed, then we have no cached response yet, and
     * we shouldn't accept any more requests with the same key.
     */
    public boolean isProcessing(String idempotencyKey) {
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
        return "PROCESSING".equals(value);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }
}
