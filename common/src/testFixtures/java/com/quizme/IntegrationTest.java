package com.quizme;

import com.quizme.entities.User;
import com.quizme.repos.UserRepo;
import com.quizme.utils.CookieUtil;
import com.quizme.utils.JwtUtil;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@AutoConfigureRestTestClient
@SpringBootTest(classes = QuizmeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTest {
    static RedisContainer redis;
    static PostgreSQLContainer<?> postgres;
    static KafkaContainer kafka;
    @Autowired
    protected RestTestClient restTestClient;
    @Autowired
    protected UserRepo userRepo;
    @Autowired
    protected JwtUtil jwtUtil;
    // Clean up database after each test
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    protected User user;
    protected String accessToken;
    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.kafka.url", kafka::getBootstrapServers);
    }

    @BeforeAll
    static void beforeAll() {
        postgres = new PostgreSQLContainer<>(
                "postgres:18-alpine"
        ).withReuse(true); // avoid recreating container for each child class
        postgres.start();

        redis = new RedisContainer(DockerImageName.parse("redis:6.2.6"))
                .withReuse(true);
        redis.start();

        kafka = new KafkaContainer("apache/kafka-native:4.3.1");
        kafka.start();
    }

    @BeforeEach
    void setup() {
        // clear cookies
        restTestClient = restTestClient.mutate()
                .defaultCookies(cookies -> {
                    cookies.remove(CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
                    cookies.remove(CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
                })
                .build();

        user = userRepo.save(new User("user@email.com", "username"));
        accessToken = jwtUtil.generateAccessToken(user.getEmail());
    }

    @AfterEach
    public void clear() {
        // reset database
        jdbcTemplate.execute("DELETE FROM user_credentials");
        jdbcTemplate.execute("DELETE FROM users");
        // reset id sequence
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");

        // clear all redis keys
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }
}
