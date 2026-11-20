package com.nelumbo.park.infrastructure.adapter.out.security;

import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.infrastructure.config.security.JwtService;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.infrastructure.exception.JwtUserNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecurityAdaptersTest {

    private final User user = new User("u1", "socio", "h", "socio@example.com", "SOCIO");

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUser_emptyWhenAnonymous() {
        SecurityContextCurrentUserAdapter adapter = new SecurityContextCurrentUserAdapter(mock(UserRepositoryPort.class));
        assertTrue(adapter.currentUser().isEmpty());
    }

    @Test
    void currentUser_loadsByIdFromAuthenticationName() {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(users.findById("u1")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "u1", null, List.of(new SimpleGrantedAuthority("SOCIO"))));

        assertSame(user, new SecurityContextCurrentUserAdapter(users).currentUser().orElseThrow());
    }

    @Test
    void currentUser_unknownIdThrows() {
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        when(users.findById("gone")).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "gone", null, List.of(new SimpleGrantedAuthority("SOCIO"))));

        assertThrows(JwtUserNotFoundException.class, () -> new SecurityContextCurrentUserAdapter(users).currentUser());
    }

    @Test
    void token_usesUsernameSubjectAndSameClaims() {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.generateToken(eq("socio"), eq(Map.of("uid", "u1", "email", "socio@example.com", "role", "SOCIO"))))
                .thenReturn("jwt");

        assertEquals("jwt", new JwtAccessTokenAdapter(jwtService).issueToken(user));
    }

    @Test
    void passwordHasher_roundTrip() {
        BCryptPasswordHasherAdapter hasher = new BCryptPasswordHasherAdapter(new BCryptPasswordEncoder());
        String hash = hasher.hash("secret1");
        assertTrue(hasher.matches("secret1", hash));
        assertFalse(hasher.matches("other", hash));
    }
}
