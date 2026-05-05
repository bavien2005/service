package org.anta.anta.config;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class JwtUtil {

    @ConfigProperty(name = "gateway.jwt.secret", defaultValue = "mysecretkeymysecretkeymysecretkey123")
    String configuredSecret;

    private String secret = "mysecretkeymysecretkeymysecretkey123";
    private SecretKey key;

    @PostConstruct
    void init() {
        setSecret(configuredSecret);
    }

    public void setSecret(String s) {
        this.secret = s;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims c = extractClaims(token);
            Date exp = c.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractRole(String token) {
        try {
            Object r = extractClaims(token).get("role");
            return r != null ? r.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> extractRolesAsList(String token) {
        Object roleClaim = extractClaims(token).get("role");

        if (roleClaim instanceof String) {
            return List.of((String) roleClaim);
        }

        if (roleClaim instanceof List<?>) {
            return ((List<?>) roleClaim)
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}