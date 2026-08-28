package com.quizme.services.cache.idempotency;

import com.quizme.IntegrationTest;
import com.quizme.dto.QuestionChoiceDto;
import com.quizme.dto.QuestionDto;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.idempotency.IdempotencyRecord;
import com.quizme.idempotency.IdempotencyService;
import com.quizme.idempotency.IdempotencyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IdempotencyService} that uses redis docker container.
 */
class IdempotencyServiceTest extends IntegrationTest {
    @Autowired
    ObjectMapper objectMapper;

    IdempotencyService idempotencyService;

    @BeforeEach
    void setup() {
        idempotencyService = new IdempotencyService(redisTemplate, objectMapper);
        // Clean up all idempotency keys before each test to ensure isolation
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    void deleteKey_deletesKey() {
        redisTemplate.opsForValue()
                .set(key("test"), "value");
        assertNotNull(redisTemplate.opsForValue().get(key("test")));

        idempotencyService.deleteKey("test");
        assertNull(redisTemplate.opsForValue().get(key("test")));
    }

    @Test
    void deleteKey_doesNotThrow_whenKeyDoesNotExist() {
        assertDoesNotThrow(() -> idempotencyService.deleteKey("nonexistent"));
    }

    @Test
    void tryReserve_WHEN_keyIsNew_RETURN_empty() {
        Optional<IdempotencyRecord> result =
                idempotencyService.tryReserve("new-key", "hash123", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void tryReserve_WHEN_keyIsNew_THEN_setsProcessingStatus() {
        idempotencyService.tryReserve("new-key", "hash123", objectMapper.constructType(String.class));

        IdempotencyRecord stored =
                objectMapper.convertValue(redisTemplate.opsForValue().get(key("new-key")), IdempotencyRecord.class);

        assertNotNull(stored);
        assertEquals(IdempotencyStatus.PROCESSING, stored.status());
        assertEquals("hash123", stored.payloadHash());
        assertNull(stored.response());
    }

    @Test
    void tryReserve_WHEN_keyAlreadyExists_RETURN_existingRecord() {
        idempotencyService.tryReserve("dup-key", "hash123", null);
        // Try to reserve it again
        Optional<IdempotencyRecord> result =
                idempotencyService.tryReserve("dup-key", "hash123", null);

        assertTrue(result.isPresent());
        assertEquals(IdempotencyStatus.PROCESSING, result.get().status());
        assertEquals("hash123", result.get().payloadHash());
    }

    @Test
    void tryReserve_WHEN_responseIsNullAndReturnTypeProvided_RETURN_recordWithNullResponse() {
        // Key exists but response is null (still PROCESSING)
        var record = new IdempotencyRecord(IdempotencyStatus.PROCESSING, "hash123", null);
        redisTemplate.opsForValue().set(key("proc-key"), record);

        JavaType returnType = objectMapper.getTypeFactory()
                .constructType(Result.class);

        Optional<IdempotencyRecord> result =
                idempotencyService.tryReserve("proc-key", "hash123", returnType);

        assertTrue(result.isPresent());
        assertEquals(IdempotencyStatus.PROCESSING, result.get().status());
        assertNull(result.get().response());
    }

    @Test
    void tryReserve_WHEN_returnTypeIsNull_RETURN_recordWithRawResponse() {
        Result<QuestionDto> mockResponse = Result.success(new QuestionDto(1,
                "q",
                Set.of(new QuestionChoiceDto(1, "c", true)), Set.of(1L), LocalDateTime.now()));
        var record = new IdempotencyRecord(IdempotencyStatus.DONE, "hash456", mockResponse);
        redisTemplate.opsForValue().set(key("raw-key"), record);

        Optional<IdempotencyRecord> result =
                idempotencyService.tryReserve("raw-key", "hash456", null);

        assertTrue(result.isPresent());
        assertEquals(IdempotencyStatus.DONE, result.get().status());
        // response is present but not cast to a concrete type
        assertNotNull(result.get().response());
    }

    @Test
    void tryReserve_RETURNS_recordWithCastedResponse() {
        Result<QuestionDto> mockResponse = Result.success(new QuestionDto(1,
                "q",
                Set.of(new QuestionChoiceDto(1, "c", true)), Set.of(1L), LocalDateTime.now()));
        var record = new IdempotencyRecord(IdempotencyStatus.DONE, "hash456", mockResponse);
        redisTemplate.opsForValue().set(key("raw-key"), record);

        Optional<IdempotencyRecord> result =
                idempotencyService.tryReserve("raw-key", "hash456", objectMapper.getTypeFactory()
                        .constructParametricType(Result.class, QuestionDto.class));

        assertTrue(result.isPresent());
        assertEquals(IdempotencyStatus.DONE, result.get().status());
        assertInstanceOf(Result.class, result.get().response());
        assertInstanceOf(QuestionDto.class, ((Result<?>) result.get().response()).success());
        assertEquals("q", ((QuestionDto) ((Result<?>) result.get().response()).success()).question());
        assertEquals(Set.of(new QuestionChoiceDto(1, "c", true)),
                ((QuestionDto) ((Result<?>) result.get().response()).success()).choices());
        assertEquals(Set.of(1L), ((QuestionDto) ((Result<?>) result.get().response()).success()).categories());
    }

    @Test
    void storeResponse_storesRecordWithDoneStatus() {
        Result<String> response = Result.success("result");

        idempotencyService.storeResponse("store-key", "hash789", response);

        IdempotencyRecord stored = objectMapper.convertValue(
                redisTemplate.opsForValue().get(key("store-key")), IdempotencyRecord.class);

        assertNotNull(stored);
        assertEquals(IdempotencyStatus.DONE, stored.status());
        assertEquals("hash789", stored.payloadHash());
        assertNotNull(stored.response());
    }

    @Test
    void storeResponse_overwritesExistingProcessingRecord() {
        // Simulate a lock placed by tryReserve
        idempotencyService.tryReserve("overwrite-key", "hashABC", null);

        // Now store the final response
        idempotencyService.storeResponse("overwrite-key", "hashABC", Result.success("final"));

        IdempotencyRecord stored = objectMapper.convertValue(
                redisTemplate.opsForValue().get(key("overwrite-key")), IdempotencyRecord.class);

        assertNotNull(stored);
        assertEquals(IdempotencyStatus.DONE, stored.status());
        assertNotNull(stored.response());
    }

    @Test
    void storeResponse_keyExpiresAfter1day() {
        idempotencyService.storeResponse("ttl-key", "hashTTL", Result.success("x"));

        long ttl = redisTemplate.getExpire(key("ttl-key"), TimeUnit.HOURS);
        // 23 not 24, the number is floored
        assertEquals(23, ttl);
    }

    private String key(String baseKey) {
        return "idempotency:" + baseKey;
    }
}