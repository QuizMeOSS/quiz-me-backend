package com.quizme.security;

import com.quizme.config.AppProperties;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final long accessTokenDurationMillis;
    private final long refreshTokenDurationMillis;
    private final SecretKey key;

    public JwtUtil(AppProperties appProperties){
        this.accessTokenDurationMillis = appProperties.getAuth().getJwt().getAccessTokenDuration();
        this.refreshTokenDurationMillis = appProperties.getAuth().getJwt().getRefreshTokenDuration();
        this.key = Keys.hmacShaKeyFor(appProperties.getAuth().getJwt().getSecret()
                .getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject) {
        var date = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(date)
                .expiration(new Date(date.getTime() + accessTokenDurationMillis))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        var date = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(date)
                .expiration(new Date(date.getTime() + refreshTokenDurationMillis))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getTokenClaims(token)
                .getSubject();
    }

    private Claims getTokenClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
