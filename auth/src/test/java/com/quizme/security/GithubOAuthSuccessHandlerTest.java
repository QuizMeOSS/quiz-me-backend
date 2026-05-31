package com.quizme.security;

import com.quizme.AuthProperties;
import com.quizme.AuthService;
import com.quizme.dto.SsoLoginDto;
import com.quizme.dto.TokensDto;
import com.quizme.utils.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GithubOAuthSuccessHandlerTest {

    static String PROVIDER = "github";

    private MockRestServiceServer mockServer;

    OAuth2AuthorizedClientService authorizedClientService = mock(OAuth2AuthorizedClientService.class);
    AuthService authService = mock(AuthService.class);
    CookieUtil cookieUtil = mock(CookieUtil.class);
    AuthProperties authProperties = mock(AuthProperties.class);

    GithubOAuthSuccessHandler successHandler;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        successHandler = new GithubOAuthSuccessHandler(
                restClient, authorizedClientService, authService, cookieUtil, authProperties
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"GITHUB", "GitHub", "git_hub", "github"})
    void onlySupportsGithubProviderWithLowercase(String provider) {
        boolean supported = successHandler.supports(provider);
        if ("github".equals(provider)) {
            assertTrue(supported);
        } else {
            assertFalse(supported);
        }
    }

    @Test
    void whenNullProvider_returnFalse() {
        assertFalse(successHandler.supports(null));
    }

    @Test
    void whenEmailExists_authServiceIsInvokedToSignUserIn() throws IOException {
        var userEmail = "anEmail";
        var userId = 2;

        var mockPrincipal = mock(OAuth2User.class);
        when(mockPrincipal.getAttribute("email")).thenReturn(userEmail);
        when(mockPrincipal.getAttribute("id")).thenReturn(userId);

        var mockAuth = mock(OAuth2AuthenticationToken.class);
        when(mockAuth.getPrincipal()).thenReturn(mockPrincipal);
        when(mockAuth.getName()).thenReturn("Ahmed");

        when(authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, mockAuth.getName(),
                PROVIDER, Integer.toString(userId))))
                .thenReturn(new TokensDto("accessToken", "refreshToken"));

        when(cookieUtil.createRefreshTokenCookie("refreshToken")).thenReturn(mock(ResponseCookie.class));
        when(cookieUtil.createAccessTokenCookie("accessToken")).thenReturn(mock(ResponseCookie.class));

        // act
        successHandler.onSuccess(null, mock(HttpServletResponse.class), mockAuth);

        // assert
        verify(authService).ssoRegisterOrLogin(new SsoLoginDto(userEmail, mockAuth.getName(),
                PROVIDER, Integer.toString(userId))
        );
    }

    @Test
    void whenEmailDoesntExist_itIsRetrievedFromGithub() throws IOException {
        var userEmail = "anEmail";
        var userId = 2;

        var mockPrincipal = mock(OAuth2User.class);
        when(mockPrincipal.getAttribute("email")).thenReturn(null);
        when(mockPrincipal.getAttribute("id")).thenReturn(userId);

        var mockAuth = mock(OAuth2AuthenticationToken.class);
        when(mockAuth.getPrincipal()).thenReturn(mockPrincipal);
        when(mockAuth.getName()).thenReturn("Ahmed");

        var mockAuthClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClientService.loadAuthorizedClient(any(), any()))
                .thenReturn(mockAuthClient);
        when(mockAuthClient.getAccessToken()).thenReturn(mock(OAuth2AccessToken.class));

        mockServer.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.USER_AGENT, "quizmeoss"))
                .andRespond(withSuccess("[" +
                        "{\"email\":\"someOtherEmail\", \"primary\":false}," +
                        "{\"email\":\"" + userEmail + "\", \"primary\":true}" +
                        "]", MediaType.APPLICATION_JSON));

        when(authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, mockAuth.getName(),
                PROVIDER, Integer.toString(userId))))
                .thenReturn(new TokensDto("accessToken", "refreshToken"));

        when(cookieUtil.createRefreshTokenCookie("refreshToken")).thenReturn(mock(ResponseCookie.class));
        when(cookieUtil.createAccessTokenCookie("accessToken")).thenReturn(mock(ResponseCookie.class));

        // act
        successHandler.onSuccess(null, mock(HttpServletResponse.class), mockAuth);

        // assert
        // email obtained and sent to auth service
        verify(authService).ssoRegisterOrLogin(new SsoLoginDto(userEmail, mockAuth.getName(),
                PROVIDER, Integer.toString(userId)));
    }

    @Test
    void whenFailedToFetchEmail_exceptionThrown() throws IOException {
        var mockPrincipal = mock(OAuth2User.class);
        when(mockPrincipal.getAttribute("email")).thenReturn(null);
        when(mockPrincipal.getAttribute("id")).thenReturn(1);

        var mockAuth = mock(OAuth2AuthenticationToken.class);
        when(mockAuth.getPrincipal()).thenReturn(mockPrincipal);
        when(mockAuth.getName()).thenReturn("Ahmed");

        var mockAuthClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClientService.loadAuthorizedClient(any(), any()))
                .thenReturn(mockAuthClient);
        when(mockAuthClient.getAccessToken()).thenReturn(mock(OAuth2AccessToken.class));

        mockServer.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.USER_AGENT, "quizmeoss"))
                .andRespond(withResourceNotFound());

        when(cookieUtil.createRefreshTokenCookie("refreshToken")).thenReturn(mock(ResponseCookie.class));
        when(cookieUtil.createAccessTokenCookie("accessToken")).thenReturn(mock(ResponseCookie.class));

        // act & assert
        // user try catch instead of assertThrows
        // otherwise, code coverage tool and IntelliJ debugger won't notice
        // the code being executed
        try {
            successHandler.onSuccess(null, mock(HttpServletResponse.class), mockAuth);
            fail("Expected runtime exception because fetching user email failed");
        } catch (RuntimeException _) {
        }
    }

    @Test
    void tokensAreAddedInResponseCookies() throws IOException {
        var userEmail = "anEmail";
        var userId = 2;

        var mockPrincipal = mock(OAuth2User.class);
        when(mockPrincipal.getAttribute("email")).thenReturn(userEmail);
        when(mockPrincipal.getAttribute("id")).thenReturn(userId);

        var mockAuth = mock(OAuth2AuthenticationToken.class);
        when(mockAuth.getPrincipal()).thenReturn(mockPrincipal);
        when(mockAuth.getName()).thenReturn("Ahmed");

        when(authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, mockAuth.getName(),
                PROVIDER, Integer.toString(userId))))
                .thenReturn(new TokensDto("accessToken", "refreshToken"));

        var accessToken = mock(ResponseCookie.class);
        var refreshToken = mock(ResponseCookie.class);
        when(cookieUtil.createRefreshTokenCookie("refreshToken")).thenReturn(accessToken);
        when(cookieUtil.createAccessTokenCookie("accessToken")).thenReturn(refreshToken);

        var httpResponse = mock(HttpServletResponse.class);

        // act
        successHandler.onSuccess(null, httpResponse, mockAuth);

        // assert
        verify(httpResponse).addHeader(HttpHeaders.SET_COOKIE, accessToken.toString());
        verify(httpResponse).addHeader(HttpHeaders.SET_COOKIE, refreshToken.toString());
    }

    @Test
    void userRedirectedToFrontendHomePage() throws IOException {
        var userEmail = "anEmail";
        var userId = 2;

        var mockPrincipal = mock(OAuth2User.class);
        when(mockPrincipal.getAttribute("email")).thenReturn(userEmail);
        when(mockPrincipal.getAttribute("id")).thenReturn(userId);

        var mockAuth = mock(OAuth2AuthenticationToken.class);
        when(mockAuth.getPrincipal()).thenReturn(mockPrincipal);
        when(mockAuth.getName()).thenReturn("Ahmed");

        when(authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, mockAuth.getName(),
                PROVIDER, Integer.toString(userId))))
                .thenReturn(new TokensDto("accessToken", "refreshToken"));

        when(cookieUtil.createRefreshTokenCookie("refreshToken")).thenReturn(mock(ResponseCookie.class));
        when(cookieUtil.createAccessTokenCookie("accessToken")).thenReturn(mock(ResponseCookie.class));

        var httpResponse = mock(HttpServletResponse.class);

        // act
        successHandler.onSuccess(null, httpResponse, mockAuth);

        // assert
        verify(httpResponse).sendRedirect(authProperties.getFrontendUrl());
    }
}