package com.quizme.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenFilterTest {
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenFilter tokenFilter;

    private final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    private final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    private final FilterChain mockChain = mock(FilterChain.class);

    @Test
    void testChainNextIsInvokedWhenAuthenticated() throws ServletException, IOException {
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "abc")});
        when(jwtUtil.getUsernameFromToken(any())).thenReturn("email");
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mock(UserDetails.class));

        tokenFilter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testChainNextIsInvokedWhenNoCookie() throws ServletException, IOException {
        // request cookies not mocked
        tokenFilter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testChainNextIsInvokedWhenUserDoesntExist() throws ServletException, IOException {
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "abc")});
        when(userDetailsService.loadUserByUsername(any())).thenThrow(UsernameNotFoundException.class);

        tokenFilter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testAccessTokenExtractedFromCookie() throws ServletException, IOException {
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "theToken"),
                new Cookie("some_other_cookie", "someValue")});
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mock(UserDetails.class));

        tokenFilter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(jwtUtil).getUsernameFromToken("theToken");
    }

    @Test
    void testUserLoadedByName() throws ServletException, IOException {
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "theToken")});
        when(jwtUtil.getUsernameFromToken("theToken")).thenReturn("theUser");
        when(userDetailsService.loadUserByUsername("theUser")).thenReturn(mock(UserDetails.class));

        tokenFilter.doFilterInternal(mockRequest, mockResponse, mockChain);

        verify(userDetailsService).loadUserByUsername("theUser");
    }

    /**
     * When user token is validated, the SecurityContext should be updated to set the request as authenticated.
     */
    @Test
    void testSecurityContextHoldsAuthenticatedUser() throws ServletException, IOException {
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", "theToken")});
        when(jwtUtil.getUsernameFromToken("theToken")).thenReturn("theUser");
        when(userDetailsService.loadUserByUsername("theUser")).thenReturn(
                User.builder()
                        .username("theUser").build()
        );

        tokenFilter.doFilterInternal(mockRequest, mockResponse, mockChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals("theUser", auth.getName());
    }
}