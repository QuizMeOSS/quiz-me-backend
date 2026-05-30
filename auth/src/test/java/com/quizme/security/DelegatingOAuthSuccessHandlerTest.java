package com.quizme.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelegatingOAuthSuccessHandlerTest {

    @Test
    void firstRelevantHandlerIsInvoked() throws IOException {
        final boolean[] invokedHandlers = {false, false};
        List<OAuthSuccessHandler> successHandlers = List.of(
                new OAuthSuccessHandler() {
                    @Override
                    public boolean supports(String registrationId) {
                        return registrationId.equals("someOtherProvider");
                    }

                    @Override
                    public void onSuccess(HttpServletRequest request, HttpServletResponse response, OAuth2AuthenticationToken authentication) throws IOException {
                        invokedHandlers[0] = true;
                    }
                },
                new OAuthSuccessHandler() {

                    @Override
                    public boolean supports(String registrationId) {
                        return registrationId.equals("aProvider");
                    }

                    @Override
                    public void onSuccess(HttpServletRequest request, HttpServletResponse response, OAuth2AuthenticationToken authentication) throws IOException {
                        invokedHandlers[1] = true;
                    }
                }
        );
        var delegatingSuccessHandler = new DelegatingOAuthSuccessHandler(successHandlers);

        // act
        delegatingSuccessHandler.onAuthenticationSuccess(null, null,
                new MockAuth("aProvider"));

        // assert
        // first handler shouldn't be invoked
        assertFalse(invokedHandlers[0]);
        assertTrue(invokedHandlers[1]);
    }

    static class MockAuth extends OAuth2AuthenticationToken {

        public MockAuth(String authorizedClientRegistrationId) {
            super(new OAuth2User() {
                @Override
                public Map<String, Object> getAttributes() {
                    return Map.of();
                }

                @Override
                public Collection<? extends GrantedAuthority> getAuthorities() {
                    return List.of();
                }

                @Override
                public String getName() {
                    return "";
                }
            }, List.of(), authorizedClientRegistrationId);
        }
    }
}