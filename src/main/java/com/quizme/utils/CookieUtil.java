package com.quizme.utils;

import com.quizme.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class CookieUtil {
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private final AppProperties appProperties;

    public CookieUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findAny();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .path("/refresh") // No need to send it except for /refresh endpoint
                .maxAge(appProperties.getAuth().getJwt().getRefreshTokenDuration() / 1000)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(appProperties.getAuth().getJwt().getAccessTokenDuration() / 1000)
                .sameSite("Strict")
                .build();
    }
}
