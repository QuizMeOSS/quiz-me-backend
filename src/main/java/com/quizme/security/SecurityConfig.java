package com.quizme.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final TokenFilter tokenFilter;
    private final DelegatingOAuthSuccessHandler delegatingOAuthSuccessHandler;

    public SecurityConfig(TokenFilter tokenFilter,
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
                        c.requestMatchers("/register").permitAll()
                                .requestMatchers("/login/**").permitAll()
                                .requestMatchers("/oauth2/**").permitAll()
                                .requestMatchers("/refresh").permitAll()
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
