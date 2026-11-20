package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.LoginUseCase.AuthenticatedUser;
import com.nelumbo.park.application.port.out.AccessTokenPort;
import com.nelumbo.park.application.port.out.LoginLogRepositoryPort;
import com.nelumbo.park.application.port.out.PasswordHasherPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.exception.EmailNotFoundException;
import com.nelumbo.park.domain.exception.InvalidPasswordException;
import com.nelumbo.park.domain.exception.LoginBlockedException;
import com.nelumbo.park.domain.model.LoginLog;
import com.nelumbo.park.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    private static final String IP = "10.0.0.1";

    private UserRepositoryPort users;
    private PasswordHasherPort hasher;
    private AccessTokenPort tokens;
    private LoginLogRepositoryPort loginLogs;
    private LoginAttemptTracker tracker;
    private AuthenticationService service;
    private final User user = Fixtures.partner("p1");

    @BeforeEach
    void setUp() {
        users = mock(UserRepositoryPort.class);
        hasher = mock(PasswordHasherPort.class);
        tokens = mock(AccessTokenPort.class);
        loginLogs = mock(LoginLogRepositoryPort.class);
        tracker = new LoginAttemptTracker(loginLogs, Fixtures.CLOCK);
        service = new AuthenticationService(users, hasher, tokens, loginLogs, tracker, Fixtures.CLOCK);
    }

    @Test
    void login_success_returnsTokenAndLogs() {
        when(users.findByEmail("p1@example.com")).thenReturn(Optional.of(user));
        when(hasher.matches("secret1", "hash")).thenReturn(true);
        when(tokens.issueToken(user)).thenReturn("token");

        AuthenticatedUser result = service.login("p1@example.com", "secret1", IP);

        assertEquals("token", result.accessToken());
        assertSame(user, result.user());
        ArgumentCaptor<LoginLog> log = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogs).save(log.capture());
        assertEquals("Loggin Success: " + IP, log.getValue().description());
        assertEquals(Fixtures.NOW, log.getValue().date());
    }

    @Test
    void login_unknownEmail() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        assertThrows(EmailNotFoundException.class, () -> service.login("x@example.com", "secret1", IP));
        verify(loginLogs, never()).save(any());
    }

    @Test
    void login_wrongPassword() {
        when(users.findByEmail(any())).thenReturn(Optional.of(user));
        when(hasher.matches(any(), any())).thenReturn(false);
        assertThrows(InvalidPasswordException.class, () -> service.login("p1@example.com", "bad", IP));
    }

    @Test
    void login_thirdFailureBlocksIp_thenRejectsWithMinutesLeft() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(EmailNotFoundException.class, () -> service.login("x@example.com", "p", IP));
        assertThrows(EmailNotFoundException.class, () -> service.login("x@example.com", "p", IP));
        LoginBlockedException third = assertThrows(LoginBlockedException.class,
                () -> service.login("x@example.com", "p", IP));
        assertEquals("Lo sentimos no puedes hacer login, durante 5 min", third.getMessage());

        LoginBlockedException next = assertThrows(LoginBlockedException.class,
                () -> service.login("x@example.com", "p", IP));
        assertEquals("Lo sentimos no puedes hacer login, puedes intentarlo de nuevo en 6 min", next.getMessage());
        verify(users, times(3)).findByEmail(any());
    }
}
