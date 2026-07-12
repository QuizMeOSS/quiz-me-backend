package com.quizme;

import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.exceptionhandler.ApiError;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final ResultToResponseEntityMapper responseMapper;
    private final AuthService authService;
    private final com.quizme.services.UserService userService;
    private final CookieUtil cookieUtil;

    public AuthController(ResultToResponseEntityMapper responseMapper,
                          AuthService authService,
                          com.quizme.services.UserService userService,
                          CookieUtil cookieUtil) {
        this.responseMapper = responseMapper;
        this.authService = authService;
        this.userService = userService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterCredentialsRequestDto body, HttpServletRequest request) {
        var result = authService.register(body);
        return responseMapper.map(result, request.getRequestURI());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CredentialsLoginRequestDto body, HttpServletRequest request) {
        var result = authService.login(body);
        if (result.failure() != null) {
            return responseMapper.map(result, request.getRequestURI());
        }

        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(result.success().refreshToken());
        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(result.success().accessToken());
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        ApiError noTokenError = new ApiError(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "Missing refresh token cookie",
                request.getRequestURI());

        var refreshTokenOpt = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
        if (refreshTokenOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(noTokenError);
        }
        var result = userService.refreshToken(refreshTokenOpt.get());
        if (result.failure() != null) {
            return responseMapper.map(result, request.getRequestURI());
        }
        var refreshCookie = cookieUtil.createRefreshTokenCookie(result.success().refreshToken());
        var accessCookie = cookieUtil.createAccessTokenCookie(result.success().accessToken());
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }
}
