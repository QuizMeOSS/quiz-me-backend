package com.quizme.security;

import com.quizme.dto.SsoLoginDto;
import com.quizme.services.AuthService;
import com.quizme.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class GithubOAuthSuccessHandler implements OAuthSuccessHandler {
    public static final String PROVIDER = "github";
    private final RestClient restClient;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    public GithubOAuthSuccessHandler(RestClient restClient,
                                     OAuth2AuthorizedClientService authorizedClientService,
                                     AuthService authService,
                                     CookieUtil cookieUtil) {
        this.restClient = restClient;
        this.authorizedClientService = authorizedClientService;
        this.authService = authService;
        this.cookieUtil = cookieUtil;
    }

    @Override
    public boolean supports(String registrationId) {
        return PROVIDER.equals(registrationId);
    }

    @Override
    public void onSuccess(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            OAuth2AuthenticationToken authentication
    ) throws IOException {

        OAuth2User oauthUser = authentication.getPrincipal();
        String userEmail = oauthUser.getAttribute("email");
        String providerUserId = Integer.toString(oauthUser.getAttribute("id"));
        // Github doesn't return email if user set it as private
        // so we need to get it from github API
        if (userEmail == null) {
            userEmail = getUserEmailAddress(authentication);
        }

        if (userEmail == null) {
            throw new RuntimeException("Couldn't get user email address.");
        }

        var result = authService.ssoRegisterOrLogin(
                new SsoLoginDto(userEmail, authentication.getName(),
                        PROVIDER, providerUserId)
        );
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(result.success().refreshToken());
        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(result.success().accessToken());
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.sendRedirect("https://localhost:3000/");
    }

    @Nullable
    private String getUserEmailAddress(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        PROVIDER,
                        authentication.getName()
                );

        String accessToken = client.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        List<Map<String, Object>> emails =
                restClient.get()
                        .uri("https://api.github.com/user/emails")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header(HttpHeaders.USER_AGENT, "quizmeoss")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });

        // get primary email
        return emails.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("primary")))
                .map(e -> (String) e.get("email"))
                .findFirst()
                .orElse(null);
    }
}
