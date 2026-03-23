package com.quizme.controllers;

import com.quizme.config.AppProperties;
import com.quizme.dto.ApiError;
import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.entities.User;
import com.quizme.repos.UserRepo;
import com.quizme.security.JwtUtil;
import com.quizme.services.AuthService;
import com.quizme.services.result.FailureReason;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerIntegrationTest extends IntegrationTest {
    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private AuthService registrationService;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private JwtUtil jwtUtil;

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
        userRepo.save(new User("email", "userWithNoCred"));
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

    @Test
    void refresh_TokensStoredInCookie_whenProvidedRefreshTokenIsValid() {
        registrationService.register(new RegisterCredentialsRequestDto("name", "email", "pw1"));
        var refreshToken = jwtUtil.generateRefreshToken("email");

        restTestClient.get()
                .uri("/refresh")
                .cookie("refresh_token", refreshToken)
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

    @Test
    void refresh_returnsError_whenProvidedRefreshTokenExpired() {
        registrationService.register(new RegisterCredentialsRequestDto("name", "email", "pw1"));
        var refreshToken = Jwts.builder()
                .subject("email")
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 1)) // 1 millisecond expiry
                .signWith(Keys.hmacShaKeyFor(appProperties.getAuth().getJwt().getSecret()
                        .getBytes(StandardCharsets.UTF_8)))
                .compact();

        restTestClient.get()
                .uri("/refresh")
                .cookie("refresh_token", refreshToken)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(new ApiError(HttpStatus.BAD_REQUEST.value(),
                            FailureReason.VALIDATION_FAILED.name(),
                            "Refresh token has expired",
                            "/refresh"), error.getResponseBody());
                });
    }
}