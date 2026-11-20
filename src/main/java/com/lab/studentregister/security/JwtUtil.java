package com.lab.studentregister.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Creates and validates JWT tokens (HS256). */
@Component
public class JwtUtil {

    // Hardcoded-by-default for this lab; override with JWT_SECRET env var.
    private static final String DEFAULT_SECRET =
            "studentregister-lab-secret-change-me-0123456789";
    private static final long TTL_MILLIS = 8L * 60 * 60 * 1000; // 8 hours

    private final SecretKey key;

    public JwtUtil() {
        String secret = System.getenv().getOrDefault("JWT_SECRET", DEFAULT_SECRET);
        // HS256 needs >= 32 bytes; pad if someone configures a short secret
        while (secret.length() < 32) secret = secret + secret;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role,
                                String fullName, Long studentId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("name", fullName)
                .claim("studentId", studentId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TTL_MILLIS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Returns the claims, or throws JwtException if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
