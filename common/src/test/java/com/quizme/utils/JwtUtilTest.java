package com.quizme.utils;

import com.quizme.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final String SECRET_KEY = "793d83ccec5e1b5df2635f318a036d1d4734d64d2238e1e8e4ad9344ac99e509";
    private static final long ACCESS_DURATION = 60_000;
    private static final long REFRESH_DURATION = 120_000;
    private static final String SUBJECT = "abc@mail.net";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppProperties appProperties;

    private JwtUtil jwtUtil;


    @BeforeEach
    public void before() {
        when(appProperties.getAuth().getJwt().getAccessTokenDuration()).thenReturn(ACCESS_DURATION);
        when(appProperties.getAuth().getJwt().getRefreshTokenDuration()).thenReturn(REFRESH_DURATION);
        when(appProperties.getAuth().getJwt().getSecret()).thenReturn(SECRET_KEY);

        jwtUtil = new JwtUtil(appProperties);
    }

    @Test
    void generatedAccessTokenIsSigned() {
        var key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        var accessToken = jwtUtil.generateAccessToken(SUBJECT);

        // should not throw exception
        Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(accessToken);
    }

    @Test
    void generatedAccessTokenIsSignedWithCorrectKey() {
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
    void generatedRefreshTokenIsSigned() {
        var key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        var token = jwtUtil.generateRefreshToken(SUBJECT);

        // should not throw exception
        Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    @Test
    void generatedRefreshTokenIsSignedWithCorrectKey() {
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
    void accessTokenHasCorrectDuration() {
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
    void refreshTokenHasCorrectDuration() {
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
    void GIVEN_validToken_WHEN_getTokenSubject_RETURN_subject() {
        var token = jwtUtil.generateAccessToken(SUBJECT);

        var username = jwtUtil.getTokenSubject(token);

        assertEquals(SUBJECT, username);
    }

    @Test
    void GIVEN_malformedToken_WHEN_getTokenSubject_RETURN_null() {
        var token = "abc";

        var username = jwtUtil.getTokenSubject(token);

        assertNull(username);
    }

    @Test
    void isExpired_returnsFalseIfTokenValid() {
        var token = jwtUtil.generateAccessToken(SUBJECT);

        assertFalse(jwtUtil.isExpired(token));
    }

    @Test
    void isExpired_returnsTrueIfTokenExpired() {
        // 1 millisecond only to expire quickly
        when(appProperties.getAuth().getJwt().getAccessTokenDuration()).thenReturn(1L);
        jwtUtil = new JwtUtil(appProperties);
        var token = jwtUtil.generateAccessToken(SUBJECT);

        assertTrue(jwtUtil.isExpired(token));
    }
}