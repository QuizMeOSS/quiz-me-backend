package com.quizme;

import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.dto.TokensDto;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.ApiError;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.utils.CookieUtil;
import com.quizme.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE))
@AutoConfigureRestTestClient
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters, this is just unit testing
class AuthControllerTest {
    @Autowired
    private RestTestClient restTestClient;
    @MockitoBean
    private ResultToResponseEntityMapper mapper;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private CookieUtil cookieUtil;

    @Test
    void register() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");
        var createdUser = new User("e", "u");
        var result = Result.success(createdUser);
        when(authService.register(requestDto)).thenReturn(result);
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
        when(authService.login(requestDto)).thenReturn(result);

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
        when(cookieUtil.createRefreshTokenCookie(any())).thenReturn(ResponseCookie.from(CookieUtil.REFRESH_TOKEN_COOKIE_NAME, "myToken").build());
        when(cookieUtil.createAccessTokenCookie(any())).thenReturn(ResponseCookie.from(CookieUtil.ACCESS_TOKEN_COOKIE_NAME, "myToken").build());
        var result = Result.success(new TokensDto("access", "refresh"));
        when(authService.login(requestDto)).thenReturn(result);

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange()
                .expectCookie()
                .exists(CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
    }

    @Test
    void login_refreshTokenStoredInCookie() {
        var requestDto = new CredentialsLoginRequestDto("email", "pw");
        when(cookieUtil.createRefreshTokenCookie(any())).thenReturn(ResponseCookie.from(CookieUtil.REFRESH_TOKEN_COOKIE_NAME, "myToken").build());
        when(cookieUtil.createAccessTokenCookie(any())).thenReturn(ResponseCookie.from(CookieUtil.ACCESS_TOKEN_COOKIE_NAME, "myToken").build());
        var result = Result.success(new TokensDto("access", "refresh"));
        when(authService.login(requestDto)).thenReturn(result);

        restTestClient.post()
                .uri("/login")
                .body(requestDto)
                .exchange()
                .expectCookie()
                .exists(CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
    }

    @Test
    void refresh_returnsBadRequest_whenNullCookies() {
        restTestClient.get()
                .uri("/refresh")
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(HttpStatus.BAD_REQUEST.value(), error.getResponseBody().status());
                    assertEquals(HttpStatus.BAD_REQUEST.name(), error.getResponseBody().error());
                    assertEquals("Missing refresh token cookie", error.getResponseBody().message());
                    assertEquals("/refresh", error.getResponseBody().path());
                });
    }

    @Test
    void refresh_returnsBadRequest_whenRefreshTokenCookieNotFound() {
        restTestClient.get()
                .uri("/refresh")
                .cookie("someOtherCookie", "abc")
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(HttpStatus.BAD_REQUEST.value(), error.getResponseBody().status());
                    assertEquals(HttpStatus.BAD_REQUEST.name(), error.getResponseBody().error());
                    assertEquals("Missing refresh token cookie", error.getResponseBody().message());
                    assertEquals("/refresh", error.getResponseBody().path());
                });
    }

    @Test
    void refresh_returnsError_whenTokenGenerationFails() {
        when(cookieUtil.getCookieValue(any(), any())).thenReturn(Optional.of(""));
        Result<TokensDto> result = Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "something wrong"));
        when(userService.refreshToken(any())).thenReturn(result);

        restTestClient.get()
                .uri("/refresh")
                .cookie(CookieUtil.REFRESH_TOKEN_COOKIE_NAME, "xyz")
                .exchange();

        verify(mapper).map(result, "/refresh");
    }

    @Test
    void refresh_refreshTokenStoredInCookie() {
        when(cookieUtil.getCookieValue(any(), any())).thenReturn(Optional.of(""));
        when(cookieUtil.createRefreshTokenCookie(any()))
                .thenReturn(ResponseCookie.from(CookieUtil.REFRESH_TOKEN_COOKIE_NAME, "refresh").build());
        when(cookieUtil.createAccessTokenCookie(any()))
                .thenReturn(ResponseCookie.from(CookieUtil.ACCESS_TOKEN_COOKIE_NAME, "access").build());
        Result<TokensDto> result = Result.success(new TokensDto("access", "refresh"));
        when(userService.refreshToken(any())).thenReturn(result);

        restTestClient.get()
                .uri("/refresh")
                .exchange()
                .expectCookie()
                .valueEquals(CookieUtil.REFRESH_TOKEN_COOKIE_NAME, "refresh");
    }

    @Test
    void refresh_accessTokenStoredInCookie() {
        when(cookieUtil.getCookieValue(any(), any())).thenReturn(Optional.of(""));
        when(cookieUtil.createRefreshTokenCookie(any()))
                .thenReturn(ResponseCookie.from(CookieUtil.REFRESH_TOKEN_COOKIE_NAME, "refresh").build());
        when(cookieUtil.createAccessTokenCookie(any()))
                .thenReturn(ResponseCookie.from(CookieUtil.ACCESS_TOKEN_COOKIE_NAME, "access").build());
        Result<TokensDto> result = Result.success(new TokensDto("access", "refresh"));
        when(userService.refreshToken(any())).thenReturn(result);

        restTestClient.get()
                .uri("/refresh")
                .exchange()
                .expectCookie()
                .valueEquals(CookieUtil.ACCESS_TOKEN_COOKIE_NAME, "access");
    }
}