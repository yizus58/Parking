package com.nelumbo.park.configuration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationHours;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-hours:6}") long expirationHours
    ) {
        System.out.println("=== CONFIGURANDO JWT SERVICE ===");
        System.out.println("Secret recibido: " + secret);
        System.out.println("Secret length: " + secret.length());
        System.out.println("Expiration hours: " + expirationHours);

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret debe tener al menos 32 caracteres");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;

        System.out.println("JWT Service configurado correctamente");
    }

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        System.out.println("=== GENERANDO TOKEN EN JWT SERVICE ===");
        System.out.println("Subject: " + subject);
        System.out.println("Claims: " + extraClaims);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationHours * 3_600_000L);

        System.out.println("Fecha de emisión: " + now);
        System.out.println("Fecha de expiración: " + expiry);

        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        System.out.println("Token JWT generado exitosamente");
        return token;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("uid", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear token JWT: " + e.getMessage(), e);
        }
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    public long getExpirationHours() {
        return expirationHours;
    }
}
