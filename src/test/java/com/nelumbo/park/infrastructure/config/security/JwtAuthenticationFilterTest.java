package com.nelumbo.park.infrastructure.config.security;

import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SIGNING_KEY = "unit-test-signing-key-0123456789abcdef";

    private final JwtService jwtService = new JwtService(SIGNING_KEY, 1);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtService, users, "application/json", "UTF-8");
    private final User partner = new User("u1", "socio", "hash", "s@example.com", "SOCIO");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse run(String authorization, MockFilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/parkings/");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private String token(String uid, String role, Date expiration) {
        return Jwts.builder().setClaims(Map.of("uid", uid, "role", role)).setSubject("socio")
                .setIssuedAt(new Date(expiration.getTime() - 3_600_000L)).setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(SIGNING_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void noHeader_passesThroughUnauthenticated() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        run(null, chain);
        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(users, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validToken_authenticatesWithTheRoleStoredInTheDatabase() throws Exception {
        when(users.findById("u1")).thenReturn(Optional.of(partner));
        String tokenClaimingAdmin = token("u1", "ADMIN", new Date(System.currentTimeMillis() + 60_000));

        MockFilterChain chain = new MockFilterChain();
        run("Bearer " + tokenClaimingAdmin, chain);

        assertNotNull(chain.getRequest());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("u1", auth.getPrincipal());
        assertEquals("SOCIO", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void expiredToken_is401TokenExpirado() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = run("Bearer " + token("u1", "SOCIO", new Date(System.currentTimeMillis() - 60_000)), chain);

        assertEquals(401, response.getStatus());
        assertEquals("{\"error\":\"Token expirado\"}", response.getContentAsString(StandardCharsets.UTF_8));
        assertNull(chain.getRequest());
    }

    @Test
    void garbageOrForeignToken_is401Invalid() throws Exception {
        for (String bad : new String[]{"not-a-jwt", "", token("u1", "SOCIO", new Date(System.currentTimeMillis() + 60_000)) + "x"}) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = run("Bearer " + bad, chain);

            assertEquals(401, response.getStatus(), bad);
            assertEquals("{\"error\":\"Token JWT inválido o firma no válida\"}", response.getContentAsString(StandardCharsets.UTF_8));
            assertNull(chain.getRequest());
        }
    }

    @Test
    void tokenOfADeletedUser_is403() throws Exception {
        when(users.findById("gone")).thenReturn(Optional.empty());
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = run("Bearer " + token("gone", "SOCIO", new Date(System.currentTimeMillis() + 60_000)), chain);

        assertEquals(403, response.getStatus());
        assertEquals("{\"error\":\"Usuario no encontrado\"}", response.getContentAsString(StandardCharsets.UTF_8));
        assertNull(chain.getRequest());
    }
}
