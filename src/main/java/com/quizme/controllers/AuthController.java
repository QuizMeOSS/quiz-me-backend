package com.quizme.controllers;

import com.quizme.config.AppProperties;
import com.quizme.dto.ApiError;
import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.services.LoginService;
import com.quizme.services.RegistrationService;
import com.quizme.services.UserService;
import com.quizme.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

@RestController
public class AuthController {

    private final ResultToResponseEntityMapper responseMapper;
    private final LoginService loginService;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final CookieUtil cookieUtil;

    public AuthController(ResultToResponseEntityMapper responseMapper,
                          LoginService loginService,
                          RegistrationService registrationService,
                          UserService userService,
                          CookieUtil cookieUtil) {
        this.responseMapper = responseMapper;
        this.loginService = loginService;
        this.registrationService = registrationService;
        this.userService = userService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterCredentialsRequestDto body, HttpServletRequest request) {
        var result = registrationService.register(body);
        return responseMapper.map(result, request.getRequestURI());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CredentialsLoginRequestDto body, HttpServletRequest request) {
        var result = loginService.login(body);
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
    public ResponseEntity<?> refresh(HttpServletRequest request){
        ApiError noTokenError = new ApiError(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "Missing refresh token cookie",
                request.getRequestURI());

        var refreshTokenOpt = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
        if(refreshTokenOpt.isEmpty()){
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
