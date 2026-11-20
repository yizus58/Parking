package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.out.LoginLogRepositoryPort;
import com.nelumbo.park.domain.model.LoginLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoginAttemptTrackerTest {

    private final LoginLogRepositoryPort loginLogs = mock(LoginLogRepositoryPort.class);

    @Test
    void blocksAfterThreeFailures_andLogsOnce() {
        LoginAttemptTracker tracker = new LoginAttemptTracker(loginLogs, Fixtures.CLOCK);

        tracker.loginFailed("ip", "a@example.com");
        tracker.loginFailed("ip", "a@example.com");
        assertFalse(tracker.isBlocked("ip"));
        tracker.loginFailed("ip", "a@example.com");

        assertTrue(tracker.isBlocked("ip"));
        assertEquals(LocalDateTime.now(Fixtures.CLOCK).plusMinutes(5), tracker.getBlockedUntil("ip"));
        ArgumentCaptor<LoginLog> log = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogs).save(log.capture());
        assertEquals("Login Failed, IP Blocked: ip", log.getValue().description());
        assertEquals(5, tracker.getBlockDurationMinutes());
    }

    @Test
    void blockExpires() {
        LoginAttemptTracker tracker = new LoginAttemptTracker(loginLogs, Fixtures.CLOCK);
        for (int i = 0; i < 3; i++) {
            tracker.loginFailed("ip", "a@example.com");
        }
        LoginAttemptTracker later = new LoginAttemptTracker(loginLogs,
                Clock.offset(Fixtures.CLOCK, Duration.ofMinutes(6)));
        assertFalse(later.isBlocked("ip"));
        assertNull(later.getBlockedUntil("ip"));
    }

    @Test
    void successClearsAttempts() {
        LoginAttemptTracker tracker = new LoginAttemptTracker(loginLogs, Fixtures.CLOCK);
        tracker.loginFailed("ip", "a@example.com");
        tracker.loginFailed("ip", "a@example.com");
        tracker.loginSucceeded("ip");
        tracker.loginFailed("ip", "a@example.com");

        assertFalse(tracker.isBlocked("ip"));
        verify(loginLogs, never()).save(any());
    }
}
