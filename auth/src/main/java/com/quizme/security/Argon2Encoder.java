package com.quizme.security;

import com.quizme.AppProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class Argon2Encoder implements PasswordEncoder {

    private final AppProperties appProperties;
    private final Argon2PasswordEncoder arg2SpringSecurity =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public Argon2Encoder(AppProperties appProperties) {
        this.appProperties = appProperties;
    }


    /**
     * Hash a password using Argon2 algorithm.
     * A pepper is added to the password before hashing for additional security.
     *
     * @param password the password to hash
     * @return the hashed password
     */
    @Override
    @Nullable
    public String encode(@Nullable CharSequence password) {
        var pepperedPassword = password + appProperties.getAuth().getPepper();
        return Objects.requireNonNull(arg2SpringSecurity.encode(pepperedPassword));
    }

    @Override
    public boolean matches(@Nullable CharSequence rawPassword, @Nullable String encodedPassword) {
        var pepperedPassword = rawPassword + appProperties.getAuth().getPepper();
        return arg2SpringSecurity.matches(pepperedPassword, encodedPassword);
    }
}
