package com.quizme.config;

import com.quizme.security.Argon2Encoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain configure(HttpSecurity http) {
        http.httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable) // TODO: research how to deal with csrf
                .authorizeHttpRequests(c ->
                        c
                                .requestMatchers("/register").permitAll()
                                .requestMatchers("/login").permitAll()
                                .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(AppProperties appProperties) {
        return new Argon2Encoder(appProperties);
    }
}
