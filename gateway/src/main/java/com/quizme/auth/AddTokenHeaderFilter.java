package com.quizme.auth;

import com.quizme.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

@Component
public class AddTokenHeaderFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    private final CookieUtil cookieUtil;

    public AddTokenHeaderFilter(CookieUtil cookieUtil) {
        this.cookieUtil = cookieUtil;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        HttpServletRequest servletRequest = request.servletRequest();
        Optional<String> tokenOpt = cookieUtil.getCookieValue(servletRequest, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);

        ServerRequest mutatedRequest = tokenOpt.map(token ->
                ServerRequest.from(request)
                        .header("Auth-Token", token)
                        .build()
        ).orElse(request);

        return next.handle(mutatedRequest);
    }
}
