package com.quizme;

import com.quizme.utils.CookieUtil;
import com.quizme.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts requests and loads the user to SecurityContext based on the JWT token
 */
@Component
public class TokenToUserFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final CookieUtil cookieUtil;

    public TokenToUserFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
                             CookieUtil cookieUtil) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.cookieUtil = cookieUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        cookieUtil.getCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)
                .filter(jwtUtil::isValid)
                .ifPresent(token -> {
                    String username = jwtUtil.getUsername(token);
                    if (username == null) {
                        return;
                    }
                    UserDetails user = userDetailsService.loadUserByUsername(username);

                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });

        chain.doFilter(request, response);
    }
}