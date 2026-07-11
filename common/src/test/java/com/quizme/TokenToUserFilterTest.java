package com.quizme;

import com.quizme.utils.CookieUtil;
import com.quizme.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenToUserFilterTest {
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CookieUtil cookieUtil;

    @InjectMocks
    private TokenToUserFilter filter;

    private final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    private final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    private final FilterChain mockChain = mock(FilterChain.class);

    @Test
    void testChainNextIsInvokedWhenAuthenticated() throws ServletException, IOException {
        when(cookieUtil.getCookieValue(mockRequest, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)).thenReturn(Optional.of(""));
        when(jwtUtil.getUsername(any())).thenReturn("email");
        when(jwtUtil.isValid(any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mock(UserDetails.class));

        filter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testChainNextIsInvokedWhenNoCookie() throws ServletException, IOException {
        // request cookies not mocked
        filter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testChainNextIsInvokedWhenInvalidCookie() throws ServletException, IOException {
        when(cookieUtil.getCookieValue(mockRequest, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)).thenReturn(Optional.of(""));
        when(jwtUtil.isValid(any())).thenReturn(false);

        filter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testAccessTokenExtractedFromCookie() throws ServletException, IOException {
        when(cookieUtil.getCookieValue(mockRequest, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)).thenReturn(Optional.of("theToken"));
        when(jwtUtil.isValid(any())).thenReturn(true);

        filter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(jwtUtil).getUsername("theToken");
    }

    @Test
    void testUserLoadedByName() throws ServletException, IOException {
        when(cookieUtil.getCookieValue(mockRequest, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)).thenReturn(Optional.of("theToken"));
        when(jwtUtil.getUsername("theToken")).thenReturn("theUser");
        when(jwtUtil.isValid(any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("theUser")).thenReturn(mock(UserDetails.class));

        filter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(userDetailsService).loadUserByUsername("theUser");
    }

    /**
     * When user token is validated, the SecurityContext should be updated to set the request as authenticated.
     */
    @Test
    void testSecurityContextHoldsAuthenticatedUser() throws ServletException, IOException {
        when(cookieUtil.getCookieValue(mockRequest, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)).thenReturn(Optional.of("theToken"));
        when(jwtUtil.getUsername("theToken")).thenReturn("theUser");
        when(jwtUtil.isValid(any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("theUser")).thenReturn(
                User.builder()
                        .username("theUser").build()
        );

        filter.doFilterInternal(mockRequest, mockResponse, mockChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals("theUser", auth.getName());
    }
}