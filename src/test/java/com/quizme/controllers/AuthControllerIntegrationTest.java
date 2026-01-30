package com.quizme.controllers;

import com.quizme.config.AppProperties;
import com.quizme.dto.ApiError;
import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.entities.User;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.repos.UserRepo;
import com.quizme.services.RegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
class AuthControllerIntegrationTest {
    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private ResultToResponseEntityMapper mapper;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private UserRepo userRepo;

    // Clean up database after each test
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void resetDatabase() {
        jdbcTemplate.execute("DELETE FROM user_credentials");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void register_userReturned_whenUniqueUsernameAndEmail() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/register")
                .body(requestDto)
                .exchange()
                .expectBody(User.class)
                .consumeWith(user -> {
                    assertEquals("e", user.getResponseBody().getEmail());
                    assertEquals("u", user.getResponseBody().getUsername());
                });
    }

    @Test
    void register_returnsHttp409_whenUsedEmail() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/register")
                .body(requestDto)
                .exchange();

        restTestClient.post()
                .uri("/register")
                .body(new RegisterCredentialsRequestDto("u2", "e", "pw2"))
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(409, error.getResponseBody().status());
                    assertEquals("ALREADY_EXISTS", error.getResponseBody().error());
                    assertEquals("This email is already registered", error.getResponseBody().message());
                    assertEquals("/register", error.getResponseBody().path());
                });
    }

    @Test
    void register_returnsHttp409_whenUsedUsername() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/register")
                .body(requestDto)
                .exchange();

        restTestClient.post()
                .uri("/register")
                .body(new RegisterCredentialsRequestDto("u", "e2", "pw2"))
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(409, error.getResponseBody().status());
                    assertEquals("ALREADY_EXISTS", error.getResponseBody().error());
                    assertEquals("Username already in use", error.getResponseBody().message());
                    assertEquals("/register", error.getResponseBody().path());
                });
    }

    @Test
    void login_returnsHttp404_whenNoEmail() {
        var requestDto = new CredentialsLoginRequestDto("email", "pw");

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(HttpStatus.NOT_FOUND.value(), error.getResponseBody().status());
                    assertEquals("NOT_FOUND", error.getResponseBody().error());
                    assertEquals("Incorrect login data", error.getResponseBody().message());
                    assertEquals("/login", error.getResponseBody().path());
                });
    }

    @Test
    void login_returnsHttp404_whenNoAssociatedCredentials() {
        userRepo.save(new User("email", "username"));
        var requestDto = new CredentialsLoginRequestDto("email", "pw");

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(HttpStatus.NOT_FOUND.value(), error.getResponseBody().status());
                    assertEquals("NOT_FOUND", error.getResponseBody().error());
                    assertEquals("Incorrect login data", error.getResponseBody().message());
                    assertEquals("/login", error.getResponseBody().path());
                });
    }

    @Test
    void login_returnsHttp404_whenIncorrectPassword() {
        registrationService.register(new RegisterCredentialsRequestDto("name", "email", "pw1"));
        var requestDto = new CredentialsLoginRequestDto("email", "pw2");

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(HttpStatus.NOT_FOUND.value(), error.getResponseBody().status());
                    assertEquals("NOT_FOUND", error.getResponseBody().error());
                    assertEquals("Incorrect login data", error.getResponseBody().message());
                    assertEquals("/login", error.getResponseBody().path());
                });
    }

    @Test
    void login_returnsTokensInCookiesWhenValidLogin() {
        registrationService.register(new RegisterCredentialsRequestDto("name", "email", "pw1"));
        var requestDto = new CredentialsLoginRequestDto("email", "pw1");

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange()
                .expectCookie()
                .httpOnly("access_token", true)
                .expectCookie()
                .secure("access_token", true)
                .expectCookie()
                .path("access_token", "/")
                .expectCookie()
                .sameSite("access_token", "Strict")
                .expectCookie()
                .maxAge("access_token", Duration.ofSeconds(appProperties.getAuth().getJwt().getAccessTokenDuration() / 1000))
                .expectCookie()
                .httpOnly("refresh_token", true)
                .expectCookie()
                .secure("refresh_token", true)
                .expectCookie()
                .path("refresh_token", "/refresh")
                .expectCookie()
                .sameSite("refresh_token", "Strict")
                .expectCookie()
                .maxAge("refresh_token", Duration.ofSeconds(appProperties.getAuth().getJwt().getRefreshTokenDuration() / 1000));
    }
}