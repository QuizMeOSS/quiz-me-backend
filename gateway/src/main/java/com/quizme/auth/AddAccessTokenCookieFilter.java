package com.quizme.auth;

import com.quizme.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

@Component
public class AddAccessTokenCookieFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    private final CookieUtil cookieUtil;

    public AddAccessTokenCookieFilter(CookieUtil cookieUtil) {
        this.cookieUtil = cookieUtil;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        HttpServletRequest servletRequest = request.servletRequest();
        Optional<String> tokenOpt = cookieUtil.getCookieValue(servletRequest, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);

        ServerRequest mutatedRequest = tokenOpt.map(token -> {
            Cookie accessTokenCookie = new Cookie("access_token", token);
            return ServerRequest.from(request)
                    .cookies(cookies -> cookies.set("access_token", accessTokenCookie))
                    .build();
        }).orElse(request);

        return next.handle(mutatedRequest);
    }
}
