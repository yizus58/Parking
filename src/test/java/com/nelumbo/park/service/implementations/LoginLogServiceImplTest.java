package com.nelumbo.park.service.implementations;

import com.nelumbo.park.entity.LoginLog;
import com.nelumbo.park.repository.LoginLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginLogServiceImplTest {

    @Mock
    private LoginLogRepository loginLogRepository;

    @InjectMocks
    private LoginLogServiceImpl loginLogService;

    private String username;
    private String email;
    private String description;
    private Date date;

    @BeforeEach
    void setUp() {
        username = "testuser";
        email = "test@example.com";
        description = "Login successful from IP 192.168.1.1";
        date = new Date();
    }

    @Test
    @DisplayName("Should save a login log successfully")
    void save_shouldSaveLoginLog() {
        LoginLog expectedLoginLog = new LoginLog();
        expectedLoginLog.setUsername(username);
        expectedLoginLog.setEmail(email);
        expectedLoginLog.setDescription(description);
        expectedLoginLog.setDate(date);

        loginLogService.save(username, email, description, date);

        ArgumentCaptor<LoginLog> loginLogCaptor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository, times(1)).save(loginLogCaptor.capture());

        LoginLog capturedLoginLog = loginLogCaptor.getValue();
        assertEquals(username, capturedLoginLog.getUsername());
        assertEquals(email, capturedLoginLog.getEmail());
        assertEquals(description, capturedLoginLog.getDescription());
        assertEquals(date, capturedLoginLog.getDate());
    }
}
