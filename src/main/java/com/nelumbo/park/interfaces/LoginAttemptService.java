package com.nelumbo.park.interfaces;

import java.time.LocalDateTime;

public interface LoginAttemptService {
    boolean isBlocked(String ip);
    void loginSucceeded(String ip);
    void loginFailed(String ip, String email);
    LocalDateTime getBlockedUntil(String ip);
    long getBlockDurationMinutes();
}
