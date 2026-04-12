package com.example.userservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET = "flash-sale-secret-key-for-jwt-token-generation-must-be-at-least-256-bits";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // Access Token 寿命 15 分钟
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L;
    // Refresh Token 寿命 7 天
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    public String generateAccessToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("role", role);
        return createToken(claims, username, ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("role", role);
        return createToken(claims, username, REFRESH_TOKEN_EXPIRATION);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(KEY)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String getTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }
}
