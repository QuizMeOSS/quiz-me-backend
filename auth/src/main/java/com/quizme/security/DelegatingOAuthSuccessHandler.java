package com.quizme.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class DelegatingOAuthSuccessHandler implements AuthenticationSuccessHandler {
    private final List<OAuthSuccessHandler> handlers;

    public DelegatingOAuthSuccessHandler(
            List<OAuthSuccessHandler> handlers
    ) {
        this.handlers = handlers;
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Authentication authentication
    ) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();

        for (OAuthSuccessHandler handler : handlers) {
            if (handler.supports(registrationId)) {
                handler.onSuccess(request, response, token);
                return;
            }
        }
    }
}
