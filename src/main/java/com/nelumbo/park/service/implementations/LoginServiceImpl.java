package com.nelumbo.park.service.implementations;

import com.nelumbo.park.dto.request.LoginRequest;
import com.nelumbo.park.dto.response.UserLoginResponse;
import com.nelumbo.park.interfaces.ILoginLogService;
import com.nelumbo.park.interfaces.LoginAttemptService;
import com.nelumbo.park.interfaces.LoginService;
import com.nelumbo.park.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserService userService;
    private final ILoginLogService loginLogService;
    private final LoginAttemptService loginAttemptService;

    @Override
    public ResponseEntity<?> authenticate(LoginRequest request, HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getRemoteAddr();
        Date date = new Date();

        if (loginAttemptService.isBlocked(ip)) {
            LocalDateTime blockedTimestamp = loginAttemptService.getBlockedUntil(ip);
            long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), blockedTimestamp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Lo sentimos no puedes hacer login, puedes intentarlo de nuevo en " + (minutesLeft + 1) + " min");
        }

        try {
            UserLoginResponse tokenResponse = userService.login(request);
            loginLogService.save(request.getEmail(), request.getEmail(), "Loggin Success: " + ip, date);
            loginAttemptService.loginSucceeded(ip);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            loginAttemptService.loginFailed(ip, request.getEmail());
            if (loginAttemptService.isBlocked(ip)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Lo sentimos no puedes hacer login, durante " + loginAttemptService.getBlockDurationMinutes() + " min");
            }
            throw e;
        }
    }
}
