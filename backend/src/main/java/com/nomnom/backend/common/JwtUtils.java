package com.nomnom.backend.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String account, Integer role, String roleCode) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("account", account)
                .claim("role", role)
                .claim("roleCode", roleCode)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public Integer getRole(String token) {
        return parseToken(token).get("role", Integer.class);
    }

    public String getRoleCode(String token) {
        return parseToken(token).get("roleCode", String.class);
    }
}
