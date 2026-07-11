package com.quizme.utils;

import com.quizme.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.jspecify.annotations.Nullable;
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

    @Nullable
    public String getUsername(String token) {
        var tokenClaims = getTokenClaims(token);
        if (tokenClaims == null) {
            return null;
        }
        return tokenClaims.getSubject();
    }

    public boolean isValid(String token){
        return !isExpired(token);
    }

    public boolean isExpired(String token){
        try{
            return getTokenClaims(token) == null;
        }catch (ExpiredJwtException e){
            return true;
        }
    }

    @Nullable
    private Claims getTokenClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException | MalformedJwtException ex) {
            return null;
        }
    }
}
