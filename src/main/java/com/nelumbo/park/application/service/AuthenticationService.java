package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.LoginUseCase;
import com.nelumbo.park.application.port.out.AccessTokenPort;
import com.nelumbo.park.application.port.out.LoginLogRepositoryPort;
import com.nelumbo.park.application.port.out.PasswordHasherPort;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.exception.InvalidCredentialsException;
import com.nelumbo.park.domain.exception.LoginBlockedException;
import com.nelumbo.park.domain.model.LoginLog;
import com.nelumbo.park.domain.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
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

    /**
     * Unknown email and wrong password fail with the same exception, and both run one BCrypt check
     * (against a dummy hash when there is no user), so neither the response nor its timing reveals
     * whether the email is registered.
     */
    private User checkCredentials(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        boolean matches = passwordHasher.matches(password, user.map(User::getPassword).orElse(null));
        if (user.isEmpty() || !matches) {
            log.warn("Login fallido para: {}", email);
            throw new InvalidCredentialsException();
        }
        return user.get();
    }
}
