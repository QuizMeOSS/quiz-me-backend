package com.quizme.utils;

import com.quizme.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieUtilTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppProperties appProperties;
    private CookieUtil cookieUtil;

    @BeforeEach
    public void setupClass() {
        cookieUtil = new CookieUtil(appProperties);
    }

    @Test
    void refreshTokenAttributes() {
        when(appProperties.getAuth().getJwt().getRefreshTokenDuration()).thenReturn(10_000L);

        var cookie = cookieUtil.createRefreshTokenCookie("someToken");

        assertEquals("refresh_token", cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("/refresh", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofSeconds(10), cookie.getMaxAge());
    }

    @Test
    void accessTokenAttributes() {
        when(appProperties.getAuth().getJwt().getAccessTokenDuration()).thenReturn(100_000L);

        var cookie = cookieUtil.createAccessTokenCookie("someToken");

        assertEquals("access_token", cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("/", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofSeconds(100), cookie.getMaxAge());
    }

    @Test
    void getCookieValue_returnsEmpty_whenNoCookies() {
        var mockRequest = mock(HttpServletRequest.class);

        // mockRequest cookies not mocked, so it will return null cookies array
        var cookieValue = cookieUtil.getCookieValue(mockRequest, "anything");

        assertTrue(cookieValue.isEmpty());
    }

    @Test
    void getCookieValue_returnsEmpty_whenCookieNotFound() {
        var mockRequest = mock(HttpServletRequest.class);
        // no cookie named 'name1'
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("name2", "val")});

        var cookieValue = cookieUtil.getCookieValue(mockRequest, "name1");

        assertTrue(cookieValue.isEmpty());
    }

    @Test
    void getCookieValue_returnsValue_whenCookieFound() {
        var mockRequest = mock(HttpServletRequest.class);
        // no cookie named 'name1'
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("name", "val")});

        var cookieValue = cookieUtil.getCookieValue(mockRequest, "name");

        assertTrue(cookieValue.isPresent());
        assertEquals("val", cookieValue.get());
    }
}