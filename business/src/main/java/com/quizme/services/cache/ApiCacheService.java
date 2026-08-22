package com.quizme.services.cache;

import com.quizme.idempotency.CacheableResults;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Caching user-specific results is done here using redis for learning only.
 * In real system, I believe it would make more sense to cache on client side.
 * That would be more cost-effective.
 */
@Service
public class ApiCacheService {

    private static final String API_KEY_PREFIX = "api";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ApiCacheService(RedisTemplate<String, Object> redisTemplate,
                           ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Used to get cached result for simple, non-generic types.
     */
    @NonNull
    public <T> Optional<T> get(CacheableResults feature, String userId, Class<T> resultType) {
        return getAndConvert(feature, userId,
                rawValue -> objectMapper.convertValue(rawValue, resultType));
    }

    /**
     * Used to get cached result that is a generic (list, set, etc.)
     */
    @NonNull
    public <T> Optional<T> get(CacheableResults feature, String userId, JavaType resultType) {
        return getAndConvert(feature, userId,
                rawValue -> objectMapper.convertValue(rawValue, resultType));
    }

    private <T> Optional<T> getAndConvert(CacheableResults feature, String userId, Function<Object, T> converter) {
        String cacheKey = buildKey(feature, userId);
        Object rawValue = redisTemplate.opsForValue().get(cacheKey);

        if (rawValue == null) {
            LOGGER.info("Cache miss -  {} for user {}", feature.name(), userId);
            return Optional.empty();
        }

        LOGGER.info("Cache hit - {} for user {}", feature.name(), userId);
        return Optional.of(converter.apply(rawValue));
    }

    /**
     * Stores object in cache with "api:" namespace.
     * This is an async operation because users don't need to wait for caching to finish.
     */
    @Async
    public void storeResult(CacheableResults feature, String userId, Object result, Duration ttl) {
        String cacheKey = buildKey(feature, userId);
        redisTemplate.opsForValue().set(cacheKey, result, ttl);
        LOGGER.info("Cached {} for user {}", feature.name(), userId);
    }

    /**
     * Removes a key from the cache. The key to remove must be in "api:" namespace.
     * This is an async operation because users don't need to wait for invalidation to finish.
     */
    @Async
    public void invalidate(CacheableResults feature, String userId) {
        var invalidated = redisTemplate.delete(buildKey(feature, userId));
        if (invalidated) {
            LOGGER.info("Invalidated {} for user {}", feature.name(), userId);
        } else {
            LOGGER.info("Failed to invalidate {} for user {}", feature.name(), userId);
        }
    }

    private String buildKey(CacheableResults feature, String userId) {
        return String.format("%s:%s:%s", API_KEY_PREFIX, feature.name().toLowerCase(), userId);
    }

    @NonNull
    public <T> JavaType createListType(Class<T> elementClass) {
        return objectMapper.getTypeFactory()
                .constructCollectionType(List.class, elementClass);
    }
}
