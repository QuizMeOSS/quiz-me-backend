package com.quizme.config;

import com.quizme.security.Argon2Encoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(AppProperties appProperties){
        return new Argon2Encoder(appProperties);
    }
}
