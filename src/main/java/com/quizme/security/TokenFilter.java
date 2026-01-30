package com.quizme.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Responsible for setting user as authenticated if request contains a valid access token.<br>
 * Otherwise, no error is thrown, because the accessed endpoint may not require users to be authenticated.
 * So we rely on the rest of the chain to decide whether an authentication error is raised or not.
 */
@Component
public class TokenFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public TokenFilter(UserDetailsService userDetailsService,
                       JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain chain) throws IOException, ServletException {
        var accessTokenOpt = extractAccessToken(request);
        accessTokenOpt.ifPresent(s -> setUserAsAuthenticated(request, s));
        chain.doFilter(request, response);
    }

    private Optional<String> extractAccessToken(HttpServletRequest request) {
        if(request.getCookies() == null){
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findAny();
    }

    private void setUserAsAuthenticated(HttpServletRequest request, String accessToken) {
        try {
            var username = jwtUtil.getUsernameFromToken(accessToken);
            var user = userDetailsService.loadUserByUsername(username);
            setAsAuthenticated(request, user);
        } catch (UsernameNotFoundException e) {
            System.out.println("Error occurred: " + e);
        }
    }

    private void setAsAuthenticated(HttpServletRequest request, UserDetails user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
