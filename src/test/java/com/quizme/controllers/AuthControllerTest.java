package com.quizme.controllers;

import com.quizme.config.AppProperties;
import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.TokensDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.entities.User;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.security.JwtUtil;
import com.quizme.services.LoginService;
import com.quizme.services.RegistrationService;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@AutoConfigureRestTestClient
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters, this is just unit testing
class AuthControllerTest {
    @Autowired
    private RestTestClient restTestClient;
    @MockitoBean
    private ResultToResponseEntityMapper mapper;
    @MockitoBean
    private RegistrationService registrationService;
    @MockitoBean
    private LoginService loginService;
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS) // avoid mocking Auth() and Jwt() explicitly
    private AppProperties appProperties;
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void register() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");
        var createdUser = new User("e", "u");
        var result = Result.success(createdUser);
        when(registrationService.register(requestDto)).thenReturn(result);
        when(mapper.map(result, "/register"))
                .thenAnswer(_ ->
                        ResponseEntity.ok(createdUser)
                );

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
    void login_failureIsMappedToApiError() {
        var requestDto = new CredentialsLoginRequestDto("email", "pw");
        Result<TokensDto> result = Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data"));
        when(loginService.login(requestDto)).thenReturn(result);

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange();

        // verify the mapper was invoked to map the response to ApiError
        verify(mapper).map(result, "/login");

    }

    @Test
    void login_accessTokenStoredInCookie() {
        var requestDto = new CredentialsLoginRequestDto("email", "pw");
        // make access token duration 1 second (1000 millis)
        when(appProperties.getAuth().getJwt().getAccessTokenDuration()).thenReturn(1000L);
        var result = Result.success(new TokensDto("access", "refresh"));
        when(loginService.login(requestDto)).thenReturn(result);

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
                .maxAge("access_token", Duration.ofSeconds(1));
    }

    @Test
    void login_refreshTokenStoredInCookie() {
        var requestDto = new CredentialsLoginRequestDto("email", "pw");
        // make refresh token duration 10 seconds (10000 millis)
        when(appProperties.getAuth().getJwt().getRefreshTokenDuration()).thenReturn(10000L);
        var result = Result.success(new TokensDto("access", "refresh"));
        when(loginService.login(requestDto)).thenReturn(result);

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange()
                .expectCookie()
                .httpOnly("refresh_token", true)
                .expectCookie()
                .secure("refresh_token", true)
                .expectCookie()
                .path("refresh_token", "/refresh")
                .expectCookie()
                .sameSite("refresh_token", "Strict")
                .expectCookie()
                .maxAge("refresh_token", Duration.ofSeconds(10));
    }
}