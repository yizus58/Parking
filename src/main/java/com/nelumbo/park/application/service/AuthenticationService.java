package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.LoginUseCase;
import com.nelumbo.park.application.port.out.AccessTokenPort;
import com.nelumbo.park.application.port.out.LoginLogRepositoryPort;
import com.nelumbo.park.application.port.out.PasswordHasherPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.exception.EmailNotFoundException;
import com.nelumbo.park.domain.exception.InvalidPasswordException;
import com.nelumbo.park.domain.exception.LoginBlockedException;
import com.nelumbo.park.domain.model.LoginLog;
import com.nelumbo.park.domain.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class AuthenticationService implements LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final LoginLogRepositoryPort loginLogRepository;
    private final LoginAttemptTracker attemptTracker;
    private final Clock clock;

    public AuthenticationService(UserRepositoryPort userRepository,
                                 PasswordHasherPort passwordHasher,
                                 AccessTokenPort accessTokenPort,
                                 LoginLogRepositoryPort loginLogRepository,
                                 LoginAttemptTracker attemptTracker,
                                 Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.loginLogRepository = loginLogRepository;
        this.attemptTracker = attemptTracker;
        this.clock = clock;
    }

    @Override
    public AuthenticatedUser login(String email, String password, String clientIp) {
        if (attemptTracker.isBlocked(clientIp)) {
            LocalDateTime until = attemptTracker.getBlockedUntil(clientIp);
            long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(clock), until);
            throw new LoginBlockedException(
                    "Lo sentimos no puedes hacer login, puedes intentarlo de nuevo en " + (minutesLeft + 1) + " min");
        }
        try {
            User user = checkCredentials(email, password);
            AuthenticatedUser result = new AuthenticatedUser(user, accessTokenPort.issueToken(user));
            loginLogRepository.save(LoginLog.of(email, email, "Loggin Success: " + clientIp, clock.instant()));
            attemptTracker.loginSucceeded(clientIp);
            return result;
        } catch (RuntimeException e) {
            attemptTracker.loginFailed(clientIp, email);
            if (attemptTracker.isBlocked(clientIp)) {
                throw new LoginBlockedException(
                        "Lo sentimos no puedes hacer login, durante " + attemptTracker.getBlockDurationMinutes() + " min");
            }
            throw e;
        }
    }

    private User checkCredentials(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(EmailNotFoundException::new);
        if (!passwordHasher.matches(password, user.getPassword())) {
            log.warn("Contraseña incorrecta para el usuario: {}", email);
            throw new InvalidPasswordException();
        }
        return user;
    }
}
