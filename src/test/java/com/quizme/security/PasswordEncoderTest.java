package com.quizme.security;

import com.quizme.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void before(){
        AppProperties props = new AppProperties();
        props.getAuth().setPepper("pep");
        passwordEncoder = new Argon2Encoder(props);
    }

    @Test
    public void testHashMatchesPepperedPassword() {
        String hash = passwordEncoder.encode("secret");

        assertTrue(passwordEncoder.matches("secretpep", hash));
    }

    @Test
    public void testHashesAreDifferentForSameInputDueToSalt() {
        String hash1 = passwordEncoder.encode("secret");
        String hash2 = passwordEncoder.encode("secret");

        // The Argon2 encoder should produce different hashes because of random salt
        assertNotEquals(hash1, hash2);
    }
}
