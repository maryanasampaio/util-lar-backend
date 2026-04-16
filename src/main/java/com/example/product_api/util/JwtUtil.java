package com.example.product_api.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilitário para geração e validação de tokens JWT
 * Tokens permanentes (sem expiração)
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private Long accessTokenExpiration; // 15 minutos

    @Value("${app.jwt.refresh-expiration-ms}")
    private Long refreshTokenExpiration; // 7 dias

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Gera Access Token (curta duração)
    public String generateAccessToken(String username, String permission) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("permission", permission);
        return createToken(claims, username, null);
    }

    // Gera Refresh Token (longa duração)
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, username, null);
    }

    // Cria o token JWT
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        var builder = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .signWith(getSigningKey());

        if (expiration != null && expiration > 0) {
            Date expiryDate = new Date(now.getTime() + expiration);
            builder.expiration(expiryDate);
        }

        return builder.compact();
    }

    // Extrai username do token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extrai data de expiração
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extrai tipo do token (access ou refresh)
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public String extractPermission(String token) {
        return extractClaim(token, claims -> claims.get("permission", String.class));
    }

    // Extrai claim específico
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extrai todos os claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Verifica se o token expirou
    private Boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        if (expiration == null) {
            return false;
        }
        return expiration.before(new Date());
    }

    // Valida o token
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // Valida se é um refresh token
    public Boolean isRefreshToken(String token) {
        String type = extractTokenType(token);
        return "refresh".equals(type);
    }

    // Valida se é um access token
    public Boolean isAccessToken(String token) {
        String type = extractTokenType(token);
        if (type == null || type.isBlank()) {
            return true;
        }
        return "access".equals(type);
    }
}
