package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.out.LoginLogRepositoryPort;
import com.nelumbo.park.domain.model.LoginLog;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory failed login counter per IP: 3 failures block the IP for 5 minutes.
 * State is per instance and lost on restart, as before.
 */
@Component
public class LoginAttemptTracker {

    static final int MAX_ATTEMPTS = 3;
    static final int BLOCK_DURATION_MINUTES = 5;

    private final LoginLogRepositoryPort loginLogRepository;
    private final Clock clock;
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> blockedUntil = new ConcurrentHashMap<>();

    public LoginAttemptTracker(LoginLogRepositoryPort loginLogRepository, Clock clock) {
        this.loginLogRepository = loginLogRepository;
        this.clock = clock;
    }

    public boolean isBlocked(String ip) {
        LocalDateTime until = blockedUntil.get(ip);
        if (until == null) {
            return false;
        }
        if (LocalDateTime.now(clock).isBefore(until)) {
            return true;
        }
        blockedUntil.remove(ip);
        attempts.remove(ip);
        return false;
    }

    public void loginSucceeded(String ip) {
        attempts.remove(ip);
        blockedUntil.remove(ip);
    }

    public void loginFailed(String ip, String email) {
        int count = attempts.merge(ip, 1, Integer::sum);
        if (count >= MAX_ATTEMPTS) {
            blockedUntil.put(ip, LocalDateTime.now(clock).plusMinutes(BLOCK_DURATION_MINUTES));
            loginLogRepository.save(LoginLog.of(email, email, "Login Failed, IP Blocked: " + ip, clock.instant()));
        }
    }

    public LocalDateTime getBlockedUntil(String ip) {
        return blockedUntil.get(ip);
    }

    public long getBlockDurationMinutes() {
        return BLOCK_DURATION_MINUTES;
    }
}
