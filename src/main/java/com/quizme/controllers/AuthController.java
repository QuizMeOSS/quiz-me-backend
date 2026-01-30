package com.quizme.controllers;

import com.quizme.config.AppProperties;
import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.services.LoginService;
import com.quizme.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final ResultToResponseEntityMapper responseMapper;
    private final LoginService loginService;
    private final RegistrationService registrationService;
    private final AppProperties appProperties;

    public AuthController(ResultToResponseEntityMapper responseMapper,
                          LoginService loginService,
                          RegistrationService registrationService,
                          AppProperties appProperties) {
        this.responseMapper = responseMapper;
        this.loginService = loginService;
        this.registrationService = registrationService;
        this.appProperties = appProperties;
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

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", result.success().refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/refresh") // No need to send it except for /refresh endpoint
                .maxAge(appProperties.getAuth().getJwt().getRefreshTokenDuration() / 1000)
                .sameSite("Strict")
                .build();

        ResponseCookie accessCookie = ResponseCookie.from("access_token", result.success().accessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(appProperties.getAuth().getJwt().getAccessTokenDuration() / 1000)
                .sameSite("Strict")
                .build();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }
}
