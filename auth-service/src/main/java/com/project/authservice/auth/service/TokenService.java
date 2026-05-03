package com.project.authservice.auth.service;


import com.project.authservice.auth.config.JwtProperties;
import com.project.authservice.auth.exception.InvalidTokenException;
import com.project.authservice.auth.model.UserCredential;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_KEY_PREFIX   = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";


    public String generateAccessToken(UserCredential user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role",  user.getRole().name())
                .claim("jti",   UUID.randomUUID().toString())   // unique per token → enables blacklisting
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.getAccessTokenExpiryMs())))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.get("jti", String.class);
            if (jti != null && isBlacklisted(jti)) {
                throw new InvalidTokenException("Token has been revoked");
            }
            return claims;

        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Access token has expired");
        } catch (JwtException ex) {
            throw new InvalidTokenException("Invalid access token: " + ex.getMessage());
        }
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.get("jti", String.class);
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (jti != null && remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_KEY_PREFIX + jti,
                        "1",
                        Duration.ofMillis(remainingMs)
                );
            }
        } catch (JwtException ignored) {
            // Already invalid — no need to blacklist
        }
    }

    private boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }
    public String generateRefreshToken(UserCredential user) {
        String tokenId = UUID.randomUUID().toString();
        String redisKey = buildRefreshKey(user.getId().toString(), tokenId);
        String value = user.getEmail() + "|" + user.getRole().name();

        redisTemplate.opsForValue().set(
                redisKey,
                value,
                Duration.ofSeconds(jwtProperties.getRefreshTokenExpirySec())
        );
        return user.getId().toString() + ":" + tokenId;
    }

    public String[] validateRefreshToken(String refreshToken) {
        String[] parts = refreshToken.split(":", 2);
        if (parts.length != 2) {
            throw new InvalidTokenException("Malformed refresh token");
        }
        String userId  = parts[0];
        String tokenId = parts[1];
        String redisKey = buildRefreshKey(userId, tokenId);

        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }
        return value.split("\\|", 2);   // [email, role]
    }

    public void revokeRefreshToken(String refreshToken) {
        String[] parts = refreshToken.split(":", 2);
        if (parts.length == 2) {
            redisTemplate.delete(buildRefreshKey(parts[0], parts[1]));
        }
    }

    public void revokeAllRefreshTokens(String userId) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String buildRefreshKey(String userId, String tokenId) {
        return REFRESH_KEY_PREFIX + userId + ":" + tokenId;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
