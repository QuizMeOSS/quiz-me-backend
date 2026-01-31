package com.quizme.security;

import com.quizme.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String SECRET_KEY = "793d83ccec5e1b5df2635f318a036d1d4734d64d2238e1e8e4ad9344ac99e509";
    private static final long ACCESS_DURATION = 60_000;
    private static final long REFRESH_DURATION = 120_000;
    private static final String SUBJECT = "abc@mail.net";

    private JwtUtil jwtUtil;

    @BeforeEach
    public void before() {
        AppProperties props = new AppProperties();
        props.getAuth().setJwt(new AppProperties.Jwt());
        props.getAuth().getJwt().setAccessTokenDuration(ACCESS_DURATION);
        props.getAuth().getJwt().setRefreshTokenDuration(REFRESH_DURATION);
        props.getAuth().getJwt().setSecret(SECRET_KEY);

        jwtUtil = new JwtUtil(props);
    }

    @Test
    void testGeneratedAccessTokenIsSigned() {
        var key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        var accessToken = jwtUtil.generateAccessToken(SUBJECT);

        // should not throw exception
        Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(accessToken);
    }

    @Test
    void testGeneratedAccessTokenIsSignedWithCorrectKey() {
        // incorrect key secret -> verification should throw an exception
        // because the token was signed with different key
        var key = Keys.hmacShaKeyFor("ddddddddec5e1b5df2635f318a036d1d4734d64d2238e1e8e4ad9344ac99dddd".getBytes(StandardCharsets.UTF_8));

        var accessToken = jwtUtil.generateAccessToken(SUBJECT);

        assertThrows(SignatureException.class, () -> {
            Jwts.parser().verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken);
        });
    }

    @Test
    void testGeneratedRefreshTokenIsSigned() {
        var key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        var token = jwtUtil.generateRefreshToken(SUBJECT);

        // should not throw exception
        Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    @Test
    void testGeneratedRefreshTokenIsSignedWithCorrectKey() {
        // incorrect key secret -> verification should throw an exception
        // because the token was signed with different key
        var key = Keys.hmacShaKeyFor("ddddddddec5e1b5df2635f318a036d1d4734d64d2238e1e8e4ad9344ac99dddd".getBytes(StandardCharsets.UTF_8));

        var token = jwtUtil.generateRefreshToken(SUBJECT);

        assertThrows(SignatureException.class, () -> {
            Jwts.parser().verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
        });
    }

    @Test
    void testAccessTokenHasCorrectDuration() {
        var key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        var token = jwtUtil.generateAccessToken(SUBJECT);

        var claims = Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Date issuedAt = claims.getIssuedAt();
        assertEquals(new Date(issuedAt.getTime() + ACCESS_DURATION), claims.getExpiration());
    }

    @Test
    void testRefreshTokenHasCorrectDuration() {
        var key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        var token = jwtUtil.generateRefreshToken(SUBJECT);

        var claims = Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Date issuedAt = claims.getIssuedAt();
        assertEquals(new Date(issuedAt.getTime() + REFRESH_DURATION), claims.getExpiration());
    }

    @Test
    void testGetUserName_ReturnsSubject() {
        var token = jwtUtil.generateAccessToken(SUBJECT);

        var username = jwtUtil.getUsername(token);

        assertEquals(SUBJECT, username);
    }
}