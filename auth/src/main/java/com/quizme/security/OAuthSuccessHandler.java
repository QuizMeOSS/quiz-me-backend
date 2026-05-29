package com.quizme.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.io.IOException;

public interface OAuthSuccessHandler {
    boolean supports(String registrationId);

    void onSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            OAuth2AuthenticationToken authentication
    ) throws IOException;
}
