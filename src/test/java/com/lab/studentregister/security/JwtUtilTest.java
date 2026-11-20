package com.lab.studentregister.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tokens: what goes in comes back out, and a changed token is refused. */
class JwtUtilTest {

    private final JwtUtil jwt = new JwtUtil();

    @Test
    void aTokenCarriesTheUserAndRole() {
        Claims claims = jwt.parse(jwt.generateToken("asha@example.com", "STUDENT", "Asha", 7L));
        assertEquals("asha@example.com", claims.getSubject());
        assertEquals("STUDENT", claims.get("role", String.class));
    }

    @Test
    void aChangedTokenIsRefused() {
        String token = jwt.generateToken("asha@example.com", "STUDENT", "Asha", 7L);
        String changed = token.substring(0, token.length() - 2)
                + (token.endsWith("AA") ? "BB" : "AA");
        assertThrows(JwtException.class, () -> jwt.parse(changed));
    }
}
