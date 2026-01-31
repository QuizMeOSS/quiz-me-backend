package com.quizme.security;

import com.quizme.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final long accessTokenDurationMillis;
    private final long refreshTokenDurationMillis;
    private final SecretKey key;

    public JwtUtil(AppProperties appProperties) {
        this.accessTokenDurationMillis = appProperties.getAuth().getJwt().getAccessTokenDuration();
        this.refreshTokenDurationMillis = appProperties.getAuth().getJwt().getRefreshTokenDuration();
        this.key = Keys.hmacShaKeyFor(appProperties.getAuth().getJwt().getSecret()
                .getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject) {
        return generateToken(subject, accessTokenDurationMillis);
    }

    public String generateRefreshToken(String subject) {
        return generateToken(subject, refreshTokenDurationMillis);
    }

    private String generateToken(String subject, long accessTokenDurationMillis) {
        var date = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(date)
                .expiration(new Date(date.getTime() + accessTokenDurationMillis))
                .signWith(key)
                .compact();
    }

    public String getUsername(String token) {
        return getTokenClaims(token)
                .getSubject();
    }

    public boolean isExpired(String token){
        try{
            getTokenClaims(token);
            return false;
        }catch (ExpiredJwtException e){
            return true;
        }
    }

    private Claims getTokenClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
