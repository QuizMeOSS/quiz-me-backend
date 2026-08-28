package com.quizme.services.cache;

import com.quizme.IntegrationTest;
import com.quizme.entities.Category;
import com.quizme.entities.Quiz;
import com.quizme.idempotency.CacheableResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ApiCacheService} that uses redis docker container.
 */
class ApiCacheServiceTest extends IntegrationTest {
    @Autowired
    ObjectMapper objectMapper;

    ApiCacheService cacheService;

    @BeforeEach
    void setup() {
        cacheService = new ApiCacheService(redisTemplate, objectMapper);
        // Clean up all cached keys before each test to ensure isolation
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    void invalidates_deletesKey() {
        // arrange
        redisTemplate.opsForValue()
                .set(key(CacheableResults.CATEGORY, "1"), "value");
        redisTemplate.opsForValue()
                .set(key(CacheableResults.CATEGORY, "2"), "value");
        assertNotNull(redisTemplate.opsForValue().get(key(CacheableResults.CATEGORY, "1")));
        assertNotNull(redisTemplate.opsForValue().get(key(CacheableResults.CATEGORY, "2")));

        // act - invalidate user 1 key only and leave user 2
        cacheService.invalidate(CacheableResults.CATEGORY, "1");

        // assert
        assertNull(redisTemplate.opsForValue().get(key(CacheableResults.CATEGORY, "1")));
        assertNotNull(redisTemplate.opsForValue().get(key(CacheableResults.CATEGORY, "2")));
    }

    @Test
    void storeResult_savesKeyValuePair() {
        // arrange
        var quiz = new Quiz(user);

        // act
        cacheService.storeResult(CacheableResults.CATEGORY, "1", quiz, Duration.ofDays(1));

        // assert
        var retrievedQuiz = cacheService.get(CacheableResults.CATEGORY, "1", Quiz.class);
        assertEquals(quiz, retrievedQuiz.get());
    }

    @Test
    void get_WHEN_keyNotFound_RETURNS_empty() {
        // arrange
        var quiz = new Quiz(user);

        // act
        cacheService.storeResult(CacheableResults.CATEGORY, "1", quiz, Duration.ofDays(1));

        // assert
        var retrievedQuiz = cacheService.get(CacheableResults.CATEGORY, "nonExisting", Quiz.class);
        assertTrue(retrievedQuiz.isEmpty());
    }

    @Test
    void get_canStoreGenericTypes() {
        // arrange
        var map = new HashMap<String, List<Category>>();
        map.put("1", List.of(new Category(user.getId(), "C1")));
        map.put("2", List.of(new Category(user.getId(), "C2")));

        var listType = cacheService.createListType(Category.class);
        var stringKeyType = objectMapper.constructType(String.class);
        var mapType = objectMapper.getTypeFactory()
                .constructMapType(Map.class, stringKeyType, listType);

        // act
        cacheService.storeResult(CacheableResults.CATEGORY, "u1", map, Duration.ofHours(2));

        // assert
        var retrievedMap = cacheService.get(CacheableResults.CATEGORY, "u1", mapType);
        assertEquals(map, retrievedMap.get());
    }

    @Test
    void itemClearedAfterTTL() throws InterruptedException {
        // arrange
        var quiz = new Quiz(user);

        // act
        cacheService.storeResult(CacheableResults.CATEGORY, "1", quiz, Duration.ofSeconds(3));

        // assert
        var retrievedQuiz = cacheService.get(CacheableResults.CATEGORY, "1", Quiz.class);
        assertEquals(quiz, retrievedQuiz.get());

        Thread.sleep(3100); // extra 100ms buffer

        retrievedQuiz = cacheService.get(CacheableResults.CATEGORY, "1", Quiz.class);
        assertTrue(retrievedQuiz.isEmpty());
    }

    private String key(CacheableResults feature, String userId) {
        return "api:" + feature.name().toLowerCase() + ":" + userId;
    }
}