package com.quizme;

import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.dto.VerifyEmailRequestDto;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.ApiError;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.repos.UserCredentialsRepo;
import com.quizme.repos.UserRepo;
import com.quizme.utils.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class AuthControllerIntegrationTest extends IntegrationTest {
    @Autowired
    private AuthService registrationService;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private UserCredentialsRepo userCredentialsRepo;
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void register_userReturned_whenUniqueUsernameAndEmail() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/api/register")
                .body(requestDto)
                .exchange()
                .expectBody(User.class)
                .consumeWith(user -> {
                    Assertions.assertEquals("e", user.getResponseBody().getEmail());
                    Assertions.assertEquals("u", user.getResponseBody().getUsername());
                });
    }

    @Test
    void GIVEN_emailAlreadyExistsAndVerified_WHEN_register_RETURN_http409() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/api/register")
                .body(requestDto)
                .exchange();
        // simulate user verifying email
        var userCredentials = userCredentialsRepo.findAll().iterator().next();
        userCredentials.setEmailVerified();
        userCredentialsRepo.save(userCredentials);

        restTestClient.post()
                .uri("/api/register")
                .body(new RegisterCredentialsRequestDto("u2", "e", "pw2"))
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(409, error.getResponseBody().status());
                    Assertions.assertEquals("ALREADY_EXISTS", error.getResponseBody().error());
                    Assertions.assertEquals("This email is already registered", error.getResponseBody().message());
                    Assertions.assertEquals("/api/register", error.getResponseBody().path());
                });
    }

    @Test
    void register_WHEN_registrationSuccessful_THEN_confirmationEmailSent() throws MessagingException {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/api/register")
                .body(requestDto)
                .exchange();
        // wait for message relay to read outbox table and publish, and for consumer to process message
        verify(emailService, timeout(15000)).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void GIVEN_anEmailWasSentFewMomentsAgo_WHEN_register_RETURN_http429() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/api/register")
                .body(requestDto)
                .exchange();
        // when registering, a confirmation email is automatically scheduled
        // next register call should try resending email and fail because not enough time has passed

        restTestClient.post()
                .uri("/api/register")
                .body(new RegisterCredentialsRequestDto("u2", "e", "pw2"))
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(429, error.getResponseBody().status());
                    Assertions.assertEquals("TOO_MANY_REQUESTS", error.getResponseBody().error());
                    Assertions.assertEquals("Can't resend confirmation email now, please try again in few minutes", error.getResponseBody().message());
                    Assertions.assertEquals("/api/register", error.getResponseBody().path());
                });
    }

    @Test
    void register_returnsHttp409_whenUsedUsername() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");

        restTestClient.post()
                .uri("/api/register")
                .body(requestDto)
                .exchange();

        restTestClient.post()
                .uri("/api/register")
                .body(new RegisterCredentialsRequestDto("u", "e2", "pw2"))
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(409, error.getResponseBody().status());
                    Assertions.assertEquals("ALREADY_EXISTS", error.getResponseBody().error());
                    Assertions.assertEquals("Username already in use", error.getResponseBody().message());
                    Assertions.assertEquals("/api/register", error.getResponseBody().path());
                });
    }

    @Test
    void login_returnsHttp404_whenNoEmail() {
        var requestDto = new CredentialsLoginRequestDto("email", "pw");

        restTestClient.post()
                .uri("/api/login")
                .body(requestDto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), error.getResponseBody().status());
                    Assertions.assertEquals("NOT_FOUND", error.getResponseBody().error());
                    Assertions.assertEquals("Incorrect login data", error.getResponseBody().message());
                    Assertions.assertEquals("/api/login", error.getResponseBody().path());
                });
    }

    @Test
    void login_returnsHttp404_whenNoAssociatedCredentials() {
        userRepo.save(new User("email", "userWithNoCred"));
        var requestDto = new CredentialsLoginRequestDto("email", "pw");

        restTestClient.post()
                .uri("/api/login")
                .body(requestDto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), error.getResponseBody().status());
                    Assertions.assertEquals("NOT_FOUND", error.getResponseBody().error());
                    Assertions.assertEquals("Incorrect login data", error.getResponseBody().message());
                    Assertions.assertEquals("/api/login", error.getResponseBody().path());
                });
    }

    @Test
    void GIVEN_emailVerified_WHEN_loginWithIncorrectPass_RETURN_http404() {
        registrationService.register(new RegisterCredentialsRequestDto("name", "email", "pw1"));
        var requestDto = new CredentialsLoginRequestDto("email", "pw2");
        // simulate user verifying email
        var userCredentials = userCredentialsRepo.findAll().iterator().next();
        userCredentials.setEmailVerified();
        userCredentialsRepo.save(userCredentials);

        restTestClient.post()
                .uri("/api/login")
                .body(requestDto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), error.getResponseBody().status());
                    Assertions.assertEquals("NOT_FOUND", error.getResponseBody().error());
                    Assertions.assertEquals("Incorrect login data", error.getResponseBody().message());
                    Assertions.assertEquals("/api/login", error.getResponseBody().path());
                });
    }

    @Test
    void GIVEN_emailNotVerified_WHEN_validLogin_RETURN_http404() {
        registrationService.register(new RegisterCredentialsRequestDto("name", "email", "pw1"));

        var requestDto = new CredentialsLoginRequestDto("email", "pw1");
        restTestClient.post()
                .uri("/api/login")
                .body(requestDto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), error.getResponseBody().status());
                    Assertions.assertEquals("NOT_FOUND", error.getResponseBody().error());
                    Assertions.assertEquals("Incorrect login data", error.getResponseBody().message());
                    Assertions.assertEquals("/api/login", error.getResponseBody().path());
                });
    }

    @Test
    void GIVEN_emailVerified_WHEN_validLogin_RETURN_tokensInCookies() {
        registrationService.register(new RegisterCredentialsRequestDto("name", "email", "pw1"));
        // simulate user verifying email
        var userCredentials = userCredentialsRepo.findAll().iterator().next();
        userCredentials.setEmailVerified();
        userCredentialsRepo.save(userCredentials);

        var requestDto = new CredentialsLoginRequestDto("email", "pw1");
        restTestClient.post()
                .uri("/api/login")
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
                .uri("/api/refresh")
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
                .uri("/api/refresh")
                .cookie("refresh_token", refreshToken)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(new ApiError(HttpStatus.BAD_REQUEST.value(),
                            FailureReason.VALIDATION_FAILED.name(),
                            "Refresh token has expired",
                            "/api/refresh"), error.getResponseBody());
                });
    }

    @Test
    void GIVEN_invalidToken_WHEN_verifyEmail_RETURNS_http400() {
        registrationService.register(new RegisterCredentialsRequestDto(user.getUsername(), user.getEmail(), "pw"));
        var verificationToken = Jwts.builder()
                .subject(userCredentialsRepo.findByUserId(user).get().getId() + "")
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 100000))
                .signWith(Keys.hmacShaKeyFor("invalid_signing_key_should_cause_error_when_parsing"
                        .getBytes(StandardCharsets.UTF_8)))
                .compact();

        var dto = new VerifyEmailRequestDto(verificationToken);

        restTestClient.post()
                .uri("/api/verify-email")
                .body(dto)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    Assertions.assertEquals(new ApiError(HttpStatus.BAD_REQUEST.value(),
                            FailureReason.VALIDATION_FAILED.name(),
                            "Invalid email verification token",
                            "/api/verify-email"), error.getResponseBody());
                });
    }

    @Test
    void GIVEN_validToken_WHEN_verifyEmail_THEN_emailVerified() {
        registrationService.register(new RegisterCredentialsRequestDto(user.getUsername(), user.getEmail(), "pw"));
        var verificationToken = Jwts.builder()
                .subject(userCredentialsRepo.findByUserId(user).get().getId() + "")
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 100000))
                .signWith(Keys.hmacShaKeyFor(appProperties.getAuth().getJwt().getSecret()
                        .getBytes(StandardCharsets.UTF_8)))
                .compact();

        var dto = new VerifyEmailRequestDto(verificationToken);

        restTestClient.post()
                .uri("/api/verify-email")
                .body(dto)
                .exchange()
                .expectStatus()
                .is2xxSuccessful();

        assertTrue(userCredentialsRepo.findByUserId(user).get().isEmailVerified());
    }
}