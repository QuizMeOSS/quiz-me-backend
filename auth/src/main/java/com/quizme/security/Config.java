package com.quizme.security;

import com.quizme.TokenToUserFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class Config {

    private final DelegatingOAuthSuccessHandler delegatingOAuthSuccessHandler;
    private final TokenToUserFilter tokenFilter;

    public Config(TokenToUserFilter tokenFilter,
                  DelegatingOAuthSuccessHandler delegatingOAuthSuccessHandler) {
        this.tokenFilter = tokenFilter;
        this.delegatingOAuthSuccessHandler = delegatingOAuthSuccessHandler;
    }

    @Bean
    SecurityFilterChain configure(HttpSecurity http) {
        http
                .addFilterBefore(tokenFilter, BasicAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable) // TODO: research how to deal with csrf
                .authorizeHttpRequests(c ->
                        c.requestMatchers("/api/register").permitAll()
                                .requestMatchers("/api/login").permitAll()
                                .requestMatchers("/api/oauth2/**").permitAll()
                                .requestMatchers("/api/refresh").permitAll()
                                .requestMatchers("/api/verify-email").permitAll()
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/actuator/health").permitAll()
                                .anyRequest().authenticated()
                )
                // invoked after oauth2 flow is successful and tokens obtained from provider
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(delegatingOAuthSuccessHandler))
                // by default, spring security asks user to login
                // in case of authentication error.
                // Instead, we just want 401 error, frontend handles the rest.
                // This is executed when unauthenticated access happens
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );
        return http.build();
    }
}
