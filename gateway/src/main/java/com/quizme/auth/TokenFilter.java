package com.quizme.auth;

import com.quizme.utils.CookieUtil;
import com.quizme.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Responsible for setting user as authenticated if request contains a valid access token.<br>
 * Otherwise, no error is thrown, because the accessed endpoint may not require users to be authenticated.
 * So we rely on the rest of the chain to decide whether an authentication error is raised or not.
 */
@Component
public class TokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    public TokenFilter(JwtUtil jwtUtil,
                       CookieUtil cookieUtil) {
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
    }

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain chain) throws IOException, ServletException {
        var accessTokenOpt = cookieUtil.getCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)
                .filter(jwtUtil::isValid);
        accessTokenOpt.ifPresent(_ -> setAsAuthenticated());
        chain.doFilter(request, response);
    }

    private void setAsAuthenticated() {
        var auth = new PreAuthenticatedAuthenticationToken("user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
