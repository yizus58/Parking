package com.nelumbo.park.service.implementations;

import com.nelumbo.park.interfaces.ILoginLogService;
import com.nelumbo.park.interfaces.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final ILoginLogService loginLogService;
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> blockedIps = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final int BLOCK_DURATION_MINUTES = 5;

    @Override
    public boolean isBlocked(String ip) {
        if (blockedIps.containsKey(ip)) {
            LocalDateTime blockedTimestamp = blockedIps.get(ip);
            if (LocalDateTime.now().isBefore(blockedTimestamp)) {
                return true;
            } else {
                blockedIps.remove(ip);
                loginAttempts.remove(ip);
                return false;
            }
        }
        return false;
    }

    @Override
    public void loginSucceeded(String ip) {
        loginAttempts.remove(ip);
        blockedIps.remove(ip);
    }

    @Override
    public void loginFailed(String ip, String email) {
        loginAttempts.put(ip, loginAttempts.getOrDefault(ip, 0) + 1);
        if (loginAttempts.get(ip) >= MAX_ATTEMPTS) {
            blockedIps.put(ip, LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES));
            loginLogService.save(email, email, "Login Failed, IP Blocked: " + ip, new Date());
        }
    }

    @Override
    public LocalDateTime getBlockedUntil(String ip) {
        return blockedIps.get(ip);
    }

    @Override
    public long getBlockDurationMinutes() {
        return BLOCK_DURATION_MINUTES;
    }
}
