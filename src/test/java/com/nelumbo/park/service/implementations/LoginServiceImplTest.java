package com.nelumbo.park.service.implementations;

import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.interfaces.ILoginLogService;
import com.nelumbo.park.interfaces.LoginAttemptService;
import com.nelumbo.park.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private ILoginLogService loginLogService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private LoginServiceImpl loginService;

    private LoginRequest loginRequest;
    private String ipAddress;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("test@example.com", "password123");
        ipAddress = "127.0.0.1";
        when(httpServletRequest.getRemoteAddr()).thenReturn(ipAddress);
    }

    @Test
    @DisplayName("Should return TOO_MANY_REQUESTS when IP is blocked")
    void authenticate_whenIpIsBlocked_shouldReturnTooManyRequests() {
        LocalDateTime blockedUntil = LocalDateTime.now().plusMinutes(5);
        when(loginAttemptService.isBlocked(ipAddress)).thenReturn(true);
        when(loginAttemptService.getBlockedUntil(ipAddress)).thenReturn(blockedUntil);

        ResponseEntity<Object> response = loginService.authenticate(loginRequest, httpServletRequest);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Lo sentimos no puedes hacer login"));
        verify(loginAttemptService, times(1)).isBlocked(ipAddress);
        verify(loginAttemptService, times(1)).getBlockedUntil(ipAddress);
        verifyNoInteractions(userService, loginLogService);
    }

    @Test
    @DisplayName("Should return OK and UserLoginResponse on successful login")
    void authenticate_whenLoginIsSuccessful_shouldReturnOkAndUserLoginResponse() {
        UserLoginResponse userLoginResponse = new UserLoginResponse("1", "testuser", "test@example.com", "USER", "Bearer", "token");
        when(loginAttemptService.isBlocked(ipAddress)).thenReturn(false);
        when(userService.login(loginRequest)).thenReturn(userLoginResponse);

        ResponseEntity<Object> response = loginService.authenticate(loginRequest, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userLoginResponse, response.getBody());
        verify(loginAttemptService, times(1)).isBlocked(ipAddress);
        verify(userService, times(1)).login(loginRequest);
        verify(loginLogService, times(1)).save(eq(loginRequest.getEmail()), eq(loginRequest.getEmail()), anyString(), any(Date.class));
        verify(loginAttemptService, times(1)).loginSucceeded(ipAddress);
    }

    @Test
    @DisplayName("Should throw exception when login fails and IP is not blocked")
    void authenticate_whenLoginFailsAndIpIsNotBlocked_shouldThrowException() {
        when(loginAttemptService.isBlocked(ipAddress)).thenReturn(false, false);
        when(userService.login(loginRequest)).thenThrow(new RuntimeException("Invalid credentials"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                loginService.authenticate(loginRequest, httpServletRequest)
        );

        assertEquals("Invalid credentials", thrown.getMessage());
        verify(loginAttemptService, times(2)).isBlocked(ipAddress);
        verify(userService, times(1)).login(loginRequest);
        verify(loginAttemptService, times(1)).loginFailed(ipAddress, loginRequest.getEmail());
        verifyNoInteractions(loginLogService);
    }

    @Test
    @DisplayName("Should return TOO_MANY_REQUESTS when login fails and IP becomes blocked")
    void authenticate_whenLoginFailsAndIpBecomesBlocked_shouldReturnTooManyRequests() {
        when(loginAttemptService.isBlocked(ipAddress)).thenReturn(false, true);
        when(userService.login(loginRequest)).thenThrow(new RuntimeException("Invalid credentials"));
        when(loginAttemptService.getBlockDurationMinutes()).thenReturn(5L);

        ResponseEntity<Object> response = loginService.authenticate(loginRequest, httpServletRequest);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Lo sentimos no puedes hacer login, durante 5 min"));
        verify(loginAttemptService, times(2)).isBlocked(ipAddress);
        verify(userService, times(1)).login(loginRequest);
        verify(loginAttemptService, times(1)).loginFailed(ipAddress, loginRequest.getEmail());
        verify(loginAttemptService, times(1)).getBlockDurationMinutes();
        verifyNoInteractions(loginLogService);
    }
}
