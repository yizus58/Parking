package com.nelumbo.park.service.implementations;

import com.nelumbo.park.interfaces.ILoginLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceImplTest {

    @Mock
    private ILoginLogService loginLogService;

    @InjectMocks
    private LoginAttemptServiceImpl loginAttemptService;

    private String ipAddress;
    private String email;

    private Map<String, Integer> getLoginAttemptsMap() throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = LoginAttemptServiceImpl.class.getDeclaredField("loginAttempts");
        field.setAccessible(true);
        return (Map<String, Integer>) field.get(loginAttemptService);
    }

    private Map<String, LocalDateTime> getBlockedIpsMap() throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = LoginAttemptServiceImpl.class.getDeclaredField("blockedIps");
        field.setAccessible(true);
        return (Map<String, LocalDateTime>) field.get(loginAttemptService);
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        ipAddress = "192.168.1.1";
        email = "test@example.com";

        getLoginAttemptsMap().clear();
        getBlockedIpsMap().clear();
    }

    @Test
    @DisplayName("Should return false when IP is not blocked")
    void isBlocked_whenIpIsNotBlocked_shouldReturnFalse() {
        assertFalse(loginAttemptService.isBlocked(ipAddress));
    }

    @Test
    @DisplayName("Should return true when IP is blocked and block duration has not expired")
    void isBlocked_whenIpIsBlockedAndNotExpired_shouldReturnTrue() throws NoSuchFieldException, IllegalAccessException {
        getBlockedIpsMap().put(ipAddress, LocalDateTime.now().plusMinutes(5));

        assertTrue(loginAttemptService.isBlocked(ipAddress));
    }

    @Test
    @DisplayName("Should return false and remove IP from blocked list when block duration has expired")
    void isBlocked_whenIpIsBlockedAndExpired_shouldReturnFalse() throws NoSuchFieldException, IllegalAccessException {
        getBlockedIpsMap().put(ipAddress, LocalDateTime.now().minusMinutes(1));

        assertFalse(loginAttemptService.isBlocked(ipAddress));
        assertFalse(getBlockedIpsMap().containsKey(ipAddress));
        assertFalse(getLoginAttemptsMap().containsKey(ipAddress));
    }

    @Test
    @DisplayName("Should clear login attempts and blocked status on successful login")
    void loginSucceeded_shouldClearAttemptsAndBlockedStatus() throws NoSuchFieldException, IllegalAccessException {
        getLoginAttemptsMap().put(ipAddress, 2);
        getBlockedIpsMap().put(ipAddress, LocalDateTime.now().plusMinutes(5));

        loginAttemptService.loginSucceeded(ipAddress);

        assertFalse(getLoginAttemptsMap().containsKey(ipAddress));
        assertFalse(getBlockedIpsMap().containsKey(ipAddress));
    }

    @Test
    @DisplayName("Should increment login attempts when login fails")
    void loginFailed_shouldIncrementLoginAttempts() throws NoSuchFieldException, IllegalAccessException {
        loginAttemptService.loginFailed(ipAddress, email);

        assertEquals(1, getLoginAttemptsMap().get(ipAddress));
        assertFalse(getBlockedIpsMap().containsKey(ipAddress));
        verifyNoInteractions(loginLogService);
    }

    @Test
    @DisplayName("Should block IP and log event when login attempts reach MAX_ATTEMPTS")
    void loginFailed_whenMaxAttemptsReached_shouldBlockIpAndLogEvent() throws NoSuchFieldException, IllegalAccessException {
        getLoginAttemptsMap().put(ipAddress, 2);

        loginAttemptService.loginFailed(ipAddress, email);

        assertEquals(3, getLoginAttemptsMap().get(ipAddress));
        assertTrue(getBlockedIpsMap().containsKey(ipAddress));
        verify(loginLogService, times(1)).save(eq(email), eq(email), anyString(), any(Date.class));
        assertTrue(getBlockedIpsMap().get(ipAddress).isAfter(LocalDateTime.now().plusMinutes(4).minusSeconds(1)));
        assertTrue(getBlockedIpsMap().get(ipAddress).isBefore(LocalDateTime.now().plusMinutes(5).plusSeconds(1)));
    }

    @Test
    @DisplayName("Should return blockedUntil timestamp when IP is blocked")
    void getBlockedUntil_whenIpIsBlocked_shouldReturnTimestamp() throws NoSuchFieldException, IllegalAccessException {
        LocalDateTime expectedBlockedUntil = LocalDateTime.now().plusMinutes(10);
        getBlockedIpsMap().put(ipAddress, expectedBlockedUntil);

        LocalDateTime actualBlockedUntil = loginAttemptService.getBlockedUntil(ipAddress);

        assertEquals(expectedBlockedUntil, actualBlockedUntil);
    }

    @Test
    @DisplayName("Should return null when getBlockedUntil is called for an unblocked IP")
    void getBlockedUntil_whenIpIsNotBlocked_shouldReturnNull() {
        assertNull(loginAttemptService.getBlockedUntil(ipAddress));
    }

    @Test
    @DisplayName("Should return the correct block duration minutes")
    void getBlockDurationMinutes_shouldReturnCorrectValue() {
        assertEquals(5, loginAttemptService.getBlockDurationMinutes());
    }
}
